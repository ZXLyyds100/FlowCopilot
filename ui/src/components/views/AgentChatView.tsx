import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { message as antdMessage } from "antd";
import AgentChatHistory from "./agentChatView/AgentChatHistory.tsx";
import AgentChatInput from "./agentChatView/AgentChatInput.tsx";
import AddAgentModal from "../modals/AddAgentModal.tsx";
import PageCanvas from "../shell/PageCanvas.tsx";
import { useShellPage } from "../shell/useShellPage.ts";
import {
  createChatMessage,
  createChatSession,
  getChatMessagesBySessionId,
  getChatSession,
} from "../../api/api.ts";
import { useAgents } from "../../hooks/useAgents.ts";
import { useChatSessions } from "../../hooks/useChatSessions.ts";
import ChatWorkspaceSidebar from "./agentChatView/ChatWorkspaceSidebar.tsx";
import EmptyAgentChatView from "./agentChatView/EmptyAgentChatView.tsx";
import type { ChatMessageVO, SseMessage, SseMessageType } from "../../types";

interface AgentChatViewProps {
  chatSessionId?: string;
  locationState?: {
    init?: boolean;
    initMessage?: string;
  } | null;
}

const AgentChatView: React.FC<AgentChatViewProps> = ({
  chatSessionId,
  locationState,
}) => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [isAddAgentModalOpen, setIsAddAgentModalOpen] = useState(false);
  const {
    agents,
    createAgentHandle,
    updateAgentHandle,
  } = useAgents();
  const {
    chatSessions,
    loading: chatSessionsLoading,
    refreshChatSessions,
  } = useChatSessions();

  const [messages, setMessages] = useState<ChatMessageVO[]>([]);

  const addMessage = (message: ChatMessageVO) => {
    setMessages((prevMessages) => [...prevMessages, message]);
  };

  const [agentId, setAgentId] = useState<string>("");

  useEffect(() => {
    if (!chatSessionId && !agentId && agents.length > 0) {
      setAgentId(agents[0].id);
    }
  }, [agentId, agents, chatSessionId]);

  const getChatMessages = useCallback(async () => {
    if (!chatSessionId) {
      return;
    }
    const resp = await getChatMessagesBySessionId(chatSessionId);
    setMessages(resp.chatMessages);

    const fetchData = async () => {
      const resp = await getChatSession(chatSessionId);
      // setChatSession(resp.chatSession);
      setAgentId(resp.chatSession.agentId);
    };
    fetchData().then();
  }, [chatSessionId]);

  useEffect(() => {
    if (!chatSessionId) {
      return;
    }
    getChatMessages().then();
  }, [chatSessionId, getChatMessages]);

  const handleSendMessage = async (value: string | { text: string }) => {
    // 处理 Sender 组件可能传递的不同格式
    const message = typeof value === "string" ? value : value.text;

    console.log(message);

    if (!message || !message.trim()) return;

    // 如果没有 chatSessionId，创建新会话
    if (!chatSessionId) {
      if (!agentId) {
        antdMessage.warning("请先创建一个智能体助手");
        return;
      }
      setLoading(true);
      try {
        const response = await createChatSession({
          agentId: agentId,
          title: message.slice(0, 20),
        });
        // 刷新聊天会话列表
        await refreshChatSessions();
        // 导航到新创建的会话
        navigate(`/chat/${response.chatSessionId}`, {
          replace: true,
          // 携带初始化消息
          state: {
            init: false,
            initMessage: message,
          },
        });
      } catch (error) {
        console.error("创建聊天会话失败:", error);
        antdMessage.error("创建聊天会话失败，请重试");
      } finally {
        setLoading(false);
      }
    } else {
      if (locationState?.init) {
        console.log("init", locationState.initMessage);
        await createChatMessage({
          agentId: agentId ?? "",
          sessionId: chatSessionId,
          role: "user",
          content: locationState.initMessage ?? "",
        });
      } else {
        console.log("ask", message);
        await createChatMessage({
          agentId: agentId ?? "",
          sessionId: chatSessionId,
          role: "user",
          content: message,
        });
      }
      await getChatMessages();
    }
  };

  const [displayAgentStatus, setDisplayAgentStatus] = useState<boolean>(false);
  const [agentStatusText, setAgentStatusText] = useState("");
  const [agentStatusType, setAgentStatusType] = useState<
    SseMessageType | undefined
  >(undefined);

  useShellPage({
    title: "聊天",
    description: chatSessionId
      ? "查看当前会话、运行状态和实时回复。"
      : "开始新对话，并在同一工作区内切换会话与管理智能体。",
    primaryAction: {
      label: "新建对话",
      onClick: () => navigate("/chat"),
    },
  });

  useEffect(() => {
    // sse 连接处理, 不是对话消息不开连接
    if (!chatSessionId) {
      return;
    }
    const es = new EventSource(
      `http://localhost:8080/sse/connect/${chatSessionId}`,
    );
    es.onmessage = (event) => {
      console.log("Received message:", event.data);
    };
    es.onerror = (error) => {
      console.error("SSE error:", error);
    };

    es.addEventListener("message", (event) => {
      // 解析 JSON
      const message = JSON.parse(event.data) as SseMessage;
      if (message.type === "AI_GENERATED_CONTENT") {
        // 将 AI 生成的内容存到 messages 中
        addMessage(message.payload.message);
      } else if (message.type === "AI_PLANNING") {
        setDisplayAgentStatus(true);
        setAgentStatusText(message.payload.statusText);
        setAgentStatusType("AI_PLANNING");
      } else if (message.type === "AI_THINKING") {
        setDisplayAgentStatus(true);
        setAgentStatusText(message.payload.statusText);
        setAgentStatusType("AI_THINKING");
      } else if (message.type === "AI_EXECUTING") {
        setDisplayAgentStatus(true);
        setAgentStatusText(message.payload.statusText);
        setAgentStatusType("AI_EXECUTING");
      } else if (message.type === "AI_DONE") {
        setDisplayAgentStatus(false);
        setAgentStatusText("");
        setAgentStatusType(undefined);
      } else {
        throw new Error(`Unknown message type: ${message.type}`);
      }
    });

    es.addEventListener("init", (event) => {
      console.log("Received init message:", event.data);
    });

    return () => {
      console.log("Closing SSE connection.");
      es.close();
    };
  }, [chatSessionId]);

  const conversationMain = chatSessionId ? (
    <div className="flex h-full flex-col rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)]">
      <AgentChatHistory
        messages={messages}
        displayAgentStatus={displayAgentStatus}
        agentStatusText={agentStatusText}
        agentStatusType={agentStatusType}
      />
      <div className="border-t border-[var(--shell-border)] bg-[var(--shell-surface)] p-4">
        <AgentChatInput onSend={handleSendMessage} />
      </div>
    </div>
  ) : (
    <EmptyAgentChatView
      agents={agents}
      loading={loading}
      handleSendMessage={handleSendMessage}
      selectedAgentId={agentId}
      onSelectAgentId={setAgentId}
    />
  );

  return (
    <>
      <PageCanvas
        secondary={(
          <ChatWorkspaceSidebar
            agents={agents}
            chatSessions={chatSessions}
            currentSessionId={chatSessionId}
            loading={chatSessionsLoading}
            onCreateChat={() => navigate("/chat")}
            onManageAgents={() => setIsAddAgentModalOpen(true)}
            onSelectSession={(id) => navigate(`/chat/${id}`)}
          />
        )}
        main={conversationMain}
      />
      <AddAgentModal
        open={isAddAgentModalOpen}
        onClose={() => setIsAddAgentModalOpen(false)}
        createAgentHandle={createAgentHandle}
        updateAgentHandle={updateAgentHandle}
        editingAgent={null}
      />
    </>
  );
};

export default AgentChatView;
