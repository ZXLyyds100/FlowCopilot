import { Navigate, Route, Routes } from "react-router-dom";
import AgentChatView from "./views/AgentChatView.tsx";
import KnowledgeBaseView from "./views/KnowledgeBaseView.tsx";
import SettingsPlaceholderView from "./views/SettingsPlaceholderView.tsx";
import WorkflowView from "./views/WorkflowView.tsx";
import AppShell from "./shell/AppShell.tsx";
import { ShellProvider } from "./shell/ShellProvider.tsx";

export default function FlowCopilotLayout() {
  return (
    <ShellProvider>
      <AppShell>
        <Routes>
          <Route path="/" element={<Navigate to="/chat" replace />} />
          <Route path="/agent" element={<Navigate to="/chat" replace />} />
          <Route path="/chat" element={<AgentChatView />} />
          <Route path="/chat/:chatSessionId" element={<AgentChatView />} />
          <Route path="/workflow" element={<WorkflowView />} />
          <Route path="/knowledge-base" element={<KnowledgeBaseView />} />
          <Route
            path="/knowledge-base/:knowledgeBaseId"
            element={<KnowledgeBaseView />}
          />
          <Route path="/settings" element={<SettingsPlaceholderView />} />
        </Routes>
      </AppShell>
    </ShellProvider>
  );
}
