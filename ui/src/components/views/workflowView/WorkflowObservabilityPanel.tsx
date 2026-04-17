import { Alert, Card, Collapse, Empty, Space, Tag, Typography } from "antd";
import { RadarChartOutlined } from "@ant-design/icons";
import type {
  GetWorkflowObservabilityResponse,
  WorkflowObservationSpanVO,
} from "../../../api/api.ts";

interface WorkflowObservabilityPanelProps {
  observability: GetWorkflowObservabilityResponse | null;
  loading?: boolean;
}

type StageKey = "knowledge_retrieval" | "vectorization" | "vector_search";

type DiagnosticCard = {
  key: StageKey;
  title: string;
  description: string;
  totalDurationMs: number;
  count: number;
  maxDurationMs: number;
  detailLines: string[];
  isSlowest: boolean;
  hasData: boolean;
};

type TimeRange = {
  startMs: number;
  endMs: number;
};

const SPAN_TYPE_LABELS: Record<string, string> = {
  workflow_run: "工作流",
  node_run: "节点",
  llm_call: "模型调用",
  tool_call: "工具调用",
  retrieval_call: "检索调用",
};

const STAGE_META: Record<
  StageKey,
  {
    title: string;
    description: string;
  }
> = {
  knowledge_retrieval: {
    title: "知识库检索",
    description: "展示检索总链路耗时，方便判断知识库阶段是否是瓶颈。",
  },
  vectorization: {
    title: "向量化",
    description: "展示查询向量生成耗时，便于判断 embedding 侧开销。",
  },
  vector_search: {
    title: "向量检索",
    description: "展示向量库检索耗时，便于判断索引命中与召回效率。",
  },
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

function formatDuration(value?: number) {
  if (typeof value !== "number" || Number.isNaN(value)) return "暂无数据";
  if (value < 1000) return `${value} ms`;
  return `${(value / 1000).toFixed(value >= 10_000 ? 1 : 2)} s`;
}

function parseAttributes(attributesJson?: string): Record<string, unknown> {
  if (!attributesJson || attributesJson === "{}") return {};

  try {
    const parsed = JSON.parse(attributesJson) as Record<string, unknown>;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function readString(
  attributes: Record<string, unknown>,
  ...keys: string[]
): string | undefined {
  for (const key of keys) {
    const value = attributes[key];
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return undefined;
}

function readNumber(
  attributes: Record<string, unknown>,
  ...keys: string[]
): number | undefined {
  for (const key of keys) {
    const value = attributes[key];
    if (typeof value === "number" && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === "string" && value.trim()) {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }
  }
  return undefined;
}

function renderTextBlock(
  label: string,
  value?: string,
  extraClassName = "text-slate-700",
) {
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

function flattenSpans(spans: WorkflowObservationSpanVO[]): WorkflowObservationSpanVO[] {
  return spans.flatMap((span) => [span, ...flattenSpans(span.children || [])]);
}

function detectStageKey(span: WorkflowObservationSpanVO): StageKey | null {
  const attributes = parseAttributes(span.attributesJson);
  const explicitStage = readString(attributes, "stageKind", "stage", "phase", "diagnosticStage");
  if (explicitStage === "knowledge_retrieval") return "knowledge_retrieval";
  if (explicitStage === "vectorization") return "vectorization";
  if (explicitStage === "vector_search") return "vector_search";

  const haystacks = [
    span.spanType,
    span.name,
    span.nodeKey,
    span.modelName,
    span.inputSummary,
    span.outputSummary,
    span.attributesJson,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

  if (
    haystacks.includes("knowledge retrieval")
    || haystacks.includes("knowledge_retrieval")
    || haystacks.includes("retriever")
  ) {
    return "knowledge_retrieval";
  }
  if (
    haystacks.includes("vectorization")
    || haystacks.includes("embedding")
    || haystacks.includes("embed")
    || haystacks.includes("bge-m3")
  ) {
    return "vectorization";
  }
  if (
    haystacks.includes("vector search")
    || haystacks.includes("vector_search")
    || haystacks.includes("pgvector")
  ) {
    return "vector_search";
  }

  return null;
}

function buildStageDetailLines(
  stageKey: StageKey,
  representativeSpan?: WorkflowObservationSpanVO,
): string[] {
  if (!representativeSpan) {
    return ["暂无埋点数据"];
  }

  const attributes = parseAttributes(representativeSpan.attributesJson);
  const lines: string[] = [];

  if (stageKey === "knowledge_retrieval") {
    const knowledgeBaseId = readString(attributes, "knowledgeBaseId");
    const sourceCount = readNumber(attributes, "sourceCount", "hitCount");

    if (knowledgeBaseId) {
      lines.push(`知识库 ID: ${knowledgeBaseId}`);
    }
    if (typeof sourceCount === "number") {
      lines.push(`召回结果: ${sourceCount}`);
    }
    if (lines.length === 0) {
      lines.push("检索链路已埋点");
    }
  }

  if (stageKey === "vectorization") {
    const modelName =
      representativeSpan.modelName
      || readString(attributes, "embeddingModel", "model", "modelName");
    const dimensions = readNumber(attributes, "embeddingDimensions", "vectorDimensions");

    if (modelName) {
      lines.push(`模型: ${modelName}`);
    }
    if (typeof dimensions === "number") {
      lines.push(`维度: ${dimensions}`);
    }
    if (lines.length === 0) {
      lines.push("向量化阶段已埋点");
    }
  }

  if (stageKey === "vector_search") {
    const topK = readNumber(attributes, "topK");
    const hitCount = readNumber(attributes, "hitCount", "sourceCount");
    const backend = readString(attributes, "searchBackend");

    if (typeof topK === "number") {
      lines.push(`TopK: ${topK}`);
    }
    if (typeof hitCount === "number") {
      lines.push(`命中: ${hitCount}`);
    }
    if (backend) {
      lines.push(`引擎: ${backend}`);
    }
    if (lines.length === 0) {
      lines.push("向量检索阶段已埋点");
    }
  }

  return lines;
}

function toTimestamp(value?: string): number | null {
  if (!value) return null;
  const timestamp = new Date(value).getTime();
  return Number.isFinite(timestamp) ? timestamp : null;
}

function buildSpanRange(span: WorkflowObservationSpanVO): TimeRange | null {
  const startMs = toTimestamp(span.startedAt);
  const endMs = toTimestamp(span.endedAt);

  if (startMs === null) return null;
  if (endMs !== null && endMs >= startMs) {
    return { startMs, endMs };
  }
  if (typeof span.durationMs === "number" && span.durationMs >= 0) {
    return { startMs, endMs: startMs + span.durationMs };
  }
  return null;
}

function computeWallTime(spans: WorkflowObservationSpanVO[]): number {
  const ranges = spans
    .map(buildSpanRange)
    .filter((range): range is TimeRange => Boolean(range))
    .sort((left, right) => left.startMs - right.startMs);

  if (ranges.length === 0) {
    return spans.reduce((sum, span) => sum + (span.durationMs || 0), 0);
  }

  let total = 0;
  let currentStart = ranges[0].startMs;
  let currentEnd = ranges[0].endMs;

  for (let index = 1; index < ranges.length; index += 1) {
    const range = ranges[index];
    if (range.startMs <= currentEnd) {
      currentEnd = Math.max(currentEnd, range.endMs);
      continue;
    }

    total += currentEnd - currentStart;
    currentStart = range.startMs;
    currentEnd = range.endMs;
  }

  total += currentEnd - currentStart;
  return total;
}

function resolveLatestTraceId(spans: WorkflowObservationSpanVO[]): string | null {
  const flattened = flattenSpans(spans);
  const latestSpan = [...flattened]
    .filter((span) => span.traceId)
    .sort((left, right) => {
      const leftTime = toTimestamp(left.startedAt) ?? 0;
      const rightTime = toTimestamp(right.startedAt) ?? 0;
      return rightTime - leftTime;
    })[0];

  return latestSpan?.traceId || null;
}

function buildDiagnosticCards(spans: WorkflowObservationSpanVO[]): DiagnosticCard[] {
  const latestTraceId = resolveLatestTraceId(spans);
  const flattened = flattenSpans(spans).filter((span) =>
    latestTraceId ? span.traceId === latestTraceId : true,
  );
  const grouped = new Map<StageKey, WorkflowObservationSpanVO[]>();

  flattened.forEach((span) => {
    const stageKey = detectStageKey(span);
    if (!stageKey) return;

    const current = grouped.get(stageKey) || [];
    current.push(span);
    grouped.set(stageKey, current);
  });

  const slowestStageKey = (Object.keys(STAGE_META) as StageKey[])
    .map((stageKey) => ({
      stageKey,
      totalDurationMs: computeWallTime(grouped.get(stageKey) || []),
    }))
    .filter((item) => item.totalDurationMs > 0)
    .sort((left, right) => right.totalDurationMs - left.totalDurationMs)[0]?.stageKey;

  return (Object.keys(STAGE_META) as StageKey[]).map((stageKey) => {
    const matches = grouped.get(stageKey) || [];
    const totalDurationMs = computeWallTime(matches);
    const maxDurationMs = matches.reduce((max, span) => Math.max(max, span.durationMs || 0), 0);
    const representativeSpan = [...matches].sort(
      (left, right) => (right.durationMs || 0) - (left.durationMs || 0),
    )[0];

    return {
      key: stageKey,
      title: STAGE_META[stageKey].title,
      description: STAGE_META[stageKey].description,
      totalDurationMs,
      count: matches.length,
      maxDurationMs,
      detailLines: buildStageDetailLines(stageKey, representativeSpan),
      isSlowest: slowestStageKey === stageKey,
      hasData: matches.length > 0,
    };
  });
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
        <div className="mt-1 text-xs text-slate-500">结束原因: {span.finishReason || "-"}</div>
      </div>
      <div className="rounded-2xl bg-slate-50 p-3">
        <Typography.Text type="secondary">Token / 花销</Typography.Text>
        <div className="mt-1 text-slate-700">
          输入 {span.inputTokens ?? "-"} / 输出 {span.outputTokens ?? "-"} / 总计 {span.totalTokens ?? "-"}
        </div>
        <div className="mt-1 text-xs text-slate-500">估算成本: {formatUsd(span.estimatedCostUsd)}</div>
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
            {typeof span.durationMs === "number" ? formatDuration(span.durationMs) : "进行中"}
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

function DiagnosticStageCard({ card }: { card: DiagnosticCard }) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-100">
      <div className="flex items-start justify-between gap-4">
        <div>
          <Space wrap size={8}>
            <Typography.Text strong>{card.title}</Typography.Text>
            {card.isSlowest && <Tag color="red">最慢阶段</Tag>}
          </Space>
          <Typography.Paragraph className="!mb-0 mt-2 text-sm text-slate-500">
            {card.description}
          </Typography.Paragraph>
        </div>
          <div className="text-right">
          <div className="text-3xl font-semibold text-slate-900">
            {card.hasData ? formatDuration(card.totalDurationMs) : "暂无埋点"}
          </div>
          <div className="mt-1 text-xs text-slate-500">
            本次运行耗时 · 调用 {card.count} 次
          </div>
        </div>
      </div>

      <div className="mt-4 rounded-2xl bg-slate-50 p-4">
        <div className="text-sm text-slate-600">
          最大单次: <span className="font-medium text-slate-900">{formatDuration(card.maxDurationMs)}</span>
        </div>
        <div className="mt-3 space-y-1 text-sm text-slate-600">
          {card.detailLines.map((line) => (
            <div key={`${card.key}-${line}`}>{line}</div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function WorkflowObservabilityPanel({
  observability,
  loading = false,
}: WorkflowObservabilityPanelProps) {
  const summary = observability?.summary;
  const spans = observability?.spans || [];
  const diagnosticCards = buildDiagnosticCards(spans);

  return (
    <Card
      title={<Space><RadarChartOutlined />可观测性</Space>}
      loading={loading}
      className="border-none bg-white/90 shadow-xl shadow-slate-200/70"
    >
      {!observability ? (
        <Empty description="选择一个工作流后，这里会展示执行指标、关键阶段诊断和完整的 Span 树。" />
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

          <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
            <div className="mb-4">
              <Typography.Text strong>关键阶段诊断</Typography.Text>
              <Typography.Paragraph className="!mb-0 mt-2 text-sm text-slate-500">
                默认展示最新一次运行的阶段耗时。阶段之间存在父子包含关系，不能横向直接相加。
              </Typography.Paragraph>
            </div>
            <div className="grid gap-4 xl:grid-cols-3">
              {diagnosticCards.map((card) => (
                <DiagnosticStageCard key={card.key} card={card} />
              ))}
            </div>
          </div>

          {spans.length === 0 ? (
            <Alert
              type="info"
              showIcon
              message="当前工作流还没有可观测 Span 数据。"
              description="新触发的已埋点运行会在这里展示为树形 Span，关键阶段诊断也会随之更新。"
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
