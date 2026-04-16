import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PageCanvas from "../PageCanvas";

describe("PageCanvas", () => {
  it("uses a single-column layout when no secondary content is provided", () => {
    render(<PageCanvas main={<div>main panel</div>} />);

    const canvas = screen.getByText("main panel").parentElement?.parentElement;

    expect(canvas).toHaveClass("grid-cols-1");
    expect(canvas).not.toHaveClass("lg:grid-cols-[320px_minmax(0,1fr)]");
  });
});
