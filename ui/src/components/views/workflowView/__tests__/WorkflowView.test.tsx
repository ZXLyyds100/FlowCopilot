import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import WorkflowView from "../../WorkflowView";

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

Object.defineProperty(window, "ResizeObserver", {
  writable: true,
  value: ResizeObserverMock,
});

vi.mock("../../../shell/useShellPage.ts", () => ({
  useShellPage: vi.fn(),
}));

vi.mock("../../../../hooks/useKnowledgeBases.ts", () => ({
  useKnowledgeBases: () => ({
    knowledgeBases: [],
  }),
}));

vi.mock("../../../../api/api.ts", () => ({
  getWorkflows: vi.fn().mockResolvedValue({ workflows: [] }),
  getWorkflowTemplates: vi.fn().mockResolvedValue({
    templates: [
      {
        code: "research",
        name: "Research",
        description: "Default research flow",
        definitionJson: "{}",
        mermaid: "graph TD; A-->B;",
      },
    ],
  }),
  getApprovals: vi.fn().mockResolvedValue({ approvals: [] }),
  getWorkflowCheckpoints: vi.fn().mockResolvedValue({ checkpoints: [] }),
  getWorkflowObservability: vi.fn().mockResolvedValue(null),
  getWorkflowTrace: vi.fn().mockResolvedValue({ traces: [] }),
  getWorkflow: vi.fn(),
  createWorkflow: vi.fn(),
  approveWorkflow: vi.fn(),
  rejectWorkflow: vi.fn(),
  replayWorkflowFromNode: vi.fn(),
  updateWorkflowTemplate: vi.fn(),
}));

describe("WorkflowView", () => {
  it("renders the studio shell and empty workflow state", async () => {
    render(<WorkflowView />);

    expect(screen.getByText("FlowCopilot Graph Studio")).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByText("创建或选择一个工作流查看第四阶段 Graph 执行详情"),
      ).toBeInTheDocument();
    });
  });
});
