# JChatMind 类职责总览

本文档用于帮助快速理解后端代码结构，重点说明每个类在系统中的职责、所处层次以及推荐阅读顺序。

配合阅读文档：

- `agent-rag-call-flow.md`: 重点解释“用户发消息之后，Agent、工具、RAG、SSE 是如何串起来运行的”。

## 1. 推荐阅读顺序

如果你希望最快理解系统运行方式，建议按下面的顺序阅读：

1. `FlowCopilotApplication.java`
2. `controller/ChatMessageController.java`
3. `service/impl/ChatMessageFacadeServiceImpl.java`
4. `event/ChatEvent.java`
5. `event/listener/ChatEventListener.java`
6. `agent/JChatMindFactory.java`
7. `agent/JChatMind.java`
8. `agent/tools/KnowledgeTools.java`
9. `service/impl/RagServiceImpl.java`
10. `service/impl/DocumentFacadeServiceImpl.java`
11. `service/impl/MarkdownParserServiceImpl.java`
12. `service/impl/SseServiceImpl.java`

## 2. 启动与基础配置

- `FlowCopilotApplication.java`: Spring Boot 启动入口，负责拉起整个应用。
- `config/AsyncConfig.java`: 配置异步线程池，给聊天事件监听和 Agent 异步执行使用。
- `config/ChatClientRegistry.java`: 维护所有可用的 `ChatClient`，按模型名查找。
- `config/CorsConfig.java`: 配置跨域访问规则，允许本地前端访问后端接口和 SSE。
- `config/MultiChatClientConfig.java`: 注册 DeepSeek 和智谱的 `ChatClient` Bean。

## 3. Controller 层

- `controller/AgentController.java`: Agent 配置管理接口，处理 Agent 的增删改查。
- `controller/ChatMessageController.java`: 聊天消息接口，用户发消息后会从这里进入主链路。
- `controller/ChatSessionController.java`: 聊天会话接口，处理会话的新建、查询、更新、删除。
- `controller/DocumentController.java`: 文档接口，处理文档上传、查询和删除。
- `controller/KnowledgeBaseController.java`: 知识库接口，负责知识库元数据的管理。
- `controller/SseController.java`: SSE 接口，为前端建立基于会话 ID 的实时连接。
- `controller/TestController.java`: 测试用途接口，用于开发期验证。
- `controller/ToolController.java`: 工具接口，供前端查询系统内可选工具列表。

## 4. Agent 核心

- `agent/AgentState.java`: Agent 运行状态枚举，例如空闲、执行中、完成、异常。
- `agent/JChatMind.java`: Agent 核心执行器，负责 Think-Execute 循环、工具调用、消息持久化和 SSE 推送。
- `agent/JChatMindFactory.java`: Agent 运行时装配器，负责从数据库恢复配置、记忆、工具和知识库。
- `agent/examples/JChatMindV1.java`: Agent 实现示例版本 1，用于学习和演示。
- `agent/examples/JChatMindV2.java`: Agent 实现示例版本 2，用于学习和演示。

## 5. 工具系统

- `agent/tools/Tool.java`: 全部工具的统一抽象接口。
- `agent/tools/ToolType.java`: 工具类型枚举，区分固定工具和可选工具。
- `agent/tools/DataBaseTools.java`: 数据库只读查询工具，允许 Agent 执行受限的 SELECT。
- `agent/tools/DirectAnswerTool.java`: 直接回答工具，用于将最终结果返回给用户。
- `agent/tools/EmailTools.java`: 邮件发送工具，通过邮件服务发送内容。
- `agent/tools/FileSystemTools.java`: 文件系统相关工具，用于执行文件读写类能力。
- `agent/tools/KnowledgeTools.java`: 知识库检索工具，Agent 调它时会触发 RAG。
- `agent/tools/TerminateTool.java`: 终止工具，允许 Agent 主动结束任务循环。
- `agent/tools/test/CityTool.java`: 测试示例工具，返回城市相关数据。
- `agent/tools/test/DateTool.java`: 测试示例工具，返回日期相关数据。
- `agent/tools/test/WeatherTool.java`: 测试示例工具，返回天气相关数据。

## 6. 事件与实时推送

- `event/ChatEvent.java`: 聊天事件对象，封装 `agentId`、`sessionId` 和消息内容。
- `event/listener/ChatEventListener.java`: 事件监听器，异步接收聊天事件并启动 Agent。
- `message/SseMessage.java`: SSE 推送消息结构，统一封装事件类型、载荷和元数据。

## 7. 异常处理

- `exception/BizException.java`: 业务异常，表示可预期的业务失败。
- `exception/GlobalExceptionHandler.java`: 全局异常处理器，将异常统一转换为接口响应。

