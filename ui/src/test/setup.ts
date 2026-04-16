import "@testing-library/jest-dom/vitest";

const originalGetComputedStyle = window.getComputedStyle.bind(window);

window.getComputedStyle = ((element: Element, pseudoElement?: string) =>
  originalGetComputedStyle(element, pseudoElement ? undefined : pseudoElement)) as typeof window.getComputedStyle;
