# FlowCopilot AI 分阶段完整实现文档

## 1. 文档目标

本文档用于指导项目从当前的单 Agent 原型系统，逐步演进为一个基于 `LangChain4j + LangGraph4j + LangSmith` 的多智能体执行平台。

本文档回答以下问题：

- 第一阶段要实现哪些能力
- 第二阶段要新增哪些模块
- 第三、第四阶段分别解决什么问题
- 前后端每一阶段如何配合
- 最终系统长什么样

本文档强调“按阶段落地”，而不是一次性实现全部设想。

## 2. 当前项目基线

当前项目已经具备以下基础能力：

- 聊天会话管理
- 基础 Agent 配置
- 单 Agent 工具调用
- 知识库上传与基础 RAG
- SSE 实时消息推送

当前核心代码：

- [JChatMind.java](/Users/anastasio/Programming/JChatMind/jchatmind/src/main/java/com/kama/jchatmind/agent/JChatMind.java)
- [JChatMindFactory.java](/Users/anastasio/Programming/JChatMind/jchatmind/src/main/java/com/kama/jchatmind/agent/JChatMindFactory.java)
- [ChatEventListener.java](/Users/anastasio/Programming/JChatMind/jchatmind/src/main/java/com/kama/jchatmind/event/listener/ChatEventListener.java)
- [RagServiceImpl.java](/Users/anastasio/Programming/JChatMind/jchatmind/src/main/java/com/kama/jchatmind/service/impl/RagServiceImpl.java)
- [SseServiceImpl.java](/Users/anastasio/Programming/JChatMind/jchatmind/src/main/java/com/kama/jchatmind/service/impl/SseServiceImpl.java)

当前系统更适合：

- 聊天问答
- 单 Agent 工具调用
- 简单知识增强

当前系统还不适合：

- 多 Agent 协作
- 图式工作流
- 人工审批节点
- 节点级状态管理
- 可恢复执行
- 系统级可观测

因此，后续阶段的目标，是在保留已有基础能力的前提下，逐步引入：

- 状态模型
- 工作流图
- 多角色 Agent
- 审批
- 产物
- Trace

## 3. 技术路线总览

### 3.1 技术栈职责

- `LangChain4j`
  负责模型接入、工具调用、结构化输出、RAG 能力封装
- `LangGraph4j`
  负责状态图、节点编排、条件路由、检查点、恢复执行
- `LangSmith`
  负责观测、Trace、回放和评测
- `Spring Boot`
  负责 API、业务层、持久化、调度与服务治理
- `React`
  负责聊天入口、工作流执行看板、审批界面、产物展示界面

### 3.2 工作流定义策略

本项目采用：

**固定骨架 + 动态决策** 的混合式工作流方案。

这意味着：

- 系统预定义合法节点和流转关系
- 模型在合法约束内决定分支、检索、角色协作和是否进入审批
- 不采用完全自由生成的不可控工作流

