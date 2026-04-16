import { Card, Empty, List, Space, Tag, Typography } from "antd";
import type { ArtifactVO } from "../../../api/api.ts";
import WorkflowMarkdown from "./WorkflowMarkdown.tsx";
import type { WorkflowStateSnapshot } from "./types.ts";

interface WorkflowResultsSectionProps {
  artifacts: ArtifactVO[];
  latestSnapshot: WorkflowStateSnapshot | null;
}

export default function WorkflowResultsSection({
  artifacts,
  latestSnapshot,
}: WorkflowResultsSectionProps) {
  return (
    <div className="grid grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] gap-4">
      <Card
        title="知识引用与 Reviewer 复核"
        className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
      >
        {!latestSnapshot ? (
          <Empty description="暂无节点快照" />
        ) : (
          <Space direction="vertical" className="w-full" size="middle">
            <div>
              <Typography.Text className="!text-slate-900" strong>
                Reviewer 结论：
              </Typography.Text>
              <Tag color={latestSnapshot.review?.passed ? "green" : "orange"} className="ml-2">
                {latestSnapshot.review?.passed ? "通过" : "待优化"}
              </Tag>
              {typeof latestSnapshot.review?.score === "number" && (
                <Tag color="blue">{latestSnapshot.review.score}/100</Tag>
              )}
            </div>
            <WorkflowMarkdown
              className="text-slate-700"
              content={latestSnapshot.reviewComment || latestSnapshot.review?.comment}
              placeholder="暂无复核意见"
            />
            <List
              size="small"
              header="引用来源"
              dataSource={latestSnapshot.sources || []}
              locale={{ emptyText: "暂无引用来源" }}
              renderItem={(source) => (
                <List.Item>
                  <Typography.Paragraph className="mb-0 !text-slate-700">
                    <Typography.Text className="!text-slate-900" strong>
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
        className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
      >
        {artifacts.length === 0 ? (
          <Empty description="审批通过并发布后生成产物" />
        ) : (
          artifacts.map((artifact) => (
            <Card
              key={artifact.id}
              type="inner"
              title={artifact.title}
              className="mb-4 overflow-hidden [&_.ant-card-head-title]:text-slate-900"
            >
              <WorkflowMarkdown
                className="max-h-[520px] overflow-auto rounded-2xl bg-slate-950 p-4 text-sm leading-8 text-slate-100 [&_code]:bg-white/10 [&_pre]:bg-transparent [&_pre]:p-0 [&_a]:text-cyan-300"
                content={artifact.content}
              />
            </Card>
          ))
        )}
      </Card>
    </div>
  );
}
