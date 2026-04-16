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

export interface ShellFallbackState {
  title: string;
  description: string;
}

export const DEFAULT_SHELL_FALLBACK: ShellFallbackState = {
  title: "FlowCopilot",
  description: "专业操作台",
};

interface ShellStateContextValue {
  page: ShellPageState | null;
  fallback: ShellFallbackState;
  isDetailDrawerOpen: boolean;
  openDetailDrawer: () => void;
  closeDetailDrawer: () => void;
}

interface ShellRegistrationContextValue {
  registerPage: (pageId: string, nextPage: ShellPageState) => void;
  unregisterPage: (pageId: string) => void;
}

const ShellStateContext = createContext<ShellStateContextValue | null>(null);
const ShellRegistrationContext = createContext<ShellRegistrationContextValue | null>(null);

export function ShellProvider({ children }: PropsWithChildren) {
  const [page, setPageState] = useState<ShellPageState | null>(null);
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
    setPageState(null);
  }, []);

  const openDetailDrawer = useCallback(() => {
    setIsDetailDrawerOpen(true);
  }, []);

  const closeDetailDrawer = useCallback(() => {
    setIsDetailDrawerOpen(false);
  }, []);

  const stateValue = useMemo(
    () => ({
      page,
      fallback: DEFAULT_SHELL_FALLBACK,
      isDetailDrawerOpen,
      openDetailDrawer,
      closeDetailDrawer,
    }),
    [closeDetailDrawer, isDetailDrawerOpen, openDetailDrawer, page],
  );

  const registrationValue = useMemo(
    () => ({
      registerPage,
      unregisterPage,
    }),
    [registerPage, unregisterPage],
  );

  return (
    <ShellRegistrationContext.Provider value={registrationValue}>
      <ShellStateContext.Provider value={stateValue}>{children}</ShellStateContext.Provider>
    </ShellRegistrationContext.Provider>
  );
}

export function useShellContext() {
  const context = useContext(ShellStateContext);

  if (!context) {
    throw new Error("useShellContext must be used within a ShellProvider");
  }

  return context;
}

export function useShellRegistration() {
  const context = useContext(ShellRegistrationContext);

  if (!context) {
    throw new Error("useShellRegistration must be used within a ShellProvider");
  }

  return context;
}