## 8. Service 接口层

- `service/AgentFacadeService.java`: Agent 业务门面接口。
- `service/ChatMessageFacadeService.java`: 聊天消息业务门面接口。
- `service/ChatSessionFacadeService.java`: 聊天会话业务门面接口。
- `service/DocumentFacadeService.java`: 文档业务门面接口。
- `service/DocumentStorageService.java`: 文档物理存储接口。
- `service/EmailService.java`: 邮件能力接口。
- `service/KnowledgeBaseFacadeService.java`: 知识库业务门面接口。
- `service/MarkdownParserService.java`: Markdown 章节提取接口。
- `service/RagService.java`: 向量化和语义检索接口。
- `service/SseService.java`: SSE 连接和消息推送接口。
- `service/ToolFacadeService.java`: 工具注册与筛选接口。

## 9. Service 实现层

- `service/impl/AgentFacadeServiceImpl.java`: Agent 配置的 CRUD 实现，负责 DTO/Entity/VO 转换和数据库持久化。
- `service/impl/ChatMessageFacadeServiceImpl.java`: 消息查询与持久化实现，创建用户消息时还会发布聊天事件。
- `service/impl/ChatSessionFacadeServiceImpl.java`: 聊天会话的完整业务实现。
- `service/impl/DocumentFacadeServiceImpl.java`: 文档业务主流程实现，包含上传、落盘、Markdown 解析、向量入库。
- `service/impl/DocumentStorageServiceImpl.java`: 本地文件系统存储实现。
- `service/impl/EmailServiceImpl.java`: 邮件发送实现，使用 Spring Mail 完成异步投递。
- `service/impl/KnowledgeBaseFacadeServiceImpl.java`: 知识库业务实现。
- `service/impl/MarkdownParserServiceImpl.java`: Markdown 解析实现，提取标题和对应正文。
- `service/impl/RagServiceImpl.java`: RAG 实现，调用本地 embedding 服务并使用 pgvector 做相似度检索。
- `service/impl/SseServiceImpl.java`: 基于 `SseEmitter` 管理前端实时连接。
- `service/impl/ToolFacadeServiceImpl.java`: 从 Spring 容器中收集全部工具并分类输出。

## 10. Converter 层

- `converter/AgentConverter.java`: Agent 的 Request/DTO/Entity/VO 转换器，并处理 JSON 字段序列化。
- `converter/ChatMessageConverter.java`: 聊天消息对象转换器。
- `converter/ChatSessionConverter.java`: 聊天会话对象转换器。
- `converter/ChunkBgeM3Converter.java`: 向量分块对象转换器。
- `converter/DocumentConverter.java`: 文档对象转换器。
- `converter/KnowledgeBaseConverter.java`: 知识库对象转换器。

## 11. Mapper 接口层

- `mapper/AgentMapper.java`: `agent` 表数据访问接口。
- `mapper/ChatMessageMapper.java`: `chat_message` 表数据访问接口。
- `mapper/ChatSessionMapper.java`: `chat_session` 表数据访问接口。
- `mapper/ChunkBgeM3Mapper.java`: `chunk_bge_m3` 表数据访问接口，同时负责向量相似检索。
- `mapper/DocumentMapper.java`: `document` 表数据访问接口。
- `mapper/KnowledgeBaseMapper.java`: `knowledge_base` 表数据访问接口。

## 12. 数据模型

### 12.1 通用响应

- `model/common/ApiResponse.java`: 通用接口返回包装类。

### 12.2 DTO

- `model/dto/AgentDTO.java`: Agent 运行时配置 DTO，包含模型、工具、知识库和对话参数。
- `model/dto/ChatMessageDTO.java`: 聊天消息 DTO，Agent 运行时最常使用。
- `model/dto/ChatSessionDTO.java`: 聊天会话 DTO。
- `model/dto/ChunkBgeM3DTO.java`: 向量分块 DTO。
- `model/dto/DocumentDTO.java`: 文档 DTO，包含文件路径等元信息。
- `model/dto/KnowledgeBaseDTO.java`: 知识库 DTO。

### 12.3 Entity

- `model/entity/Agent.java`: Agent 数据库实体，`allowedTools`、`allowedKbs`、`chatOptions` 以 JSON 字符串形式存储。
- `model/entity/ChatMessage.java`: 聊天消息数据库实体。
- `model/entity/ChatSession.java`: 聊天会话数据库实体。
- `model/entity/ChunkBgeM3.java`: 向量分块数据库实体。
- `model/entity/Document.java`: 文档数据库实体。
- `model/entity/KnowledgeBase.java`: 知识库数据库实体。

