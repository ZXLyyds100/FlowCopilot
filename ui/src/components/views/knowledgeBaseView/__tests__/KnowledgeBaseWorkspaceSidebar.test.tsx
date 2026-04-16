import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import KnowledgeBaseWorkspaceSidebar from "../KnowledgeBaseWorkspaceSidebar";

describe("KnowledgeBaseWorkspaceSidebar", () => {
  it("renders the page-owned knowledge base list and create action", async () => {
    const user = userEvent.setup();
    const onSelectKnowledgeBase = vi.fn();

    render(
      <KnowledgeBaseWorkspaceSidebar
        knowledgeBases={[
          { knowledgeBaseId: "kb-1", name: "项目资料", description: "答辩材料" },
        ]}
        selectedKnowledgeBaseId="kb-1"
        onCreateKnowledgeBase={vi.fn()}
        onSelectKnowledgeBase={onSelectKnowledgeBase}
      />,
    );

    expect(screen.getByRole("button", { name: "新建知识库" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "项目资料" }));
    expect(onSelectKnowledgeBase).toHaveBeenCalledWith("kb-1");
  });
});
