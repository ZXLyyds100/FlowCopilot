export default function SettingsPlaceholderView() {
  return (
    <div className="flex h-full items-center justify-center rounded-3xl border border-[var(--shell-border)] bg-white">
      <div className="space-y-2 px-6 text-center">
        <h2 className="text-lg font-semibold text-slate-900">设置</h2>
        <p className="text-sm text-slate-500">
          本阶段只保留设置入口，具体配置内容后续单独设计。
        </p>
      </div>
    </div>
  );
}
