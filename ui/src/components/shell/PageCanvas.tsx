import type { ReactNode } from "react";

interface PageCanvasProps {
  secondary?: ReactNode;
  main: ReactNode;
}

export default function PageCanvas({ secondary, main }: PageCanvasProps) {
  return (
    <div
      className={`grid h-full gap-4 ${secondary ? "grid-cols-1 lg:grid-cols-[320px_minmax(0,1fr)]" : "grid-cols-1"}`}
    >
      {secondary ? <div className="min-h-0">{secondary}</div> : null}
      <div className="min-h-0">{main}</div>
    </div>
  );
}
