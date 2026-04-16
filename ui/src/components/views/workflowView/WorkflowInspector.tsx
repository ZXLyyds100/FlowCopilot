import { List, Tag, Typography } from "antd";
import type { ApprovalRecordVO, WorkflowInstanceVO } from "../../../api/api.ts";

interface WorkflowInspectorProps {
  pendingApprovals: ApprovalRecordVO[];
  workflows: WorkflowInstanceVO[];
  liveStatus: string;
  onSelectWorkflow: (workflowId: string) => void;
}

export default function WorkflowInspector({
  pendingApprovals,
  workflows,
  liveStatus,
  onSelectWorkflow,
}: WorkflowInspectorProps) {
  return (
    <div className="flex h-full flex-col gap-6 text-slate-800">
      <section>
        <Typography.Title level={5} className="!mb-2 !text-slate-900">运行摘要</Typography.Title>
        <Typography.Paragraph className="mb-0 !text-slate-700">
          {liveStatus}
        </Typography.Paragraph>
      </section>

      <section>
        <Typography.Title level={5} className="!mb-2 !text-slate-900">待审批事项</Typography.Title>
        <List
          dataSource={pendingApprovals}
          locale={{ emptyText: "暂无待审批事项" }}
          renderItem={(approval) => (
            <List.Item>
              <div className="flex w-full items-center justify-between gap-3">
                <span className="text-slate-800">{approval.title}</span>
                <Tag color="orange">{approval.status}</Tag>
              </div>
            </List.Item>
          )}
        />
      </section>

      <section>
        <Typography.Title level={5} className="!mb-2 !text-slate-900">最近工作流</Typography.Title>
        <List
          dataSource={workflows}
          locale={{ emptyText: "暂无工作流" }}
          renderItem={(workflow) => (
            <List.Item>
              <button
                type="button"
                onClick={() => onSelectWorkflow(workflow.id)}
                className="w-full rounded-xl px-2 py-1 text-left text-slate-800 transition hover:bg-slate-50 hover:text-slate-900"
              >
                {workflow.title}
              </button>
            </List.Item>
          )}
        />
      </section>
    </div>
  );
}
