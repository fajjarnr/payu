"use client";

import React from "react";
import { useLocale } from "next-intl";
import { Link } from "@/lib/navigation";
import {
  Search,
  ChevronDown,
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
import { Button, Card, Col, Divider, Dropdown, Row, Skeleton, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";

const { Title, Text } = Typography;

import { useTransactions, useCancelTransaction } from "@/hooks";
import { useAuthStore } from "@/stores";
import { notify as toast } from "@/lib/notify";
import { formatCurrency } from "@/lib/currency";
import type { Transaction } from "@/services/TransactionService";

const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: "Menunggu", color: "warning" },
  VALIDATING: { label: "Validasi", color: "warning" },
  PROCESSING: { label: "Diproses", color: "processing" },
  COMPLETED: { label: "Selesai", color: "success" },
  FAILED: { label: "Gagal", color: "error" },
  CANCELLED: { label: "Batal", color: "default" },
};

// Helper to check if transaction type is a credit (income)
const isCreditType = (type: string): boolean => type === "TOP_UP";

interface TransferActivityProps {
  className?: string;
}

const QUICK_CATEGORIES = [
  { icon: Landmark, label: "Bank" },
  { icon: Smartphone, label: "E-Wallet" },
  { icon: ReceiptText, label: "Tagihan" },
  { icon: MoreIcon, label: "Lain" },
];

