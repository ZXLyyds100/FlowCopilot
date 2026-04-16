import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren,
  type ReactNode,
} from "react";

export interface ShellAction {
  label: string;
  onClick: () => void;
}

export interface ShellPageState {
  title: string;
  description: string;
  primaryAction?: ShellAction;
  secondaryActions?: ShellAction[];
  detailTitle?: string;
  detailContent?: ReactNode;
}

export const DEFAULT_SHELL_PAGE_STATE: ShellPageState = {
  title: "FlowCopilot",
  description: "专业操作台",
};

interface ShellContextValue {
  page: ShellPageState;
  registerPage: (pageId: string, nextPage: ShellPageState) => void;
  unregisterPage: (pageId: string) => void;
  isDetailDrawerOpen: boolean;
  openDetailDrawer: () => void;
  closeDetailDrawer: () => void;
}

const ShellContext = createContext<ShellContextValue | null>(null);

export function ShellProvider({ children }: PropsWithChildren) {
  const [page, setPageState] = useState<ShellPageState>(DEFAULT_SHELL_PAGE_STATE);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);
  const activePageIdRef = useRef<string | null>(null);

  const registerPage = useCallback((pageId: string, nextPage: ShellPageState) => {
    if (activePageIdRef.current !== pageId) {
      activePageIdRef.current = pageId;
      setIsDetailDrawerOpen(false);
    }

    setPageState(nextPage);
  }, []);

  const unregisterPage = useCallback((pageId: string) => {
    if (activePageIdRef.current !== pageId) {
      return;
    }

    activePageIdRef.current = null;
    setIsDetailDrawerOpen(false);
    setPageState(DEFAULT_SHELL_PAGE_STATE);
  }, []);

  const openDetailDrawer = useCallback(() => {
    setIsDetailDrawerOpen(true);
  }, []);

  const closeDetailDrawer = useCallback(() => {
    setIsDetailDrawerOpen(false);
  }, []);

  const value = useMemo(
    () => ({
      page,
      registerPage,
      unregisterPage,
      isDetailDrawerOpen,
      openDetailDrawer,
      closeDetailDrawer,
    }),
    [closeDetailDrawer, isDetailDrawerOpen, openDetailDrawer, page, registerPage, unregisterPage],
  );

  return <ShellContext.Provider value={value}>{children}</ShellContext.Provider>;
}

export function useShellContext() {
  const context = useContext(ShellContext);

  if (!context) {
    throw new Error("useShellContext must be used within a ShellProvider");
  }

  return context;
}
