import { useEffect } from "react";
import { useShellContext, type ShellPageState } from "./ShellProvider";

export function useShellPage(nextPage: ShellPageState) {
  const { setPage, resetPage } = useShellContext();

  useEffect(() => {
    setPage(nextPage);

    return () => {
      resetPage();
    };
  }, [
    nextPage.title,
    nextPage.description,
    nextPage.primaryAction,
    nextPage.secondaryActions,
    nextPage.detailTitle,
    nextPage.detailContent,
    resetPage,
    setPage,
  ]);
}
