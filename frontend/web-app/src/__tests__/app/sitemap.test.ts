import { afterEach, describe, expect, it } from "vitest";

import sitemap from "@/app/sitemap";

describe("sitemap base URL (WEB-004)", () => {
  const originalBaseUrl = process.env.NEXT_PUBLIC_BASE_URL;

  afterEach(() => {
    if (originalBaseUrl === undefined) {
      delete process.env.NEXT_PUBLIC_BASE_URL;
    } else {
      process.env.NEXT_PUBLIC_BASE_URL = originalBaseUrl;
    }
  });

  it("defaults to production domain", async () => {
    delete process.env.NEXT_PUBLIC_BASE_URL;
    const entries = await sitemap();
    expect(entries[0]?.url).toBe("https://payu.fajjjar.my.id/id");
  });

  it("uses NEXT_PUBLIC_BASE_URL when set", async () => {
    process.env.NEXT_PUBLIC_BASE_URL = "https://payu-dev.apps.fajjjar.my.id";
    const entries = await sitemap();
    expect(entries.length).toBeGreaterThan(0);
    expect(entries.every((entry) =>
      entry.url.startsWith("https://payu-dev.apps.fajjjar.my.id/"),
    )).toBe(true);
  });
});
