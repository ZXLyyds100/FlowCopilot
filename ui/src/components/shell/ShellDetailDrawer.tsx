import { Button, Drawer } from "antd";
import { useShellContext } from "./ShellProvider";

export default function ShellDetailDrawer() {
  const { closeDetailDrawer, fallback, isDetailDrawerOpen, page } = useShellContext();

  return (
    <Drawer
      title={page?.detailTitle || page?.title || fallback.title}
      placement="right"
      size={360}
      open={Boolean(page?.detailContent) && isDetailDrawerOpen}
      onClose={closeDetailDrawer}
      footer={(
        <div className="flex justify-end">
          <Button onClick={closeDetailDrawer}>关闭</Button>
        </div>
      )}
    >
      {page?.detailContent}
    </Drawer>
  );
}
