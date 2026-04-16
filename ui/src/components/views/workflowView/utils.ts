import type {
  ExecutionTraceRefVO,
  WorkflowStepInstanceVO,
} from "../../../api/api.ts";
import type {
  NodeMeta,
  WorkflowStateSnapshot,
} from "./types.ts";

export const NODE_ORDER = [
  "planner",
  "retriever",
  "executor",
  "reviewer",
  "approval",
  "publish",
];

const NODE_LABELS: Record<string, NodeMeta> = {
  planner: { name: "Planner", role: "任务拆解", tone: "from-sky-500 to-cyan-400" },
  retriever: { name: "Retriever", role: "知识检索", tone: "from-emerald-500 to-teal-400" },
  executor: { name: "Executor", role: "内容执行", tone: "from-amber-500 to-orange-400" },
  reviewer: { name: "Reviewer", role: "质量复核", tone: "from-fuchsia-500 to-rose-400" },
  approval: { name: "Approval", role: "人工确认", tone: "from-indigo-500 to-violet-400" },
  publish: { name: "Publisher", role: "产物发布", tone: "from-stone-700 to-stone-500" },
};

export function statusColor(status?: string) {
  if (status === "COMPLETED" || status === "APPROVED") return "green";
  if (status === "RUNNING" || status === "STREAMING") return "blue";
  if (status === "WAITING_APPROVAL" || status === "PENDING") return "orange";
  if (status === "FAILED" || status === "REJECTED") return "red";
  if (status === "CREATED") return "default";
  return "purple";
}

export function parseJson<T>(value?: string): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export function findLatestSnapshot(steps: WorkflowStepInstanceVO[]) {
  return [...steps]
    .reverse()
    .map((step) => parseJson<WorkflowStateSnapshot>(step.outputSnapshot))
    .find((snapshot): snapshot is WorkflowStateSnapshot => Boolean(snapshot));
}

export function getStepSummary(step: WorkflowStepInstanceVO) {
  const snapshot = parseJson<WorkflowStateSnapshot>(step.outputSnapshot);
  if (!snapshot) return step.errorMessage || step.nodeKey;
  if (step.nodeKey === "planner") return snapshot.plan || snapshot.taskType || "已生成结构化计划";
  if (step.nodeKey === "retriever") return `已整理 ${snapshot.sources?.length || 0} 条引用来源`;
  if (step.nodeKey === "executor") return snapshot.draft || snapshot.draftResult || "已生成初稿";
  if (step.nodeKey === "reviewer") return snapshot.reviewComment || snapshot.review?.comment || "已完成质量复核";
  if (step.nodeKey === "approval") return `等待审批：${snapshot.approvalRecordId || "-"}`;
  if (step.nodeKey === "publish") return "已发布最终 Markdown 产物";
  return step.nodeKey;
}

export function nodeMeta(nodeKey: string): NodeMeta {
  return NODE_LABELS[nodeKey] || {
    name: nodeKey,
    role: "Graph Node",
    tone: "from-slate-500 to-slate-400",
  };
}

export function shortId(value?: string) {
  if (!value) return "-";
  return value.length > 12 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

export function traceStatusTone(trace: ExecutionTraceRefVO) {
  if (trace.status === "FAILED" || trace.eventType === "NODE_FAILED") return "red";
  if (trace.status === "RUNNING" || trace.eventType === "NODE_STARTED") return "blue";
  if (trace.status === "COMPLETED" || trace.eventType === "NODE_COMPLETED") return "green";
  return "gray";
}

export function normalizeGraphPath(path?: string[] | string) {
  if (Array.isArray(path)) return path;
  if (!path) return [];
  return path
    .split("->")
    .map((node) => node.trim())
    .filter(Boolean);
}
