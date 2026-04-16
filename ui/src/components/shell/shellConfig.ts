import { matchPath } from "react-router-dom";

export type ShellModuleKey =
  | "chat"
  | "workflow"
  | "knowledge-base"
  | "settings";

export interface ShellRouteMatch {
  moduleKey: ShellModuleKey;
  to: string;
  label: string;
  title: string;
  description: string;
}

export interface ShellRouteContext {
  moduleKey: ShellModuleKey;
  pathname: string;
  chatSessionId?: string;
  knowledgeBaseId?: string;
}

const chatRoute: ShellRouteMatch = {
  moduleKey: "chat",
  to: "/chat",
  label: "聊天",
  title: "聊天",
  description: "管理会话、智能体和当前对话。",
};

const workflowRoute: ShellRouteMatch = {
  moduleKey: "workflow",
  to: "/workflow",
  label: "工作流",
  title: "工作流",
  description: "运行 Graph 模板并查看实时执行状态。",
};

const knowledgeBaseRoute: ShellRouteMatch = {
  moduleKey: "knowledge-base",
  to: "/knowledge-base",
  label: "知识库",
  title: "知识库",
  description: "浏览知识库、上传文档并查看资料内容。",
};

const settingsRoute: ShellRouteMatch = {
  moduleKey: "settings",
  to: "/settings",
  label: "设置",
  title: "设置",
  description: "查看全局设置和保留的工具入口。",
};

const ROUTES: ShellRouteMatch[] = [
  chatRoute,
  workflowRoute,
  knowledgeBaseRoute,
  settingsRoute,
];

function matchesShellPath(pathname: string, basePath: string): boolean {
  return pathname === basePath || pathname.startsWith(`${basePath}/`);
}

export function matchShellRoute(pathname: string): ShellRouteMatch | null {
  if (pathname === "/" || matchesShellPath(pathname, "/chat") || matchesShellPath(pathname, "/agent")) {
    return chatRoute;
  }

  if (matchesShellPath(pathname, "/workflow")) return workflowRoute;
  if (matchesShellPath(pathname, "/knowledge-base")) return knowledgeBaseRoute;
  if (matchesShellPath(pathname, "/settings")) return settingsRoute;

  return null;
}

export const shellRoutes = ROUTES;

export function resolveShellRouteContext(pathname: string): ShellRouteContext | null {
  if (pathname === "/") {
    return {
      moduleKey: "chat",
      pathname: "/chat",
    };
  }

  const agentAliasMatch = matchPath("/agent/:chatSessionId", pathname);
  if (agentAliasMatch) {
    return {
      moduleKey: "chat",
      pathname,
      chatSessionId: agentAliasMatch.params.chatSessionId,
    };
  }

  const chatMatch = matchPath("/chat/:chatSessionId", pathname);
  if (chatMatch) {
    return {
      moduleKey: "chat",
      pathname,
      chatSessionId: chatMatch.params.chatSessionId,
    };
  }

  if (pathname === "/chat" || pathname === "/agent") {
    return {
      moduleKey: "chat",
      pathname,
    };
  }

  const knowledgeBaseMatch = matchPath("/knowledge-base/:knowledgeBaseId", pathname);
  if (knowledgeBaseMatch) {
    return {
      moduleKey: "knowledge-base",
      pathname,
      knowledgeBaseId: knowledgeBaseMatch.params.knowledgeBaseId,
    };
  }

  if (pathname === "/knowledge-base") {
    return {
      moduleKey: "knowledge-base",
      pathname,
    };
  }

  if (matchesShellPath(pathname, "/workflow")) {
    return {
      moduleKey: "workflow",
      pathname,
    };
  }

  if (matchesShellPath(pathname, "/settings")) {
    return {
      moduleKey: "settings",
      pathname,
    };
  }

  return null;
}
