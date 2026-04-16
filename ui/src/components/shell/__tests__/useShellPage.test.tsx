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

function UpdatingShellPage() {
  const [variant, setVariant] = useState<"draft" | "published">("draft");

  useShellPage({
    title: variant === "draft" ? "草稿工作流" : "已发布工作流",
    description:
      variant === "draft"
        ? "当前显示草稿版本。"
        : "当前显示发布后的版本。",
    secondaryActions: [
      {
        label: variant === "draft" ? "预览" : "回滚",
        onClick: () => undefined,
      },
    ],
  });

  return (
    <button
      type="button"
      onClick={() => setVariant((current) => (current === "draft" ? "published" : "draft"))}
    >
      切换状态
    </button>
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

  it("reflects live shell copy updates after mount", async () => {
    const user = userEvent.setup();

    renderWithRouter(
      <ShellProvider>
        <AppShell>
          <UpdatingShellPage />
        </AppShell>
      </ShellProvider>,
      { route: "/workflow" },
    );

    expect(screen.getByRole("heading", { name: "草稿工作流" })).toBeInTheDocument();
    expect(screen.getByText("当前显示草稿版本。")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /预\s*览/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "切换状态" }));

    expect(screen.getByRole("heading", { name: "已发布工作流" })).toBeInTheDocument();
    expect(screen.getByText("当前显示发布后的版本。")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /回\s*滚/ })).toBeInTheDocument();
  });
});
