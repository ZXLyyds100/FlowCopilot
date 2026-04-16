import { Navigate, useLocation } from "react-router-dom";
import { useRef, type ReactNode } from "react";
import AgentChatView from "./views/AgentChatView.tsx";
import KnowledgeBaseView from "./views/KnowledgeBaseView.tsx";
import SettingsPlaceholderView from "./views/SettingsPlaceholderView.tsx";
import WorkflowView from "./views/WorkflowView.tsx";
import AppShell from "./shell/AppShell.tsx";
import { ShellProvider } from "./shell/ShellProvider.tsx";
import { ShellPageActivityProvider } from "./shell/useShellPage.ts";
import {
  resolveShellRouteContext,
  type ShellRouteContext,
} from "./shell/shellConfig.ts";

interface CachedModuleContexts {
  chat: ShellRouteContext;
  workflow: ShellRouteContext;
  "knowledge-base": ShellRouteContext;
  settings: ShellRouteContext;
}

export default function FlowCopilotLayout() {
  const location = useLocation();
  const currentContext = resolveShellRouteContext(location.pathname);
  const cachedContextsRef = useRef<CachedModuleContexts>({
    chat: {
      moduleKey: "chat",
      pathname: "/chat",
    },
    workflow: {
      moduleKey: "workflow",
      pathname: "/workflow",
    },
    "knowledge-base": {
      moduleKey: "knowledge-base",
      pathname: "/knowledge-base",
    },
    settings: {
      moduleKey: "settings",
      pathname: "/settings",
    },
  });

  if (location.pathname === "/") {
    return <Navigate to="/chat" replace />;
  }

  if (location.pathname === "/agent") {
    return <Navigate to="/chat" replace />;
  }

  if (location.pathname.startsWith("/agent/")) {
    return <Navigate to={location.pathname.replace(/^\/agent/, "/chat")} replace />;
  }

  if (!currentContext) {
    return <Navigate to="/chat" replace />;
  }

  cachedContextsRef.current[currentContext.moduleKey] = currentContext;
  const cachedContexts = cachedContextsRef.current;

  return (
    <ShellProvider>
      <AppShell>
        <ModulePane active={currentContext.moduleKey === "chat"}>
          <AgentChatView
            chatSessionId={cachedContexts.chat.chatSessionId}
            locationState={location.state}
          />
        </ModulePane>
        <ModulePane active={currentContext.moduleKey === "workflow"}>
          <WorkflowView />
        </ModulePane>
        <ModulePane active={currentContext.moduleKey === "knowledge-base"}>
          <KnowledgeBaseView knowledgeBaseId={cachedContexts["knowledge-base"].knowledgeBaseId} />
        </ModulePane>
        <ModulePane active={currentContext.moduleKey === "settings"}>
          <SettingsPlaceholderView />
        </ModulePane>
      </AppShell>
    </ShellProvider>
  );
}

function ModulePane({
  active,
  children,
}: {
  active: boolean;
  children: ReactNode;
}) {
  return (
    <ShellPageActivityProvider active={active}>
      <div
        hidden={!active}
        aria-hidden={!active}
        className={active ? "flex min-h-0 flex-1 flex-col" : undefined}
        data-module-active={active ? "true" : "false"}
      >
        {children}
      </div>
    </ShellPageActivityProvider>
  );
}
