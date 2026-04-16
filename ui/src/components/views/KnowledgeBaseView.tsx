import React, { useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Card,
  Typography,
  Button,
  Table,
  Popconfirm,
  Space,
  message,
  Empty,
} from "antd";
import {
  BookOutlined,
  UploadOutlined,
  DeleteOutlined,
  FileOutlined,
} from "@ant-design/icons";
import AddKnowledgeBaseModal from "../modals/AddKnowledgeBaseModal.tsx";
import PageCanvas from "../shell/PageCanvas.tsx";
import { useShellPage } from "../shell/useShellPage.ts";
import { useKnowledgeBases } from "../../hooks/useKnowledgeBases.ts";
import { useDocuments } from "../../hooks/useDocuments.ts";
import { uploadDocument, type DocumentVO } from "../../api/api.ts";
import KnowledgeBaseWorkspaceSidebar from "./knowledgeBaseView/KnowledgeBaseWorkspaceSidebar.tsx";

const { Title, Text, Paragraph } = Typography;

interface KnowledgeBaseViewProps {
  knowledgeBaseId?: string;
}

const KnowledgeBaseView: React.FC<KnowledgeBaseViewProps> = ({
  knowledgeBaseId,
}) => {
  const navigate = useNavigate();
  const uploadInputRef = useRef<HTMLInputElement | null>(null);
  const { knowledgeBases, createKnowledgeBaseHandle } = useKnowledgeBases();
  const { documents, loading, refreshDocuments, deleteDocument } =
    useDocuments(knowledgeBaseId);

  const [uploading, setUploading] = useState(false);
  const [isAddKnowledgeBaseModalOpen, setIsAddKnowledgeBaseModalOpen] = useState(false);

  // 查找当前知识库的详细信息
  const currentKnowledgeBase = useMemo(() => {
    if (!knowledgeBaseId) return null;
    return (
      knowledgeBases.find((kb) => kb.knowledgeBaseId === knowledgeBaseId) ||
      null
    );
  }, [knowledgeBaseId, knowledgeBases]);

  const handleUpload = async (file: File) => {
    if (!knowledgeBaseId) {
      message.error("请先选择知识库");
      return;
    }

    setUploading(true);

    try {
      await uploadDocument(knowledgeBaseId, file);
      message.success("文档上传成功");
      await refreshDocuments();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "上传失败");
    } finally {
      setUploading(false);
    }
  };

  useShellPage({
    title: currentKnowledgeBase?.name || "知识库",
    description: currentKnowledgeBase?.description || "浏览知识库、上传文档并管理资料内容。",
    primaryAction: knowledgeBaseId
      ? {
          label: "上传文档",
          onClick: () => uploadInputRef.current?.click(),
        }
      : {
          label: "新建知识库",
          onClick: () => setIsAddKnowledgeBaseModalOpen(true),
        },
  });

  // 格式化文件大小
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  };

  // 表格列定义
  const columns = [
    {
      title: "文件名",
      dataIndex: "filename",
      key: "filename",
      render: (text: string) => (
        <Space>
          <FileOutlined />
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: "类型",
      dataIndex: "filetype",
      key: "filetype",
      width: 120,
    },
    {
      title: "大小",
      dataIndex: "size",
      key: "size",
      width: 120,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_: unknown, record: DocumentVO) => (
        <Popconfirm
          title="确定要删除这个文档吗？"
          description="删除后将无法恢复"
          onConfirm={() => deleteDocument(record.id)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="text" danger icon={<DeleteOutlined />} size="small">
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  const contentMain = !knowledgeBaseId ? (
    <div className="flex h-full items-center justify-center rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)]">
      <Empty description="请先从左侧选择一个知识库。" />
    </div>
  ) : !currentKnowledgeBase ? (
    <div className="flex h-full items-center justify-center rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)]">
      <Empty description="知识库不存在，请检查当前选择。" />
    </div>
  ) : (
    <div className="flex h-full flex-col gap-4 overflow-y-auto rounded-3xl border border-[var(--shell-border)] bg-[var(--shell-surface)] p-6">
      <Card>
        <div className="flex items-start gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-[var(--shell-canvas)] text-3xl">
            <BookOutlined />
          </div>
          <div className="flex-1">
            <Title level={3} className="mb-2">
              {currentKnowledgeBase.name}
            </Title>
            {currentKnowledgeBase.description && (
              <Paragraph className="mb-0 text-gray-600">
                {currentKnowledgeBase.description}
              </Paragraph>
            )}
            <Text type="secondary" className="text-sm">
              知识库 ID: {currentKnowledgeBase.knowledgeBaseId}
            </Text>
          </div>
        </div>
      </Card>

      <Card title="上传文档">
        <input
          ref={uploadInputRef}
          type="file"
          accept=".md"
          className="hidden"
          onChange={async (event) => {
            const file = event.target.files?.[0];
            if (file) {
              await handleUpload(file);
              event.target.value = "";
            }
          }}
        />
        <Button
          type="primary"
          icon={<UploadOutlined />}
          loading={uploading}
          size="large"
          onClick={() => uploadInputRef.current?.click()}
        >
          选择文件上传
        </Button>
        <Text type="secondary" className="mt-2 block text-xs">
          支持格式: Markdown
        </Text>
      </Card>

      <Card title={`文档列表 (${documents.length})`}>
        {loading ? (
          <div className="py-8 text-center">
            <Text type="secondary">加载中...</Text>
          </div>
        ) : documents.length === 0 ? (
          <Empty description={<Text type="secondary">暂无文档，请上传文档</Text>} />
        ) : (
          <Table
            columns={columns}
            dataSource={documents}
            rowKey="id"
            pagination={{
              pageSize: 10,
              showTotal: (total) => `共 ${total} 条`,
            }}
          />
        )}
      </Card>
    </div>
  );

  return (
    <>
      <PageCanvas
        secondary={(
          <KnowledgeBaseWorkspaceSidebar
            knowledgeBases={knowledgeBases}
            selectedKnowledgeBaseId={knowledgeBaseId}
            onCreateKnowledgeBase={() => setIsAddKnowledgeBaseModalOpen(true)}
            onSelectKnowledgeBase={(id) => navigate(`/knowledge-base/${id}`)}
          />
        )}
        main={contentMain}
      />
      <AddKnowledgeBaseModal
        open={isAddKnowledgeBaseModalOpen}
        onClose={() => setIsAddKnowledgeBaseModalOpen(false)}
        createKnowledgeBaseHandle={createKnowledgeBaseHandle}
      />
    </>
  );
};

export default KnowledgeBaseView;
