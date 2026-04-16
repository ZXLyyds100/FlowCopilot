import * as React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Link, MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import FlowCopilotLayout from "../FlowCopilotLayout";

vi.mock("../shell/AppShell.tsx", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div>
      <nav aria-label="主导航">
        <Link to="/chat">聊天</Link>
        <Link to="/workflow">工作流</Link>
        <Link to="/knowledge-base">知识库</Link>
        <Link to="/settings">设置</Link>
      </nav>
      <div>{children}</div>
    </div>
  ),
}));

vi.mock("../views/AgentChatView.tsx", () => ({
  default: function MockAgentChatView() {
    const [draft, setDraft] = React.useState("");
    const [modalOpen, setModalOpen] = React.useState(false);

    return (
      <section>
        <h1>聊天页</h1>
        <button type="button" onClick={() => setModalOpen(true)}>
          打开智能体弹窗
        </button>
        {modalOpen ? (
          <div role="dialog" aria-label="智能体助手">
            <label>
              智能体名称
              <input
                aria-label="智能体名称"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
              />
            </label>
          </div>
        ) : null}
      </section>
    );
  },
}));

vi.mock("../views/WorkflowView.tsx", () => ({
  default: function MockWorkflowView() {
    const [title, setTitle] = React.useState("");
    const [task, setTask] = React.useState("");

    return (
      <section>
        <h1>工作流页</h1>
        <label>
          任务标题
          <input
            aria-label="任务标题"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </label>
        <label>
          任务描述
          <textarea
            aria-label="任务描述"
            value={task}
            onChange={(event) => setTask(event.target.value)}
          />
        </label>
      </section>
    );
  },
}));

vi.mock("../views/KnowledgeBaseView.tsx", () => ({
  default: function MockKnowledgeBaseView() {
    const [modalOpen, setModalOpen] = React.useState(false);
    const [name, setName] = React.useState("");

    return (
      <section>
        <h1>知识库页</h1>
        <button type="button" onClick={() => setModalOpen(true)}>
          打开知识库弹窗
        </button>
        {modalOpen ? (
          <div role="dialog" aria-label="新建知识库">
            <label>
              知识库名称
              <input
                aria-label="知识库名称"
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>
          </div>
        ) : null}
      </section>
    );
  },
}));

vi.mock("../views/SettingsPlaceholderView.tsx", () => ({
  default: function MockSettingsPlaceholderView() {
    return <h1>设置页</h1>;
  },
}));

describe("FlowCopilotLayout keep alive", () => {
  it("preserves the workflow draft when switching modules", async () => {
    const user = userEvent.setup();

    renderAt("/workflow");

    await user.type(screen.getByLabelText("任务标题"), "保留中的标题");
    await user.type(screen.getByLabelText("任务描述"), "切页后不应该丢失");

    await user.click(screen.getByRole("link", { name: "知识库" }));
    await user.click(screen.getByRole("link", { name: "工作流" }));

    expect(screen.getByDisplayValue("保留中的标题")).toBeInTheDocument();
    expect(screen.getByDisplayValue("切页后不应该丢失")).toBeInTheDocument();
  });

  it("preserves the knowledge base modal state when switching modules", async () => {
    const user = userEvent.setup();

    renderAt("/knowledge-base");

    await user.click(screen.getByRole("button", { name: "打开知识库弹窗" }));
    await user.type(screen.getByLabelText("知识库名称"), "架构资料库");

    await user.click(screen.getByRole("link", { name: "工作流" }));
    await user.click(screen.getByRole("link", { name: "知识库" }));

    expect(screen.getByRole("dialog", { name: "新建知识库" })).toBeInTheDocument();
    expect(screen.getByDisplayValue("架构资料库")).toBeInTheDocument();
  });

  it("preserves the chat modal state when switching modules", async () => {
    const user = userEvent.setup();

    renderAt("/chat");

    await user.click(screen.getByRole("button", { name: "打开智能体弹窗" }));
    await user.type(screen.getByLabelText("智能体名称"), "长期保活智能体");

    await user.click(screen.getByRole("link", { name: "工作流" }));
    await user.click(screen.getByRole("link", { name: "聊天" }));

    expect(screen.getByRole("dialog", { name: "智能体助手" })).toBeInTheDocument();
    expect(screen.getByDisplayValue("长期保活智能体")).toBeInTheDocument();
  });
});

function renderAt(route: string) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <FlowCopilotLayout />
    </MemoryRouter>,
  );
}
