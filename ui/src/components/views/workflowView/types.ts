import type {
  ExecutionTraceRefVO,
  WorkflowStepInstanceVO,
} from "../../../api/api.ts";

export interface WorkflowSource {
  index?: number;
  sourceType?: string;
  title?: string;
  content?: string;
}

export interface WorkflowReview {
  score?: number;
  passed?: boolean;
  comment?: string;
  suggestions?: string[];
}

export interface WorkflowStateSnapshot {
  taskType?: string;
  plan?: string;
  retrievedContents?: string[];
  sources?: WorkflowSource[];
  draft?: string;
  draftResult?: string;
  review?: WorkflowReview;
  reviewComment?: string;
  approvalRecordId?: string;
  approvalStatus?: string;
  approvalComment?: string;
  approvalRequired?: boolean;
  finalOutput?: string;
  templateCode?: string;
  traceId?: string;
  currentNodeKey?: string;
  graphPath?: string[] | string;
  retryCount?: number;
}

export interface WorkflowMetadata {
  knowledgeBaseId?: string;
  templateCode?: string;
}

export interface WorkflowSseMessage {
  type: string;
  payload?: {
    workflowInstanceId?: string;
    nodeKey?: string;
    nodeName?: string;
    stepStatus?: string;
    statusText?: string;
    content?: string;
    approvalRecordId?: string;
    approvalStatus?: string;
    done?: boolean;
  };
  metadata?: {
    stepId?: string;
    workflowInstanceId?: string;
    approvalRecordId?: string;
  };
}

export interface StreamStage {
  nodeKey: string;
  nodeName: string;
  status: string;
  content: string;
}

export interface NodeMeta {
  name: string;
  role: string;
  tone: string;
}

export interface WorkflowGraphCardProps {
  currentStep?: string;
  graphPath: string[];
  latestSnapshot: WorkflowStateSnapshot | null;
  selectedTemplateCode: string;
  selectedTemplateMermaid?: string;
  setTemplateCode: (value: string) => void;
  steps: WorkflowStepInstanceVO[];
  templateCapabilityTags: string[];
  templateEditorValue: string;
  templateSaving: boolean;
  templates: Array<{
    code: string;
    definitionJson?: string;
    description?: string;
    name: string;
    sourceType?: string;
    supportsCheckpoint?: boolean;
    supportsParallel?: boolean;
    supportsSubGraph?: boolean;
  }>;
  onResetTemplateEditor: () => void;
  onSaveTemplate: () => void;
  onTemplateEditorChange: (value: string) => void;
}

export type WorkflowTrace = ExecutionTraceRefVO;
