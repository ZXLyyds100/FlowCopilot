import {
  CommentOutlined,
  DatabaseOutlined,
  SettingOutlined,
  ShareAltOutlined,
} from "@ant-design/icons";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { matchShellRoute, shellRoutes, type ShellModuleKey } from "./shellConfig";

const routeIcons: Record<ShellModuleKey, ReactNode> = {
  chat: <CommentOutlined />,
  workflow: <ShareAltOutlined />,
  "knowledge-base": <DatabaseOutlined />,
  settings: <SettingOutlined />,
};

interface AppRailProps {
  pathname: string;
}

export default function AppRail({ pathname }: AppRailProps) {
  const activeModuleKey = matchShellRoute(pathname)?.moduleKey;

  return (
    <aside className="flex min-h-0 flex-col border-r border-[var(--shell-border)] bg-[var(--shell-surface)] px-3 py-4">
      <div className="mb-6 rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-canvas)] px-4 py-5">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--shell-muted)]">
          FlowCopilot
        </p>
        <h1 className="mt-2 text-lg font-semibold text-[var(--shell-text)]">App Shell</h1>
        <p className="mt-2 text-sm text-[var(--shell-muted)]">统一导航、页面标题和详情侧栏。</p>
      </div>

      <nav aria-label="主导航" className="flex flex-1 flex-col gap-2">
        {shellRoutes.map((route) => {
          const isActive = route.moduleKey === activeModuleKey;

          return (
            <Link
              key={route.moduleKey}
              to={route.to}
              aria-current={isActive ? "page" : undefined}
              className={`flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-medium transition-colors ${
                isActive
                  ? "bg-[var(--shell-text)] text-white"
                  : "text-[var(--shell-muted)] hover:bg-[var(--shell-canvas)] hover:text-[var(--shell-text)]"
              }`}
            >
              <span aria-hidden="true" className="text-base">
                {routeIcons[route.moduleKey]}
              </span>
              <span>{route.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
