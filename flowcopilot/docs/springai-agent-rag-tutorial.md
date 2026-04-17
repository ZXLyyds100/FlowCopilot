# JChatMind 教学文档：Agent（Spring AI）与 RAG 实现详解

## 1. 先给结论

1. Agent 部分：是基于 Spring AI 搭建的。
2. RAG 部分：不是使用 Spring AI 的 VectorStore 方案，而是项目自定义实现（本地 embedding 服务 + PostgreSQL pgvector 检索），再通过工具调用接入 Agent。

也就是说，这个项目是“Spring AI 做 Agent 编排 + 自定义 RAG 检索底座”的混合架构。

---

## 2. Agent 为什么说是基于 Spring AI

从依赖和代码两层可以确认。

## 2.1 依赖层证据

`pom.xml` 引入了 Spring AI BOM 和模型 starter：

1. `spring-ai-bom`
2. `spring-ai-starter-model-deepseek`
3. `spring-ai-starter-model-zhipuai`

这说明模型接入和核心 AI 抽象来自 Spring AI。

## 2.2 代码层证据

`JChatMind` 使用了 Spring AI 关键能力：

1. `ChatClient`：统一模型调用入口
2. `MessageWindowChatMemory`：消息记忆窗口
3. `Prompt` / `ChatOptions`：提示与参数控制
4. `ToolCallingManager`：工具调用执行器
5. `ToolCallback`：工具注册和调用适配

因此 Agent 的对话、记忆、工具调用调度都在 Spring AI 体系中运行。

---

## 3. Agent 运行机制（教学版）

## 3.1 关键角色

1. `ChatMessageController`：接收用户消息 API。
2. `ChatMessageFacadeServiceImpl`：写入消息并发布事件。
3. `ChatEventListener`：异步监听事件并启动 Agent。
4. `JChatMindFactory`：根据 Agent 配置构建运行时实例。
5. `JChatMind`：执行 Agent Loop（think/execute）。
6. `SseService`：实时推送 AI 和工具结果给前端。

## 3.2 一次对话的完整链路

1. 前端发送 `POST /api/chat-messages`。
2. 后端保存用户消息（role=user）。
3. 发布 `ChatEvent(agentId, sessionId, userInput)`。
4. 事件监听器异步创建 `JChatMind` 并执行 `run()`。
5. `JChatMind` 进入循环：
   - think：调用模型，得到 assistant 输出和可能的 tool calls
   - execute：如有工具调用，则执行工具并把结果回写记忆
6. 每一步新增消息都会持久化到聊天消息表。
7. 同时通过 SSE 推给前端，用户看到流式结果。

## 3.3 Agent Loop 的核心思想

1. 不是“一问一答”一次结束。
2. 而是“思考 -> （可选）调用工具 -> 再思考 -> … -> 结束”的多步决策。
3. 结束条件：
   - 模型不再返回工具调用
   - 或工具里调用 `terminate`
   - 或达到最大步数（MAX_STEPS）

---

## 4. “添加 Agent”到底在做什么

添加 Agent = 新增一份“运行配置模板”，并非新增 Java 类。

保存的数据包括：

1. 模型：`deepseek-chat` 或 `glm-4.6`
2. 系统提示词：决定角色和行为约束
3. allowedTools：允许调用哪些可选工具
4. allowedKbs：允许访问哪些知识库
5. chatOptions：温度、topP、历史窗口长度

运行时由 `JChatMindFactory` 读取这些配置，动态组装出具体 Agent 实例。

---

## 5. RAG 在这个项目里的实现方式

## 5.1 RAG 总体设计

这个项目采用“文档离线入库 + 在线向量检索”的模式：

1. 离线：上传文档后切分并向量化，写入向量表。
2. 在线：用户提问时，通过 `KnowledgeTool` 调 `RagService.similaritySearch` 检索相关片段。

## 5.2 文档入库阶段

入口：`POST /api/documents/upload`

处理步骤：

1. 保存文档记录（拿 documentId）
2. 保存物理文件
3. 若是 Markdown：
   - 用 `MarkdownParserService` 解析章节
   - 每个章节生成 chunk
   - 调 embedding 接口生成向量
   - 写入 `chunk_bge_m3`

## 5.3 检索阶段

`RagServiceImpl` 的核心逻辑：

1. 用同一 embedding 模型把 query 转向量
2. 转成 pgvector 字面量 `[x1,x2,...]`
3. SQL：`ORDER BY embedding <-> queryVector LIMIT 3`
4. 返回 TopK chunk 内容给工具调用结果

说明：

