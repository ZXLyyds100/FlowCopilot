import { MinusOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, Space, Typography } from "antd";
import { useEffect, useRef, useState } from "react";
import WorkflowMermaid from "./WorkflowMermaid.tsx";

interface WorkflowMermaidViewerProps {
  content?: string | null;
}

const MIN_SCALE = 0.2;
const MAX_SCALE = 2.5;
const ZOOM_STEP_BUTTON = 0.2;
const ZOOM_STEP_WHEEL = 0.1;

function clampScale(value: number) {
  return Math.min(MAX_SCALE, Math.max(MIN_SCALE, Number(value.toFixed(2))));
}

export default function WorkflowMermaidViewer({ content }: WorkflowMermaidViewerProps) {
  const [scale, setScale] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const dragStateRef = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null);

  useEffect(() => {
    const handleMouseMove = (event: MouseEvent) => {
      const dragState = dragStateRef.current;
      if (!dragState) return;

      setOffset({
        x: dragState.originX + (event.clientX - dragState.startX),
        y: dragState.originY + (event.clientY - dragState.startY),
      });
    };

    const handleMouseUp = () => {
      dragStateRef.current = null;
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, []);

  const resetView = () => {
    setScale(1);
    setOffset({ x: 0, y: 0 });
  };

  const handleZoom = (delta: number) => {
    setScale((current) => clampScale(current + delta));
  };

  return (
    <div className="flex h-full min-h-0 flex-col rounded-[28px] bg-slate-950 text-slate-100">
      <div className="flex items-center justify-between gap-3 border-b border-white/10 px-5 py-4">
        <div>
          <Typography.Title level={5} className="!mb-1 !text-slate-100">
            Mermaid 图预览
          </Typography.Title>
          <Typography.Text className="!text-slate-400">
            鼠标拖动画布，滚轮或按钮缩放。
          </Typography.Text>
        </div>
        <Space>
          <Button aria-label="缩小" icon={<MinusOutlined />} onClick={() => handleZoom(-ZOOM_STEP_BUTTON)} />
          <Button aria-label="放大" icon={<PlusOutlined />} onClick={() => handleZoom(ZOOM_STEP_BUTTON)} />
          <Button aria-label="重置视图" icon={<ReloadOutlined />} onClick={resetView} />
        </Space>
      </div>

      <div
        className="relative flex-1 overflow-hidden rounded-b-[28px] bg-[radial-gradient(circle_at_top,#1e293b_0,#020617_68%)]"
        data-testid="workflow-mermaid-viewport"
        onMouseDown={(event) => {
          dragStateRef.current = {
            startX: event.clientX,
            startY: event.clientY,
            originX: offset.x,
            originY: offset.y,
          };
        }}
        onWheel={(event) => {
          event.preventDefault();
          handleZoom(event.deltaY < 0 ? ZOOM_STEP_WHEEL : -ZOOM_STEP_WHEEL);
        }}
      >
        <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(148,163,184,0.08)_1px,transparent_1px),linear-gradient(90deg,rgba(148,163,184,0.08)_1px,transparent_1px)] bg-[size:32px_32px]" />
        <div className="flex h-full items-center justify-center overflow-hidden p-6">
          <div
            className="will-change-transform"
            data-testid="workflow-mermaid-canvas"
            style={{ transform: `translate(${offset.x}px, ${offset.y}px) scale(${scale})` }}
          >
            <WorkflowMermaid
              content={content}
              className="min-w-[960px] shadow-[0_24px_80px_rgba(8,145,178,0.18)]"
              placeholder="Graph definition loading..."
            />
          </div>
        </div>
      </div>
    </div>
  );
}
