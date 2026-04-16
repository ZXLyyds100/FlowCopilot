import React, { useMemo, useState } from "react";
import { Select, Typography } from "antd";
import {
  DownOutlined,
  MessageOutlined,
  RobotOutlined,
} from "@ant-design/icons";
import { Sender } from "@ant-design/x";
import type { AgentVO } from "../../../api/api.ts";
import { getAgentEmoji } from "../../../utils";

const { Paragraph, Title, Text } = Typography;

interface DefaultAgentChatViewProps {
  handleSendMessage: (message: string) => Promise<void>;
  loading: boolean;
  agents: AgentVO[];
  selectedAgentId: string;
  onSelectAgentId: (agentId: string) => void;
}

const EmptyAgentChatView: React.FC<DefaultAgentChatViewProps> = ({
  handleSendMessage,
  loading,
  agents,
  selectedAgentId,
  onSelectAgentId,
}) => {
  const [message, setMessage] = useState("");

  const agentsWithEmoji = useMemo(() => {
    return agents.map((agent) => ({
      ...agent,
      emoji: getAgentEmoji(agent.id),
    }));
  }, [agents]);

  const effectiveAgentId = useMemo(() => {
    if (selectedAgentId) {
      return selectedAgentId;
    }
    return agents.length > 0 ? agents[0].id : null;
  }, [selectedAgentId, agents]);

  return (
    <div className="flex h-full flex-col rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)]">
      <div className="border-b border-[var(--shell-border)] px-6 py-5">
        {agents.length > 0 ? (
          <div className="flex items-center justify-between gap-4">
            <div>
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--shell-muted)]">
                默认智能体
              </div>
              <div className="mt-2 text-sm text-[var(--shell-muted)]">
                选择一个智能体开始新会话。
              </div>
            </div>
            <Select
              value={effectiveAgentId}
              onChange={onSelectAgentId}
              style={{ width: 200 }}
              className="agent-selector"
              suffixIcon={<DownOutlined className="text-gray-400" />}
              placeholder="选择智能体助手"
              optionRender={(option) => (
                <div className="flex items-center gap-2">
                  <span className="text-lg">
                    {agentsWithEmoji.find((a) => a.id === option.value)?.emoji}
                  </span>
                  <span className="text-sm">{option.label}</span>
                </div>
              )}
              options={agentsWithEmoji.map((agent) => ({
                value: agent.id,
                label: agent.name,
              }))}
            />
          </div>
        ) : (
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--shell-muted)]">
              默认智能体
            </div>
            <div className="mt-2 text-sm text-[var(--shell-muted)]">
              还没有可用智能体，请先在左侧点击“管理智能体”创建一个。
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-1 items-center justify-center px-6 py-10">
        <div className="max-w-3xl space-y-8">
          <div className="text-center">
            <Title level={2} className="!mb-3">
              开始新的对话
            </Title>
            <Paragraph type="secondary" className="!mb-0 text-base">
              当前壳层已经把会话切换、智能体管理和聊天输入收回到一个工作区里。
              先选择智能体，再用下面的输入框直接生成新会话。
            </Paragraph>
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <div className="rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-canvas)] p-5">
              <RobotOutlined className="text-xl text-slate-900" />
              <div className="mt-4 text-base font-semibold text-slate-900">对话上下文</div>
              <Text type="secondary">
                每个会话绑定智能体，适合持续追问和长链任务。
              </Text>
            </div>
            <div className="rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-canvas)] p-5">
              <MessageOutlined className="text-xl text-slate-900" />
              <div className="mt-4 text-base font-semibold text-slate-900">即时开聊</div>
              <Text type="secondary">
                输入第一条消息后自动创建会话，不需要额外步骤。
              </Text>
            </div>
            <div className="rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-canvas)] p-5">
              <DownOutlined className="text-xl text-slate-900" />
              <div className="mt-4 text-base font-semibold text-slate-900">快速切换</div>
              <Text type="secondary">
                历史会话保留在左侧，方便切换和继续上下文。
              </Text>
            </div>
          </div>

          <div className="rounded-[28px] border border-[var(--shell-border)] bg-white p-4 shadow-sm">
            <Sender
              onSubmit={async () => {
                if (!effectiveAgentId || !message.trim()) {
                  return;
                }
                await handleSendMessage(message);
                setMessage("");
              }}
              value={message}
              loading={loading}
              placeholder="输入消息开始对话..."
              onChange={(value) => {
                setMessage(value);
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmptyAgentChatView;
