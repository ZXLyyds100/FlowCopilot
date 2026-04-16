import { MessageOutlined, PlusOutlined, RobotOutlined } from "@ant-design/icons";
import { Button } from "antd";
import type { AgentVO, ChatSessionVO } from "../../../api/api.ts";

interface ChatWorkspaceSidebarProps {
  agents: AgentVO[];
  chatSessions: ChatSessionVO[];
  currentSessionId?: string;
  loading: boolean;
  onCreateChat: () => void;
  onManageAgents: () => void;
  onSelectSession: (chatSessionId: string) => void;
}

export default function ChatWorkspaceSidebar({
  agents,
  chatSessions,
  currentSessionId,
  loading,
  onCreateChat,
  onManageAgents,
  onSelectSession,
}: ChatWorkspaceSidebarProps) {
  return (
    <aside className="flex h-full flex-col gap-4 rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)] p-4">
      <div className="space-y-2">
        <Button
          type="primary"
          icon={<PlusOutlined />}
          block
          onClick={onCreateChat}
          aria-label="新建对话"
        >
          新建对话
        </Button>
        <Button
          icon={<RobotOutlined />}
          block
          onClick={onManageAgents}
          aria-label="管理智能体"
        >
          管理智能体
        </Button>
      </div>

      <section className="rounded-2xl bg-[var(--shell-canvas)] px-4 py-3">
        <div className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--shell-muted)]">
          聊天上下文
        </div>
        <div className="mt-2 text-sm text-[var(--shell-text)]">
          智能体 {agents.length} 个
        </div>
        <div className="mt-1 text-sm text-[var(--shell-muted)]">
          会话切换和智能体管理都在聊天页内完成。
        </div>
      </section>

      <section className="min-h-0 flex-1">
        <div className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--shell-muted)]">
          最近会话
        </div>
        <div className="flex max-h-full flex-col gap-2 overflow-y-auto pr-1">
          {loading ? (
            <div className="rounded-2xl border border-dashed border-[var(--shell-border)] px-4 py-5 text-sm text-[var(--shell-muted)]">
              正在加载会话...
            </div>
          ) : chatSessions.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-[var(--shell-border)] px-4 py-5 text-sm text-[var(--shell-muted)]">
              还没有历史会话，先从右侧开始一段新的对话。
            </div>
          ) : (
            chatSessions.map((session) => {
              const isActive = session.id === currentSessionId;

              return (
                <button
                  key={session.id}
                  type="button"
                  onClick={() => onSelectSession(session.id)}
                  aria-label={session.title || "未命名会话"}
                  className={`flex w-full items-center gap-3 rounded-2xl border px-3 py-3 text-left transition ${
                    isActive
                      ? "border-slate-900 bg-slate-950 text-white shadow-lg"
                      : "border-[var(--shell-border)] bg-white text-[var(--shell-text)] hover:border-slate-400 hover:bg-slate-50"
                  }`}
                >
                  <MessageOutlined className={isActive ? "text-cyan-200" : "text-[var(--shell-muted)]"} />
                  <span className="truncate text-sm font-medium">
                    {session.title || "未命名会话"}
                  </span>
                </button>
              );
            })
          )}
        </div>
      </section>
    </aside>
  );
}
