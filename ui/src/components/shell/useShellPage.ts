import { useEffect, useId } from "react";
import { useShellRegistration, type ShellPageState } from "./ShellProvider";

export function useShellPage(nextPage: ShellPageState) {
  const pageId = useId();
  const { registerPage, unregisterPage } = useShellRegistration();

  useEffect(() => {
    registerPage(pageId, nextPage);
  }, [
    nextPage.description,
    nextPage.detailContent,
    nextPage.detailTitle,
    nextPage.primaryAction,
    nextPage.secondaryActions,
    nextPage.title,
    pageId,
    registerPage,
  ]);

  useEffect(() => {
    return () => {
      unregisterPage(pageId);
    };
  }, [pageId, unregisterPage]);
}
