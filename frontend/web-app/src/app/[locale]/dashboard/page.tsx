"use client";

import { useTranslations } from "next-intl";
import dynamic from "next/dynamic";
import {
  useLogout,
  useBalance,
  useUserMetrics,
  useSpendingTrends,
  useCashFlow,
} from "@/hooks";
import { useAuthStore } from "@/stores";
import DashboardLayout from "@/components/DashboardLayout";
import BalanceCard from "@/components/dashboard/BalanceCard";
import QuickActions from "@/components/dashboard/QuickActions";
import { Skeleton } from "antd";
import { cn } from "@/lib/utils";
import { SkipLink } from "@/lib/a11y";

// Below-the-fold activity stays lazy; everything else (carousel, health,
// insights, charts, investments, budget, offers, invest CTA, promo popup)
// lives on its own route — home answers "saldo + ada yang aneh?" and
// nothing else.
const TransferActivity = dynamic(
  () => import("@/components/dashboard/TransferActivity"),
  {
    loading: () => (
      <Skeleton
        title={false}
        paragraph={false}
        className={cn(
          "block animate-pulse rounded-xl bg-muted/50",
          "h-[200px] w-full rounded-2xl",
        )}
      />
    ),
  },
);

export default function Home() {
  const logout = useLogout();
  const user = useAuthStore((state) => state.user);
  const t = useTranslations();

  const handleLogout = () => {
    logout.mutate();
  };

  const username = user?.fullName || t("common.user");

  return <Dashboard username={username} handleLogout={handleLogout} />;
}

function Dashboard({
  username,
  handleLogout,
}: {
  username: string;
  handleLogout: () => void;
}) {
  const accountId = useAuthStore((state) => state.accountId);
  // FE-AUDIT-006: analytics events are keyed by account_id (backend BUG-AUTH-013),
  // so queries must use accountId — Keycloak sub returns zero rows.
  const { data: balance, isLoading: balanceLoading } = useBalance(
    accountId || undefined,
  );
  // Mounted, not rendered: FE-AUDIT-006 pins these queries to accountId;
  // dropping them would silently regress the event key.
  useUserMetrics(accountId || undefined);
  const { data: cashFlow } = useCashFlow(accountId || undefined);
  useSpendingTrends(accountId || undefined);

  return (
    <DashboardLayout username={username} onLogout={handleLogout}>
      {/* Accessibility Skip Link */}
      <SkipLink href="#main-content" />

      <main id="main-content" className="overflow-x-hidden">
        <div className="space-y-6">
          {/* Saldo — the one job of this screen */}
          <div>
            {balanceLoading ? (
              <Skeleton
                title={false}
                paragraph={false}
                className={cn(
                  "block animate-pulse rounded-xl bg-muted/50",
                  "h-64 rounded-2xl",
                )}
              />
            ) : (
              <BalanceCard
                balance={balance?.balance ?? "0"}
                income={cashFlow?.income}
                expense={cashFlow?.expenses}
              />
            )}
          </div>

          {/* Next actions — the three money moves, nothing else */}
          <QuickActions maxActions={3} />

          {/* Ada yang aneh? — recent activity is the fraud check */}
          <TransferActivity />
        </div>
      </main>
    </DashboardLayout>
  );
}
