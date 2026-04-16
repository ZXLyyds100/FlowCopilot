import { Alert, Button, Card, Collapse, Empty, Space, Tag, Typography } from "antd";
import { BranchesOutlined, LinkOutlined, RadarChartOutlined } from "@ant-design/icons";
import type {
  GetWorkflowObservabilityResponse,
  WorkflowObservationSpanVO,
} from "../../../api/api.ts";

interface WorkflowObservabilityPanelProps {
  observability: GetWorkflowObservabilityResponse | null;
  loading?: boolean;
}

const SPAN_TYPE_LABELS: Record<string, string> = {
  workflow_run: "工作流",
  node_run: "节点",
  llm_call: "模型调用",
  tool_call: "工具调用",
  retrieval_call: "检索调用",
};

function statusColor(status?: string) {
  if (status === "COMPLETED") return "green";
  if (status === "RUNNING") return "blue";
  if (status === "WAITING_APPROVAL") return "orange";
  if (status === "FAILED") return "red";
  return "default";
}

function spanTypeColor(spanType?: string) {
  if (spanType === "workflow_run") return "geekblue";
  if (spanType === "node_run") return "cyan";
  if (spanType === "llm_call") return "magenta";
  if (spanType === "tool_call") return "gold";
  if (spanType === "retrieval_call") return "green";
  return "default";
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  return new Date(value).toLocaleString();
}

function formatUsd(value?: number) {
  if (typeof value !== "number") return "未配置";
  return `$${value.toFixed(6)}`;
}

function renderTextBlock(label: string, value?: string, extraClassName = "text-slate-700") {
  if (!value) return null;
  return (
    <div>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Paragraph className={`!mb-0 whitespace-pre-wrap ${extraClassName}`}>
        {value}
      </Typography.Paragraph>
    </div>
  );
}

function renderMetric(label: string, value: string | number) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
      <Typography.Text type="secondary">{label}</Typography.Text>
      <div className="mt-2 break-all text-base font-semibold text-slate-900">{value}</div>
    </div>
  );
}

function LlmUsageBlock({ span }: { span: WorkflowObservationSpanVO }) {
  if (
    span.spanType !== "llm_call"
    && typeof span.inputTokens !== "number"
    && typeof span.outputTokens !== "number"
    && typeof span.totalTokens !== "number"
    && typeof span.estimatedCostUsd !== "number"
  ) {
    return null;
  }

  return (
    <div className="grid gap-3 md:grid-cols-2">
      <div className="rounded-2xl bg-slate-50 p-3">
        <Typography.Text type="secondary">模型信息</Typography.Text>
        <div className="mt-1 text-slate-700">{span.modelName || "未返回模型名"}</div>
        <div className="mt-1 text-xs text-slate-500">结束原因：{span.finishReason || "-"}</div>
      </div>
      <div className="rounded-2xl bg-slate-50 p-3">
        <Typography.Text type="secondary">Token / 花销</Typography.Text>
        <div className="mt-1 text-slate-700">
          输入 {span.inputTokens ?? "-"} / 输出 {span.outputTokens ?? "-"} / 总计 {span.totalTokens ?? "-"}
        </div>
        <div className="mt-1 text-xs text-slate-500">估算成本：{formatUsd(span.estimatedCostUsd)}</div>
      </div>
    </div>
  );
}

