# JChatMind LangChain4j 接口与使用说明

## 1. 适用范围

本文对应当前主线代码，也就是已经迁移到 LangChain4j 的 `flowcopilot` 后端。

如果你想先了解为什么迁移、Spring AI 和 LangChain4j 的差异、配置项如何替换，请先阅读：

- [LangChain4j 迁移说明](./langchain4j-migration-guide.md)

旧文档仍然保留在 `docs/` 目录中，它们主要用于理解历史实现，不作为当前主链路说明。

## 2. 运行前置条件

### 2.1 必需项

- Java 21
- PostgreSQL
- 可写的 `document.storage.base-path`

### 2.2 模型相关

当前支持的模型标识保持不变：

- `deepseek-chat`
- `glm-4.6`

对应配置前缀已经改为：

```yaml
flowcopilot.llm.providers.*
```

示例：

```yaml
flowcopilot:
  llm:
    providers:
      deepseek:
        enabled: true
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com
        model: deepseek-chat
        timeout: 60s
        log-requests: false
        log-responses: false
      zhipu:
        enabled: false
        api-key:
        base-url: https://open.bigmodel.cn/api/paas/v4
        model: glm-4.6
        timeout: 60s
        log-requests: false
        log-responses: false
```

### 2.3 无 key 启动行为

当前版本支持“无 key 也能启动”：

- `enabled=false` 时，不注册该模型
- `enabled=true` 且 `api-key` 为空时，记录 warning 并跳过注册
- 如果某个 Agent 运行时选中了未注册模型，会在运行时抛出明确异常

这意味着你可以先联调 CRUD、SSE、数据库和消息流，再补模型配置。

## 3. 对外接口不变的部分

这次迁移只替换后端底层 AI 集成层，对外接口保持不变：

- REST API 路径不变
- 数据库 schema 不变
- SSE 建连方式不变
- Agent / Session / Message / KnowledgeBase / Document 的基本语义不变

## 4. 统一响应格式

`/api/**` 接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 5. ChatMessage metadata 新结构

本次迁移里最关键的接口层变化，是 `ChatMessage.metadata` 改成了框架无关结构。

### 5.1 Assistant 消息中的 toolCalls

```json
{
  "toolCalls": [
    {
      "id": "call-1",
      "name": "KnowledgeTool",
      "arguments": "{\"query\":\"LangChain4j\"}"
    }
  ]
}
```

### 5.2 Tool 消息中的 toolResponse

```json
{
  "toolResponse": {
    "id": "call-1",
    "name": "KnowledgeTool",
    "responseData": "knowledge result"
  }
}
```

### 5.3 历史兼容

后端仍兼容旧字段：

- `toolResponse.toolName`
- `toolResponse.text`

因此旧数据库记录不需要改表。

## 6. SSE 说明

### 6.1 建连

- Method: `GET`
- Path: `/sse/connect/{chatSessionId}`
- Content-Type: `text/event-stream`

### 6.2 事件类型

- 初始事件：`init`
- 初始数据：`connected`
- 业务事件：`message`

示例：

```json
{
  "type": "AI_GENERATED_CONTENT",
  "payload": {
    "message": {
      "id": "msg_1",
      "sessionId": "sess_1",
      "role": "assistant",
      "content": "",
      "metadata": {
        "toolCalls": [
          {
            "id": "call-1",
            "name": "KnowledgeTool",
            "arguments": "{\"query\":\"LangChain4j\"}"
          }
        ]
      }
    }
  },
  "metadata": {
    "chatMessageId": "msg_1"
  }
}
```

## 7. 常用接口

### 7.1 Agent

查询：

- `GET /api/agents`

创建：

- `POST /api/agents`

示例：

```json
{
  "name": "知识库助手",
  "description": "基于知识库回答问题",
  "systemPrompt": "你是一个知识库问答助手。",
  "model": "deepseek-chat",
  "allowedTools": ["databaseQuery", "sendEmail"],
  "allowedKbs": ["kb_1"],
  "chatOptions": {
    "temperature": 0.7,
    "topP": 0.9,
    "messageLength": 10
  }
}
```

### 7.2 ChatSession

创建：

- `POST /api/chat-sessions`

示例：

```json
{
  "agentId": "agent_1",
  "title": "LangChain4j 联调"
}
```

### 7.3 ChatMessage

发送用户消息：

- `POST /api/chat-messages`

示例：

```json
{
  "agentId": "agent_1",
  "sessionId": "session_1",
  "role": "user",
  "content": "请帮我总结这个知识库",
  "metadata": null
}
```

工具调用后的三段式消息通常表现为：

1. assistant 消息，`metadata.toolCalls` 不为空
2. tool 消息，`metadata.toolResponse` 不为空
3. assistant 消息，输出最终答案

### 7.4 KnowledgeBase

- `GET /api/knowledge-bases`
- `POST /api/knowledge-bases`
- `PATCH /api/knowledge-bases/{knowledgeBaseId}`
- `DELETE /api/knowledge-bases/{knowledgeBaseId}`

### 7.5 Document

上传：

- `POST /api/documents/upload`
- Content-Type: `multipart/form-data`

表单字段：

- `kbId`
- `file`

### 7.6 Tool

查询可选工具：

- `GET /api/tools`

### 7.7 Health

- `GET /health`

## 8. 本地联调建议

### 8.1 第一步：先不配模型 key

先验证：

- 应用能启动
- `/health` 正常
- CRUD 正常
- SSE 能连上

### 8.2 第二步：再配一个模型

推荐顺序：

1. 配置 DeepSeek 或智谱其中一个 provider
2. 重启应用
3. 创建对应模型的 Agent
4. 建立 SSE 连接
5. 发一条用户消息
6. 观察 assistant / tool / assistant 消息链路

### 8.3 第三步：联调知识库

1. 创建 KnowledgeBase
2. 上传 Markdown 文档
3. 创建允许访问该知识库的 Agent
4. 发一个需要检索上下文的问题
5. 观察 `KnowledgeTool` 是否被调用

## 9. 常见问题

### 9.1 没有任何模型 key，哪些功能还能用

以下能力仍然可用：

- Agent CRUD
- ChatSession CRUD
- ChatMessage CRUD
- SSE 建连
- 文档与知识库管理

### 9.2 `temperature` 和 `topP` 现在会生效吗

会。当前实现会把 `Agent.chatOptions.temperature` 与 `Agent.chatOptions.topP` 直接写入 LangChain4j `ChatRequest`。

### 9.3 RAG 是否也迁移到 LangChain4j 了

没有。RAG 仍然沿用现有 embedding + pgvector 实现，这次迁移主要影响 Agent Runtime、工具链路和模型接入层。
