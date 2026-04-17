import React, { useEffect, useId, useState } from "react";
import mermaid from "mermaid";

interface WorkflowMermaidProps {
  content?: string | null;
  className?: string;
  placeholder?: string;
}

mermaid.initialize({
  startOnLoad: false,
  securityLevel: "loose",
  theme: "base",
  themeVariables: {
    background: "#020617",
    primaryColor: "#0f172a",
    primaryTextColor: "#e2e8f0",
    primaryBorderColor: "#22d3ee",
    lineColor: "#67e8f9",
    secondaryColor: "#0f172a",
    tertiaryColor: "#0f172a",
    clusterBkg: "#0f172a",
    clusterBorder: "#38bdf8",
  },
  flowchart: {
    useMaxWidth: true,
    htmlLabels: true,
    curve: "basis",
  },
});

const WorkflowMermaid: React.FC<WorkflowMermaidProps> = ({
  content,
  className = "",
  placeholder = "",
}) => {
  const diagramId = useId().replace(/:/g, "");
  const [svg, setSvg] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    const normalizedContent = content?.trim();
    let active = true;

    if (!normalizedContent) {
      setSvg("");
      setError("");
      return () => {
        active = false;
      };
    }

    setSvg("");
    setError("");

    mermaid
      .render(`workflow-mermaid-${diagramId}`, normalizedContent)
      .then((result) => {
        if (!active) return;
        setSvg(result.svg);
      })
      .catch((renderError) => {
        console.error(renderError);
        if (!active) return;
        setError("Mermaid 图渲染失败，请检查模板定义。");
      });

    return () => {
      active = false;
    };
  }, [content, diagramId]);

  if (error) {
    return (
      <div className={`rounded-2xl border border-rose-400/30 bg-rose-500/10 p-4 text-sm text-rose-100 ${className}`.trim()}>
        {error}
      </div>
    );
  }

  if (!content?.trim()) {
    return (
      <div className={`rounded-2xl border border-dashed border-white/15 bg-black/10 p-4 text-sm text-slate-300 ${className}`.trim()}>
        {placeholder}
      </div>
    );
  }

  if (!svg) {
    return (
      <div className={`rounded-2xl border border-white/10 bg-black/10 p-4 text-sm text-slate-300 ${className}`.trim()}>
        正在渲染 Mermaid 图...
      </div>
    );
  }

  return (
    <div
      className={`overflow-auto rounded-2xl border border-cyan-400/20 bg-slate-950/80 p-3 [&_svg]:h-auto [&_svg]:w-full [&_svg]:min-w-[720px] ${className}`.trim()}
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
};

export default WorkflowMermaid;
