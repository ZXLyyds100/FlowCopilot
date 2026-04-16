import { ForkOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { Avatar, Button, Card, Input, Select, Space, Typography } from "antd";
import type { WorkflowTemplateVO } from "../../../api/api.ts";
import type { KnowledgeBase } from "../../../types";

const { TextArea } = Input;

interface WorkflowCreateCardProps {
  knowledgeBaseId?: string;
  knowledgeBases: KnowledgeBase[];
  loading: boolean;
  taskInput: string;
  templateCode: string;
  templates: WorkflowTemplateVO[];
  title: string;
  onCreateWorkflow: () => void;
  onKnowledgeBaseChange: (value: string | undefined) => void;
  onTaskInputChange: (value: string) => void;
  onTemplateCodeChange: (value: string) => void;
  onTitleChange: (value: string) => void;
}

export default function WorkflowCreateCard({
  knowledgeBaseId,
  knowledgeBases,
  loading,
  taskInput,
  templateCode,
  templates,
  title,
  onCreateWorkflow,
  onKnowledgeBaseChange,
  onTaskInputChange,
  onTemplateCodeChange,
  onTitleChange,
}: WorkflowCreateCardProps) {
  return (
    <Card className="overflow-hidden border-none bg-slate-950 py-4 text-white shadow-2xl shadow-slate-200">
      <div className="absolute -right-20 -top-24 h-56 w-56 rounded-full bg-cyan-400/30 blur-3xl" />
      <div className="absolute -bottom-24 -left-16 h-56 w-56 rounded-full bg-amber-300/20 blur-3xl" />
      <div className="relative">
        <Space align="center" className="mb-5">
          <Avatar size={44} className="bg-white text-slate-950" icon={<ForkOutlined />} />
          <div>
            <Typography.Title level={4} className="!mb-0 !text-white">
              FlowCopilot Graph Studio
            </Typography.Title>
            <Typography.Text className="!text-slate-300">
              第四阶段 · LangGraph4j 工作流驾驶舱
            </Typography.Text>
          </div>
        </Space>

        <Space direction="vertical" className="w-full" size="middle">
          <Input
            size="large"
            placeholder="可选：任务标题"
            value={title}
            onChange={(event) => onTitleChange(event.target.value)}
          />
          <TextArea
            rows={7}
            placeholder="输入复杂任务，例如：基于知识库整理一份项目答辩介绍，并给出可执行路线"
            value={taskInput}
            onChange={(event) => onTaskInputChange(event.target.value)}
          />
          <Select
            size="large"
            value={templateCode}
            onChange={onTemplateCodeChange}
            options={templates.map((template) => ({
              label: `${template.name} · ${template.code}`,
              value: template.code,
            }))}
            placeholder="选择 Graph 模板"
          />
          <Select
            allowClear
            size="large"
            placeholder="可选：选择知识库增强"
            value={knowledgeBaseId}
            onChange={onKnowledgeBaseChange}
            options={knowledgeBases.map((kb) => ({
              label: kb.name,
              value: kb.knowledgeBaseId,
            }))}
          />
          <Button
            type="primary"
            size="large"
            icon={<ThunderboltOutlined />}
            loading={loading}
            onClick={onCreateWorkflow}
            block
          >
            启动 Graph 智能流程
          </Button>
        </Space>
      </div>
    </Card>
  );
}
