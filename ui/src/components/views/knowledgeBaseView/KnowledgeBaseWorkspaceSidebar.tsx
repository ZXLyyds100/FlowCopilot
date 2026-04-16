import { BookOutlined, PlusOutlined } from "@ant-design/icons";
import { Button } from "antd";
import type { KnowledgeBase } from "../../../types";

interface KnowledgeBaseWorkspaceSidebarProps {
  knowledgeBases: KnowledgeBase[];
  selectedKnowledgeBaseId?: string;
  onCreateKnowledgeBase: () => void;
  onSelectKnowledgeBase: (knowledgeBaseId: string) => void;
}

export default function KnowledgeBaseWorkspaceSidebar({
  knowledgeBases,
  selectedKnowledgeBaseId,
  onCreateKnowledgeBase,
  onSelectKnowledgeBase,
}: KnowledgeBaseWorkspaceSidebarProps) {
  return (
    <aside className="flex h-full flex-col gap-4 rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)] p-4">
      <Button
        type="primary"
        icon={<PlusOutlined />}
        block
        onClick={onCreateKnowledgeBase}
        aria-label="新建知识库"
      >
        新建知识库
      </Button>

      <section className="rounded-2xl bg-[var(--shell-canvas)] px-4 py-3">
        <div className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--shell-muted)]">
          资料库
        </div>
        <div className="mt-2 text-sm text-[var(--shell-muted)]">
          在这里切换知识库，右侧查看资料详情和文档清单。
        </div>
      </section>

      <div className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--shell-muted)]">
        知识库列表
      </div>
      <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto pr-1">
        {knowledgeBases.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-[var(--shell-border)] px-4 py-5 text-sm text-[var(--shell-muted)]">
            还没有知识库，先创建一个用于上传资料。
          </div>
        ) : (
          knowledgeBases.map((kb) => {
            const isActive = kb.knowledgeBaseId === selectedKnowledgeBaseId;

            return (
              <button
                key={kb.knowledgeBaseId}
                type="button"
                onClick={() => onSelectKnowledgeBase(kb.knowledgeBaseId)}
                aria-label={kb.name}
                className={`flex w-full items-start gap-3 rounded-2xl border px-3 py-3 text-left transition ${
                  isActive
                    ? "border-slate-900 bg-slate-950 text-white shadow-lg"
                    : "border-[var(--shell-border)] bg-white text-[var(--shell-text)] hover:border-slate-400 hover:bg-slate-50"
                }`}
              >
                <BookOutlined className={isActive ? "text-cyan-200" : "text-[var(--shell-muted)]"} />
                <div className="min-w-0">
                  <div className="truncate text-sm font-medium">{kb.name}</div>
                  {kb.description ? (
                    <div className={`mt-1 line-clamp-2 text-xs ${isActive ? "text-slate-300" : "text-[var(--shell-muted)]"}`}>
                      {kb.description}
                    </div>
                  ) : null}
                </div>
              </button>
            );
          })
        )}
      </div>
    </aside>
  );
}
