import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
  type ReactNode,
  type SetStateAction,
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
  setPage: React.Dispatch<SetStateAction<ShellPageState>>;
  resetPage: () => void;
  isDetailDrawerOpen: boolean;
  openDetailDrawer: () => void;
  closeDetailDrawer: () => void;
}

const ShellContext = createContext<ShellContextValue | null>(null);

export function ShellProvider({ children }: PropsWithChildren) {
  const [page, setPageState] = useState<ShellPageState>(DEFAULT_SHELL_PAGE_STATE);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);

  const setPage = useCallback((nextPage: SetStateAction<ShellPageState>) => {
    setIsDetailDrawerOpen(false);
    setPageState(nextPage);
  }, []);

  const resetPage = useCallback(() => {
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
      setPage,
      resetPage,
      isDetailDrawerOpen,
      openDetailDrawer,
      closeDetailDrawer,
    }),
    [closeDetailDrawer, isDetailDrawerOpen, openDetailDrawer, page, resetPage, setPage],
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
