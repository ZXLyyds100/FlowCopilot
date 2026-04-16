import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import AppShell from "../AppShell";
import { ShellProvider } from "../ShellProvider";
import { useShellPage } from "../useShellPage";
import { renderWithRouter } from "../../../test/renderWithRouter";

function RegisteredShellPage() {
  const [renderCount, setRenderCount] = useState(0);

  useShellPage({
    title: "工作流控制台",
    description: "观察当前页面状态是否稳定。",
    detailTitle: "执行详情",
    detailContent: <div>drawer payload</div>,
    secondaryActions: [
      {
        label: "刷新",
        onClick: () => undefined,
      },
    ],
  });

  return (
    <button type="button" onClick={() => setRenderCount((count) => count + 1)}>
      重新渲染 {renderCount}
    </button>
  );
}

function ToggleShellPage() {
  const [visible, setVisible] = useState(true);

  return (
    <>
      <button type="button" onClick={() => setVisible(false)}>
        卸载页面
      </button>
      {visible ? <RegisteredShellPage /> : <div>fallback body</div>}
    </>
  );
}

describe("useShellPage", () => {
  it("keeps page state and drawer visibility stable across ordinary re-renders", async () => {
    const user = userEvent.setup();

    renderWithRouter(
      <ShellProvider>
        <AppShell>
          <RegisteredShellPage />
        </AppShell>
      </ShellProvider>,
      { route: "/workflow" },
    );

    expect(
      screen.getByRole("heading", { name: "工作流控制台" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "查看详情" }));

    expect(screen.getByText("drawer payload")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "重新渲染 0" }));

    expect(screen.getByText("drawer payload")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "工作流控制台" }),
    ).toBeInTheDocument();
  });

  it("resets to the route-derived shell copy when the page unmounts", async () => {
    const user = userEvent.setup();

    renderWithRouter(
      <ShellProvider>
        <AppShell>
          <ToggleShellPage />
        </AppShell>
      </ShellProvider>,
      { route: "/workflow" },
    );

    expect(
      screen.getByRole("heading", { name: "工作流控制台" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "卸载页面" }));

    expect(screen.getByRole("heading", { name: "工作流" })).toBeInTheDocument();
    expect(screen.getByText("运行 Graph 模板并查看实时执行状态。")).toBeInTheDocument();
  });
});
