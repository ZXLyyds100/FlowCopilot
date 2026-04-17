import { get, post, patch, del, BASE_URL } from "./http.ts";
import type { ChatMessageVO, MessageType } from "../types";

// 类型定义
export interface ChatOptions {
  temperature?: number;
  topP?: number;
  messageLength?: number;
}

export type ModelType = "deepseek-chat" | "glm-4.6";

export interface CreateAgentRequest {
  name: string;
  description?: string;
  systemPrompt?: string;
  model: ModelType;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
}

export interface UpdateAgentRequest {
  name?: string;
  description?: string;
  systemPrompt?: string;
  model?: ModelType;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
}

export interface CreateAgentResponse {
  agentId: string;
}

export interface AgentVO {
  id: string;
  name: string;
  description?: string;
  systemPrompt?: string;
  model: ModelType;
  allowedTools?: string[];
  allowedKbs?: string[];
  chatOptions?: ChatOptions;
  createdAt?: string;
  updatedAt?: string;
}

export interface GetAgentsResponse {
  agents: AgentVO[];
}

/**
 * 获取所有 agents
 */
export async function getAgents(): Promise<GetAgentsResponse> {
  return get<GetAgentsResponse>("/agents");
}

/**
 * 创建 agent
 */
export async function createAgent(
  request: CreateAgentRequest,
): Promise<CreateAgentResponse> {
  return post<CreateAgentResponse>("/agents", request);
}

/**
 * 删除 agent
 */
export async function deleteAgent(agentId: string): Promise<void> {
  return del<void>(`/agents/${agentId}`);
}

/**
 * 更新 agent
 */
export async function updateAgent(
  agentId: string,
  request: UpdateAgentRequest,
): Promise<void> {
  return patch<void>(`/agents/${agentId}`, request);
}

/**
 * 创建聊天会话
 */
export interface CreateChatSessionRequest {
  agentId: string;
  title?: string;
}

export interface CreateChatSessionResponse {
  chatSessionId: string;
}

export async function createChatSession(
  request: CreateChatSessionRequest,
): Promise<CreateChatSessionResponse> {
  return post<CreateChatSessionResponse>("/chat-sessions", request);
}

/**
 * 聊天会话相关类型和接口
 */
export interface ChatSessionVO {
  id: string;
  agentId: string;
  title?: string;
}

export interface GetChatSessionsResponse {
  chatSessions: ChatSessionVO[];
}

export interface GetChatSessionResponse {
  chatSession: ChatSessionVO;
}

export interface UpdateChatSessionRequest {
  title?: string;
}

/**
 * 获取所有聊天会话
 */
export async function getChatSessions(): Promise<GetChatSessionsResponse> {
  return get<GetChatSessionsResponse>("/chat-sessions");
}

/**
 * 获取单个聊天会话
 */
export async function getChatSession(
  chatSessionId: string,
): Promise<GetChatSessionResponse> {
  return get<GetChatSessionResponse>(`/chat-sessions/${chatSessionId}`);
}

/**
 * 根据 agentId 获取聊天会话
 */
export async function getChatSessionsByAgentId(
  agentId: string,
): Promise<GetChatSessionsResponse> {
  return get<GetChatSessionsResponse>(`/chat-sessions/agent/${agentId}`);
}

/**
 * 更新聊天会话
 */
export async function updateChatSession(
  chatSessionId: string,
  request: UpdateChatSessionRequest,
): Promise<void> {
  return patch<void>(`/chat-sessions/${chatSessionId}`, request);
}

/**
 * 删除聊天会话
 */
export async function deleteChatSession(chatSessionId: string): Promise<void> {
  return del<void>(`/chat-sessions/${chatSessionId}`);
}

/**
 * 聊天消息相关类型和接口
 */
export interface MetaData {
  [key: string]: unknown;
}

export interface GetChatMessagesResponse {
  chatMessages: ChatMessageVO[];
}

export interface CreateChatMessageRequest {
  agentId: string;
  sessionId: string;
  role: MessageType;
  content: string;
  metadata?: MetaData;
}

export interface CreateChatMessageResponse {
  chatMessageId: string;
}

export interface UpdateChatMessageRequest {
  content?: string;
  metadata?: MetaData;
}

/**
 * 根据 sessionId 获取聊天消息
 */
export async function getChatMessagesBySessionId(
  sessionId: string,
): Promise<GetChatMessagesResponse> {
  return get<GetChatMessagesResponse>(`/chat-messages/session/${sessionId}`);
}

/**
 * 创建聊天消息
 */
export async function createChatMessage(
  request: CreateChatMessageRequest,
): Promise<CreateChatMessageResponse> {
  return post<CreateChatMessageResponse>("/chat-messages", request);
}

/**
 * 更新聊天消息
 */
export async function updateChatMessage(
  chatMessageId: string,
  request: UpdateChatMessageRequest,
): Promise<void> {
  return patch<void>(`/chat-messages/${chatMessageId}`, request);
}

/**
 * 删除聊天消息
 */
export async function deleteChatMessage(chatMessageId: string): Promise<void> {
  return del<void>(`/chat-messages/${chatMessageId}`);
}

