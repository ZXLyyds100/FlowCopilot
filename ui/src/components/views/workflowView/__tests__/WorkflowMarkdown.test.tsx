import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import WorkflowMarkdown from "../WorkflowMarkdown";

describe("WorkflowMarkdown", () => {
  it("renders basic markdown structures", () => {
    render(<WorkflowMarkdown content={"# 标题\n\n- 条目\n\n`code`\n\n```ts\nconst value = 1;\n```"} />);

    expect(screen.getByRole("heading", { level: 1, name: "标题" })).toBeInTheDocument();
    expect(screen.getByText("条目")).toBeInTheDocument();
    expect(screen.getByText("code")).toBeInTheDocument();
    expect(screen.getByText("const value = 1;")).toBeInTheDocument();
  });

  it("falls back to placeholder when content is empty", () => {
    render(<WorkflowMarkdown content="" placeholder="正在思考和执行..." />);

    expect(screen.getByText("正在思考和执行...")).toBeInTheDocument();
  });
});
