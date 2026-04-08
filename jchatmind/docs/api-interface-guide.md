# JChatMind 接口与功能说明文档

## 1. 项目功能总览

JChatMind 后端当前实现了以下核心能力：

1. Agent 管理：创建、查询、更新、删除 Agent，并配置模型、可用工具、可访问知识库和聊天参数。
2. 会话管理：创建与维护聊天会话，支持按 Agent 过滤会话。
3. 消息管理：查询消息、写入用户消息、修改和删除消息。
4. Agent 异步运行：用户发送消息后，系统通过事件机制异步唤起 Agent 执行推理与工具调用。
5. SSE 实时推送：Agent 生成的消息通过 SSE 推送到前端会话流。
6. 知识库管理：知识库 CRUD。
7. 文档管理：文档 CRUD、文档上传、Markdown 自动切分并写入向量检索表。
8. 工具中心：向前端暴露可选工具列表。
9. 健康检查：基础连通性检查接口。

---

## 2. 统一响应格式与错误处理

### 2.1 API 统一响应结构（`/api/**`）

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

字段说明：

- `code`: 200 表示成功，500 表示业务或系统异常。
- `message`: 成功为 `success`；异常时由全局异常处理返回错误信息。
- `data`: 业务数据，删除/更新等接口通常为 `null`。

### 2.2 错误处理规则

- `BizException`：返回 `ApiResponse.error(e.getMessage())`，即 `code=500`，message 为可读业务错误。
- 未处理异常：返回 `ApiResponse.error("服务器内部错误")`。
- 404：由 `NoResourceFoundException` 返回 HTTP 404。

### 2.3 SSE 响应格式（`/sse/**`）

- Content-Type: `text/event-stream`
- 连接成功先发 `init` 事件，数据为 `connected`
- 业务消息事件名为 `message`，data 是 JSON 序列化后的 `SseMessage`

---

## 3. 关键业务链路

### 3.1 用户发送消息后的处理流程

1. 调用 `POST /api/chat-messages` 写入用户消息。
2. 后端发布 `ChatEvent(agentId, sessionId, content)`。
3. `ChatEventListener` 异步监听事件，创建 `JChatMind` 实例并执行 `run()`。
4. Agent 在循环中进行“思考 ->（可选）工具调用 -> 持久化消息”。
5. 新增的 AI/工具消息通过 `SseService.send(sessionId, SseMessage)` 推送给前端。

### 3.2 文档上传后的处理流程

1. 调用 `POST /api/documents/upload` 上传文件。
2. 先创建文档记录，后保存文件路径到 metadata。
3. 若为 Markdown 文件（`md/markdown`），自动解析章节并生成 chunks。
4. 对章节标题做 embedding，写入 `chunk_bge_m3` 相关存储，用于后续 RAG 检索。

---

## 4. 接口清单（共 28 个）

## 4.1 Agent 管理

### 4.1.1 查询 Agent 列表
- Method: `GET`
- Path: `/api/agents`
- 请求参数: 无
- 响应: `ApiResponse<GetAgentsResponse>`
- data 结构:
```json
{
  "agents": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "systemPrompt": "string",
      "model": "deepseek-chat",
      "allowedTools": ["string"],
      "allowedKbs": ["string"],
      "chatOptions": {
        "temperature": 0.7,
        "topP": 1.0,
        "messageLength": 10
      }
    }
  ]
}
```
- 说明: 查询全部 Agent，数据库实体经过 converter 转换为 VO 返回。

### 4.1.2 创建 Agent
- Method: `POST`
- Path: `/api/agents`
- 请求体: `CreateAgentRequest`
```json
{
  "name": "string",
  "description": "string",
  "systemPrompt": "string",
  "model": "deepseek-chat",
  "allowedTools": ["string"],
  "allowedKbs": ["string"],
  "chatOptions": {
    "temperature": 0.7,
    "topP": 1.0,
    "messageLength": 10
  }
}
```
- 响应: `ApiResponse<CreateAgentResponse>`，`data.agentId`
- 说明: 插入 Agent 记录并设置 `createdAt/updatedAt`。

### 4.1.3 删除 Agent
- Method: `DELETE`
- Path: `/api/agents/{agentId}`
- 路径参数: `agentId`
- 响应: `ApiResponse<Void>`
- 说明: 先校验 Agent 存在，再删除。

### 4.1.4 更新 Agent
- Method: `PATCH`
- Path: `/api/agents/{agentId}`
- 路径参数: `agentId`
- 请求体: `UpdateAgentRequest`（字段与创建一致，按更新逻辑覆盖）
- 响应: `ApiResponse<Void>`
- 说明: 保留原 ID/创建时间，更新其余字段及 `updatedAt`。

---

## 4.2 ChatSession 会话管理

### 4.2.1 查询全部会话
- Method: `GET`
- Path: `/api/chat-sessions`
- 响应: `ApiResponse<GetChatSessionsResponse>`
- data 结构:
```json
{
  "chatSessions": [
    {
      "id": "string",
      "agentId": "string",
      "title": "string"
    }
  ]
}
```