export default function TransferActivity({
  className = "",
}: TransferActivityProps) {
  // BUG-FE-008 FIX: Use dynamic locale instead of hardcoded 'id-ID'
  const locale = useLocale();
  const bcp47Locale = locale === "id" ? "id-ID" : "en-US";
  const accountId = useAuthStore((state) => state.accountId);
  const { data: transactions, isLoading } = useTransactions(
    accountId || undefined,
    0,
    5,
  );
  const cancelTransaction = useCancelTransaction();

  const handleCancel = async (transactionId: string) => {
    try {
      await cancelTransaction.mutateAsync(transactionId);
      toast.success("Transaksi berhasil dibatalkan");
    } catch {
      toast.error("Gagal membatalkan transaksi");
    }
  };

  const canCancel = (status: string) =>
    status === "PENDING" || status === "PROCESSING";

  // Wire data can carry DECIMAL as a JSON number; formatCurrency already
  // normalizes number input. Keep the string path byte-identical (direction
  // sign is rendered by the caller from transaction type).
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
      title: "Tanggal",
      render: (_, item) => (
        <Space direction="vertical" size={4}>
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
            {item.referenceNumber}
          </Text>
        </Space>
      ),
    },
    {
      key: "recipient",
      title: "Penerima",
      render: (_, item) => (
        <Space size={16} align="center">
          <div
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
                color: "var(--ant-color-primary)",
              }}
            />
          </div>
          <Space direction="vertical" size={4}>
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
      title: "Status",
      align: "center",
      render: (_, item) => (
        <Tag
          bordered={false}
          color={statusConfig[item.status]?.color ?? "warning"}
        >
          {statusConfig[item.status]?.label || item.status}
        </Tag>
      ),
    },
    {
      key: "amount",
      title: "Jumlah",
      align: "right",
      render: (_, item) => (
        <Text
          strong
          style={{
            fontSize: 14,
            fontVariantNumeric: "tabular-nums",
            color: isCreditType(item.type)
              ? "var(--ant-color-primary)"
              : undefined,
          }}
        >
          {isCreditType(item.type) ? "+" : "-"}
          {formatAmount(item.amount)}
        </Text>
      ),
    },
    {
      key: "actions",
      title: "",
      align: "right",
      render: (_, item) => (
        <Dropdown
          menu={{
            items: [
              {
                key: "detail",
                label: "Lihat Detail",
              },
              ...(canCancel(item.status)
                ? [
                    {
                      key: "cancel",
                      danger: true,
                      label: (
                        <Space size={8} align="center">
                          <X style={{ width: 16, height: 16 }} />
                          Batalkan
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
            aria-label="Opsi transaksi"
            icon={<MoreHorizontal style={{ width: 16, height: 16 }} />}
          />
        </Dropdown>
      ),
    },
  ];

  return (
    <Space
      direction="vertical"
      size={24}
      style={{ width: "100%" }}
      data-testid="transfer-activity-section"
      className={className || undefined}
    >
      {/* Quick Transfer strip */}
      <Card data-testid="quick-transfer-card">
        <Space direction="vertical" size={24} style={{ width: "100%" }}>
          <Row justify="space-between" align="middle">
            <Title level={4} style={{ margin: 0 }}>
              Kirim Cepat
            </Title>
            <Button
              type="text"
              aria-label="Cari transfer"
              icon={<Search style={{ width: 20, height: 20 }} />}
            />
          </Row>

          <Space direction="vertical" size={8} style={{ width: "100%" }}>
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
              Kategori Favorit
            </Text>
            <Row gutter={[16, 16]}>
              {QUICK_CATEGORIES.map((item) => (
                <Col span={6} key={item.label}>
                  <Button
                    type="text"
                    data-testid={`quick-transfer-category-${item.label.toLowerCase()}`}
                    style={{ height: "auto", width: "100%", padding: 8 }}
                  >
                    <Space direction="vertical" size={8} align="center">
                      <div
                        style={{
                          width: 56,
                          height: 56,
                          borderRadius: 16,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          backgroundColor: "var(--ant-color-primary-bg)",
                          border:
                            "1px solid var(--ant-color-primary-border)",
                        }}
                      >
                        <item.icon
                          style={{
                            width: 24,
                            height: 24,
                            color: "var(--ant-color-primary)",
                          }}
                        />
                      </div>
                      <Text
                        type="secondary"
                        strong
                        style={{ fontSize: 12, textTransform: "uppercase" }}
                      >
                        {item.label}
                      </Text>
                    </Space>
                  </Button>
                </Col>
              ))}
            </Row>
          </Space>

          <Space direction="vertical" size={8} style={{ width: "100%" }}>
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
              Kontak Terbaru
            </Text>
            <Row justify="space-between">
              {[1, 2, 3, 4, 5].map((i) => (
                <Button
                  key={i}
                  aria-label={`Kontak ${i}`}
                  icon={
                    <User
                      style={{
                        width: 24,
                        height: 24,
                        color: "var(--ant-color-primary)",
                      }}
                    />
                  }
                  style={{ width: 52, height: 52, borderRadius: 16 }}
                />
              ))}
            </Row>
          </Space>

          <Button
            data-testid="quick-transfer-send-button"
            type="primary"
            size="large"
            block
            style={{
              height: 56,
              fontWeight: 700,
              fontSize: 12,
              textTransform: "uppercase",
              letterSpacing: "0.1em",
            }}
          >
            Kirim Sekarang
          </Button>
        </Space>
      </Card>

      {/* Recent Transfer Activity ledger */}
      <Card data-testid="recent-activity-card">
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Row justify="space-between" align="middle">
            <Title level={4} style={{ margin: 0 }}>
              Aktivitas Terakhir
            </Title>
            <Button
              type="default"
              size="small"
              aria-label="Pilih bulan"
              style={{ minHeight: 44 }}
            >
              <Space size={8} align="center">
                <Text
                  type="secondary"
                  strong
                  style={{
                    fontSize: 12,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                  }}
                >
                  Januari
                </Text>
                <ChevronDown style={{ width: 16, height: 16 }} />
              </Space>
            </Button>
          </Row>

          {isLoading ? (
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              {[1, 2, 3, 4].map((i) => (
                <Skeleton
                  key={i}
                  active
                  title={false}
                  paragraph={{ rows: 1 }}
                />
              ))}
            </Space>
          ) : (
            <>
              <div className="hidden md:block">
                <Table<Transaction>
                  columns={columns}
                  dataSource={displayTransactions}
                  rowKey="id"
                  pagination={false}
                  loading={isLoading}
                  locale={{ emptyText: "Belum ada transaksi" }}
                  onRow={(record) =>
                    ({
                      "data-testid": `transfer-row-${record.id}`,
                    }) as React.HTMLAttributes<HTMLElement>
                  }
                />
              </div>

              {/* Mobile Card Layout */}
              <div className="md:hidden">
                <Space direction="vertical" size={16} style={{ width: "100%" }}>
                  {displayTransactions.map((item: Transaction) => (
                    <Card
                      key={item.id}
                      size="small"
                      data-testid={`transfer-card-mobile-${item.id}`}
                    >
                      <Space
                        direction="vertical"
                        size={12}
                        style={{ width: "100%" }}
                      >
                        <Row justify="space-between" align="middle">
                          <Space size={12} align="center">
                            <div
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
                                  color: "var(--ant-color-primary)",
                                }}
                              />
                            </div>
                            <Space direction="vertical" size={2}>
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
                            style={{ fontVariantNumeric: "tabular-nums" }}
                          >
                            {isCreditType(item.type) ? "+" : "-"}
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
                              bordered={false}
                              color={
                                statusConfig[item.status]?.color ?? "warning"
                              }
                            >
                              {statusConfig[item.status]?.label || item.status}
                            </Tag>
                            {canCancel(item.status) && (
                              <Button
                                type="text"
                                aria-label="Batalkan transaksi"
                                danger
                                icon={
                                  <X style={{ width: 16, height: 16 }} />
                                }
                                onClick={() => handleCancel(item.id)}
                              />
                            )}
                          </Space>
                        </Row>
                      </Space>
                    </Card>
                  ))}
                </Space>
              </div>

              {!displayTransactions.length && (
                <Space
                  direction="vertical"
                  size={8}
                  align="center"
                  style={{ width: "100%", textAlign: "center", padding: "48px 0" }}
                >
                  <div
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
                    Belum Ada Transaksi
                  </Title>
                  <Text type="secondary">
                    Transaksi Anda akan muncul di sini
                  </Text>
                </Space>
              )}

              <Divider style={{ margin: 0 }} />
              <Row justify="space-between" align="middle">
                <Button
                  type="link"
                  size="small"
                  data-testid="repeat-last-transfer-button"
                  icon={<RotateCcw style={{ width: 16, height: 16 }} />}
                  style={{
                    fontWeight: 700,
                    fontSize: 12,
                    textTransform: "uppercase",
                    minHeight: 44,
                  }}
                >
                  Ulangi Transfer Terakhir
                </Button>
                <Link href="/transactions">
                  <Button
                    type="link"
                    size="small"
                    data-testid="view-full-history-button"
                    icon={<ArrowRight style={{ width: 16, height: 16 }} />}
                    style={{
                      fontWeight: 700,
                      fontSize: 12,
                      textTransform: "uppercase",
                      minHeight: 44,
                    }}
                  >
                    Riwayat Lengkap
                  </Button>
                </Link>
              </Row>
            </>
          )}
        </Space>
      </Card>
    </Space>
  );
}