/**
 * FlowCopilot workflow 相关类型和接口
 */
export interface WorkflowInstanceVO {
  id: string;
  definitionId?: string;
  title: string;
  input: string;
  status: string;
  currentStep?: string;
  result?: string;
  metadata?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowStepInstanceVO {
  id: string;
  workflowInstanceId: string;
  nodeKey: string;
  nodeName: string;
  status: string;
  inputSnapshot?: string;
  outputSnapshot?: string;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ArtifactVO {
  id: string;
  workflowInstanceId: string;
  type: string;
  title: string;
  content?: string;
  metadata?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateWorkflowRequest {
  title?: string;
  input: string;
  knowledgeBaseId?: string;
  templateCode?: string;
}

export interface CreateWorkflowResponse {
  workflowInstanceId: string;
  workflow: WorkflowInstanceVO;
}

export interface GetWorkflowResponse {
  workflow: WorkflowInstanceVO;
  steps: WorkflowStepInstanceVO[];
  artifacts: ArtifactVO[];
}

export interface GetWorkflowsResponse {
  workflows: WorkflowInstanceVO[];
}

export interface WorkflowTemplateVO {
  code: string;
  name: string;
  description?: string;
  mermaid?: string;
  definitionJson?: string;
  sourceType?: string;
  supportsCheckpoint?: boolean;
  supportsSubGraph?: boolean;
  supportsParallel?: boolean;
}

export interface GetWorkflowTemplatesResponse {
  templates: WorkflowTemplateVO[];
}

export interface UpdateWorkflowTemplateRequest {
  name?: string;
  description?: string;
  definitionJson?: string;
}

export interface ExecutionTraceRefVO {
  id: string;
  workflowInstanceId: string;
  traceId: string;
  graphTemplate?: string;
  nodeKey: string;
  eventType: string;
  status: string;
  inputSnapshot?: string;
  outputSnapshot?: string;
  errorMessage?: string;
  durationMs?: number;
  createdAt?: string;
}

export interface GetWorkflowTraceResponse {
  workflowInstanceId: string;
  traces: ExecutionTraceRefVO[];
}

export interface WorkflowExecutionCheckpointVO {
  id: string;
  workflowInstanceId: string;
  traceId: string;
  runId: string;
  nodeKey: string;
  checkpointType: string;
  stateSnapshot: string;
  metadata?: string;
  createdAt?: string;
}

export interface GetWorkflowCheckpointsResponse {
  workflowInstanceId: string;
  checkpoints: WorkflowExecutionCheckpointVO[];
}

export interface WorkflowObservationSummaryVO {
  totalSpans: number;
  workflowRuns: number;
  nodeRuns: number;
  llmCalls: number;
  toolCalls: number;
  retrievalCalls: number;
  errorCount: number;
  inputTokens?: number;
  outputTokens?: number;
  totalTokens?: number;
  estimatedCostUsd?: number;
  exporterStatus: string;
  latestTraceId?: string;
}

export interface WorkflowObservationSpanVO {
  id: string;
  runId: string;
  traceId: string;
  spanId: string;
  parentSpanId?: string;
  workflowInstanceId: string;
  nodeKey?: string;
  spanType: string;
  name: string;
  status: string;
  inputSummary?: string;
  outputSummary?: string;
  errorMessage?: string;
  attributesJson?: string;
  modelName?: string;
  responseId?: string;
  finishReason?: string;
  inputTokens?: number;
  outputTokens?: number;
  totalTokens?: number;
  estimatedCostUsd?: number;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  children: WorkflowObservationSpanVO[];
}

export interface WorkflowObservationExternalTraceVO {
  enabled: boolean;
  provider: string;
  status: string;
  traceId?: string;
  projectName?: string;
  url?: string;
  lastErrorMessage?: string;
}

export interface GetWorkflowObservabilityResponse {
  workflowInstanceId: string;
  summary: WorkflowObservationSummaryVO;
  spans: WorkflowObservationSpanVO[];
  externalTrace: WorkflowObservationExternalTraceVO;
}

export async function createWorkflow(
  request: CreateWorkflowRequest,
): Promise<CreateWorkflowResponse> {
  return post<CreateWorkflowResponse>("/workflows", request);
}

export async function getWorkflow(
  workflowInstanceId: string,
): Promise<GetWorkflowResponse> {
  return get<GetWorkflowResponse>(`/workflows/${workflowInstanceId}`);
}

export async function getWorkflows(limit = 20): Promise<GetWorkflowsResponse> {
  return get<GetWorkflowsResponse>("/workflows", { limit });
}

export async function getWorkflowTemplates(): Promise<GetWorkflowTemplatesResponse> {
  return get<GetWorkflowTemplatesResponse>("/workflows/templates");
}

export async function updateWorkflowTemplate(
  templateCode: string,
  request: UpdateWorkflowTemplateRequest,
): Promise<WorkflowTemplateVO> {
  return fetch(`${BASE_URL}/workflows/templates/${templateCode}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  }).then(async (response) => {
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const apiResponse = await response.json();
    if (apiResponse.code !== 200) {
      throw new Error(apiResponse.message || "更新模板失败");
    }
    return apiResponse.data as WorkflowTemplateVO;
  });
}

export async function getWorkflowTrace(
  workflowInstanceId: string,
): Promise<GetWorkflowTraceResponse> {
  return get<GetWorkflowTraceResponse>(`/workflows/${workflowInstanceId}/trace`);
}

export async function getWorkflowCheckpoints(
  workflowInstanceId: string,
): Promise<GetWorkflowCheckpointsResponse> {
  return get<GetWorkflowCheckpointsResponse>(`/workflows/${workflowInstanceId}/checkpoints`);
}

export async function getWorkflowObservability(
  workflowInstanceId: string,
): Promise<GetWorkflowObservabilityResponse> {
  return get<GetWorkflowObservabilityResponse>(`/workflows/${workflowInstanceId}/observability`);
}

export async function replayWorkflowFromNode(
  workflowInstanceId: string,
  nodeKey: string,
): Promise<void> {
  return post<void>(`/workflows/${workflowInstanceId}/replay/${nodeKey}`);
}

export interface ApprovalRecordVO {
  id: string;
  workflowInstanceId: string;
  status: string;
  title: string;
  summary?: string;
  comment?: string;
  decidedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GetApprovalsResponse {
  approvals: ApprovalRecordVO[];
}

export interface ApprovalDecisionRequest {
  comment?: string;
}

export async function getApprovals(status = "PENDING"): Promise<GetApprovalsResponse> {
  return get<GetApprovalsResponse>("/approvals", { status });
}

export async function approveWorkflow(
  approvalRecordId: string,
  request: ApprovalDecisionRequest,
): Promise<ApprovalRecordVO> {
  return post<ApprovalRecordVO>(`/approvals/${approvalRecordId}/approve`, request);
}

export async function rejectWorkflow(
  approvalRecordId: string,
  request: ApprovalDecisionRequest,
): Promise<ApprovalRecordVO> {
  return post<ApprovalRecordVO>(`/approvals/${approvalRecordId}/reject`, request);
}

/**
 * 知识库相关类型和接口
 */
export interface KnowledgeBaseVO {
  id: string;
  name: string;
  description?: string;
}

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
}

export interface UpdateKnowledgeBaseRequest {
  name?: string;
  description?: string;
}

export interface GetKnowledgeBasesResponse {
  knowledgeBases: KnowledgeBaseVO[];
}

export interface CreateKnowledgeBaseResponse {
  knowledgeBaseId: string;
}

/**
 * 获取所有知识库
 */
export async function getKnowledgeBases(): Promise<GetKnowledgeBasesResponse> {
  return get<GetKnowledgeBasesResponse>("/knowledge-bases");
}

/**
 * 创建知识库
 */
export async function createKnowledgeBase(
  request: CreateKnowledgeBaseRequest,
): Promise<CreateKnowledgeBaseResponse> {
  return post<CreateKnowledgeBaseResponse>("/knowledge-bases", request);
}

/**
 * 删除知识库
 */
export async function deleteKnowledgeBase(
  knowledgeBaseId: string,
): Promise<void> {
  return del<void>(`/knowledge-bases/${knowledgeBaseId}`);
}

/**
 * 更新知识库
 */
export async function updateKnowledgeBase(
  knowledgeBaseId: string,
  request: UpdateKnowledgeBaseRequest,
): Promise<void> {
  return patch<void>(`/knowledge-bases/${knowledgeBaseId}`, request);
}

/**
 * 文档相关类型和接口
 */
export interface DocumentVO {
  id: string;
  kbId: string;
  filename: string;
  filetype: string;
  size: number;
}

export interface GetDocumentsResponse {
  documents: DocumentVO[];
}

export interface CreateDocumentResponse {
  documentId: string;
}

/**
 * 根据知识库 ID 获取文档列表
 */
export async function getDocumentsByKbId(
  kbId: string,
): Promise<GetDocumentsResponse> {
  return get<GetDocumentsResponse>(`/documents/kb/${kbId}`);
}

/**
 * 上传文档
 */
export async function uploadDocument(
  kbId: string,
  file: File,
): Promise<CreateDocumentResponse> {
  const formData = new FormData();
  formData.append("kbId", kbId);
  formData.append("file", file);

  const response = await fetch(`${BASE_URL}/documents/upload`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const apiResponse = await response.json();
  if (apiResponse.code !== 200) {
    throw new Error(apiResponse.message || "上传失败");
  }

  return apiResponse.data;
}

/**
 * 删除文档
 */
export async function deleteDocument(documentId: string): Promise<void> {
  return del<void>(`/documents/${documentId}`);
}

/**
 * 工具相关类型和接口
 */
export type ToolType = "FIXED" | "OPTIONAL";

export interface ToolVO {
  name: string;
  description: string;
  type: ToolType;
}

export interface GetOptionalToolsResponse {
  tools: ToolVO[];
}

/**
 * 获取可选工具列表
 */
export async function getOptionalTools(): Promise<GetOptionalToolsResponse> {
  const tools = await get<ToolVO[]>("/tools");
  return { tools };
}
