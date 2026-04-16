import {
  createElement,
  createContext,
  useContext,
  useEffect,
  useId,
  type PropsWithChildren,
} from "react";
import { useShellRegistration, type ShellPageState } from "./ShellProvider";

const ShellPageActivityContext = createContext(true);

export function ShellPageActivityProvider({
  active,
  children,
}: PropsWithChildren<{ active: boolean }>) {
  return createElement(ShellPageActivityContext.Provider, { value: active }, children);
}

export function useShellPage(nextPage: ShellPageState) {
  const pageId = useId();
  const isActive = useContext(ShellPageActivityContext);
  const { registerPage, unregisterPage } = useShellRegistration();

  useEffect(() => {
    if (!isActive) {
      unregisterPage(pageId);
      return;
    }

    registerPage(pageId, nextPage);

    return () => {
      unregisterPage(pageId);
    };
  }, [
    isActive,
    nextPage.description,
    nextPage.detailContent,
    nextPage.detailTitle,
    nextPage.primaryAction,
    nextPage.secondaryActions,
    nextPage.title,
    pageId,
    registerPage,
    unregisterPage,
  ]);
}
