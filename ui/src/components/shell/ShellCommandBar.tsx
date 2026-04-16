import { Button } from "antd";
import { useShellContext, type ShellFallbackState, type ShellPageState } from "./ShellProvider";
import type { ShellRouteMatch } from "./shellConfig";

interface ShellCommandBarProps {
  activeRoute: ShellRouteMatch | null;
}

function resolvePageCopy(
  page: ShellPageState | null,
  activeRoute: ShellRouteMatch | null,
  fallback: ShellFallbackState,
) {
  const title = page?.title ?? activeRoute?.title ?? fallback.title;
  const description = page?.description ?? activeRoute?.description ?? fallback.description;

  return { title, description };
}

export default function ShellCommandBar({ activeRoute }: ShellCommandBarProps) {
  const { fallback, page, openDetailDrawer } = useShellContext();
  const { title, description } = resolvePageCopy(page, activeRoute, fallback);

  return (
    <header className="border-b border-[var(--shell-border)] bg-[var(--shell-surface)] px-4 py-4 md:px-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--shell-muted)]">
            工作区
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-[var(--shell-text)]">{title}</h2>
          <p className="mt-2 max-w-2xl text-sm text-[var(--shell-muted)]">{description}</p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {page?.secondaryActions?.map((action) => (
            <Button key={action.label} onClick={action.onClick}>
              {action.label}
            </Button>
          ))}
          {page?.detailContent ? (
            <Button onClick={openDetailDrawer}>查看详情</Button>
          ) : null}
          {page?.primaryAction ? (
            <Button type="primary" onClick={page.primaryAction.onClick}>
              {page.primaryAction.label}
            </Button>
          ) : null}
        </div>
      </div>
    </header>
  );
}
