import {
  CheckCircleOutlined,
  CloudSyncOutlined,
  ExperimentOutlined,
  HistoryOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import {
  Alert,
  Avatar,
  Button,
  Card,
  Collapse,
  Empty,
  Input,
  List,
  Space,
  Tag,
  Timeline,
  Typography,
} from "antd";
import type {
  ApprovalRecordVO,
  ExecutionTraceRefVO,
  WorkflowExecutionCheckpointVO,
  WorkflowStepInstanceVO,
} from "../../../api/api.ts";
import WorkflowMarkdown from "./WorkflowMarkdown.tsx";
import type { StreamStage, WorkflowStateSnapshot } from "./types.ts";
import {
  getStepSummary,
  isSubGraphStep,
  nodeMeta,
  shortId,
  statusColor,
  traceStatusTone,
} from "./utils.ts";

const { TextArea } = Input;

interface WorkflowRuntimeSectionProps {
  approvalComment: string;
  checkpoints: WorkflowExecutionCheckpointVO[];
  currentApprovalId?: string;
  currentWorkflowStatus?: string;
  expandedStepSummaries: Record<string, boolean>;
  latestSnapshot: WorkflowStateSnapshot | null;
  pendingApprovals: ApprovalRecordVO[];
  replayingNode: string | null;
  steps: WorkflowStepInstanceVO[];
  streamStageList: StreamStage[];
  traces: ExecutionTraceRefVO[];
  onApprovalCommentChange: (value: string) => void;
  onApprove: () => void;
  onReject: () => void;
  onReplay: (nodeKey: string) => void;
  onToggleStepSummary: (stepId: string) => void;
}

export default function WorkflowRuntimeSection({
  approvalComment,
  checkpoints,
  currentApprovalId,
  currentWorkflowStatus,
  expandedStepSummaries,
  latestSnapshot,
  pendingApprovals,
  replayingNode,
  steps,
  streamStageList,
  traces,
  onApprovalCommentChange,
  onApprove,
  onReject,
  onReplay,
  onToggleStepSummary,
}: WorkflowRuntimeSectionProps) {
  const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString() : "-");

  return (
    <>
      <div className="grid grid-cols-[minmax(0,1.25fr)_minmax(360px,0.75fr)] gap-4">
        <Card
          title={<Space><CloudSyncOutlined />实时流式输出</Space>}
          className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
        >
          {streamStageList.length === 0 ? (
            <Empty description="等待节点输出" />
          ) : (
            <Space direction="vertical" className="w-full" size="middle">
              {streamStageList.map((stage) => {
                const meta = nodeMeta(stage.nodeKey);
                return (
                  <div
                    key={stage.nodeKey}
                    className="rounded-[28px] border border-slate-100 bg-white p-5 shadow-sm"
                  >
                    <div className="mb-4 flex items-center justify-between gap-3">
                      <Space>
                        <Avatar className={`bg-gradient-to-br ${meta.tone}`} icon={<ExperimentOutlined />} />
                        <div>
                          <Typography.Text className="!text-slate-900" strong>
                            {stage.nodeName}
                          </Typography.Text>
                          <div className="text-xs text-slate-600">{meta.role}</div>
                        </div>
                      </Space>
                      <Tag color={statusColor(stage.status)}>{stage.status}</Tag>
                    </div>
                    <WorkflowMarkdown
                      className="mb-0 text-[14px] leading-8 text-slate-800"
                      content={stage.content}
                      placeholder="正在思考和执行..."
                    />
                  </div>
                );
              })}
            </Space>
          )}
        </Card>

        <Card
          title={<Space><HistoryOutlined />Trace 时间线</Space>}
          className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
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
                      <Typography.Text className="!text-slate-900" strong>
                        {nodeMeta(trace.nodeKey).name}
                      </Typography.Text>
                      <Tag color={traceStatusTone(trace)}>{trace.eventType}</Tag>
                    </div>
                    <div className="text-xs text-slate-600">
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
                            onClick={() => onReplay(trace.nodeKey)}
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

      {currentWorkflowStatus === "WAITING_APPROVAL" && (
        <Card
          title="人工审批"
          className="border-none bg-amber-50 text-slate-800 shadow-xl shadow-amber-100 [&_.ant-card-head-title]:text-amber-950"
        >
          <Space direction="vertical" className="w-full" size="middle">
            <WorkflowMarkdown
              className="text-slate-800"
              content={
                pendingApprovals.find((approval) => approval.id === currentApprovalId)?.summary
                || latestSnapshot?.reviewComment
              }
              placeholder="Reviewer 已完成复核，请确认是否进入发布阶段。"
            />
            <TextArea
              rows={3}
              placeholder="审批意见。驳回时会作为 Executor Agent 重试依据。"
              value={approvalComment}
              onChange={(event) => onApprovalCommentChange(event.target.value)}
            />
            <Space>
              <Button type="primary" icon={<CheckCircleOutlined />} onClick={onApprove}>
                通过并继续发布
              </Button>
              <Button danger onClick={onReject}>驳回并重新生成</Button>
            </Space>
          </Space>
        </Card>
      )}

      <Card
        title={<Space><PlayCircleOutlined />节点状态快照</Space>}
        className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
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
                  onClick={() => onReplay(step.nodeKey)}
                >
                  从此重放
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={<Avatar className={`bg-gradient-to-br ${nodeMeta(step.nodeKey).tone}`} />}
                title={(
                  <Space>
                    <span>{step.nodeName}</span>
                    <Tag color={statusColor(step.status)}>{step.status}</Tag>
                    {isSubGraphStep(step.nodeKey) && <Tag color="purple">子图步骤</Tag>}
                  </Space>
                )}
                description={(
                  <Space direction="vertical" className="w-full">
                    <div className="w-full">
                      <div
                        className={`relative overflow-hidden text-slate-700 ${
                          expandedStepSummaries[step.id] ? "" : "max-h-28"
                        }`}
                      >
                        <WorkflowMarkdown content={getStepSummary(step)} />
                        {!expandedStepSummaries[step.id] && (
                          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-10 bg-gradient-to-t from-white via-white/90 to-transparent" />
                        )}
                      </div>
                      <Button
                        type="link"
                        size="small"
                        className="mt-1 px-0"
                        onClick={() => onToggleStepSummary(step.id)}
                      >
                        {expandedStepSummaries[step.id] ? "收起" : "展开"}
                      </Button>
                    </div>
                    <Collapse
                      size="small"
                      ghost
                      items={[
                        {
                          key: "snapshot",
                          label: "查看节点状态快照",
                          children: (
                            <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-2xl bg-slate-950 p-4 text-xs leading-6 text-slate-100">
                              {step.outputSnapshot || step.inputSnapshot || "{}"}
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
      </Card>

      <Card
        title={<Space><HistoryOutlined />Checkpoint</Space>}
        className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
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
    </>
  );
}