function ObservationTreeNode({ span, depth = 0 }: { span: WorkflowObservationSpanVO; depth?: number }) {
  const children = span.children || [];

  return (
    <div className={depth > 0 ? "pl-5" : ""}>
      <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <Space wrap>
            <Tag color={spanTypeColor(span.spanType)}>
              {SPAN_TYPE_LABELS[span.spanType] || span.spanType}
            </Tag>
            <Typography.Text strong>{span.name}</Typography.Text>
            {span.nodeKey && <Tag>{span.nodeKey}</Tag>}
            <Tag color={statusColor(span.status)}>{span.status}</Tag>
          </Space>
          <Typography.Text type="secondary">
            {typeof span.durationMs === "number" ? `${span.durationMs} ms` : "进行中"}
          </Typography.Text>
        </div>

        <div className="mt-3 grid gap-3 text-sm md:grid-cols-2">
          <div className="rounded-2xl bg-slate-50 p-3">
            <Typography.Text type="secondary">Trace / Span</Typography.Text>
            <div className="mt-1 break-all font-mono text-xs text-slate-700">{span.traceId}</div>
            <div className="mt-1 break-all font-mono text-xs text-slate-500">{span.spanId}</div>
          </div>
          <div className="rounded-2xl bg-slate-50 p-3">
            <Typography.Text type="secondary">开始 / 结束</Typography.Text>
            <div className="mt-1 text-slate-700">{formatDateTime(span.startedAt)}</div>
            <div className="mt-1 text-slate-500">{formatDateTime(span.endedAt)}</div>
          </div>
        </div>

        <div className="mt-3 space-y-3">
          <LlmUsageBlock span={span} />
          {renderTextBlock("输入摘要", span.inputSummary)}
          {renderTextBlock("输出摘要", span.outputSummary)}
          {renderTextBlock("错误信息", span.errorMessage, "text-red-600")}
          {span.responseId && (
            <div>
              <Typography.Text type="secondary">响应 ID</Typography.Text>
              <Typography.Paragraph className="!mb-0 break-all font-mono text-xs text-slate-600">
                {span.responseId}
              </Typography.Paragraph>
            </div>
          )}
          {span.attributesJson && span.attributesJson !== "{}" && (
            <Collapse
              size="small"
              ghost
              items={[
                {
                  key: `${span.spanId}-attributes`,
                  label: "查看属性",
                  children: (
                    <pre className="max-h-56 overflow-auto whitespace-pre-wrap rounded-2xl bg-slate-950 p-4 text-xs text-slate-100">
                      {span.attributesJson}
                    </pre>
                  ),
                },
              ]}
            />
          )}
        </div>
      </div>

      {children.length > 0 && (
        <div className="mt-3 space-y-3 border-l-2 border-slate-100">
          {children.map((child) => (
            <ObservationTreeNode key={child.spanId} span={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function WorkflowObservabilityPanel({
  observability,
  loading = false,
}: WorkflowObservabilityPanelProps) {
  const summary = observability?.summary;
  const externalTrace = observability?.externalTrace;
  const spans = observability?.spans || [];
  const exporterStatus = summary?.exporterStatus || "not_instrumented";
  const providerName = externalTrace?.provider === "langsmith" ? "LangSmith" : (externalTrace?.provider || "local");

  return (
    <Card
      title={<Space><RadarChartOutlined />可观测性</Space>}
      loading={loading}
      className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
    >
      {!observability ? (
        <Empty description="选择一个工作流后，这里会展示可观测树和外部 Trace 状态。" />
      ) : (
        <Space direction="vertical" className="w-full" size="large">
          <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-5">
            {renderMetric("总 Span 数", summary?.totalSpans ?? 0)}
            {renderMetric("工作流运行", summary?.workflowRuns ?? 0)}
            {renderMetric("节点运行", summary?.nodeRuns ?? 0)}
            {renderMetric("模型调用", summary?.llmCalls ?? 0)}
            {renderMetric("检索调用", summary?.retrievalCalls ?? 0)}
            {renderMetric("输入 Token", summary?.inputTokens ?? "-")}
            {renderMetric("输出 Token", summary?.outputTokens ?? "-")}
            {renderMetric("总 Token", summary?.totalTokens ?? "-")}
            {renderMetric("估算花销", formatUsd(summary?.estimatedCostUsd))}
            {renderMetric("错误数", summary?.errorCount ?? 0)}
          </div>

          <div className="grid gap-4 lg:grid-cols-[minmax(0,1.2fr)_minmax(260px,0.8fr)]">
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <Space className="mb-3">
                <BranchesOutlined />
                <Typography.Text strong>导出状态</Typography.Text>
                <Tag color={exporterStatus === "error" ? "red" : exporterStatus === "enabled" ? "green" : "default"}>
                  {exporterStatus}
                </Tag>
              </Space>
              <div className="grid gap-3 md:grid-cols-2">
                <div>
                  <Typography.Text type="secondary">Provider</Typography.Text>
                  <div className="mt-1 text-slate-700">{providerName}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">项目</Typography.Text>
                  <div className="mt-1 text-slate-700">{externalTrace?.projectName || "未配置"}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">外部 Trace 状态</Typography.Text>
                  <div className="mt-1 text-slate-700">{externalTrace?.status || "not_instrumented"}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">最新 Trace</Typography.Text>
                  <div className="mt-1 break-all font-mono text-xs text-slate-700">
                    {externalTrace?.traceId || summary?.latestTraceId || "-"}
                  </div>
                </div>
              </div>
              {externalTrace?.lastErrorMessage && (
                <Alert className="mt-3" type="error" showIcon message={externalTrace.lastErrorMessage} />
              )}
            </div>

            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <Typography.Text strong>外部 Trace</Typography.Text>
              <div className="mt-3 space-y-2 text-sm">
                <div>
                  <Typography.Text type="secondary">Trace 链接</Typography.Text>
                  <div className="mt-2">
                    {externalTrace?.url ? (
                      <Button type="link" href={externalTrace.url} target="_blank" icon={<LinkOutlined />}>
                        打开外部 Trace
                      </Button>
                    ) : (
                      <Typography.Text type="secondary">当前未配置可跳转的外部 Trace 链接。</Typography.Text>
                    )}
                  </div>
                </div>
                <div>
                  <Typography.Text type="secondary">说明</Typography.Text>
                  <Typography.Paragraph className="!mb-0 mt-1 text-slate-600">
                    当前面板继续保留平台内树形观测，外部系统只作为补充跳转入口，不替换原有 Trace 时间线。
                  </Typography.Paragraph>
                </div>
              </div>
            </div>
          </div>

          {spans.length === 0 ? (
            <Alert
              type="info"
              showIcon
              message="当前工作流还没有可观测 Span 数据。"
              description="历史工作流仍可在原有 Trace 时间线里查看；新的已埋点运行会在这里显示为树形 Span。"
            />
          ) : (
            <Space direction="vertical" className="w-full" size="middle">
              {spans.map((span) => (
                <ObservationTreeNode key={span.spanId} span={span} />
              ))}
            </Space>
          )}
        </Space>
      )}
    </Card>
  );
}
