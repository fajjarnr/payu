import { notify as toast } from "@/lib/notify";
import { describe, it, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import ForgotPasswordPage from "@/app/[locale]/forgot-password/page";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

vi.mock("@/lib/notify", () => ({
  notify: { error: vi.fn(), info: vi.fn(), success: vi.fn(), warning: vi.fn() },
}));

vi.mock("@/lib/navigation", () => ({
  Link: ({
    children,
    ...props
  }: React.AnchorHTMLAttributes<HTMLAnchorElement> & {
    children: React.ReactNode;
  }) => <a {...props}>{children}</a>,
}));

describe("ForgotPasswordPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render the form heading and email input", () => {
    renderWithIntl(<ForgotPasswordPage />);
    expect(screen.getByRole("heading")).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Kirim Instruksi" }),
    ).toBeInTheDocument();
  });

  it("should warn when submitting without an email", () => {
    renderWithIntl(<ForgotPasswordPage />);
    fireEvent.click(screen.getByRole("button", { name: "Kirim Instruksi" }));
    expect(toast.error).toHaveBeenCalled();
  });

  it("hands the browser to the identity provider reset flow", async () => {
    // The BFF has no forgot-password route; the real flow is Keycloak's own
    // reset-credentials screen, so assert the redirect rather than a fetch.
    const assign = vi.fn();
    vi.stubGlobal("location", {
      ...window.location,
      set href(v: string) {
        assign(v);
      },
      origin: "https://payu-dev.apps.fajjjar.my.id",
    });
    renderWithIntl(<ForgotPasswordPage />);
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "user@payu.id" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Kirim Instruksi" }));
    await vi.waitFor(() => {
      expect(assign).toHaveBeenCalledWith(
        expect.stringContaining("/login-actions/reset-credentials"),
      );
    });
    vi.unstubAllGlobals();
  });
});
