import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ChatWorkspaceSidebar from "../ChatWorkspaceSidebar";

describe("ChatWorkspaceSidebar", () => {
  it("shows agent tools and session list inside the chat page", async () => {
    const user = userEvent.setup();
    const onSelectSession = vi.fn();

    render(
      <ChatWorkspaceSidebar
        agents={[{ id: "a-1", name: "研究助手", model: "deepseek-chat" }]}
        chatSessions={[{ id: "s-1", agentId: "a-1", title: "项目答辩" }]}
        currentSessionId="s-1"
        loading={false}
        onCreateChat={vi.fn()}
        onManageAgents={vi.fn()}
        onSelectSession={onSelectSession}
      />,
    );

    await user.click(screen.getByRole("button", { name: "项目答辩" }));

    expect(screen.getByRole("button", { name: "新建对话" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "管理智能体" })).toBeInTheDocument();
    expect(onSelectSession).toHaveBeenCalledWith("s-1");
  });
});
