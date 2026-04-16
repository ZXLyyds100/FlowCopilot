import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Collapse,
  Empty,
  Input,
  List,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import {
  approveWorkflow,
  createWorkflow,
  getApprovals,
  getWorkflow,
  getWorkflows,
  rejectWorkflow,
  type ApprovalRecordVO,
  type ArtifactVO,
  type WorkflowInstanceVO,
  type WorkflowStepInstanceVO,
} from "../../api/api.ts";
import { BASE_URL } from "../../api/http.ts";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";

const { TextArea } = Input;

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

function parseSnapshot(snapshot?: string): WorkflowStateSnapshot | null {
  if (!snapshot) return null;
  try {
    return JSON.parse(snapshot) as WorkflowStateSnapshot;
  } catch {
    return null;
  }
}

function findLatestSnapshot(steps: WorkflowStepInstanceVO[]) {
  return [...steps]
    .reverse()
    .map((step) => parseSnapshot(step.outputSnapshot))
    .find((snapshot): snapshot is WorkflowStateSnapshot => Boolean(snapshot));
}

function getStepSummary(step: WorkflowStepInstanceVO) {
  const snapshot = parseSnapshot(step.outputSnapshot);
  if (!snapshot) return step.errorMessage || step.nodeKey;
  if (step.nodeKey === "planner") return snapshot.plan || snapshot.taskType || "已生成结构化计划";
  if (step.nodeKey === "retriever") return `已整理 ${snapshot.sources?.length || 0} 条引用来源`;
  if (step.nodeKey === "executor") return snapshot.draft || snapshot.draftResult || "已生成初稿";
  if (step.nodeKey === "reviewer") return snapshot.reviewComment || snapshot.review?.comment || "已完成质量复核";
  if (step.nodeKey === "approval") return `等待审批：${snapshot.approvalRecordId || "-"}`;
  if (step.nodeKey === "publish") return "已发布最终 Markdown 产物";
  return step.nodeKey;
}

