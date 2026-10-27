import { render, screen } from "@testing-library/react";
import { beforeAll, describe, expect, it, vi } from "vitest";

import { HeroTitle } from "@/components/landing/HeroTitle";

// The landing page is a Server Component; the H1 sanitizer is the unit under
// test (page.tsx feeds it `t.raw("heroTitle")` verbatim).
describe("HeroTitle", () => {
  beforeAll(() => {
    vi.stubGlobal("IntersectionObserver", class {
      observe() {}
      disconnect() {}
    });
  });

  it("renders only the supported line break without interpreting other HTML", () => {
    render(
      <h1>
        <HeroTitle rawTitle='<img src="x" onerror="alert(1)">Safe<br />Title' />
      </h1>,
    );

    const heading = screen.getByRole("heading", { level: 1 });
    expect(heading.querySelector("img")).toBeNull();
    expect(heading.querySelector("br")).not.toBeNull();
    expect(heading).toHaveTextContent('<img src="x" onerror="alert(1)">SafeTitle');
  });
});
