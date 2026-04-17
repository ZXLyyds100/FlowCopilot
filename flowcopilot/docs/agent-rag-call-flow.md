# Agent 与 RAG 调用流程说明

本文档专门说明 JChatMind 在“用户发一条消息”之后，后端是如何串起 Agent、工具调用、RAG 检索和 SSE 推送的。

## 1. 总览

核心流程可以概括为：

1. 前端提交用户消息
2. 后端先把用户消息写库
3. 发布聊天事件
4. 异步创建运行时 Agent
5. Agent 进入 Think-Execute 循环
6. 如模型决定调用知识库工具，则进入 RAG 检索
7. 检索结果作为工具返回写回上下文
8. Agent 继续推理，直到结束
9. 每一步生成的结果通过 SSE 增量推送给前端

## 2. 前端入口

关键文件：

- `ui/src/components/views/AgentChatView.tsx`

关键动作：

- 当用户点击发送消息时，前端调用 `createChatMessage(...)`
- 如果当前还没有会话，会先创建会话，再发送第一条消息
- 页面同时建立 SSE 连接：`/sse/connect/{chatSessionId}`

这意味着前端其实做了两件事：

- 发 HTTP 请求提交消息
- 打开 SSE 连接等待后台增量结果

## 3. 消息入库与事件触发

关键文件：

- `controller/ChatMessageController.java`
- `service/impl/ChatMessageFacadeServiceImpl.java`
- `event/ChatEvent.java`

调用链：

1. `ChatMessageController.createChatMessage(...)`
2. `ChatMessageFacadeServiceImpl.createChatMessage(...)`
3. `doCreateChatMessage(...)` 把用户消息写入 `chat_message`
4. 写完后发布 `ChatEvent(agentId, sessionId, content)`

设计意义：

- 用户消息先落库，保证对话历史完整
- Agent 的执行通过事件异步触发，不阻塞接口线程

## 4. 聊天事件异步消费

关键文件：

- `config/AsyncConfig.java`
- `event/listener/ChatEventListener.java`

调用链：

1. `ChatEventListener.handle(...)`
2. 方法上带 `@Async`
3. 使用线程池后台执行 Agent，不占用当前请求线程

设计意义：

- 用户发消息后，HTTP 接口能尽快返回
- 真正耗时的模型推理和工具调用放到后台线程执行

## 5. Agent 运行时组装

关键文件：

- `agent/JChatMindFactory.java`
- `config/ChatClientRegistry.java`
- `config/MultiChatClientConfig.java`
- `converter/AgentConverter.java`
- `service/impl/ToolFacadeServiceImpl.java`

`JChatMindFactory.create(agentId, chatSessionId)` 做了这些事：

1. 从 `agent` 表加载 Agent 配置
2. 用 `AgentConverter` 把数据库中的 JSON 字段还原成 `AgentDTO`
3. 按 `chatSessionId` 读取最近消息，恢复记忆窗口
4. 按 `allowedKbs` 解析运行时可访问知识库
5. 按 `allowedTools` 解析运行时可调用工具
6. 根据 `agent.model` 去注册表里取对应的 `ChatClient`
7. 组装成一个新的 `JChatMind`

这里的关键点是：

- 模型不是写死在代码里，而是由数据库中 `agent.model` 决定
- 工具不是全部开放，而是“固定工具 + 当前 Agent 允许的可选工具”

## 6. Agent 核心循环

关键文件：

- `agent/JChatMind.java`

### 6.1 初始化

Agent 初始化时会拿到：

- 基本信息：`agentId`、`name`、`description`
- 系统提示词：`systemPrompt`
- 模型客户端：`ChatClient`
- 历史记忆：`List<Message>`
- 工具回调：`List<ToolCallback>`
- 知识库列表：`List<KnowledgeBaseDTO>`
- 会话 ID：`chatSessionId`

此外还会：

- 创建 `MessageWindowChatMemory`
- 把历史消息放入记忆窗口
- 如果存在系统提示词，则追加到记忆中
- 关闭 Spring AI 自带的自动工具执行，改成手动控制

### 6.2 `run()`

`run()` 是总入口。

它会在最大步数限制内循环调用 `step()`：

- 默认最多 20 步
- 任一步出错则 Agent 状态变为 `ERROR`
- 正常结束时状态变为 `FINISHED`

### 6.3 `step()`

一步流程只有两个阶段：

1. `think()`
2. 如果模型给出工具调用，再 `execute()`

如果 `think()` 没有返回工具调用，说明本轮已经能直接给出答案，于是结束。

## 7. Think 阶段

关键方法：

- `JChatMind.think()`

做的事情：

1. 动态构造一段“决策提示词”
2. 把当前记忆和可用知识库描述发给模型
3. 把工具回调也传给模型，让模型知道自己能调哪些工具
4. 得到 `ChatResponse`
5. 提取出 `AssistantMessage`
6. 判断这条 Assistant 消息是否包含工具调用

额外动作：

- 会把 AssistantMessage 持久化到 `chat_message`
- 会通过 SSE 把新生成的内容推给前端

所以前端看到的 AI 输出，并不是最后一次性回来的，而是 Agent 每轮思考后的结果被持续推送。

## 8. Execute 阶段

关键方法：

- `JChatMind.execute()`

做的事情：

1. 从上一次 `ChatResponse` 里读取工具调用
2. 调用 `ToolCallingManager.executeToolCalls(...)`
3. 得到 `ToolExecutionResult`
4. 用工具执行后的 `conversationHistory` 刷新聊天记忆
5. 把最后一个 `ToolResponseMessage` 持久化到数据库
6. 再通过 SSE 推给前端