这与 LangGraph4j 的 `StateGraph + conditional edges + checkpoints` 模式是匹配的。  
LangGraph4j 官方说明其适用于构建 `stateful, multi-agent applications`，并支持 `StateGraph`、条件边、检查点、断点、子图与并行节点执行。  
来源：
- [LangGraph4j Overview](https://langgraph4j.github.io/langgraph4j/)

### 3.3 RAG 实现策略

RAG 不再只是聊天场景的附属能力，而是工作流中的标准节点能力。

LangChain4j 官方文档中明确支持：

- `AI Services`
- `tools`
- `ContentRetriever`
- `RetrievalAugmentor`
- 更细粒度的 RAG pipeline 组件

来源：
- [LangChain4j AI Services](https://docs.langchain4j.dev/tutorials/ai-services/)
- [LangChain4j RAG](https://docs.langchain4j.dev/tutorials/rag)

### 3.4 观测策略

LangSmith 的接入建议采用 OpenTelemetry 路线，这样 Java 服务可以通过 OTEL 将图执行链路、节点信息、模型调用数据接入 LangSmith。

来源：
- [LangSmith Trace with OpenTelemetry](https://docs.langchain.com/langsmith/collector-proxy)

说明：
这里我根据官方文档做了工程性推断：对于 Java 项目，LangSmith 最稳妥的接入方式应优先采用 OTEL，而不是等待某个 Java 一等 SDK 方案。

## 4. 分阶段实现总原则

项目实施遵循以下原则：

- 每阶段必须形成可运行成果
- 每阶段都应有可展示功能
- 每阶段前后端都有明确范围
- 后端优先做骨架，前端逐步接可视化
- 不追求一步到位，避免系统性重构失控

总的阶段划分如下：

- 第一阶段：工作流最小闭环
- 第二阶段：多 Agent 协作与知识增强
- 第三阶段：人机协同与执行看板
- 第四阶段：平台化能力与可观测
- 最终阶段：产品化完善与可扩展能力

---

## 5. 第一阶段：工作流最小闭环

### 5.1 阶段目标

目标是把当前“聊天 + 单 Agent Loop”升级成“任务驱动 + 固定骨架工作流”的最小版本。

第一阶段不追求复杂编排，只追求：

- 能创建一个流程实例
- 能按固定节点顺序执行
- 能记录每一步执行结果
- 能把最终结果展示出来

### 5.2 第一阶段必须实现的能力

- 聊天输入升级为任务输入
- 引入 `workflow_instance`
- 引入 `workflow_step_instance`
- 定义统一 `WorkflowState`
- 支持固定骨架工作流执行
- 至少完成 4 个核心节点
- 节点结果持久化
- 前端展示工作流执行详情

### 5.3 第一阶段工作流骨架

建议第一阶段固定为以下节点：

- `InputNode`
- `PlannerNode`
- `RetrieverNode`
- `ExecutorNode`
- `PublishNode`

执行链：

`用户输入 -> Planner -> Retriever -> Executor -> Publish`

### 5.4 第一阶段后端实现内容

#### 数据库

新增表：

- `workflow_definition`
- `workflow_instance`
- `workflow_step_instance`
- `artifact`

保留原有：

- `chat_session`
- `chat_message`
- `knowledge_base`
- `document`

#### 新增包结构

建议新增：

- `workflow/state`
- `workflow/model`
- `workflow/node`
- `workflow/runtime`
- `workflow/service`
- `workflow/controller`

#### 核心类

建议先实现：

- `WorkflowState`
- `WorkflowDefinition`
- `WorkflowInstance`
- `WorkflowStepInstance`
- `WorkflowNode`
- `WorkflowRuntimeService`
- `WorkflowInstanceService`
- `WorkflowController`

#### 节点实现

第一阶段实现 4 个节点：

- `PlannerNode`
  负责理解用户任务，生成初步计划
- `RetrieverNode`
  负责调用 RAG 检索补充上下文
- `ExecutorNode`
  负责完成主要执行输出
- `PublishNode`
  负责将结果写入产物并准备返回前端

#### RAG 改造

第一阶段不必完全重写 RAG，只需将现有 [RagServiceImpl.java](/Users/anastasio/Programming/JChatMind/jchatmind/src/main/java/com/kama/jchatmind/service/impl/RagServiceImpl.java) 包装成可供 `RetrieverNode` 调用的服务接口。

### 5.5 第一阶段前端实现内容

#### 保留页面

- 保留 [AgentChatView.tsx](/Users/anastasio/Programming/JChatMind/ui/src/components/views/AgentChatView.tsx) 作为任务提交入口

#### 新增页面

- `WorkflowDetailView`

展示内容：

- 当前流程状态
- 当前节点
- 每个节点执行状态
- 最终输出

#### 新增 API

- `POST /api/workflows`
- `GET /api/workflows/{id}`
- `GET /api/workflows/{id}/steps`

### 5.6 第一阶段交付物

- 后端可创建并运行最小工作流
- 前端可看到任务执行过程
- 最终可生成一个文本型产物

### 5.7 第一阶段验收标准

- 能输入任务并触发流程
- 能看到 4 个节点依次执行
- 能查到 workflow instance 和 step 记录
- 能输出最终结果
- 前端能展示流程详情

---

## 6. 第二阶段：多 Agent 协作与知识增强

### 6.1 阶段目标

在第一阶段固定工作流基础上，引入真正的多角色 Agent 协作能力，并把 RAG 从“附属能力”升级为“标准流程节点能力”。

### 6.2 第二阶段必须实现的能力

- 多角色 Agent 装配
- Agent 权限隔离
- Agent 角色职责边界
- 知识检索增强升级
- 节点内部结构化输出
- WorkflowState 丰富化

### 6.3 第二阶段角色设计

最小角色集合建议为：

- `Planner Agent`
- `Retriever Agent`
- `Executor Agent`
- `Reviewer Agent`
- `Reporter Agent`

其中第二阶段至少落地：

- `Planner`
- `Retriever`
- `Executor`
- `Reviewer`

### 6.4 第二阶段后端实现内容

#### AI 层重构

引入 LangChain4j，新增：

- `ChatModelProvider`
- `ToolRegistry`
- `StructuredOutputService`
- `AgentRoleService`

#### RAG 层重构

从单一服务拆成：

- `KnowledgeIndexService`
- `RetrieverService`
- `RetrievalRecordService`

#### 节点升级

- `PlannerNode` 输出结构化计划
- `RetrieverNode` 输出引用知识与召回结果
- `ExecutorNode` 输出初稿
- `ReviewerNode` 给出质量判断和修改建议

#### State 升级

`WorkflowState` 增加字段：

- `taskType`
- `plan`
- `retrievedContents`
- `draft`
- `reviewComment`
- `finalOutput`
- `sources`

### 6.5 第二阶段前端实现内容

新增展示：

- 节点输出摘要
- 角色 Agent 名称
- 检索引用来源
- 最终结构化输出

页面增强：

- `WorkflowDetailView` 增加角色显示
- 增加“知识引用”面板

### 6.6 第二阶段交付物

- 多角色 Agent 协同链路
- 检索增强成为节点级能力
- 最终输出带知识引用

### 6.7 第二阶段验收标准

- 不同节点由不同角色 Agent 负责
- 检索结果能够影响执行结果
- Reviewer 节点能对结果进行复核
- 前端可区分不同角色输出

---

## 7. 第三阶段：人机协同与执行看板

### 7.1 阶段目标

引入人工审批与流程暂停恢复能力，同时将前端升级为“执行平台界面”，而不是单纯聊天界面。

### 7.2 第三阶段必须实现的能力

- 审批节点
- 流程暂停
- 审批后继续执行
- 驳回重试
- 执行状态可视化
- SSE 流程事件

### 7.3 第三阶段工作流骨架升级

升级链路：

`Input -> Planner -> Retriever -> Executor -> Reviewer -> Approval -> Publish`

如果 `Reviewer` 判定质量不达标，可以：

- 回退到 `Executor`
- 或进入人工驳回

### 7.4 第三阶段后端实现内容

#### 新增表

- `approval_record`

#### 新增模块

- `ApprovalService`
- `ApprovalController`
- `WorkflowResumeService`

#### 新增节点

- `ApprovalNode`

节点职责：

- 将流程状态设置为 `WAITING_APPROVAL`
- 创建审批记录
- 等待前端人工操作

#### SSE 增强

扩展 SSE 事件类型：

- `WORKFLOW_STARTED`
- `STEP_STARTED`
- `STEP_COMPLETED`
- `STEP_FAILED`
- `APPROVAL_REQUIRED`
- `WORKFLOW_FINISHED`

### 7.5 第三阶段前端实现内容

新增页面：

- `ApprovalView`
- `WorkflowListView`

新增能力：

- 查看待审批事项
- 通过/驳回审批
- 查看当前流程图状态
- 实时更新节点状态

### 7.6 第三阶段交付物

- 可暂停等待审批的工作流
- 可恢复继续执行的流程实例
- 可实时展示状态变化的执行面板

### 7.7 第三阶段验收标准

- 流程能在审批节点暂停
- 审批通过后能继续执行
- 驳回后能进入重试逻辑
- 前端能实时看到节点状态变化

---

## 8. 第四阶段：平台化能力与可观测

### 8.1 阶段目标

在功能闭环基础上，引入真正的平台型能力：

- 工作流模板化
- 子图与条件路由
- LangSmith Trace
- 回放调试

### 8.2 第四阶段必须实现的能力

- 多工作流模板
- 条件路由
- 子图支持
- Trace 接入
- 节点级可观测
- 失败回放

### 8.3 第四阶段后端实现内容

#### Workflow Definition 升级

支持多个模板，例如：

- `ResearchWorkflow`
- `AnalysisWorkflow`
- `TaskExecutionWorkflow`

#### LangGraph4j 引入

这一阶段将第一阶段实现的简化流程执行器，逐步迁移为真正的 LangGraph4j 状态图执行。

重点引入：

- `StateGraph`
- normal edges
- conditional edges
- checkpoints
- pause / resume

这里基于官方文档可确认 LangGraph4j 已支持：

- `StateGraph`
- conditional edges
- checkpoints
- breakpoints
- sub-graphs
- parallel node execution

来源：
- [LangGraph4j Overview](https://langgraph4j.github.io/langgraph4j/)

#### LangSmith 接入

建议此阶段通过 OTEL 接入 LangSmith。

需要采集：

- workflow run
- step run
- model invocation
- tool invocation
- retrieval trace

#### TraceRef 设计

新增表：

- `execution_trace_ref`

### 8.4 第四阶段前端实现内容

新增视图：

- `TraceSummaryView`
- `WorkflowTemplateView`

展示内容：

- 流程模板
- 节点执行树
- Trace 摘要
- 错误节点
- 耗时与结果对比

### 8.5 第四阶段交付物

- 基于 LangGraph4j 的状态图工作流
- 基于 LangSmith 的执行观测
- 多模板工作流体系

### 8.6 第四阶段验收标准

- 至少支持两类流程模板
- 能显示不同流程路径
- 能记录完整 trace
- 能定位节点失败原因

---

## 9. 最终阶段：产品化完善与扩展能力

### 9.1 阶段目标

将系统从“技术原型”升级为“可扩展平台”。

### 9.2 最终阶段能力

- 工作流模板管理
- 角色 Agent 配置管理
- 知识域隔离
- 产物归档
- Prompt 配置化
- 执行策略配置化
- 节点并行与汇总
- 更丰富的产物类型
- 更完整的评测体系

### 9.3 后端最终形态

后端将形成以下中心模块：

- Workflow Center
- Agent Role Center
- Knowledge Center
- Approval Center
- Artifact Center
- Trace Center

### 9.4 前端最终形态

前端将形成以下页面体系：

- 聊天任务入口
- 工作流列表页
- 工作流详情页
- 审批页面
- 产物页面
- Trace 摘要页
- 模板管理页
- Agent 配置页

### 9.5 最终交付形态

最终系统应当具备以下特征：

- 用户通过自然语言发起复杂任务
- 系统自动创建工作流实例
- 多 Agent 协同推进任务
- 知识库参与执行决策
- 人工可在关键节点审核
- 最终生成结构化交付结果
- 整个过程可在 LangSmith 中追踪和回放

---

## 10. 各阶段实施顺序建议

如果按真实开发节奏推进，建议顺序如下：

1. 第一阶段后端
2. 第一阶段前端
3. 第二阶段后端
4. 第二阶段前端
5. 第三阶段后端
6. 第三阶段前端
7. 第四阶段后端观测能力
8. 第四阶段前端 Trace 展示
9. 最终阶段平台化增强

原因是：

- 先把执行主链跑通
- 再增强协作能力
- 再做人机协同
- 最后补平台化和观测

这样不会在一开始就陷入大规模重构失控。

## 11. 每阶段完成标志

### 第一阶段完成标志

- 已有固定骨架工作流
- 可展示最小任务执行闭环

### 第二阶段完成标志

- 已有多角色 Agent 协作
- 已有知识增强节点

### 第三阶段完成标志

- 已有人机协同
- 已有执行看板

### 第四阶段完成标志

- 已有 LangGraph4j 图执行
- 已有 LangSmith Trace

### 最终阶段完成标志

- 已形成可扩展的多智能体执行平台

## 12. 结语

这份分阶段实现文档的核心目标，是帮助项目从“Agent Demo”平滑演进为“多智能体执行平台”。

实施的重点不在于一开始就做出最复杂的图和最多的 Agent，而在于：

- 每一阶段都形成完整闭环
- 每一阶段都能明确展示能力升级
- 每一阶段都让系统更接近平台化目标

最终，你得到的将不再是一个只能聊天的 Agent，而是一个：

**能够理解任务、组织协作、执行流程、调用知识、等待审核、生成交付物、并可被观测与优化的多智能体执行系统。**

## 13. 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChain4j AI Services](https://docs.langchain4j.dev/tutorials/ai-services/)
- [LangChain4j RAG](https://docs.langchain4j.dev/tutorials/rag)
- [LangGraph4j 官方文档](https://langgraph4j.github.io/langgraph4j/)
- [LangSmith OpenTelemetry Tracing](https://docs.langchain.com/langsmith/collector-proxy)
