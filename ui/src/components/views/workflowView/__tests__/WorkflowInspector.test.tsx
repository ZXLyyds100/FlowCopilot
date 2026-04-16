import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import WorkflowInspector from "../WorkflowInspector";

describe("WorkflowInspector", () => {
  it("shows approvals, recent workflows, and trace summary in the drawer content", () => {
    render(
      <WorkflowInspector
        pendingApprovals={[
          { id: "ap-1", workflowInstanceId: "wf-1", status: "PENDING", title: "审批中" },
        ]}
        workflows={[
          { id: "wf-1", title: "研究任务", input: "整理项目资料", status: "RUNNING" },
        ]}
        liveStatus="正在执行 Planner"
        onSelectWorkflow={vi.fn()}
      />,
    );

    expect(screen.getByText("待审批事项")).toBeInTheDocument();
    expect(screen.getByText("研究任务")).toBeInTheDocument();
    expect(screen.getByText("正在执行 Planner")).toBeInTheDocument();
  });
});
