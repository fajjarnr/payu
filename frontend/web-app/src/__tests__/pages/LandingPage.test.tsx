import { render, screen } from "@testing-library/react";
import type { ComponentProps } from "react";
import { beforeAll, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/navigation", () => ({
  Link: ({ children, href, ...props }: ComponentProps<"a">) => (
    <a href={href} {...props}>{children}</a>
  ),
}));

vi.mock("next-intl", () => ({
  useTranslations: () => Object.assign(
    (key: string) => key,
    { raw: () => '<img src="x" onerror="alert(1)">Safe<br />Title' },
  ),
}));

import LandingPage from "@/app/[locale]/page";

describe("LandingPage", () => {
  beforeAll(() => {
    vi.stubGlobal("IntersectionObserver", class {
      observe() {}
      disconnect() {}
    });
  });

  it("renders only the supported line break without interpreting other HTML", () => {
    render(<LandingPage />);

    const heading = screen.getByRole("heading", { level: 1 });
    expect(heading.querySelector("img")).toBeNull();
    expect(heading.querySelector("br")).not.toBeNull();
    expect(heading).toHaveTextContent('<img src="x" onerror="alert(1)">SafeTitle');
  });
});
