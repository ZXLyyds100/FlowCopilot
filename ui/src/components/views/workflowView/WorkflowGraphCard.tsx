import { ApartmentOutlined, CodeOutlined, NodeIndexOutlined } from "@ant-design/icons";
import { Card, Space, Tag, Typography } from "antd";
import React from "react";
import type { WorkflowGraphCardProps } from "./types.ts";
import { NODE_ORDER, nodeMeta, statusColor } from "./utils.ts";

export default function WorkflowGraphCard({
  currentStep,
  graphPath,
  latestSnapshot,
  selectedTemplateCode,
  selectedTemplateMermaid,
  setTemplateCode,
  steps,
  templates,
}: WorkflowGraphCardProps) {
  return (
    <Card
      title={<Space><NodeIndexOutlined />Graph 路径与动态路由</Space>}
      extra={<Tag color="geekblue">{selectedTemplateCode}</Tag>}
      className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
    >
      <div className="grid grid-cols-[minmax(0,1fr)_360px] gap-5">
        <div>
          <div className="mb-5 flex flex-wrap items-center gap-3">
            {NODE_ORDER.map((nodeKey, index) => {
              const meta = nodeMeta(nodeKey);
              const step = steps.find((item) => item.nodeKey === nodeKey);
              const active = latestSnapshot?.currentNodeKey === nodeKey || currentStep === meta.name;
              const visited = graphPath.includes(nodeKey) || Boolean(step);

              return (
                <React.Fragment key={nodeKey}>
                  <div
                    className={`min-w-32 rounded-3xl border p-4 transition ${
                      active
                        ? "border-slate-950 bg-slate-950 text-white shadow-xl"
                        : visited
                          ? "border-cyan-200 bg-cyan-50 text-slate-900"
                          : "border-slate-200 bg-white text-slate-500"
                    }`}
                  >
                    <div className={`mb-3 h-2 rounded-full bg-gradient-to-r ${meta.tone}`} />
                    <div className="font-semibold">{meta.name}</div>
                    <div className="text-xs opacity-70">{meta.role}</div>
                    {step && (
                      <Tag className="mt-2" color={statusColor(step.status)}>
                        {step.status}
                      </Tag>
                    )}
                  </div>
                  {index < NODE_ORDER.length - 1 && (
                    <div className="hidden h-px w-8 bg-slate-300 md:block" />
                  )}
                </React.Fragment>
              );
            })}
          </div>
          <Typography.Text className="!text-slate-600">
            实际路径：
            {graphPath.length ? graphPath.map((node) => nodeMeta(node).name).join(" -> ") : "等待节点执行"}
          </Typography.Text>
        </div>
        <div className="rounded-3xl bg-slate-950 p-4 text-slate-100">
          <Space direction="vertical" className="w-full" size="middle">
            <div>
              <Space className="mb-3">
                <ApartmentOutlined />
                <Typography.Text className="!text-slate-100">Graph 模板</Typography.Text>
              </Space>
              <div className="flex flex-col gap-2">
                {templates.map((template) => (
                  <button
                    key={template.code}
                    type="button"
                    className={`w-full rounded-2xl border p-3 text-left transition ${
                      selectedTemplateCode === template.code
                        ? "border-cyan-300 bg-white/10 text-white"
                        : "border-white/10 bg-black/10 text-slate-200 hover:border-cyan-200"
                    }`}
                    onClick={() => setTemplateCode(template.code)}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span className="font-semibold">{template.name}</span>
                      <Tag color={selectedTemplateCode === template.code ? "cyan" : "blue"}>
                        {template.code}
                      </Tag>
                    </div>
                    <div className="mt-1 text-xs leading-5 text-slate-300">{template.description}</div>
                  </button>
                ))}
              </div>
            </div>
            <div>
              <Space className="mb-3">
                <CodeOutlined />
                <Typography.Text className="!text-slate-100">LangGraph4j Mermaid</Typography.Text>
              </Space>
              <pre className="max-h-72 overflow-auto whitespace-pre-wrap text-xs leading-6 text-cyan-50">
                {selectedTemplateMermaid || "Graph definition loading..."}
              </pre>
            </div>
          </Space>
        </div>
      </div>
    </Card>
  );
}
