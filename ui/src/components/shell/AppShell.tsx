import type { PropsWithChildren } from "react";
import { useLocation } from "react-router-dom";
import { matchShellRoute } from "./shellConfig";
import AppRail from "./AppRail";
import ShellCommandBar from "./ShellCommandBar";
import ShellDetailDrawer from "./ShellDetailDrawer";

export default function AppShell({ children }: PropsWithChildren) {
  const location = useLocation();
  const activeRoute = matchShellRoute(location.pathname);

  return (
    <>
      <a href="#main-content" className="shell-skip-link">
        跳到主内容
      </a>
      <div className="grid h-dvh grid-cols-[196px_minmax(0,1fr)] bg-[var(--shell-canvas)]">
        <AppRail pathname={location.pathname} />
        <div className="flex min-h-0 flex-col">
          <ShellCommandBar activeRoute={activeRoute} />
          <main
            id="main-content"
            className="flex min-h-0 flex-1 flex-col p-4 md:p-6"
            tabIndex={-1}
          >
            {children}
          </main>
        </div>
      </div>
      <ShellDetailDrawer />
    </>
  );
}
