import {
  BookOutlined,
  ForkOutlined,
  NodeIndexOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { Button, Card, Flex, Input, Select, Space, Tag, Typography } from "antd";
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
    <Card
      title={(
        <div>
          <Space className="mb-1">
            <ForkOutlined />
            <span>FlowCopilot Graph Studio</span>
          </Space>
          <Typography.Text className="!text-slate-500">
            第四阶段 · LangGraph4j 工作流驾驶舱
          </Typography.Text>
        </div>
      )}
      extra={<Tag color="geekblue">任务编排</Tag>}
      className="border-none bg-white/90 text-slate-800 shadow-xl shadow-slate-200/70 [&_.ant-card-head-title]:text-slate-900"
    >
      <div className="grid grid-cols-[minmax(0,1.15fr)_320px] gap-5">
        <Flex vertical gap="middle" className="w-full">
          <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
            <Typography.Title level={5} className="!mb-2 !text-slate-900">
              任务输入
            </Typography.Title>
            <Typography.Paragraph className="!mb-4 !text-slate-600">
              描述目标、补充上下文，并选择执行模板。创建后会立即进入 Graph 实时运行视图。
            </Typography.Paragraph>
            <Flex vertical gap="middle" className="w-full">
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
            </Flex>
          </div>
        </Flex>

        <div className="rounded-3xl bg-slate-950 p-5 text-slate-100">
          <Flex vertical gap="large" className="w-full">
            <div>
              <Space className="mb-2">
                <NodeIndexOutlined />
                <Typography.Text className="!text-slate-100">推荐配置</Typography.Text>
              </Space>
              <Flex vertical gap="middle" className="w-full">
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
              </Flex>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
              <Space className="mb-2">
                <BookOutlined />
                <Typography.Text className="!text-slate-100">执行提示</Typography.Text>
              </Space>
              <ul className="mb-0 space-y-2 pl-5 text-sm leading-6 text-slate-300">
                <li>标题可为空，系统会按任务内容生成默认命名。</li>
                <li>模板决定节点路径、审批策略与最终发布方式。</li>
                <li>选择知识库后，Retriever 会优先引用已索引内容。</li>
              </ul>
            </div>
          </Flex>
        </div>
      </div>
    </Card>
  );
}
