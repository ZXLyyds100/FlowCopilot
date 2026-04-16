import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import AppShell from "../AppShell";
import { ShellProvider } from "../ShellProvider";
import { renderWithRouter } from "../../../test/renderWithRouter";

describe("AppShell", () => {
  it("renders a skip link, highlights the active nav item, and exposes main content", () => {
    renderWithRouter(
      <ShellProvider>
        <AppShell>
          <div>workspace body</div>
        </AppShell>
      </ShellProvider>,
      { route: "/workflow" },
    );

    expect(screen.getByRole("link", { name: "跳到主内容" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "工作流" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("heading", { name: "工作流" })).toBeInTheDocument();
    expect(screen.getByText("运行 Graph 模板并查看实时执行状态。")).toBeInTheDocument();
    expect(screen.getByRole("main")).toHaveTextContent("workspace body");
  });
});