### 4.2.2 查询单个会话
- Method: `GET`
- Path: `/api/chat-sessions/{chatSessionId}`
- 路径参数: `chatSessionId`
- 响应: `ApiResponse<GetChatSessionResponse>`
- 说明: 不存在会抛业务异常。

### 4.2.3 按 Agent 查询会话
- Method: `GET`
- Path: `/api/chat-sessions/agent/{agentId}`
- 路径参数: `agentId`
- 响应: `ApiResponse<GetChatSessionsResponse>`

### 4.2.4 创建会话
- Method: `POST`
- Path: `/api/chat-sessions`
- 请求体: `CreateChatSessionRequest`
```json
{
  "agentId": "string",
  "title": "string"
}
```
- 响应: `ApiResponse<CreateChatSessionResponse>`，`data.chatSessionId`
- 说明: 创建后用于后续消息流的 `sessionId`。

### 4.2.5 删除会话
- Method: `DELETE`
- Path: `/api/chat-sessions/{chatSessionId}`
- 说明: 校验存在后删除。

### 4.2.6 更新会话
- Method: `PATCH`
- Path: `/api/chat-sessions/{chatSessionId}`
- 请求体: `UpdateChatSessionRequest`
```json
{
  "title": "string"
}
```
- 说明: 保留原 `agentId` 和创建时间。

---

## 4.3 ChatMessage 消息管理

### 4.3.1 按会话查询消息
- Method: `GET`
- Path: `/api/chat-messages/session/{sessionId}`
- 响应: `ApiResponse<GetChatMessagesResponse>`
- data 结构:
```json
{
  "chatMessages": [
    {
      "id": "string",
      "sessionId": "string",
      "role": "user",
      "content": "string",
      "metadata": {
        "toolResponse": {},
        "toolCalls": []
      }
    }
  ]
}
```

### 4.3.2 创建消息（用户入口）
- Method: `POST`
- Path: `/api/chat-messages`
- 请求体: `CreateChatMessageRequest`
```json
{
  "agentId": "string",
  "sessionId": "string",
  "role": "user",
  "content": "请帮我总结这个知识库",
  "metadata": {
    "toolResponse": null,
    "toolCalls": null
  }
}
```
- 响应: `ApiResponse<CreateChatMessageResponse>`，`data.chatMessageId`
- 说明:
  - 先写入消息。
  - 发布 `ChatEvent`，异步触发 Agent 运行。
  - Agent 后续产生的消息会继续落库并通过 SSE 推送。

### 4.3.3 删除消息
- Method: `DELETE`
- Path: `/api/chat-messages/{chatMessageId}`
- 说明: 先查存在再删。

### 4.3.4 更新消息
- Method: `PATCH`
- Path: `/api/chat-messages/{chatMessageId}`
- 请求体: `UpdateChatMessageRequest`
```json
{
  "content": "string",
  "metadata": {
    "toolResponse": {},
    "toolCalls": []
  }
}
```
- 说明: 保留原 `sessionId`、`role`、创建时间。

---

## 4.4 KnowledgeBase 知识库管理

### 4.4.1 查询知识库列表
- Method: `GET`
- Path: `/api/knowledge-bases`
- 响应: `ApiResponse<GetKnowledgeBasesResponse>`

### 4.4.2 创建知识库
- Method: `POST`
- Path: `/api/knowledge-bases`
- 请求体: `CreateKnowledgeBaseRequest`
```json
{
  "name": "产品手册",
  "description": "面向客服问答"
}
```
- 响应: `ApiResponse<CreateKnowledgeBaseResponse>`，`data.knowledgeBaseId`

### 4.4.3 删除知识库
- Method: `DELETE`
- Path: `/api/knowledge-bases/{knowledgeBaseId}`

### 4.4.4 更新知识库
- Method: `PATCH`
- Path: `/api/knowledge-bases/{knowledgeBaseId}`
- 请求体: `UpdateKnowledgeBaseRequest`
```json
{
  "name": "新名称",
  "description": "新描述"
}
```

---

## 4.5 Document 文档管理

### 4.5.1 查询全部文档
- Method: `GET`
- Path: `/api/documents`
- 响应: `ApiResponse<GetDocumentsResponse>`
- data 结构:
```json
{
  "documents": [
    {
      "id": "string",
      "kbId": "string",
      "filename": "guide.md",
      "filetype": "md",
      "size": 1024
    }
  ]
}
```

### 4.5.2 按知识库查询文档
- Method: `GET`
- Path: `/api/documents/kb/{kbId}`

### 4.5.3 创建文档记录（不上传文件）
- Method: `POST`
- Path: `/api/documents`
- 请求体: `CreateDocumentRequest`
```json
{
  "kbId": "string",
  "filename": "guide.md",
  "filetype": "md",
  "size": 1024
}
```
- 说明: 仅数据库建档，不保存物理文件。

### 4.5.4 上传文档（推荐入口）
- Method: `POST`
- Path: `/api/documents/upload`
- Content-Type: `multipart/form-data`
- 表单参数:
  - `kbId`: string
  - `file`: file
