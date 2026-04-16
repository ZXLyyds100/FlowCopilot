import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import WorkflowMermaidViewer from "../WorkflowMermaidViewer";

vi.mock("../WorkflowMermaid.tsx", () => ({
  default: ({ content, className }: { content?: string; className?: string }) => (
    <div className={className} data-testid="workflow-mermaid-render">
      {content}
    </div>
  ),
}));

describe("WorkflowMermaidViewer", () => {
  it("renders zoom controls and resets transform", async () => {
    const user = userEvent.setup();

    render(<WorkflowMermaidViewer content="graph TD; A-->B;" />);

    const canvas = screen.getByTestId("workflow-mermaid-canvas");
    expect(canvas).toHaveStyle({ transform: "translate(0px, 0px) scale(1)" });

    await user.click(screen.getByRole("button", { name: "放大" }));
    expect(canvas).toHaveStyle({ transform: "translate(0px, 0px) scale(1.2)" });

    await user.click(screen.getByRole("button", { name: "重置视图" }));
    expect(canvas).toHaveStyle({ transform: "translate(0px, 0px) scale(1)" });
  });

  it("supports wheel zoom and drag panning", () => {
    render(<WorkflowMermaidViewer content="graph TD; A-->B;" />);

    const viewport = screen.getByTestId("workflow-mermaid-viewport");
    const canvas = screen.getByTestId("workflow-mermaid-canvas");

    fireEvent.wheel(viewport, { deltaY: -120 });
    expect(canvas).toHaveStyle({ transform: "translate(0px, 0px) scale(1.1)" });

    fireEvent.mouseDown(viewport, { clientX: 100, clientY: 120 });
    fireEvent.mouseMove(window, { clientX: 140, clientY: 165 });
    fireEvent.mouseUp(window);

    expect(canvas).toHaveStyle({ transform: "translate(40px, 45px) scale(1.1)" });
  });
});