1. embedding 来源是本地 `http://localhost:11434/api/embeddings`
2. 模型固定为 `bge-m3`
3. 向量检索依赖 PostgreSQL + pgvector

## 5.4 RAG 与 Agent 的关系

这个项目的 RAG 是“工具化 RAG”：

1. Agent 自主决定要不要查知识库。
2. 一旦调用 `KnowledgeTool`，就进入 RAG 检索链路。
3. 检索结果作为工具响应进入后续推理上下文。

这比“每次都强制检索”的流水线更灵活。

---

## 6. 工具调用机制（和 RAG 的连接点）

## 6.1 工具分类

1. FIXED：所有 Agent 必有（如 `terminate`、`KnowledgeTool`）。
2. OPTIONAL：可配置开启（如数据库查询、发邮件）。

## 6.2 运行时如何选工具

1. 固定工具默认加入。
2. 再按 Agent 的 `allowedTools` 追加可选工具。
3. 构造成 `ToolCallback[]` 交给 Spring AI。

## 6.3 执行细节

1. 模型返回 tool calls。
2. `ToolCallingManager.executeToolCalls(...)` 执行。
3. 工具响应写入会话记忆和数据库。
4. SSE 实时推送给前端。

---

## 7. 你要学哪些内容（重点学习清单）

下面按“必须掌握 -> 进阶”划分。

## 7.1 Agent 部分学习清单

必须掌握：

1. Spring AI 基础抽象
   - `ChatClient`
   - `Prompt`
   - `ChatResponse`
2. 消息体系与记忆
   - `Message` 类型（user/assistant/tool/system）
   - `MessageWindowChatMemory` 窗口策略
3. 工具调用
   - `@Tool` 注解
   - `ToolCallback`
   - `ToolCallingManager`
4. Agent Loop 设计
   - think/execute 状态机
   - 最大步数、终止条件、异常处理
5. 异步事件驱动
   - `ApplicationEventPublisher`
   - `@EventListener` + `@Async`
6. SSE 推送
   - `SseEmitter` 生命周期
   - 会话级连接管理

进阶建议：

1. 多 Agent 协作（Supervisor/Worker）
2. 结构化输出与 JSON Schema 约束
3. 观测性（token、时延、工具失败率）

## 7.2 RAG 部分学习清单

必须掌握：

1. 文档处理
   - Markdown/PDF 解析
   - 分块策略（按标题、按长度、重叠窗口）
2. Embedding
   - 向量维度一致性
   - 查询和入库必须同模型同维度
3. 向量数据库
   - pgvector 建模
   - 相似度算子 `<->`
   - TopK 与过滤条件
4. 召回质量
   - 命中率、噪声、重复召回
   - chunk 粒度对效果影响
5. 工具化接入
   - 把检索能力做成工具，交由 Agent 决策使用

进阶建议：

1. 重排（Rerank）
2. 混合检索（关键词 + 向量）
3. 查询改写（Query Rewrite）
4. 结果压缩与引用溯源

---

## 8. 推荐学习路径（按周）

第 1 周（认知搭建）：

1. 通读 Agent 主链路：Controller -> Event -> Factory -> JChatMind
2. 跑通一次完整会话 + SSE
3. 理解每类消息何时落库

第 2 周（Agent 深入）：

1. 新增一个可选工具（例如天气查询 mock）
2. 在添加 Agent 页面勾选并验证工具生效
3. 加入失败重试/超时保护

第 3 周（RAG 深入）：

1. 跑通文档上传 -> 切分 -> 向量化 -> 检索
2. 调整 chunk 规则并评估回答质量
3. 观察不同 TopK 对结果影响

第 4 周（工程化）：

1. 增加链路日志与指标
2. 增加关键测试（至少集成测试）
3. 输出一份你自己的“问题定位手册”

---

## 9. 实操检查单（你可以直接照着做）

1. 创建一个新 Agent，限制为只允许 `KnowledgeTool`。
2. 新建知识库并上传一份 markdown 文档。
3. 建立 SSE 连接后发送问题。
4. 确认消息顺序：user -> assistant/tool -> assistant。
5. 查看数据库是否写入了 chunk 向量和聊天消息。
6. 调整 `messageLength`、`temperature`，观察行为变化。

---

## 10. 关键认知总结

1. Agent 是“可配置的运行时编排器”，核心在流程与边界，而不在单次生成。
2. RAG 是“外部知识检索能力”，核心在数据质量、分块和召回。
3. 工具调用把大模型从“只会说”变成“会行动”。
4. 这个项目最值得学的是：把 Spring AI 的标准能力与自定义 RAG 工程化拼接起来。
