import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import SupportPage from "@/app/[locale]/support/page";

vi.mock("@/services/SupportService", () => ({
  default: { getFAQs: () => Promise.resolve([]) },
}));

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => {
    const map: Record<string, string> = {
      title: "Terminal Bantuan",
      liveChat: "Bantuan Langsung",
      email: "Protokol Email",
      phone: "Panggilan Suara",
      faqs: "Repositori Inteligensi",
    };
    return map[key] || key;
  },
}));

vi.mock("@/lib/navigation", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/navigation")>()),
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock("@/hooks/useSupport", () => ({
  useTickets: () => ({
    data: [],
    isLoading: false,
  }),
  useCreateTicket: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useTrainingStatus: () => ({
    data: [],
    isLoading: false,
  }),
}));

describe("SupportPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render within DashboardLayout", () => {
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <SupportPage />
      </QueryClientProvider>,
    );
    expect(screen.getByTestId("dashboard-layout")).toBeInTheDocument();
  });

  it("should render page title", () => {
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <SupportPage />
      </QueryClientProvider>,
    );
    expect(screen.getByText("Terminal Bantuan")).toBeInTheDocument();
  });

  it("should render the channels that actually exist", () => {
    // Phone support has no published number and live chat has no backend in this
    // deployment, so only the ticket form and the support mailbox are shown.
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <SupportPage />
      </QueryClientProvider>,
    );
    expect(screen.getByText("Bantuan Langsung")).toBeInTheDocument();
    expect(screen.getByText("Protokol Email")).toBeInTheDocument();
    expect(screen.queryByText("Panggilan Suara")).not.toBeInTheDocument();
  });

  it("should render knowledge repository", () => {
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <SupportPage />
      </QueryClientProvider>,
    );
    expect(screen.getByText("Repositori Inteligensi")).toBeInTheDocument();
  });

  it("should render system status", () => {
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <SupportPage />
      </QueryClientProvider>,
    );
    expect(screen.getByText("Gateway: —")).toBeInTheDocument();
    expect(screen.getByText("Backend: —")).toBeInTheDocument();
  });
});