- 响应: `ApiResponse<CreateDocumentResponse>`，`data.documentId`
- 说明:
  - 创建记录 -> 保存文件 -> 更新文件路径元数据。
  - 对 `md/markdown` 自动解析并生成向量 chunk。
  - 其他格式当前仅保存文件并告警日志（待扩展解析器）。

### 4.5.5 删除文档
- Method: `DELETE`
- Path: `/api/documents/{documentId}`
- 说明: 先尝试删除物理文件，再删除数据库记录。

### 4.5.6 更新文档
- Method: `PATCH`
- Path: `/api/documents/{documentId}`
- 请求体: `UpdateDocumentRequest`
```json
{
  "filename": "new.md",
  "filetype": "md",
  "size": 2048
}
```
- 说明: 保留原 `kbId` 和创建时间。

---

## 4.6 Tool 工具接口

### 4.6.1 查询可选工具列表
- Method: `GET`
- Path: `/api/tools`
- 响应: `ApiResponse<List<Tool>>`
- Tool 字段:
  - `name`: 工具名
  - `description`: 工具描述
  - `type`: `OPTIONAL` 或 `FIXED`
- 说明: 该接口返回 `OPTIONAL` 工具（用于前端 Agent 配置可选项），不是全部工具。

当前代码中常见工具：
- 固定工具: `terminate`, `KnowledgeTool`（`directAnswer` 当前未注册 Spring Bean）
- 可选工具: `databaseQuery`（DataBaseTools）, `sendEmail`（EmailTools）
- 文件系统工具目前被注释禁用，不会自动注入。

---

## 4.7 SSE 接口

### 4.7.1 建立 SSE 连接
- Method: `REQUEST`（控制器未限制方法，实际建议 `GET`）
- Path: `/sse/connect/{chatSessionId}`
- Produces: `text/event-stream`
- 返回: `SseEmitter`
- 说明:
  - 连接超时时间 30 分钟。
  - 首包事件: `init -> connected`
  - 后续业务事件: `message -> SseMessage(JSON)`

SseMessage 结构示例：
```json
{
  "type": "AI_GENERATED_CONTENT",
  "payload": {
    "message": {
      "id": "msg_1",
      "sessionId": "sess_1",
      "role": "assistant",
      "content": "这是 AI 回复",
      "metadata": {
        "toolResponse": null,
        "toolCalls": []
      }
    },
    "statusText": null,
    "done": null
  },
  "metadata": {
    "chatMessageId": "msg_1"
  }
}
```

---

## 4.8 测试与健康接口

### 4.8.1 健康检查
- Method: `REQUEST`
- Path: `/health`
- 响应: 字符串 `ok`

### 4.8.2 SSE 测试占位
- Method: `GET`
- Path: `/sse-test`
- 响应: 字符串 `ok`

---

## 5. 请求模型速查

## 5.1 Agent

- `CreateAgentRequest` / `UpdateAgentRequest`
  - `name`: String
  - `description`: String
  - `systemPrompt`: String
  - `model`: String（实际可用模型枚举见 `AgentDTO.ModelType`）
  - `allowedTools`: List<String>
  - `allowedKbs`: List<String>
  - `chatOptions.temperature`: Double
  - `chatOptions.topP`: Double
  - `chatOptions.messageLength`: Integer

## 5.2 会话

- `CreateChatSessionRequest`
  - `agentId`: String
  - `title`: String

- `UpdateChatSessionRequest`
  - `title`: String

## 5.3 消息

- `CreateChatMessageRequest`
  - `agentId`: String
  - `sessionId`: String
  - `role`: `user/assistant/system/tool`
  - `content`: String
  - `metadata.toolResponse`: ToolResponse
  - `metadata.toolCalls`: List<ToolCall>

- `UpdateChatMessageRequest`
  - `content`: String
  - `metadata`: 同上

## 5.4 知识库

- `CreateKnowledgeBaseRequest` / `UpdateKnowledgeBaseRequest`
  - `name`: String
  - `description`: String

## 5.5 文档

- `CreateDocumentRequest`
  - `kbId`: String
  - `filename`: String
  - `filetype`: String
  - `size`: Long

- `UpdateDocumentRequest`
  - `filename`: String
  - `filetype`: String
  - `size`: Long

---

## 6. 注意事项与改进建议

1. `SseController` 使用 `@RequestMapping`，建议改为 `@GetMapping` 以减少歧义。
2. `/api/tools` 当前只返回可选工具；若前端需要展示固定工具，建议新增 `/api/tools/all`。
3. 文档上传仅对 Markdown 做自动解析与向量化，PDF/Word 尚未实现。
4. 错误码目前统一 500，若对接外部系统建议细分业务错误码。
5. `DirectAnswerTool` 与 `FileSystemTools` 当前未注册 Bean，实际不会出现在运行时工具集合中。

---

## 7. 文档维护方式

当新增接口时，建议同步更新以下信息：

1. 功能总览中对应模块能力。
2. 接口清单中的 Method/Path/参数/响应。
3. 是否有异步副作用（事件、SSE、向量化、外部调用）。
4. 请求与响应模型速查字段。
