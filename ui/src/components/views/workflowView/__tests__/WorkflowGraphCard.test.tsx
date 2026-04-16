import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import WorkflowGraphCard from "../WorkflowGraphCard";

vi.mock("../WorkflowMermaidViewer.tsx", () => ({
  default: ({ content }: { content?: string }) => <div>viewer:{content}</div>,
}));

describe("WorkflowGraphCard", () => {
  it("opens mermaid modal from the original section button", async () => {
    const user = userEvent.setup();

    render(
      <WorkflowGraphCard
        currentStep="Planner"
        graphPath={["planner"]}
        latestSnapshot={null}
        selectedTemplateCode="research"
        selectedTemplateMermaid="graph TD; A-->B;"
        setTemplateCode={vi.fn()}
        steps={[]}
        templates={[
          {
            code: "research",
            description: "研究型知识增强流程",
            name: "ResearchWorkflow",
          },
        ]}
      />,
    );

    await user.click(screen.getByRole("button", { name: /查看 Mermaid 图/ }));

    expect(screen.getByRole("dialog", { name: "LangGraph4j Mermaid 预览" })).toBeInTheDocument();
    expect(screen.getByText("viewer:graph TD; A-->B;")).toBeInTheDocument();
  });
});
