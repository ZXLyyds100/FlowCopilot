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
  return (
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
            {currentWorkflow?.input
              || "创建任务后，系统会按 Graph 模板执行 Planner、Retriever、Executor、Reviewer、Approval、Publisher，并实时展示每个节点的状态、输出与 Trace。"}
          </Typography.Paragraph>
        </div>
        <div className="rounded-3xl border border-white/15 bg-white/12 p-4 backdrop-blur">
          <Typography.Text className="!text-slate-100">Graph 进度</Typography.Text>
          <Progress
            percent={progressPercent}
            strokeColor={{ "0%": "#22d3ee", "100%": "#facc15" }}
            trailColor="rgba(255,255,255,0.16)"
          />
          <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
            <div className="rounded-2xl bg-white/10 p-3">
              <div className="text-slate-200">Trace</div>
              <div className="font-semibold text-white">
                {shortId(latestSnapshot?.traceId || traces[0]?.traceId)}
              </div>
            </div>
            <div className="rounded-2xl bg-white/10 p-3">
              <div className="text-slate-200">Retry</div>
              <div className="font-semibold text-white">{latestSnapshot?.retryCount || 0}</div>
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
}
