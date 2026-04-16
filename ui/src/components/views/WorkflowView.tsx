import React, { startTransition, useEffect, useMemo, useRef, useState } from "react";
import { Alert, Card, Empty, message } from "antd";
import { useShellPage } from "../shell/useShellPage.ts";
import {
  approveWorkflow,
  createWorkflow,
  getApprovals,
  getWorkflow,
  getWorkflowTemplates,
  getWorkflows,
  getWorkflowTrace,
  rejectWorkflow,
  replayWorkflowFromNode,
  type ApprovalRecordVO,
  type ArtifactVO,
  type ExecutionTraceRefVO,
  type WorkflowInstanceVO,
  type WorkflowStepInstanceVO,
  type WorkflowTemplateVO,
} from "../../api/api.ts";
import { BASE_URL } from "../../api/http.ts";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";
import WorkflowCreateCard from "./workflowView/WorkflowCreateCard.tsx";
import WorkflowGraphCard from "./workflowView/WorkflowGraphCard.tsx";
import WorkflowInspector from "./workflowView/WorkflowInspector.tsx";
import WorkflowOverviewCard from "./workflowView/WorkflowOverviewCard.tsx";
import WorkflowResultsSection from "./workflowView/WorkflowResultsSection.tsx";
import WorkflowRuntimeSection from "./workflowView/WorkflowRuntimeSection.tsx";
import type {
  StreamStage,
  WorkflowMetadata,
  WorkflowSseMessage,
  WorkflowStateSnapshot,
} from "./workflowView/types.ts";
import {
  findLatestSnapshot,
  nodeMeta,
  normalizeGraphPath,
  parseJson,
  NODE_ORDER,
} from "./workflowView/utils.ts";

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
  const [artifacts, setArtifacts] = useState<ArtifactVO[]>([]);
  const [streamStages, setStreamStages] = useState<Record<string, StreamStage>>({});
  const [expandedStepSummaries, setExpandedStepSummaries] = useState<Record<string, boolean>>({});
  const [liveStatus, setLiveStatus] = useState("等待创建任务");
  const eventSourceRef = useRef<EventSource | null>(null);
  const { knowledgeBases } = useKnowledgeBases();

  const latestSnapshot = useMemo<WorkflowStateSnapshot | null>(
    () => findLatestSnapshot(steps) ?? null,
    [steps],
  );
  const workflowMetadata = useMemo(
    () => parseJson<WorkflowMetadata>(currentWorkflow?.metadata),
    [currentWorkflow?.metadata],
  );
  const selectedTemplateCode = latestSnapshot?.templateCode || workflowMetadata?.templateCode || templateCode;
  const selectedTemplate = templates.find((template) => template.code === selectedTemplateCode) || templates[0];
  const currentApprovalId =
    latestSnapshot?.approvalRecordId
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

  const streamStageList = useMemo(
    () =>
      Object.values(streamStages).sort((left, right) => {
        const leftIndex = NODE_ORDER.indexOf(left.nodeKey);
        const rightIndex = NODE_ORDER.indexOf(right.nodeKey);
        return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex);
      }),
    [streamStages],
  );

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

  const loadWorkflow = async (workflowInstanceId: string) => {
    const response = await getWorkflow(workflowInstanceId);
    setCurrentWorkflow(response.workflow);
    setSteps(response.steps);
    setArtifacts(response.artifacts);
    await refreshTrace(workflowInstanceId).catch(console.error);
  };

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
          const nodeKey = payload.nodeKey;
          setStreamStages((prev) => ({
            ...prev,
            [nodeKey]: {
              nodeKey,
              nodeName: payload.nodeName || nodeKey,
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
    setExpandedStepSummaries({});
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
      await refreshTrace(currentWorkflow.id);
      message.success(`已从 ${nodeMeta(nodeKey).name} 节点重新执行`);
    } catch (error) {
      console.error(error);
      message.error("节点重放失败");
    } finally {
      setReplayingNode(null);
    }
  };

  const toggleStepSummary = (stepId: string) => {
    setExpandedStepSummaries((prev) => ({
      ...prev,
      [stepId]: !prev[stepId],
    }));
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
        <WorkflowCreateCard
          title={title}
          taskInput={taskInput}
          knowledgeBaseId={knowledgeBaseId}
          templateCode={templateCode}
          loading={loading}
          templates={templates}
          knowledgeBases={knowledgeBases}
          onTitleChange={setTitle}
          onTaskInputChange={setTaskInput}
          onKnowledgeBaseChange={setKnowledgeBaseId}
          onTemplateCodeChange={setTemplateCode}
          onCreateWorkflow={handleCreateWorkflow}
        />

        <WorkflowOverviewCard
          currentWorkflow={currentWorkflow}
          latestSnapshot={latestSnapshot}
          progressPercent={progressPercent}
          selectedTemplate={selectedTemplate}
          traces={traces}
        />

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
            <WorkflowGraphCard
              currentStep={currentWorkflow.currentStep}
              graphPath={graphPath}
              latestSnapshot={latestSnapshot}
              selectedTemplateCode={selectedTemplateCode}
              selectedTemplateMermaid={selectedTemplate?.mermaid}
              setTemplateCode={setTemplateCode}
              steps={steps}
              templates={templates}
            />

            <WorkflowRuntimeSection
              approvalComment={approvalComment}
              currentApprovalId={currentApprovalId}
              currentWorkflowStatus={currentWorkflow.status}
              expandedStepSummaries={expandedStepSummaries}
              latestSnapshot={latestSnapshot}
              pendingApprovals={pendingApprovals}
              replayingNode={replayingNode}
              steps={steps}
              streamStageList={streamStageList}
              traces={traces}
              onApprovalCommentChange={setApprovalComment}
              onApprove={handleApprove}
              onReject={handleReject}
              onReplay={handleReplay}
              onToggleStepSummary={toggleStepSummary}
            />

            <WorkflowResultsSection artifacts={artifacts} latestSnapshot={latestSnapshot} />
          </>
        )}
      </div>
    </div>
  );
};

export default WorkflowView;