const WorkflowView: React.FC = () => {
  const [taskInput, setTaskInput] = useState("");
  const [title, setTitle] = useState("");
  const [knowledgeBaseId, setKnowledgeBaseId] = useState<string | undefined>();
  const [approvalComment, setApprovalComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [workflows, setWorkflows] = useState<WorkflowInstanceVO[]>([]);
  const [pendingApprovals, setPendingApprovals] = useState<ApprovalRecordVO[]>([]);
  const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowInstanceVO | null>(null);
  const [steps, setSteps] = useState<WorkflowStepInstanceVO[]>([]);
  const [artifacts, setArtifacts] = useState<ArtifactVO[]>([]);
  const [streamStages, setStreamStages] = useState<Record<string, StreamStage>>({});
  const [liveStatus, setLiveStatus] = useState("等待创建任务");
  const eventSourceRef = useRef<EventSource | null>(null);
  const { knowledgeBases } = useKnowledgeBases();

  const latestSnapshot = useMemo(() => findLatestSnapshot(steps), [steps]);
  const currentApprovalId = latestSnapshot?.approvalRecordId
    || pendingApprovals.find((approval) => approval.workflowInstanceId === currentWorkflow?.id)?.id;

  const refreshWorkflows = async () => {
    const response = await getWorkflows();
    setWorkflows(response.workflows);
  };

  const refreshApprovals = async () => {
    const response = await getApprovals();
    setPendingApprovals(response.approvals);
  };

  const loadWorkflow = async (workflowInstanceId: string) => {
    const response = await getWorkflow(workflowInstanceId);
    setCurrentWorkflow(response.workflow);
    setSteps(response.steps);
    setArtifacts(response.artifacts);
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
    setLiveStatus("正在创建工作流");
    try {
      const response = await createWorkflow({
        title: title.trim() || undefined,
        input: taskInput.trim(),
        knowledgeBaseId,
      });
      setCurrentWorkflow(response.workflow);
      connectWorkflowStream(response.workflowInstanceId);
      await refreshWorkflows();
      await loadWorkflow(response.workflowInstanceId);
      message.success("工作流已启动，正在实时执行");
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

  const streamStageList = Object.values(streamStages);

  return (
    <div className="h-full overflow-auto bg-[#f7f3ea] p-6">
      <div className="mx-auto grid max-w-7xl grid-cols-[390px_minmax(0,1fr)] gap-6">
        <div className="space-y-4">
          <Card title="FlowCopilot 任务入口" className="shadow-sm">
            <Space direction="vertical" className="w-full" size="middle">
              <Input
                placeholder="可选：任务标题"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
              <TextArea
                rows={7}
                placeholder="输入复杂任务，例如：基于知识库整理一份项目答辩介绍"
                value={taskInput}
                onChange={(event) => setTaskInput(event.target.value)}
              />
              <Select
                allowClear
                placeholder="可选：选择知识库增强"
                value={knowledgeBaseId}
                onChange={setKnowledgeBaseId}
                options={knowledgeBases.map((kb) => ({
                  label: kb.name,
                  value: kb.knowledgeBaseId,
                }))}
              />
              <Button type="primary" loading={loading} onClick={handleCreateWorkflow} block>
                启动第三阶段实时工作流
              </Button>
            </Space>
          </Card>

          <Card title="待审批事项" className="shadow-sm">
            <List
              size="small"
              dataSource={pendingApprovals}
              locale={{ emptyText: "暂无待审批事项" }}
              renderItem={(approval) => (
                <List.Item
                  className="cursor-pointer rounded px-2 hover:bg-stone-100"
                  onClick={() => {
                    connectWorkflowStream(approval.workflowInstanceId);
                    loadWorkflow(approval.workflowInstanceId).catch(console.error);
                  }}
                >
                  <List.Item.Meta
                    title={<span>{approval.title}</span>}
                    description={<Tag color="orange">{approval.status}</Tag>}
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card title="最近工作流" className="shadow-sm">
            <List
              dataSource={workflows}
              locale={{ emptyText: "暂无工作流" }}
              renderItem={(workflow) => (
                <List.Item
                  className="cursor-pointer rounded px-2 hover:bg-stone-100"
                  onClick={() => {
                    connectWorkflowStream(workflow.id);
                    loadWorkflow(workflow.id).catch(console.error);
                  }}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <span>{workflow.title}</span>
                        <Tag color={statusColor(workflow.status)}>{workflow.status}</Tag>
                      </Space>
                    }
                    description={workflow.currentStep}
                  />
                </List.Item>
              )}
            />
          </Card>
        </div>

        <div className="min-w-0 space-y-4">
          <Card className="border-none bg-gradient-to-br from-[#1d241f] to-[#394036] text-white shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <Typography.Title level={3} className="!mb-2 !text-white">
                  {currentWorkflow?.title || "实时 Agent 工作台"}
                </Typography.Title>
                <Typography.Paragraph className="!mb-0 !text-stone-200">
                  {currentWorkflow?.input || "创建任务后，Planner、Retriever、Executor、Reviewer、Approval、Reporter 会在这里实时输出。"}
                </Typography.Paragraph>
              </div>
              <Tag color={statusColor(currentWorkflow?.status)}>{currentWorkflow?.status || "IDLE"}</Tag>
            </div>
          </Card>

          <Alert
            type={currentWorkflow?.status === "FAILED" ? "error" : "info"}
            showIcon
            message={liveStatus}
            description={currentWorkflow?.currentStep ? `当前节点：${currentWorkflow.currentStep}` : "等待工作流事件"}
          />

          {!currentWorkflow ? (
            <Card>
              <Empty description="创建或选择一个工作流查看实时执行详情" />
            </Card>
          ) : (
            <>
              <Card title="实时流式输出" className="shadow-sm">
                {streamStageList.length === 0 ? (
                  <Empty description="等待节点输出" />
                ) : (
                  <Space direction="vertical" className="w-full" size="middle">
                    {streamStageList.map((stage) => (
                      <div key={stage.nodeKey} className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-100">
                        <Space className="mb-3">
                          <Tag color={statusColor(stage.status)}>{stage.status}</Tag>
                          <Typography.Text strong>{stage.nodeName}</Typography.Text>
                        </Space>
                        <pre className="mb-0 whitespace-pre-wrap font-sans text-[14px] leading-7 text-stone-800">
                          {stage.content || "正在思考和执行..."}
                        </pre>
                      </div>
                    ))}
                  </Space>
                )}
              </Card>

              {currentWorkflow.status === "WAITING_APPROVAL" && (
                <Card title="人工审批" className="border-orange-200 shadow-sm">
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
                      <Button type="primary" onClick={handleApprove}>通过并继续发布</Button>
                      <Button danger onClick={handleReject}>驳回并重新生成</Button>
                    </Space>
                  </Space>
                </Card>
              )}

              <Card title="执行状态可视化" className="shadow-sm">
                <List
                  dataSource={steps}
                  renderItem={(step) => (
                    <List.Item>
                      <List.Item.Meta
                        title={
                          <Space>
                            <span>{step.nodeName}</span>
                            <Tag color={statusColor(step.status)}>{step.status}</Tag>
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
                                    <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded bg-slate-900 p-3 text-xs text-slate-100">
                                      {step.outputSnapshot || "{}"}
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

              <Card title="知识引用与 Reviewer 复核" className="shadow-sm">
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

              <Card title="最终产物" className="shadow-sm">
                {artifacts.length === 0 ? (
                  <Empty description="审批通过并发布后生成产物" />
                ) : (
                  artifacts.map((artifact) => (
                    <Card key={artifact.id} type="inner" title={artifact.title} className="mb-4">
                      <pre className="whitespace-pre-wrap rounded bg-slate-900 p-4 text-sm text-slate-100">
                        {artifact.content}
                      </pre>
                    </Card>
                  ))
                )}
              </Card>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default WorkflowView;
