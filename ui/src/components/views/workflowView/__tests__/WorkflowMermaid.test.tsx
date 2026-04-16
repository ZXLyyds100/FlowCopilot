import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import WorkflowMermaid from "../WorkflowMermaid";

const { renderMock, initializeMock } = vi.hoisted(() => ({
  renderMock: vi.fn(),
  initializeMock: vi.fn(),
}));

vi.mock("mermaid", () => ({
  default: {
    initialize: initializeMock,
    render: renderMock,
  },
}));

describe("WorkflowMermaid", () => {
  beforeEach(() => {
    initializeMock.mockReset();
    renderMock.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders placeholder when content is empty", () => {
    render(<WorkflowMermaid content="" placeholder="Graph definition loading..." />);

    expect(screen.getByText("Graph definition loading...")).toBeInTheDocument();
    expect(renderMock).not.toHaveBeenCalled();
  });

  it("renders svg when mermaid content is valid", async () => {
    renderMock.mockResolvedValue({
      svg: "<svg><text>Research Workflow</text></svg>",
      bindFunctions: undefined,
    });

    const { container } = render(<WorkflowMermaid content="graph TD; A-->B;" />);

    await waitFor(() => {
      expect(renderMock).toHaveBeenCalledWith(expect.stringMatching(/^workflow-mermaid-/), "graph TD; A-->B;");
    });

    expect(container.querySelector("svg")).toBeInTheDocument();
    expect(screen.getByText("Research Workflow")).toBeInTheDocument();
  });

  it("shows fallback error when mermaid rendering fails", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    renderMock.mockRejectedValue(new Error("invalid mermaid"));

    render(<WorkflowMermaid content="graph TD; A-;" />);

    await waitFor(() => {
      expect(screen.getByText("Mermaid 图渲染失败，请检查模板定义。")).toBeInTheDocument();
    });

    expect(errorSpy).toHaveBeenCalled();
  });
});
