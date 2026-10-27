"use client";

import React from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/lib/navigation";
import {
  Search,
  MoreHorizontal,
  RotateCcw,
  ArrowRight,
  User,
  Landmark,
  Smartphone,
  ReceiptText,
  MoreHorizontal as MoreIcon,
  X,
} from "@/components/icons";
import { Alert, Button, Card, Col, Descriptions, Divider, Dropdown, Modal, Row, Skeleton, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";

const { Title, Text } = Typography;

import { useTransactions, useCancelTransaction } from "@/hooks";
import { useAuthStore } from "@/stores";
import { notify as toast } from "@/lib/notify";
import { ON_PRIMARY } from "@/theme/tokens";
import { formatCurrency } from "@/lib/currency";
import type { Transaction } from "@/services/TransactionService";

const statusConfig: Record<string, { labelKey: string; color: string }> = {
  PENDING: { labelKey: "statusPending", color: "warning" },
  VALIDATING: { labelKey: "statusValidating", color: "warning" },
  PROCESSING: { labelKey: "statusProcessing", color: "processing" },
  COMPLETED: { labelKey: "statusCompleted", color: "success" },
  FAILED: { labelKey: "statusFailed", color: "error" },
  CANCELLED: { labelKey: "statusCancelled", color: "default" },
};

// Credit when the money flows to the viewer: explicit TOP_UP, or any
// transfer whose recipient is the current account (mirrors transactions/).
const isCreditType = (
  type: string,
  t?: Transaction,
  currentAccountId?: string | null,
): boolean => {
  if (type === "TOP_UP") return true;
  if (t && currentAccountId && t.recipientAccountId === currentAccountId)
    return true;
  return false;
};

// Reference numbers are opaque ids: show a truncated tail, never the full id.
const maskReference = (ref: string): string =>
  ref.length > 8 ? `…${ref.slice(-6)}` : ref;

interface TransferActivityProps {
  className?: string;
}

// Each tile is a shortcut into the flow it names (A-04: a control that looks
// actionable must do something).
const QUICK_CATEGORIES = [
  { id: "bank", icon: Landmark, labelKey: "categoryBank", href: "/transfer" },
  { id: "e-wallet", icon: Smartphone, labelKey: "categoryEWallet", href: "/pockets" },
  { id: "tagihan", icon: ReceiptText, labelKey: "categoryBills", href: "/bills" },
  { id: "lain", icon: MoreIcon, labelKey: "categoryOther", href: "/split-bill" },
];

export function TransferEmpty({ title, hint }: { title: string; hint: string }) {
  return (
    <Space
      orientation="vertical"
      size={8}
      align="center"
      style={{ width: "100%", textAlign: "center", padding: "48px 0" }}
      data-testid="transfer-empty"
    >
      <div
        aria-hidden="true"
        style={{
          width: 64,
          height: 64,
          borderRadius: "50%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: "var(--ant-color-fill-tertiary)",
        }}
      >
        <ReceiptText
          style={{
            width: 32,
            height: 32,
            color: "var(--ant-color-text-tertiary)",
          }}
        />
      </div>
      <Title level={4} style={{ margin: 0 }}>
        {title}
      </Title>
      <Text type="secondary">{hint}</Text>
    </Space>
  );
}

export default function TransferActivity({
  className = "",
}: TransferActivityProps) {
  const t = useTranslations("dashboard");
  const bcp47Locale = 'id-ID'; // Single-locale app (Indonesian only)
  const accountId = useAuthStore((state) => state.accountId);
  const {
    data: transactions,
    isLoading,
    isError,
    refetch,
  } = useTransactions(accountId || undefined, 0, 5);
  const cancelTransaction = useCancelTransaction();
  const [cancelAnnouncement, setCancelAnnouncement] = React.useState("");
  // A-04: the row menu's "Detail" entry opens the transaction, it is not a no-op.
  const [detailTransaction, setDetailTransaction] = React.useState<Transaction | null>(null);

  const handleCancel = async (transactionId: string) => {
    try {
      await cancelTransaction.mutateAsync(transactionId);
      toast.success(t("cancelSuccess"));
      // F-08: antd message toasts are not live-regions; mirror the outcome
      // into a polite announcer so SR users hear cancel results.
      setCancelAnnouncement(t("cancelAnnouncement", { status: t("cancelSuccess") }));
    } catch {
      toast.error(t("cancelFailed"));
      setCancelAnnouncement(t("cancelAnnouncement", { status: t("cancelFailed") }));
    }
  };

  const canCancel = (status: string) =>
    status === "PENDING" || status === "PROCESSING";

  const formatAmount = (amount: string | number) => {
    const unsigned =
      typeof amount === "string" ? amount.replace(/^-/, "") : amount;
    return formatCurrency(unsigned, { locale: bcp47Locale });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(bcp47Locale, {
      day: "2-digit",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    });
  };
  const displayTransactions = transactions || [];
  const columns: TableColumnsType<Transaction> = [
    {
      key: "date",
      title: t("columnDate"),
      render: (_, item) => (
        <Space orientation="vertical" size={4}>
          <Text
            type="secondary"
            strong
            style={{
              fontSize: 12,
              fontVariantNumeric: "tabular-nums",
              textTransform: "uppercase",
            }}
          >
            {formatDate(item.createdAt)}
          </Text>
          <Text
            type="secondary"
            style={{ fontSize: 12, fontFamily: "ui-monospace, monospace" }}
          >
            {maskReference(item.referenceNumber)}
          </Text>
        </Space>
      ),
    },
    {
      key: "recipient",
      title: t("columnRecipient"),
      render: (_, item) => (
        <Space size={16} align="center">
          <div
            aria-hidden="true"
            style={{
              width: 48,
              height: 48,
              borderRadius: 12,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              backgroundColor: "var(--ant-color-fill-secondary)",
              border: "1px solid var(--ant-color-border-secondary)",
              flexShrink: 0,
            }}
          >
            <User
              style={{
                width: 24,
                height: 24,
                color: "var(--ant-color-primary-text)",
              }}
            />
          </div>
          <Space orientation="vertical" size={4}>
            <Text
              type="secondary"
              strong
              style={{ fontSize: 12, textTransform: "uppercase" }}
            >
              {item.type}
            </Text>
            <Text strong ellipsis style={{ maxWidth: 150 }}>
              {item.description}
            </Text>
          </Space>
        </Space>
      ),
    },
    {
      key: "status",
      title: t("columnStatus"),
      align: "center",
      render: (_, item) => (
        <Tag
          variant="filled"
          color={statusConfig[item.status]?.color ?? "warning"}
        >
          {statusConfig[item.status]
            ? t(statusConfig[item.status].labelKey)
            : item.status}
        </Tag>
      ),
    },
    {
      key: "amount",
      title: t("columnAmount"),
      align: "right",
      render: (_, item) => (
        <Text
          strong
          style={{
            fontSize: 14,
            fontVariantNumeric: "tabular-nums",
            color: isCreditType(item.type, item, accountId)
              ? "var(--ant-color-success-text-active)"
              : undefined,
          }}
        >
          {isCreditType(item.type, item, accountId) ? "+" : "-"}
          {formatAmount(item.amount)}
        </Text>
      ),
    },
    {
      key: "actions",
      // A screen reader needs to know what the column holds (axe
      // empty-table-header); the visual header stays empty.
      title: <span className="sr-only">{t("columnActions")}</span>,
      align: "right",
      render: (_, item) => (
        <Dropdown
          menu={{
            items: [
              {
                key: "detail",
                label: t("txDetail"),
                onClick: () => setDetailTransaction(item),
              },
              ...(canCancel(item.status)
                ? [
                    {
                      key: "cancel",
                      danger: true,
                      label: (
                        <Space size={8} align="center">
                          <X style={{ width: 16, height: 16 }} />
                          {t("txCancelShort")}
                        </Space>
                      ),
                      onClick: () => handleCancel(item.id),
                    },
                  ]
                : []),
            ],
          }}
          trigger={["click"]}
          placement="bottomRight"
        >
          <Button
            type="text"
            aria-label={t("txOptions", { description: item.description })}
            icon={<MoreHorizontal style={{ width: 16, height: 16 }} />}
            style={{ minWidth: 44, minHeight: 44 }}
          />
        </Dropdown>
      ),
    },
  ];

  return (
    <Space
      orientation="vertical"
      size={24}
      style={{ width: "100%" }}
      data-testid="transfer-activity-section"
      className={className || undefined}
    >
      <span role="status" aria-live="polite" data-testid="transfer-cancel-announcement">
        {cancelAnnouncement}
      </span>
      <Modal
        open={detailTransaction !== null}
        onCancel={() => setDetailTransaction(null)}
        footer={null}
        title={t("txDetail")}
        data-testid="transaction-detail-modal"
      >
        {detailTransaction && (
          <Descriptions
            column={1}
            size="small"
            items={[
              { key: "description", label: t("columnDescription"), children: detailTransaction.description },
              { key: "amount", label: t("columnAmount"), children: formatAmount(detailTransaction.amount) },
              { key: "date", label: t("columnDate"), children: formatDate(detailTransaction.createdAt) },
              { key: "status", label: t("columnStatus"), children: detailTransaction.status },
              { key: "reference", label: t("columnReference"), children: maskReference(detailTransaction.referenceNumber) },
            ]}
          />
        )}
      </Modal>
      {/* Quick Transfer strip */}
      <Card data-testid="quick-transfer-card">
        <Space orientation="vertical" size={24} style={{ width: "100%" }}>
          <Row justify="space-between" align="middle">
            <Title level={4} style={{ margin: 0 }}>
              {t("quickTransfer")}
            </Title>
            <Link
              href="/transactions"
              className="payu-focus-ring"
              aria-label={t("searchTransfer")}
              style={{
                display: "inline-flex",
                width: 44,
                height: 44,
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <Search style={{ width: 20, height: 20 }} aria-hidden="true" />
            </Link>
          </Row>

          <Space orientation="vertical" size={8} style={{ width: "100%" }}>
            <Text
              type="secondary"
              strong
              style={{
                fontSize: 12,
                letterSpacing: "0.1em",
                textTransform: "uppercase",
                textAlign: "center",
                display: "block",
              }}
            >
              {t("favoriteCategory")}
            </Text>
            <Row gutter={[16, 16]}>
              {QUICK_CATEGORIES.map((item) => (
                <Col span={6} key={item.id}>
                  <Link
                    href={item.href}
                    data-testid={`quick-transfer-category-${item.id}`}
                    style={{ display: "block" }}
                  >
                  <Button
                    type="text"
                    style={{ height: "auto", width: "100%", padding: 8, minHeight: 44 }}
                  >
                    <Space orientation="vertical" size={8} align="center">
                      <div
                        aria-hidden="true"
                        style={{
                          width: 56,
                          height: 56,
                          borderRadius: 24,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          backgroundColor: "var(--ant-color-primary-bg)",
                          border: "1px solid var(--ant-color-primary-border)",
                        }}
                      >
                        <item.icon
                          style={{
                            width: 24,
                            height: 24,
                            color: "var(--ant-color-primary-text)",
                          }}
                        />
                      </div>
                      <Text
                        type="secondary"
                        strong
                        style={{ fontSize: 12, textTransform: "uppercase" }}
                      >
                        {t(item.labelKey)}
                      </Text>
                    </Space>
                  </Button>
                  </Link>
                </Col>
              ))}
            </Row>
          </Space>

          <Link
            href="/transfer"
            className="payu-focus-ring"
            data-testid="quick-transfer-send-button"
            style={{
              display: "block",
              height: 48,
              fontWeight: 600,
              fontSize: 16,
              textAlign: "center",
              lineHeight: "48px",
              borderRadius: 24,
              background: "var(--ant-color-primary)",
              color: ON_PRIMARY,
            }}
          >
            {t("sendNow")}
          </Link>
        </Space>
      </Card>

      {/* Recent Transfer Activity ledger */}
      <Card data-testid="recent-activity-card">
        <Space orientation="vertical" size={16} style={{ width: "100%" }}>
          <Row justify="space-between" align="middle">
            <Title level={4} style={{ margin: 0 }}>
              {t("recentActivity")}
            </Title>
          </Row>

          {isLoading ? (
            <Space orientation="vertical" size={16} style={{ width: "100%" }}>
              {[1, 2, 3, 4].map((i) => (
                <Skeleton
                  key={i}
                  active
                  title={false}
                  paragraph={{ rows: 1 }}
                />
              ))}
            </Space>
          ) : isError ? (
            <Alert
              type="error"
              showIcon
              data-testid="transfer-error"
              message={t("transferLoadError")}
              action={
                <Button size="small" onClick={() => refetch()}>
                  {t("retryAction")}
                </Button>
              }
            />
          ) : (
            <>
              <div className="hidden md:block">
                <Table<Transaction>
                  columns={columns}
                  dataSource={displayTransactions}
                  rowKey="id"
                  pagination={false}
                  loading={isLoading}
                  locale={{
                    emptyText: (
                      <TransferEmpty title={t("transferEmpty")} hint={t("transferEmptyHint")} />
                    ),
                  }}
                  onRow={(record) =>
                    ({
                      "data-testid": `transfer-row-${record.id}`,
                    }) as React.HTMLAttributes<HTMLElement>
                  }
                />
              </div>

              {/* Mobile Card Layout */}
              <div className="md:hidden">
                <Space orientation="vertical" size={16} style={{ width: "100%" }}>
                  {displayTransactions.length === 0 ? (
                    <TransferEmpty title={t("transferEmpty")} hint={t("transferEmptyHint")} />
                  ) : (
                    displayTransactions.map((item: Transaction) => (
                    <Card
                      key={item.id}
                      size="small"
                      data-testid={`transfer-card-mobile-${item.id}`}
                    >
                      <Space
                        orientation="vertical"
                        size={12}
                        style={{ width: "100%" }}
                      >
                        <Row justify="space-between" align="middle">
                          <Space size={12} align="center">
                            <div
                              aria-hidden="true"
                              style={{
                                width: 40,
                                height: 40,
                                borderRadius: 12,
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                backgroundColor:
                                  "var(--ant-color-fill-secondary)",
                                border:
                                  "1px solid var(--ant-color-border-secondary)",
                                flexShrink: 0,
                              }}
                            >
                              <User
                                style={{
                                  width: 20,
                                  height: 20,
                                  color: "var(--ant-color-primary-text)",
                                }}
                              />
                            </div>
                            <Space orientation="vertical" size={2}>
                              <Text strong ellipsis style={{ maxWidth: 120 }}>
                                {item.description}
                              </Text>
                              <Text
                                type="secondary"
                                style={{
                                  fontSize: 12,
                                  textTransform: "uppercase",
                                }}
                              >
                                {item.type}
                              </Text>
                            </Space>
                          </Space>
                          <Text
                            strong
                            style={{
                              fontVariantNumeric: "tabular-nums",
                              color: isCreditType(item.type, item, accountId)
                                ? "var(--ant-color-success-text-active)"
                                : undefined,
                            }}
                          >
                            {isCreditType(item.type, item, accountId) ? "+" : "-"}
                            {formatAmount(item.amount)}
                          </Text>
                        </Row>
                        <Divider style={{ margin: 0 }} />
                        <Row justify="space-between" align="middle">
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {formatDate(item.createdAt)}
                          </Text>
                          <Space size={8} align="center">
                            <Tag
                              variant="filled"
                              color={
                                statusConfig[item.status]?.color ?? "warning"
                              }
                            >
                              {statusConfig[item.status]
                                ? t(statusConfig[item.status].labelKey)
                                : item.status}
                            </Tag>
                            {canCancel(item.status) && (
                              <Button
                                type="text"
                                aria-label={t("txCancel", {
                                  description: item.description,
                                  amount: formatAmount(item.amount),
                                })}
                                danger
                                icon={
                                  <X style={{ width: 16, height: 16 }} />
                                }
                                onClick={() => handleCancel(item.id)}
                                style={{ minWidth: 44, minHeight: 44 }}
                              />
                            )}
                          </Space>
                        </Row>
                      </Space>
                    </Card>
                    ))
                  )}
                </Space>
              </div>

              <Divider style={{ margin: 0 }} />
              <Row justify="space-between" align="middle">
                <Link
                  href="/transfer"
                  className="payu-focus-ring"
                  data-testid="repeat-last-transfer-button"
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 8,
                    fontWeight: 600,
                    fontSize: 14,
                    minHeight: 44,
                  }}
                >
                  <RotateCcw style={{ width: 16, height: 16 }} aria-hidden="true" />
                  {t("repeatLastTransfer")}
                </Link>
                <Button
                  type="link"
                  size="small"
                  href="/transactions"
                  data-testid="view-full-history-button"
                  icon={<ArrowRight style={{ width: 16, height: 16 }} />}
                  style={{
                    fontWeight: 600,
                    fontSize: 14,
                    minHeight: 44,
                  }}
                >
                  {t("fullHistory")}
                </Button>
              </Row>
            </>
          )}
        </Space>
      </Card>
    </Space>
  );
}
