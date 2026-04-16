import { describe, expect, it } from "vitest";
import { matchShellRoute } from "../shellConfig";

describe("matchShellRoute", () => {
  it("maps workflow routes to the workflow module", () => {
    expect(matchShellRoute("/workflow")).toMatchObject({
      moduleKey: "workflow",
      title: "工作流",
    });
  });

  it("maps chat detail routes back to the chat module", () => {
    expect(matchShellRoute("/chat/abc-123")?.moduleKey).toBe("chat");
  });

  it("keeps the legacy /agent alias inside chat", () => {
    expect(matchShellRoute("/agent")?.moduleKey).toBe("chat");
  });

  it.each(["/chatty", "/agent-builder", "/workflow-old", "/settings2"])(
    "does not match near-miss path %s",
    (pathname) => {
      expect(matchShellRoute(pathname)).toBeNull();
    },
  );

  it("returns null for unknown paths", () => {
    expect(matchShellRoute("/unknown")).toBeNull();
  });
});
