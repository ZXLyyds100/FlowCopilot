import { useEffect, useId, useRef } from "react";
import { useShellContext, type ShellPageState } from "./ShellProvider";

export function useShellPage(nextPage: ShellPageState) {
  const pageId = useId();
  const { registerPage, unregisterPage } = useShellContext();
  const initialPageRef = useRef(nextPage);

  useEffect(() => {
    registerPage(pageId, initialPageRef.current);
  }, [pageId, registerPage]);

  useEffect(() => {
    return () => {
      unregisterPage(pageId);
    };
  }, [pageId, unregisterPage]);
}
