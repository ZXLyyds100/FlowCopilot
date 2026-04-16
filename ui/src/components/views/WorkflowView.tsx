import React, { useEffect, useState } from "react";
import { Button, Card, Collapse, Empty, Input, List, Select, Space, Tag, Typography, message } from "antd";
import {
  createWorkflow,
  getWorkflow,
  getWorkflows,
  type ArtifactVO,
  type WorkflowInstanceVO,
  type WorkflowStepInstanceVO,
} from "../../api/api.ts";
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
  finalOutput?: string;
}

function statusColor(status?: string) {
  if (status === "COMPLETED") return "green";
  if (status === "RUNNING") return "blue";
  if (status === "FAILED") return "red";
  if (status === "CREATED" || status === "PENDING") return "default";
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

function getStepSummary(step: WorkflowStepInstanceVO) {
  const snapshot = parseSnapshot(step.outputSnapshot);
  if (!snapshot) return step.errorMessage || step.nodeKey;
  if (step.nodeKey === "planner") return snapshot.taskType || "已生成结构化计划";
  if (step.nodeKey === "retriever") return `已整理 ${snapshot.sources?.length || 0} 条引用来源`;
  if (step.nodeKey === "executor") return snapshot.draft || snapshot.draftResult || "已生成初稿";
  if (step.nodeKey === "reviewer") return snapshot.reviewComment || snapshot.review?.comment || "已完成质量复核";
  if (step.nodeKey === "publish") return "已发布最终 Markdown 产物";
  return step.nodeKey;
}

function findLatestSnapshot(steps: WorkflowStepInstanceVO[]) {
  return [...steps]
    .reverse()
    .map((step) => parseSnapshot(step.outputSnapshot))
    .find((snapshot): snapshot is WorkflowStateSnapshot => Boolean(snapshot));
}

const WorkflowView: React.FC = () => {
  const [taskInput, setTaskInput] = useState("");
  const [title, setTitle] = useState("");
  const [knowledgeBaseId, setKnowledgeBaseId] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [workflows, setWorkflows] = useState<WorkflowInstanceVO[]>([]);
  const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowInstanceVO | null>(null);
  const [steps, setSteps] = useState<WorkflowStepInstanceVO[]>([]);
  const [artifacts, setArtifacts] = useState<ArtifactVO[]>([]);
  const { knowledgeBases } = useKnowledgeBases();

  const refreshWorkflows = async () => {
    const response = await getWorkflows();
    setWorkflows(response.workflows);
  };

  const loadWorkflow = async (workflowInstanceId: string) => {
    const response = await getWorkflow(workflowInstanceId);
    setCurrentWorkflow(response.workflow);
    setSteps(response.steps);
    setArtifacts(response.artifacts);
  };

  useEffect(() => {
    refreshWorkflows().catch((error) => {
      console.error(error);
    });
  }, []);

  const handleCreateWorkflow = async () => {
    if (!taskInput.trim()) {
      message.warning("请输入要执行的任务");
      return;
    }
    setLoading(true);
    try {
      const response = await createWorkflow({
        title: title.trim() || undefined,
        input: taskInput.trim(),
        knowledgeBaseId,
      });
      await refreshWorkflows();
      await loadWorkflow(response.workflowInstanceId);
      message.success("工作流执行完成");
    } catch (error) {
      console.error(error);
      message.error("工作流执行失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-full overflow-auto bg-slate-50 p-6">
      <div className="mx-auto flex max-w-7xl gap-6">
        <div className="w-[420px] shrink-0 space-y-4">
          <Card title="FlowCopilot 任务入口">
            <Space direction="vertical" className="w-full" size="middle">
              <Input
                placeholder="可选：任务标题"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
              <TextArea
                rows={6}
                placeholder="输入一个复杂任务，例如：基于知识库整理一份项目分析摘要"
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
                创建并执行第二阶段协作工作流
              </Button>
            </Space>
          </Card>

          <Card title="最近工作流">
            <List
              dataSource={workflows}
              locale={{ emptyText: "暂无工作流" }}
              renderItem={(workflow) => (
                <List.Item
                  className="cursor-pointer rounded px-2 hover:bg-slate-100"
                  onClick={() => loadWorkflow(workflow.id)}
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

        <div className="min-w-0 flex-1 space-y-4">
          {!currentWorkflow ? (
            <Card>
              <Empty description="创建或选择一个工作流查看执行详情" />
            </Card>
          ) : (
            (() => {
              const latestSnapshot = findLatestSnapshot(steps);
              return (
            <>
              <Card
                title={currentWorkflow.title}
                extra={<Tag color={statusColor(currentWorkflow.status)}>{currentWorkflow.status}</Tag>}
              >
                <Typography.Paragraph>
                  <Typography.Text strong>任务输入：</Typography.Text>
                  {currentWorkflow.input}
                </Typography.Paragraph>
                <Typography.Paragraph>
                  <Typography.Text strong>当前节点：</Typography.Text>
                  {currentWorkflow.currentStep || "-"}
                </Typography.Paragraph>
                <Typography.Paragraph>
                  <Typography.Text strong>任务类型：</Typography.Text>
                  {latestSnapshot?.taskType || "-"}
                </Typography.Paragraph>
              </Card>

              <Card title="节点执行记录">
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

              <Card title="知识引用与 Reviewer 复核">
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

              <Card title="产物">
                {artifacts.length === 0 ? (
                  <Empty description="暂无产物" />
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
              );
            })()
          )}
        </div>
      </div>
    </div>
  );
};

export default WorkflowView;
