import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { WorkflowTemplateVO } from "../../../../api/api.ts";
import type { KnowledgeBase } from "../../../../types";
import WorkflowCreateCard from "../WorkflowCreateCard";
import WorkflowOverviewCard from "../WorkflowOverviewCard";

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

vi.stubGlobal("ResizeObserver", ResizeObserverMock);

const templates: WorkflowTemplateVO[] = [
  {
    code: "research",
    description: "研究型知识增强流程",
    mermaid: "graph TD;",
    name: "Research Workflow",
  },
] as WorkflowTemplateVO[];

const knowledgeBases: KnowledgeBase[] = [
  {
    knowledgeBaseId: "kb-1",
    name: "项目知识库",
  } as KnowledgeBase,
];

describe("workflow cards", () => {
  it("renders create card with standard sections and triggers create action", async () => {
    const user = userEvent.setup();
    const onCreateWorkflow = vi.fn();

    render(
      <WorkflowCreateCard
        knowledgeBaseId="kb-1"
        knowledgeBases={knowledgeBases}
        loading={false}
        taskInput="整理答辩路线"
        templateCode="research"
        templates={templates}
        title="季度复盘"
        onCreateWorkflow={onCreateWorkflow}
        onKnowledgeBaseChange={vi.fn()}
        onTaskInputChange={vi.fn()}
        onTemplateCodeChange={vi.fn()}
        onTitleChange={vi.fn()}
      />,
    );

    expect(screen.getByText("FlowCopilot Graph Studio")).toBeInTheDocument();
    expect(screen.getByText("任务编排")).toBeInTheDocument();
    expect(screen.getByText("推荐配置")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /启动 Graph 智能流程/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /启动 Graph 智能流程/ }));

    expect(onCreateWorkflow).toHaveBeenCalledTimes(1);
  });

  it("renders overview card using the shared card layout with progress metrics", () => {
    render(
      <WorkflowOverviewCard
        currentWorkflow={{
          id: "wf-1",
          input: "基于知识库整理答辩介绍",
          status: "RUNNING",
          title: "答辩任务",
        } as never}
        latestSnapshot={{
          currentNodeKey: "reviewer",
          retryCount: 2,
          traceId: "trace-123456789",
        }}
        progressPercent={68}
        selectedTemplate={templates[0]}
        traces={[{ traceId: "trace-fallback" } as never]}
      />,
    );

    expect(screen.getByText("运行概览")).toBeInTheDocument();
    expect(screen.getByText("Research Workflow")).toBeInTheDocument();
    expect(screen.getByText("执行进度")).toBeInTheDocument();
    expect(screen.getByText("当前节点")).toBeInTheDocument();
    expect(screen.getByText("reviewer")).toBeInTheDocument();
    expect(screen.getByText("trace-12...6789")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
  });
});