如果工具返回中包含 `terminate`，则直接结束 Agent 任务。

## 9. 知识库工具如何触发 RAG

关键文件：

- `agent/tools/KnowledgeTools.java`
- `service/impl/RagServiceImpl.java`

`KnowledgeTools` 是固定工具，每个 Agent 默认都有。

当模型决定调用 `KnowledgeTool(kbsId, query)` 时：

1. 进入 `KnowledgeTools.knowledgeQuery(...)`
2. 调用 `ragService.similaritySearch(kbsId, query)`
3. 拿到相似片段列表
4. 用换行拼成字符串返回给模型

于是对模型来说，RAG 不是“系统强制每次都先查”，而是一个可被自主调用的工具能力。

## 10. RAG 检索流程

关键文件：

- `service/impl/RagServiceImpl.java`
- `mapper/ChunkBgeM3Mapper.java`
- `resources/mapper/ChunkBgeM3Mapper.xml`

### 10.1 文本向量化

`RagServiceImpl.doEmbed(text)` 会：

1. 调本地 `http://localhost:11434/api/embeddings`
2. 请求体里指定模型 `bge-m3`
3. 返回 `float[] embedding`

### 10.2 向量检索

`similaritySearch(kbId, title)` 会：

1. 先把 query 文本转向量
2. 再转成 PostgreSQL vector literal，例如 `[0.1,0.2,...]`
3. 调 `ChunkBgeM3Mapper.similaritySearch(...)`
4. SQL 中按 `embedding <-> queryVector` 排序
5. 取 Top 3 内容片段

这说明项目里的“向量数据库”其实就是：

- PostgreSQL
- `pgvector` 扩展
- 表 `chunk_bge_m3`

## 11. 文档如何进入 RAG

关键文件：

- `controller/DocumentController.java`
- `service/impl/DocumentFacadeServiceImpl.java`
- `service/impl/DocumentStorageServiceImpl.java`
- `service/impl/MarkdownParserServiceImpl.java`

### 11.1 上传入口

接口：

- `POST /api/documents/upload`

### 11.2 业务步骤

`DocumentFacadeServiceImpl.uploadDocument(...)` 会：

1. 创建 `document` 表记录
2. 保存物理文件到本地目录
3. 把文件路径回写到文档 metadata
4. 如果文件是 Markdown，则调用 `processMarkdownDocument(...)`

### 11.3 Markdown 解析

`processMarkdownDocument(...)` 会：

1. 读取已保存的 Markdown 文件
2. 调 `MarkdownParserServiceImpl.parseMarkdown(...)`
3. 提取一组章节：`title + content`
4. 对每个 `title` 调 embedding
5. 把正文 `content` 和向量写入 `chunk_bge_m3`

所以这里的切块策略是：

- 用标题表达该 chunk 的语义
- 用内容作为返回给模型的知识正文

## 12. SSE 如何把结果推给前端

关键文件：

- `controller/SseController.java`
- `service/impl/SseServiceImpl.java`
- `message/SseMessage.java`

流程：

1. 前端打开 `/sse/connect/{chatSessionId}`
2. `SseServiceImpl.connect(...)` 创建 `SseEmitter`
3. 后端把这个 emitter 缓存在 `clients` map 中
4. Agent 每生成内容后调用 `sseService.send(...)`
5. 前端收到 `AI_GENERATED_CONTENT`、`AI_PLANNING`、`AI_THINKING`、`AI_EXECUTING`、`AI_DONE`

因此前端不仅能看到最终答案，还能看到 Agent 的执行过程。

## 13. 关键设计点总结

### 13.1 Agent 是事件驱动的

不是 HTTP 请求线程里直接跑模型，而是：

- 用户消息入库
- 发布事件
- 后台线程消费事件
- Agent 异步执行

### 13.2 RAG 是工具化的

不是固定流水线，而是模型按需调用 `KnowledgeTool`。

### 13.3 工具执行是手动接管的

项目关闭了 Spring AI 默认自动工具执行，转而自己控制：

- 思考阶段只让模型决定“调什么”
- 执行阶段才真的去跑工具

这样更容易做：

- 状态管理
- 日志追踪
- 数据库存档
- SSE 实时推送

### 13.4 消息、工具、推理是统一落库的

这带来的好处是：

- 可以恢复记忆窗口
- 可以回放执行过程
- 可以做前端历史展示

## 14. 关键源码入口建议

如果你想带着“功能是怎么跑起来的”去看源码，建议按下面的顺序：

1. `controller/ChatMessageController.java`
2. `service/impl/ChatMessageFacadeServiceImpl.java`
3. `event/listener/ChatEventListener.java`
4. `agent/JChatMindFactory.java`
5. `agent/JChatMind.java`
6. `agent/tools/KnowledgeTools.java`
7. `service/impl/RagServiceImpl.java`
8. `service/impl/DocumentFacadeServiceImpl.java`
9. `service/impl/MarkdownParserServiceImpl.java`
10. `service/impl/SseServiceImpl.java`

## 15. 一句话理解这个项目

这个项目本质上是在做一套“可配置的 Agent 执行平台”：

- 会话层负责保存上下文
- Agent 层负责循环推理和决策
- 工具层负责把模型从“会说”变成“会做”
- RAG 层负责补充外部知识
- SSE 层负责把整个执行过程实时展示给前端