### 12.4 Request

- `model/request/CreateAgentRequest.java`: 创建 Agent 请求。
- `model/request/CreateChatMessageRequest.java`: 创建聊天消息请求。
- `model/request/CreateChatSessionRequest.java`: 创建聊天会话请求。
- `model/request/CreateDocumentRequest.java`: 创建文档记录请求。
- `model/request/CreateKnowledgeBaseRequest.java`: 创建知识库请求。
- `model/request/UpdateAgentRequest.java`: 更新 Agent 请求。
- `model/request/UpdateChatMessageRequest.java`: 更新聊天消息请求。
- `model/request/UpdateChatSessionRequest.java`: 更新聊天会话请求。
- `model/request/UpdateDocumentRequest.java`: 更新文档请求。
- `model/request/UpdateKnowledgeBaseRequest.java`: 更新知识库请求。

### 12.5 Response

- `model/response/CreateAgentResponse.java`: 创建 Agent 的返回值。
- `model/response/CreateChatMessageResponse.java`: 创建聊天消息的返回值。
- `model/response/CreateChatSessionResponse.java`: 创建聊天会话的返回值。
- `model/response/CreateDocumentResponse.java`: 创建文档的返回值。
- `model/response/CreateKnowledgeBaseResponse.java`: 创建知识库的返回值。
- `model/response/GetAgentsResponse.java`: Agent 列表返回值。
- `model/response/GetChatMessagesResponse.java`: 聊天消息列表返回值。
- `model/response/GetChatSessionResponse.java`: 单个聊天会话返回值。
- `model/response/GetChatSessionsResponse.java`: 聊天会话列表返回值。
- `model/response/GetDocumentsResponse.java`: 文档列表返回值。
- `model/response/GetKnowledgeBasesResponse.java`: 知识库列表返回值。

### 12.6 VO

- `model/vo/AgentVO.java`: 面向前端展示的 Agent 对象。
- `model/vo/ChatMessageVO.java`: 面向前端展示的聊天消息对象。
- `model/vo/ChatSessionVO.java`: 面向前端展示的聊天会话对象。
- `model/vo/DocumentVO.java`: 面向前端展示的文档对象。
- `model/vo/KnowledgeBaseVO.java`: 面向前端展示的知识库对象。

## 13. 类型处理

- `typehandler/PgVectorTypeHandler.java`: MyBatis 与 PostgreSQL `vector` 字段之间的类型转换器。

## 14. 与 Agent 和 RAG 关系最紧密的类

如果你当前重点研究 Agent 和 RAG，建议先抓住下面这些类：

- `agent/JChatMind.java`: Agent 的循环执行器。
- `agent/JChatMindFactory.java`: Agent 运行时的组装入口。
- `agent/tools/KnowledgeTools.java`: Agent 访问知识库的桥梁。
- `service/impl/RagServiceImpl.java`: 检索逻辑核心实现。
- `service/impl/DocumentFacadeServiceImpl.java`: 文档入库和向量化的主流程。
- `service/impl/MarkdownParserServiceImpl.java`: Markdown 切块逻辑。
- `mapper/ChunkBgeM3Mapper.java`: 向量表访问入口。
- `typehandler/PgVectorTypeHandler.java`: 向量字段映射关键点。

## 15. 数据流总结

### 15.1 Agent 数据流

用户消息 -> `ChatMessageController` -> `ChatMessageFacadeServiceImpl` -> `ChatEvent` -> `ChatEventListener` -> `JChatMindFactory` -> `JChatMind`

### 15.2 RAG 数据流

上传 Markdown -> `DocumentController` -> `DocumentFacadeServiceImpl` -> `MarkdownParserServiceImpl` -> `RagServiceImpl.embed` -> `ChunkBgeM3Mapper.insert`

### 15.3 运行时检索数据流

模型决定调用 `KnowledgeTool` -> `KnowledgeTools` -> `RagServiceImpl.similaritySearch` -> `ChunkBgeM3Mapper.similaritySearch` -> 返回相关文本片段 -> Agent 继续推理

## 16. 后续建议

如果你准备继续深入，推荐下一步把下列文件连起来一起读：

- `service/impl/ChatMessageFacadeServiceImpl.java`
- `event/listener/ChatEventListener.java`
- `agent/JChatMindFactory.java`
- `agent/JChatMind.java`
- `agent/tools/KnowledgeTools.java`
- `service/impl/RagServiceImpl.java`
- `service/impl/DocumentFacadeServiceImpl.java`

这样能最快建立“消息触发 Agent，Agent 决定是否调用 RAG，RAG 返回结果再继续推理”的完整脑图。
