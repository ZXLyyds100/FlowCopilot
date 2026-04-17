import React from "react";
import XMarkdown from "@ant-design/x-markdown";

interface WorkflowMarkdownProps {
  content?: string | null;
  className?: string;
  placeholder?: string;
}

const BASE_CLASS_NAME = [
  "workflow-markdown",
  "[&_h1]:mb-4 [&_h1]:text-2xl [&_h1]:font-semibold [&_h1]:leading-tight",
  "[&_h2]:mb-3 [&_h2]:text-xl [&_h2]:font-semibold [&_h2]:leading-tight",
  "[&_h3]:mb-2 [&_h3]:text-lg [&_h3]:font-semibold",
  "[&_p]:mb-3 [&_p:last-child]:mb-0",
  "[&_ul]:mb-3 [&_ul]:list-disc [&_ul]:pl-5",
  "[&_ol]:mb-3 [&_ol]:list-decimal [&_ol]:pl-5",
  "[&_li]:mb-1",
  "[&_pre]:mb-3 [&_pre]:overflow-auto [&_pre]:rounded-2xl [&_pre]:bg-slate-950 [&_pre]:p-4 [&_pre]:text-sm [&_pre]:leading-7",
  "[&_code]:rounded [&_code]:bg-black/5 [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-[0.95em]",
  "[&_pre_code]:bg-transparent [&_pre_code]:p-0",
  "[&_blockquote]:my-3 [&_blockquote]:border-l-4 [&_blockquote]:border-current/20 [&_blockquote]:pl-4 [&_blockquote]:opacity-80",
  "[&_a]:text-cyan-700 [&_a]:underline [&_a]:underline-offset-2",
].join(" ");

const WorkflowMarkdown: React.FC<WorkflowMarkdownProps> = ({
  content,
  className = "",
  placeholder = "",
}) => {
  const normalizedContent = content?.trim() ? content : placeholder;

  return (
    <div className={`${BASE_CLASS_NAME} ${className}`.trim()}>
      <XMarkdown streaming={{ enableAnimation: false, hasNextChunk: true }}>
        {normalizedContent}
      </XMarkdown>
    </div>
  );
};

export default WorkflowMarkdown;
