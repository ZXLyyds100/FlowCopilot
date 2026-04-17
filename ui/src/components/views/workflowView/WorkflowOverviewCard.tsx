import { Badge, Card, Progress, Space, Tag, Typography } from "antd";
import type {
  ExecutionTraceRefVO,
  WorkflowInstanceVO,
  WorkflowTemplateVO,
} from "../../../api/api.ts";
import type { WorkflowStateSnapshot } from "./types.ts";
import { shortId, statusColor } from "./utils.ts";

interface WorkflowOverviewCardProps {
  currentWorkflow: WorkflowInstanceVO | null;
  latestSnapshot: WorkflowStateSnapshot | null;
  progressPercent: number;
  selectedTemplate?: WorkflowTemplateVO;
  traces: ExecutionTraceRefVO[];
}

export default function WorkflowOverviewCard({
  currentWorkflow,
  latestSnapshot,
  progressPercent,
  selectedTemplate,
  traces,
}: WorkflowOverviewCardProps) {
  const currentNodeLabel = latestSnapshot?.currentNodeKey || currentWorkflow?.currentStep || "等待执行";

  return (
    <Card
      title={(
        <Space>
          <Badge status={currentWorkflow?.status === "RUNNING" ? "processing" : "default"} />
          <span>运行概览</span>
        </Space>
      )}
      extra={(
        <Space size="small">
          <Typography.Text className="!text-slate-500">
            {selectedTemplate?.name || "Graph 工作流"}
          </Typography.Text>
          <Tag color={statusColor(currentWorkflow?.status)}>{currentWorkflow?.status || "IDLE"}</Tag>
        </Space>
      )}
      className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
    >
      <div className="grid grid-cols-[minmax(0,1fr)_320px] gap-5">
        <div>
          <Typography.Title level={3} className="!mb-3 !text-slate-900">
            {currentWorkflow?.title || "实时 Agent Graph 工作台"}
          </Typography.Title>
          <Typography.Paragraph className="!mb-5 !text-slate-600">
            {currentWorkflow?.input
              || "创建任务后，系统会按 Graph 模板执行 Planner、Retriever、Executor、Reviewer、Approval、Publisher，并实时展示每个节点的状态、输出与 Trace。"}
          </Typography.Paragraph>
          <div className="grid grid-cols-3 gap-3">
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">当前节点</div>
              <div className="mt-2 text-base font-semibold text-slate-900">{currentNodeLabel}</div>
            </div>
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">Trace</div>
              <div className="mt-2 text-base font-semibold text-slate-900">
                {shortId(latestSnapshot?.traceId || traces[0]?.traceId)}
              </div>
            </div>
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="text-sm text-slate-500">Retry</div>
              <div className="mt-2 text-base font-semibold text-slate-900">{latestSnapshot?.retryCount || 0}</div>
            </div>
          </div>
        </div>

        <div className="rounded-3xl bg-slate-950 p-5 text-slate-100">
          <Typography.Text className="!text-slate-100">执行进度</Typography.Text>
          <Progress
            percent={progressPercent}
            strokeColor={{ "0%": "#22d3ee", "100%": "#facc15" }}
            railColor="rgba(255,255,255,0.12)"
          />
          <div className="mt-4 rounded-2xl border border-white/10 bg-white/5 p-4">
            <div className="mb-2 flex items-center justify-between gap-3">
              <Typography.Text className="!text-slate-100">模板视角</Typography.Text>
              <Tag color="cyan">{selectedTemplate?.code || "default"}</Tag>
            </div>
            <Typography.Paragraph className="!mb-0 !text-slate-300">
              {selectedTemplate?.description || "等待选择 Graph 模板并启动执行。"}
            </Typography.Paragraph>
          </div>
        </div>
      </div>
    </Card>
  );
}
