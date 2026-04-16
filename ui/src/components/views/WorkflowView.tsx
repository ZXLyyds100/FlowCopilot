import React, { startTransition, useEffect, useMemo, useRef, useState } from "react";
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Collapse,
  Empty,
  Input,
  List,
  Progress,
  Select,
  Space,
  Tag,
  Timeline,
  Typography,
  message,
} from "antd";
import {
  ApartmentOutlined,
  CheckCircleOutlined,
  CloudSyncOutlined,
  CodeOutlined,
  ExperimentOutlined,
  ForkOutlined,
  HistoryOutlined,
  NodeIndexOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { useShellPage } from "../shell/useShellPage.ts";
import {
  approveWorkflow,
  createWorkflow,
  getApprovals,
  getWorkflowCheckpoints,
  getWorkflowObservability,
  getWorkflow,
  getWorkflowTemplates,
  getWorkflows,
  getWorkflowTrace,
  rejectWorkflow,
  replayWorkflowFromNode,
  updateWorkflowTemplate,
  type ApprovalRecordVO,
  type ArtifactVO,
  type ExecutionTraceRefVO,
  type GetWorkflowObservabilityResponse,
  type WorkflowExecutionCheckpointVO,
  type WorkflowInstanceVO,
  type WorkflowStepInstanceVO,
  type WorkflowTemplateVO,
} from "../../api/api.ts";
import { BASE_URL } from "../../api/http.ts";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";
import WorkflowInspector from "./workflowView/WorkflowInspector.tsx";
import WorkflowObservabilityPanel from "./workflowView/WorkflowObservabilityPanel.tsx";

const { TextArea } = Input;

const NODE_ORDER = ["planner", "retriever", "executor", "reviewer", "approval", "publish"];

const NODE_LABELS: Record<string, { name: string; role: string; tone: string }> = {
  planner: { name: "Planner", role: "任务拆解", tone: "from-sky-500 to-cyan-400" },
  retriever: { name: "Retriever", role: "知识检索", tone: "from-emerald-500 to-teal-400" },
  executor: { name: "Executor", role: "内容执行", tone: "from-amber-500 to-orange-400" },
  reviewer: { name: "Reviewer", role: "质量复核", tone: "from-fuchsia-500 to-rose-400" },
  approval: { name: "Approval", role: "人工确认", tone: "from-indigo-500 to-violet-400" },
  publish: { name: "Publisher", role: "产物发布", tone: "from-stone-700 to-stone-500" },
};

interface WorkflowSource {
  index?: number;
  sourceType?: string;
  title?: string;
  content?: string;
}

interface WorkflowReview {
  score?: number;
  passed?: boolean;
  comment?: string;
  suggestions?: string[];
}

interface WorkflowStateSnapshot {
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

interface WorkflowMetadata {
  knowledgeBaseId?: string;
  templateCode?: string;
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  return new Date(value).toLocaleString();
}

function isSubGraphStep(nodeKey: string) {
  return nodeKey.includes(".");
}

interface WorkflowSseMessage {
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

interface StreamStage {
  nodeKey: string;
  nodeName: string;
  status: string;
  content: string;
}

function statusColor(status?: string) {
  if (status === "COMPLETED" || status === "APPROVED") return "green";
  if (status === "RUNNING" || status === "STREAMING") return "blue";
  if (status === "WAITING_APPROVAL" || status === "PENDING") return "orange";
  if (status === "FAILED" || status === "REJECTED") return "red";
  if (status === "CREATED") return "default";
  return "purple";
}

function parseJson<T>(value?: string): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

function findLatestSnapshot(steps: WorkflowStepInstanceVO[]) {
  return [...steps]
    .reverse()
    .map((step) => parseJson<WorkflowStateSnapshot>(step.outputSnapshot))
    .find((snapshot): snapshot is WorkflowStateSnapshot => Boolean(snapshot));
}

function getStepSummary(step: WorkflowStepInstanceVO) {
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

function nodeMeta(nodeKey: string) {
  return NODE_LABELS[nodeKey] || { name: nodeKey, role: "Graph Node", tone: "from-slate-500 to-slate-400" };
}

function shortId(value?: string) {
  if (!value) return "-";
  return value.length > 12 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

function traceStatusTone(trace: ExecutionTraceRefVO) {
  if (trace.status === "FAILED" || trace.eventType === "NODE_FAILED") return "red";
  if (trace.status === "RUNNING" || trace.eventType === "NODE_STARTED") return "blue";
  if (trace.status === "COMPLETED" || trace.eventType === "NODE_COMPLETED") return "green";
  return "gray";
}

function normalizeGraphPath(path?: string[] | string) {
  if (Array.isArray(path)) return path;
  if (!path) return [];
  return path.split("->").map((node) => node.trim()).filter(Boolean);
}

const WorkflowView: React.FC = () => {
  const [taskInput, setTaskInput] = useState("");
  const [title, setTitle] = useState("");
  const [knowledgeBaseId, setKnowledgeBaseId] = useState<string | undefined>();
  const [templateCode, setTemplateCode] = useState("research");
  const [approvalComment, setApprovalComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [replayingNode, setReplayingNode] = useState<string | null>(null);
  const [workflows, setWorkflows] = useState<WorkflowInstanceVO[]>([]);
  const [templates, setTemplates] = useState<WorkflowTemplateVO[]>([]);
  const [pendingApprovals, setPendingApprovals] = useState<ApprovalRecordVO[]>([]);
  const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowInstanceVO | null>(null);
  const [steps, setSteps] = useState<WorkflowStepInstanceVO[]>([]);
  const [traces, setTraces] = useState<ExecutionTraceRefVO[]>([]);
  const [checkpoints, setCheckpoints] = useState<WorkflowExecutionCheckpointVO[]>([]);
  const [observability, setObservability] = useState<GetWorkflowObservabilityResponse | null>(null);
  const [observabilityLoading, setObservabilityLoading] = useState(false);
  const [artifacts, setArtifacts] = useState<ArtifactVO[]>([]);
  const [streamStages, setStreamStages] = useState<Record<string, StreamStage>>({});
  const [templateEditorValue, setTemplateEditorValue] = useState("");
  const [templateSaving, setTemplateSaving] = useState(false);
  const [liveStatus, setLiveStatus] = useState("等待创建任务");
  const eventSourceRef = useRef<EventSource | null>(null);
  const { knowledgeBases } = useKnowledgeBases();

  const latestSnapshot = useMemo(() => findLatestSnapshot(steps), [steps]);
  const workflowMetadata = useMemo(
    () => parseJson<WorkflowMetadata>(currentWorkflow?.metadata),
    [currentWorkflow?.metadata],
  );
  const selectedTemplateCode = latestSnapshot?.templateCode || workflowMetadata?.templateCode || templateCode;
  const selectedTemplate = templates.find((template) => template.code === selectedTemplateCode) || templates[0];
  const checkpointCount = checkpoints.length;
  const currentApprovalId = latestSnapshot?.approvalRecordId
    || pendingApprovals.find((approval) => approval.workflowInstanceId === currentWorkflow?.id)?.id;
  const completedStepCount = steps.filter((step) => step.status === "COMPLETED").length;
  const progressPercent = currentWorkflow
    ? Math.min(100, Math.round((completedStepCount / NODE_ORDER.length) * 100))
    : 0;

  const graphPath = useMemo(() => {
    const snapshotPath = normalizeGraphPath(latestSnapshot?.graphPath);
    if (snapshotPath.length) return snapshotPath;
    const completedNodes = traces
      .filter((trace) => trace.eventType === "NODE_COMPLETED" || trace.eventType === "NODE_FAILED")
      .map((trace) => trace.nodeKey);
    return Array.from(new Set(completedNodes));
  }, [latestSnapshot?.graphPath, traces]);

  const streamStageList = useMemo(() => (
    Object.values(streamStages).sort((left, right) => {
      const leftIndex = NODE_ORDER.indexOf(left.nodeKey);
      const rightIndex = NODE_ORDER.indexOf(right.nodeKey);
      return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex);
    })
  ), [streamStages]);
  const templateCapabilityTags = useMemo(() => {
    if (!selectedTemplate) return [];
    const tags: string[] = [];
    if (selectedTemplate.supportsCheckpoint) tags.push("Checkpoint");
    if (selectedTemplate.supportsSubGraph) tags.push("子图");
    if (selectedTemplate.supportsParallel) tags.push("并行");
    if (selectedTemplate.sourceType) tags.push(selectedTemplate.sourceType === "builtin" ? "内置模板" : "数据库模板");
    return tags;
  }, [selectedTemplate]);

  const refreshWorkflows = async () => {
    const response = await getWorkflows();
    setWorkflows(response.workflows);
  };

  const refreshTemplates = async () => {
    const response = await getWorkflowTemplates();
    setTemplates(response.templates);
    if (!response.templates.some((template) => template.code === templateCode)) {
      setTemplateCode(response.templates[0]?.code || "research");
    }
  };

  const refreshApprovals = async () => {
    const response = await getApprovals();
    setPendingApprovals(response.approvals);
  };

  const refreshTrace = async (workflowInstanceId: string) => {
    const response = await getWorkflowTrace(workflowInstanceId);
    setTraces(response.traces);
  };

  const refreshCheckpoints = async (workflowInstanceId: string) => {
    const response = await getWorkflowCheckpoints(workflowInstanceId);
    setCheckpoints(response.checkpoints);
  };

  const refreshObservability = async (workflowInstanceId: string) => {
    setObservabilityLoading(true);
    try {
      const response = await getWorkflowObservability(workflowInstanceId);
      setObservability(response);
    } finally {
      setObservabilityLoading(false);
    }
  };

  const loadWorkflow = async (workflowInstanceId: string) => {
    const response = await getWorkflow(workflowInstanceId);
    setCurrentWorkflow(response.workflow);
    setSteps(response.steps);
    setArtifacts(response.artifacts);
    await Promise.all([
      refreshCheckpoints(workflowInstanceId).catch(console.error),
      refreshTrace(workflowInstanceId).catch(console.error),
      refreshObservability(workflowInstanceId).catch(console.error),
    ]);
  };

  useEffect(() => {
    setTemplateEditorValue(selectedTemplate?.definitionJson || "");
  }, [selectedTemplate?.code, selectedTemplate?.definitionJson]);

  const closeEventSource = () => {
    eventSourceRef.current?.close();
    eventSourceRef.current = null;
  };

  const connectWorkflowStream = (workflowInstanceId: string) => {
    closeEventSource();
    const sseBaseUrl = BASE_URL.replace(/\/api$/, "/sse");
    const eventSource = new EventSource(`${sseBaseUrl}/connect/${workflowInstanceId}`);
    eventSourceRef.current = eventSource;

    eventSource.addEventListener("message", (event) => {
      const data = JSON.parse(event.data) as WorkflowSseMessage;
      const payload = data.payload || {};

      startTransition(() => {
        if (payload.statusText) {
          setLiveStatus(payload.statusText);
        }

        if (data.type === "STEP_STARTED" && payload.nodeKey) {
          setStreamStages((prev) => ({
            ...prev,
            [payload.nodeKey!]: {
              nodeKey: payload.nodeKey!,
              nodeName: payload.nodeName || payload.nodeKey!,
              status: "RUNNING",
              content: "",
            },
          }));
        }

        if (data.type === "STEP_DELTA" && payload.nodeKey) {
          setStreamStages((prev) => {
            const existing = prev[payload.nodeKey!] || {
              nodeKey: payload.nodeKey!,
              nodeName: payload.nodeName || payload.nodeKey!,
              status: "STREAMING",
              content: "",
            };
            return {
              ...prev,
              [payload.nodeKey!]: {
                ...existing,
                status: "STREAMING",
                content: `${existing.content}${payload.content || ""}`,
              },
            };
          });
        }

        if (data.type === "STEP_COMPLETED" && payload.nodeKey) {
          setStreamStages((prev) => {
            const existing = prev[payload.nodeKey!] || {
              nodeKey: payload.nodeKey!,
              nodeName: payload.nodeName || payload.nodeKey!,
              status: "COMPLETED",
              content: "",
            };
            return {
              ...prev,
              [payload.nodeKey!]: {
                ...existing,
                status: "COMPLETED",
                content: existing.content || payload.content || "",
              },
            };
          });
        }
      });

      if (data.type === "STEP_COMPLETED") {
        loadWorkflow(workflowInstanceId).catch(console.error);
      }

      if (data.type === "APPROVAL_REQUIRED") {
        refreshApprovals().catch(console.error);
        loadWorkflow(workflowInstanceId).catch(console.error);
      }

      if (data.type === "WORKFLOW_FINISHED" || data.type === "WORKFLOW_FAILED") {
        loadWorkflow(workflowInstanceId).catch(console.error);
        refreshWorkflows().catch(console.error);
        refreshApprovals().catch(console.error);
        closeEventSource();
      }
    });

    eventSource.onerror = () => {
      setLiveStatus("实时连接暂时中断，可通过详情继续查看状态");
    };
  };

  useEffect(() => {
    refreshTemplates().catch(console.error);
    refreshWorkflows().catch(console.error);
    refreshApprovals().catch(console.error);
    return closeEventSource;
  }, []);

  const handleCreateWorkflow = async () => {
    if (!taskInput.trim()) {
      message.warning("请输入要执行的任务");
      return;
    }
    setLoading(true);
    setStreamStages({});
    setArtifacts([]);
    setSteps([]);
    setTraces([]);
    setCheckpoints([]);
    setObservability(null);
    setLiveStatus("正在创建 Graph 工作流");
    try {
      const response = await createWorkflow({
        title: title.trim() || undefined,
        input: taskInput.trim(),
        knowledgeBaseId,
        templateCode,
      });
      setCurrentWorkflow(response.workflow);
      connectWorkflowStream(response.workflowInstanceId);
      await refreshWorkflows();
      await loadWorkflow(response.workflowInstanceId);
      message.success("Graph 工作流已启动，正在实时执行");
    } catch (error) {
      console.error(error);
      message.error("工作流启动失败");
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!currentApprovalId) {
      message.warning("当前没有待审批记录");
      return;
    }
    if (currentWorkflow && !eventSourceRef.current) {
      connectWorkflowStream(currentWorkflow.id);
    }
    await approveWorkflow(currentApprovalId, { comment: approvalComment });
    setApprovalComment("");
    message.success("已通过审批，工作流继续发布");
    if (currentWorkflow) {
      await loadWorkflow(currentWorkflow.id);
    }
    await refreshApprovals();
  };

  const handleReject = async () => {
    if (!currentApprovalId) {
      message.warning("当前没有待审批记录");
      return;
    }
    if (currentWorkflow && !eventSourceRef.current) {
      connectWorkflowStream(currentWorkflow.id);
    }
    await rejectWorkflow(currentApprovalId, { comment: approvalComment || "请根据审批意见重新生成" });
    setApprovalComment("");
    message.success("已驳回，工作流回退到 Executor Agent 重试");
    if (currentWorkflow) {
      await loadWorkflow(currentWorkflow.id);
    }
    await refreshApprovals();
  };

  const handleReplay = async (nodeKey: string) => {
    if (!currentWorkflow) return;
    setReplayingNode(nodeKey);
    setLiveStatus(`正在从 ${nodeMeta(nodeKey).name} 节点重放`);
    try {
      connectWorkflowStream(currentWorkflow.id);
      await replayWorkflowFromNode(currentWorkflow.id, nodeKey);
      await loadWorkflow(currentWorkflow.id);
      message.success(`已从 ${nodeMeta(nodeKey).name} 节点重新执行`);
    } catch (error) {
      console.error(error);
      message.error("节点重放失败");
    } finally {
      setReplayingNode(null);
    }
  };

  const handleResetTemplateEditor = () => {
    setTemplateEditorValue(selectedTemplate?.definitionJson || "");
  };

  const handleSaveTemplate = async () => {
    if (!selectedTemplate) return;
    setTemplateSaving(true);
    try {
      const saved = await updateWorkflowTemplate(selectedTemplate.code, {
        name: selectedTemplate.name,
        description: selectedTemplate.description,
        definitionJson: templateEditorValue,
      });
      setTemplates((prev) => prev.map((template) => (
        template.code === saved.code ? saved : template
      )));
      setTemplateEditorValue(saved.definitionJson || "");
      message.success("模板已保存");
      if (currentWorkflow) {
        await loadWorkflow(currentWorkflow.id);
      }
    } catch (error) {
      console.error(error);
      message.error("模板保存失败");
    } finally {
      setTemplateSaving(false);
    }
  };

  useShellPage({
    title: "工作流",
    description: "运行 Graph 模板、查看实时状态并处理审批。",
    primaryAction: {
      label: "启动工作流",
      onClick: handleCreateWorkflow,
    },
    detailTitle: "工作流检查栏",
    detailContent: (
      <WorkflowInspector
        pendingApprovals={pendingApprovals}
        workflows={workflows}
        liveStatus={liveStatus}
        onSelectWorkflow={(id) => {
          connectWorkflowStream(id);
          loadWorkflow(id).catch(console.error);
        }}
      />
    ),
  });

  return (
    <div className="h-full overflow-auto bg-[radial-gradient(circle_at_top_left,#e8f7ff_0,#f6f1e8_34%,#f8fafc_70%)]">
      <div className="mx-auto max-w-[1240px] space-y-4">
        <Card className="overflow-hidden border-none bg-slate-950 text-white shadow-2xl shadow-slate-200">
            <div className="absolute -right-20 -top-24 h-56 w-56 rounded-full bg-cyan-400/30 blur-3xl" />
            <div className="absolute -bottom-24 -left-16 h-56 w-56 rounded-full bg-amber-300/20 blur-3xl" />
            <div className="relative">
              <Space align="center" className="mb-5">
                <Avatar size={44} className="bg-white text-slate-950" icon={<ForkOutlined />} />
                <div>
                  <Typography.Title level={4} className="!mb-0 !text-white">
                    FlowCopilot Graph Studio
                  </Typography.Title>
                  <Typography.Text className="!text-slate-300">第四阶段 · LangGraph4j 工作流驾驶舱</Typography.Text>
                </div>
              </Space>

              <Space direction="vertical" className="w-full" size="middle">
                <Input
                  size="large"
                  placeholder="可选：任务标题"
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                />
                <TextArea
                  rows={7}
                  placeholder="输入复杂任务，例如：基于知识库整理一份项目答辩介绍，并给出可执行路线"
                  value={taskInput}
                  onChange={(event) => setTaskInput(event.target.value)}
                />
                <Select
                  size="large"
                  value={templateCode}
                  onChange={setTemplateCode}
                  options={templates.map((template) => ({
                    label: `${template.name} · ${template.code}`,
                    value: template.code,
                  }))}
                  placeholder="选择 Graph 模板"
                />
                <Select
                  allowClear
                  size="large"
                  placeholder="可选：选择知识库增强"
                  value={knowledgeBaseId}
                  onChange={setKnowledgeBaseId}
                  options={knowledgeBases.map((kb) => ({
                    label: kb.name,
                    value: kb.knowledgeBaseId,
                  }))}
                />
                <Button
                  type="primary"
                  size="large"
                  icon={<ThunderboltOutlined />}
                  loading={loading}
                  onClick={handleCreateWorkflow}
                  block
                >
                  启动 Graph 智能流程
                </Button>
              </Space>
            </div>
          </Card>
          <Card className="overflow-hidden border-none bg-slate-950 text-white shadow-2xl shadow-slate-300">
            <div className="absolute right-0 top-0 h-full w-1/2 bg-[radial-gradient(circle_at_top,#22d3ee55,transparent_55%)]" />
            <div className="relative grid grid-cols-[minmax(0,1fr)_280px] gap-6">
              <div>
                <Space className="mb-3">
                  <Badge status={currentWorkflow?.status === "RUNNING" ? "processing" : "default"} />
                  <Typography.Text className="!text-cyan-200">
                    {selectedTemplate?.name || "Graph 工作流"}
                  </Typography.Text>
                  <Tag color={statusColor(currentWorkflow?.status)}>{currentWorkflow?.status || "IDLE"}</Tag>
                </Space>
                <Typography.Title level={2} className="!mb-3 !text-white">
                  {currentWorkflow?.title || "实时 Agent Graph 工作台"}
                </Typography.Title>
                <Typography.Paragraph className="!mb-0 !text-slate-300">
                  {currentWorkflow?.input || "创建任务后，系统会按 Graph 模板执行 Planner、Retriever、Executor、Reviewer、Approval、Publisher，并实时展示每个节点的状态、输出与 Trace。"}
                </Typography.Paragraph>
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/10 p-4 backdrop-blur">
                <Typography.Text className="!text-slate-300">Graph 进度</Typography.Text>
                <Progress
                  percent={progressPercent}
                  strokeColor={{ "0%": "#22d3ee", "100%": "#facc15" }}
                  trailColor="rgba(255,255,255,0.16)"
                />
                <div className="mt-3 grid grid-cols-3 gap-3 text-sm">
                  <div className="rounded-2xl bg-white/10 p-3">
                    <div className="text-slate-400">Trace</div>
                    <div className="font-semibold text-white">{shortId(latestSnapshot?.traceId || traces[0]?.traceId)}</div>
                  </div>
                  <div className="rounded-2xl bg-white/10 p-3">
                    <div className="text-slate-400">Retry</div>
                    <div className="font-semibold text-white">{latestSnapshot?.retryCount || 0}</div>
                  </div>
                  <div className="rounded-2xl bg-white/10 p-3">
                    <div className="text-slate-400">Checkpoint</div>
                    <div className="font-semibold text-white">{checkpointCount}</div>
                  </div>
                </div>
              </div>
            </div>
          </Card>

          <Alert
            type={currentWorkflow?.status === "FAILED" ? "error" : "info"}
            showIcon
            message={liveStatus}
            description={currentWorkflow?.currentStep ? `当前节点：${currentWorkflow.currentStep}` : "等待 Graph 事件"}
          />

          {!currentWorkflow ? (
            <Card className="border-none bg-white/90 shadow-xl shadow-slate-200/70">
              <Empty description="创建或选择一个工作流查看第四阶段 Graph 执行详情" />
            </Card>
          ) : (
            <>
              <Card
                title={<Space><NodeIndexOutlined />Graph 路径与动态路由</Space>}
                extra={<Tag color="geekblue">{selectedTemplateCode}</Tag>}
                className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
              >
                <div className="grid grid-cols-[minmax(0,1fr)_360px] gap-5">
                  <div>
                    <div className="mb-5 flex flex-wrap items-center gap-3">
                      {NODE_ORDER.map((nodeKey, index) => {
                        const meta = nodeMeta(nodeKey);
                        const step = steps.find((item) => item.nodeKey === nodeKey);
                        const active = latestSnapshot?.currentNodeKey === nodeKey || currentWorkflow.currentStep === meta.name;
                        const visited = graphPath.includes(nodeKey) || Boolean(step);
                        return (
                          <React.Fragment key={nodeKey}>
                            <div
                              className={`min-w-32 rounded-3xl border p-4 transition ${
                                active
                                  ? "border-slate-950 bg-slate-950 text-white shadow-xl"
                                  : visited
                                    ? "border-cyan-200 bg-cyan-50 text-slate-900"
                                    : "border-slate-200 bg-white text-slate-500"
                              }`}
                            >
                              <div className={`mb-3 h-2 rounded-full bg-gradient-to-r ${meta.tone}`} />
                              <div className="font-semibold">{meta.name}</div>
                              <div className="text-xs opacity-70">{meta.role}</div>
                              {step && <Tag className="mt-2" color={statusColor(step.status)}>{step.status}</Tag>}
                            </div>
                            {index < NODE_ORDER.length - 1 && (
                              <div className="hidden h-px w-8 bg-slate-300 md:block" />
                            )}
                          </React.Fragment>
                        );
                      })}
                    </div>
                    <Typography.Text type="secondary">
                      实际路径：{graphPath.length ? graphPath.map((node) => nodeMeta(node).name).join(" -> ") : "等待节点执行"}
                    </Typography.Text>
                  </div>
                  <div className="rounded-3xl bg-slate-950 p-4 text-slate-100">
                    <Space direction="vertical" className="w-full" size="middle">
                      <div>
                        <Space className="mb-3">
                          <ApartmentOutlined />
                          <Typography.Text className="!text-slate-100">Graph 模板</Typography.Text>
                        </Space>
                        <div className="mb-3 flex flex-wrap gap-2">
                          {templateCapabilityTags.map((tag) => (
                            <Tag key={tag} color="blue">{tag}</Tag>
                          ))}
                        </div>
                        <div className="flex flex-col gap-2">
                          {templates.map((template) => (
                            <button
                              key={template.code}
                              type="button"
                              className={`w-full rounded-2xl border p-3 text-left transition ${
                                selectedTemplateCode === template.code
                                  ? "border-cyan-300 bg-white/10 text-white"
                                  : "border-white/10 bg-black/10 text-slate-300 hover:border-cyan-200"
                              }`}
                              onClick={() => setTemplateCode(template.code)}
                            >
                              <div className="flex items-center justify-between gap-3">
                                <span className="font-semibold">{template.name}</span>
                                <Tag color={selectedTemplateCode === template.code ? "cyan" : "blue"}>
                                  {template.code}
                                </Tag>
                              </div>
                              <div className="mt-1 text-xs text-slate-400">
                                {template.description}
                              </div>
                            </button>
                          ))}
                        </div>
                      </div>
                      <div>
                        <Space className="mb-3">
                          <CodeOutlined />
                          <Typography.Text className="!text-slate-100">LangGraph4j Mermaid</Typography.Text>
                        </Space>
                        <pre className="max-h-72 overflow-auto whitespace-pre-wrap text-xs leading-6 text-cyan-100">
                          {selectedTemplate?.mermaid || "Graph definition loading..."}
                        </pre>
                      </div>
                      <div>
                        <Space className="mb-3">
                          <CodeOutlined />
                          <Typography.Text className="!text-slate-100">模板 JSON</Typography.Text>
                        </Space>
                        <TextArea
                          rows={12}
                          value={templateEditorValue}
                          onChange={(event) => setTemplateEditorValue(event.target.value)}
                          className="font-mono"
                        />
                        <Space className="mt-3">
                          <Button type="primary" loading={templateSaving} onClick={handleSaveTemplate}>
                            保存模板
                          </Button>
                          <Button onClick={handleResetTemplateEditor}>
                            重置
                          </Button>
                        </Space>
                      </div>
                    </Space>
                  </div>
                </div>
              </Card>

              <div className="grid grid-cols-[minmax(0,1.25fr)_minmax(360px,0.75fr)] gap-4">
                <Card
                  title={<Space><CloudSyncOutlined />实时流式输出</Space>}
                  className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
                >
                  {streamStageList.length === 0 ? (
                    <Empty description="等待节点输出" />
                  ) : (
                    <Space direction="vertical" className="w-full" size="middle">
                      {streamStageList.map((stage) => {
                        const meta = nodeMeta(stage.nodeKey);
                        return (
                          <div key={stage.nodeKey} className="rounded-[28px] border border-slate-100 bg-white p-5 shadow-sm">
                            <div className="mb-4 flex items-center justify-between gap-3">
                              <Space>
                                <Avatar className={`bg-gradient-to-br ${meta.tone}`} icon={<ExperimentOutlined />} />
                                <div>
                                  <Typography.Text strong>{stage.nodeName}</Typography.Text>
                                  <div className="text-xs text-slate-500">{meta.role}</div>
                                </div>
                              </Space>
                              <Tag color={statusColor(stage.status)}>{stage.status}</Tag>
                            </div>
                            <pre className="mb-0 whitespace-pre-wrap font-sans text-[14px] leading-7 text-slate-800">
                              {stage.content || "正在思考和执行..."}
                            </pre>
                          </div>
                        );
                      })}
                    </Space>
                  )}
                </Card>

                <Card
                  title={<Space><HistoryOutlined />Trace 时间线</Space>}
                  className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
                >
                  {traces.length === 0 ? (
                    <Empty description="暂无 Trace 记录" />
                  ) : (
                    <Timeline
                      items={traces.map((trace) => ({
                        color: traceStatusTone(trace),
                        children: (
                          <div className="rounded-2xl bg-slate-50 p-3">
                            <div className="mb-1 flex items-center justify-between gap-2">
                              <Typography.Text strong>{nodeMeta(trace.nodeKey).name}</Typography.Text>
                              <Tag color={traceStatusTone(trace)}>{trace.eventType}</Tag>
                            </div>
                            <div className="text-xs text-slate-500">
                              {trace.status} · {trace.durationMs ? `${trace.durationMs}ms` : "pending"} · {shortId(trace.traceId)}
                            </div>
                            {trace.errorMessage && (
                              <Alert
                                className="mt-2"
                                type="error"
                                message={trace.errorMessage}
                                action={(
                                  <Button
                                    size="small"
                                    icon={<ReloadOutlined />}
                                    loading={replayingNode === trace.nodeKey}
                                    onClick={() => handleReplay(trace.nodeKey)}
                                  >
                                    从此重放
                                  </Button>
                                )}
                              />
                            )}
                          </div>
                        ),
                      }))}
                    />
                  )}
                </Card>
              </div>

              <WorkflowObservabilityPanel
                observability={observability}
                loading={observabilityLoading}
              />

              {currentWorkflow.status === "WAITING_APPROVAL" && (
                <Card title="人工审批" className="border-none bg-amber-50 shadow-xl shadow-amber-100">
                  <Space direction="vertical" className="w-full" size="middle">
                    <Typography.Paragraph className="whitespace-pre-wrap">
                      {pendingApprovals.find((approval) => approval.id === currentApprovalId)?.summary
                        || latestSnapshot?.reviewComment
                        || "Reviewer 已完成复核，请确认是否进入发布阶段。"}
                    </Typography.Paragraph>
                    <TextArea
                      rows={3}
                      placeholder="审批意见。驳回时会作为 Executor Agent 重试依据。"
                      value={approvalComment}
                      onChange={(event) => setApprovalComment(event.target.value)}
                    />
                    <Space>
                      <Button type="primary" icon={<CheckCircleOutlined />} onClick={handleApprove}>通过并继续发布</Button>
                      <Button danger onClick={handleReject}>驳回并重新生成</Button>
                    </Space>
                  </Space>
                </Card>
              )}

              <Card
                title={<Space><PlayCircleOutlined />节点状态快照</Space>}
                className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
              >
                <List
                  dataSource={steps}
                  renderItem={(step) => (
                    <List.Item
                      actions={[
                        <Button
                          key="replay"
                          size="small"
                          icon={<ReloadOutlined />}
                          loading={replayingNode === step.nodeKey}
                          onClick={() => handleReplay(step.nodeKey)}
                        >
                          从此重放
                        </Button>,
                      ]}
                    >
                      <List.Item.Meta
                        avatar={<Avatar className={`bg-gradient-to-br ${nodeMeta(step.nodeKey).tone}`} />}
                        title={
                          <Space>
                            <span>{step.nodeName}</span>
                            <Tag color={statusColor(step.status)}>{step.status}</Tag>
                            {isSubGraphStep(step.nodeKey) && <Tag color="purple">子图步骤</Tag>}
                          </Space>
                        }
                        description={
                          <Space direction="vertical" className="w-full">
                            <Typography.Paragraph
                              className="mb-0"
                              ellipsis={{ rows: 3, expandable: true, symbol: "展开" }}
                            >
                              {getStepSummary(step)}
                            </Typography.Paragraph>
                            <Collapse
                              size="small"
                              ghost
                              items={[
                                {
                                  key: "snapshot",
                                  label: "查看节点状态快照",
                                  children: (
                                    <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-2xl bg-slate-950 p-4 text-xs text-slate-100">
                                      {step.outputSnapshot || step.inputSnapshot || "{}"}
                                    </pre>
                                  ),
                                },
                              ]}
                            />
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              </Card>

              <Card
                title={<Space><HistoryOutlined />Checkpoint</Space>}
                className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
              >
                {checkpoints.length === 0 ? (
                  <Empty description="暂无 checkpoint 记录" />
                ) : (
                  <List
                    dataSource={checkpoints}
                    renderItem={(checkpoint) => (
                      <List.Item>
                        <List.Item.Meta
                          title={(
                            <Space wrap>
                              <span>{checkpoint.nodeKey}</span>
                              <Tag color="blue">{checkpoint.checkpointType}</Tag>
                              <Tag>{formatDateTime(checkpoint.createdAt)}</Tag>
                            </Space>
                          )}
                          description={(
                            <Space direction="vertical" className="w-full">
                              <Typography.Text type="secondary">
                                runId: {shortId(checkpoint.runId)} · traceId: {shortId(checkpoint.traceId)}
                              </Typography.Text>
                              <Collapse
                                size="small"
                                ghost
                                items={[
                                  {
                                    key: `${checkpoint.id}-state`,
                                    label: "查看状态快照",
                                    children: (
                                      <pre className="max-h-72 overflow-auto whitespace-pre-wrap rounded-2xl bg-slate-950 p-4 text-xs text-slate-100">
                                        {checkpoint.stateSnapshot}
                                      </pre>
                                    ),
                                  },
                                  {
                                    key: `${checkpoint.id}-meta`,
                                    label: "查看元数据",
                                    children: (
                                      <pre className="max-h-56 overflow-auto whitespace-pre-wrap rounded-2xl bg-slate-950 p-4 text-xs text-slate-100">
                                        {checkpoint.metadata || "{}"}
                                      </pre>
                                    ),
                                  },
                                ]}
                              />
                            </Space>
                          )}
                        />
                      </List.Item>
                    )}
                  />
                )}
              </Card>

              <div className="grid grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] gap-4">
                <Card
                  title="知识引用与 Reviewer 复核"
                  className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
                >
                  {!latestSnapshot ? (
                    <Empty description="暂无节点快照" />
                  ) : (
                    <Space direction="vertical" className="w-full" size="middle">
                      <div>
                        <Typography.Text strong>Reviewer 结论：</Typography.Text>
                        <Tag color={latestSnapshot.review?.passed ? "green" : "orange"} className="ml-2">
                          {latestSnapshot.review?.passed ? "通过" : "待优化"}
                        </Tag>
                        {typeof latestSnapshot.review?.score === "number" && (
                          <Tag color="blue">{latestSnapshot.review.score}/100</Tag>
                        )}
                      </div>
                      <Typography.Paragraph className="whitespace-pre-wrap">
                        {latestSnapshot.reviewComment || latestSnapshot.review?.comment || "暂无复核意见"}
                      </Typography.Paragraph>
                      <List
                        size="small"
                        header="引用来源"
                        dataSource={latestSnapshot.sources || []}
                        locale={{ emptyText: "暂无引用来源" }}
                        renderItem={(source) => (
                          <List.Item>
                            <Typography.Paragraph className="mb-0">
                              <Typography.Text strong>
                                [{source.index || "-"}] {source.title || source.sourceType || "引用来源"}：
                              </Typography.Text>
                              {source.content}
                            </Typography.Paragraph>
                          </List.Item>
                        )}
                      />
                    </Space>
                  )}
                </Card>

                <Card
                  title="最终产物"
                  className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
                >
                  {artifacts.length === 0 ? (
                    <Empty description="审批通过并发布后生成产物" />
                  ) : (
                    artifacts.map((artifact) => (
                      <Card key={artifact.id} type="inner" title={artifact.title} className="mb-4 overflow-hidden">
                        <pre className="max-h-[520px] overflow-auto whitespace-pre-wrap rounded-2xl bg-slate-950 p-4 text-sm leading-7 text-slate-100">
                          {artifact.content}
                        </pre>
                      </Card>
                    ))
                  )}
                </Card>
              </div>
            </>
          )}
      </div>
    </div>
  );
};

export default WorkflowView;
