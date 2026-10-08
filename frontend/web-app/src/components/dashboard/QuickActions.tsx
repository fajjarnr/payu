import React, { useState } from "react";
import {
  ArrowRightLeft,
  QrCode,
  Receipt,
  Wallet,
  CreditCard,
  Smartphone,
  MoreHorizontal,
  GripVertical,
  ChevronRight,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import { Button, Card, Col, Divider, Flex, Row, Space, Typography } from "antd";

const { Title, Text } = Typography;

interface QuickAction {
  id: string;
  label: string;
  icon: React.ElementType;
  href: string;
  color: string;
  bgColor: string;
  description?: string;
  ariaLabel: string;
}

interface QuickActionsProps {
  actions?: QuickAction[];
  maxActions?: number;
  className?: string;
  onReorder?: (actions: QuickAction[]) => void;
}

const defaultActions: QuickAction[] = [
  {
    id: "transfer",
    label: "Transfer",
    icon: ArrowRightLeft,
    href: "/transfer",
    color: "text-primary",
    bgColor: "bg-success-light",
    description: "Kirim uang instan",
    ariaLabel: "Transfer uang ke akun lain",
  },
  {
    id: "qris",
    label: "QRIS",
    icon: QrCode,
    href: "/qris",
    color: "text-primary",
    bgColor: "bg-chart-2",
    description: "Scan QR untuk bayar",
    ariaLabel: "Pembayaran QRIS",
  },
  {
    id: "bills",
    label: "Tagihan",
    icon: Receipt,
    href: "/bills",
    color: "text-primary",
    bgColor: "bg-chart-3",
    description: "Bayar tagihan & isi ulang",
    ariaLabel: "Bayar tagihan dan isi ulang",
  },
  {
    id: "pockets",
    label: "Kantong",
    icon: Wallet,
    href: "/pockets",
    color: "text-primary",
    bgColor: "bg-chart-green1",
    description: "Kelola kantong uang",
    ariaLabel: "Kelola kantong",
  },
  {
    id: "cards",
    label: "Kartu",
    icon: CreditCard,
    href: "/cards",
    color: "text-primary",
    bgColor: "bg-chart-green2",
    description: "Kartu virtual",
    ariaLabel: "Kelola kartu virtual",
  },
  {
    id: "topup",
    label: "Isi Ulang",
    icon: Smartphone,
    href: "/bills?category=pulsa",
    color: "text-primary",
    bgColor: "bg-chart-green3",
    description: "Isi pulsa & paket data",
    ariaLabel: "Isi ulang pulsa",
  },
];

export default function QuickActions({
  actions = defaultActions,
  maxActions = 6,
  className = "",
  onReorder,
}: QuickActionsProps) {
  const t = useTranslations("dashboard");
  const [items, setItems] = useState(actions.slice(0, maxActions));
  const [isEditMode, setIsEditMode] = useState(false);

  const moveItem = (id: string, dir: -1 | 1) => {
    setItems((prev) => {
      const idx = prev.findIndex((item) => item.id === id);
      const next = idx + dir;
      if (idx < 0 || next < 0 || next >= prev.length) return prev;
      const newItems = [...prev];
      [newItems[idx], newItems[next]] = [newItems[next], newItems[idx]];
      onReorder?.(newItems);
      return newItems;
    });
  };

  return (
    <Card
      data-testid="quick-actions-card"
      role="region"
      aria-labelledby="quick-actions-title"
      style={{ position: "relative", overflow: "hidden" }}
      className={className}
    >
      <Space direction="vertical" size={24} style={{ width: "100%" }}>
        <Row justify="space-between" align="top">
          <div>
            <Title level={3} id="quick-actions-title" style={{ margin: 0 }}>
              {t("quickActionsTitle")}
            </Title>
            <Text type="secondary" strong style={{ fontSize: 12, textTransform: "uppercase", letterSpacing: "0.1em", display: "block", marginTop: 4 }}>
              {t("quickActionsSubtitle")}
            </Text>
          </div>

          <Button
            type={isEditMode ? "primary" : "default"}
            size="small"
            data-testid="edit-quick-actions-button"
            onClick={() => setIsEditMode(!isEditMode)}
            aria-label={
              isEditMode ? "Selesai mengedit" : "Edit urutan aksi cepat"
            }
            aria-pressed={isEditMode}
          >
            {isEditMode ? "Selesai" : "Edit"}
          </Button>
        </Row>

        {/* Reorder hint in edit mode */}
        {isEditMode && (
          <Card size="small" style={{ backgroundColor: "var(--ant-color-primary-bg)" }} role="status" aria-live="polite">
            <Space size={12}>
              <GripVertical style={{ width: 20, height: 20 }} aria-hidden="true" />
              <Text type="secondary" style={{ fontSize: 12 }}>
                {t("quickActionsDragHint")} - Gunakan tombol panah atau tombol
                naik/turun untuk mengatur ulang
              </Text>
            </Space>
          </Card>
        )}

        {/* Actions Grid */}
        <Row gutter={[24, 24]}>
          {items.map((action, index) => (
            <Col xs={12} sm={6} lg={6} xl={4} key={action.id}>
              <QuickActionItem
                action={action}
                isEditMode={isEditMode}
                isFirst={index === 0}
                isLast={index === items.length - 1}
                onMoveUp={() => moveItem(action.id, -1)}
                onMoveDown={() => moveItem(action.id, 1)}
              />
            </Col>
          ))}
        </Row>

        {/* More Actions Link */}
        <Divider style={{ margin: "32px 0 0" }} />
        <Button
          type="text"
          data-testid="view-all-features-button"
          aria-label="Lihat semua fitur"
          block
        >
          <Space size={12}>
            <MoreHorizontal style={{ width: 20, height: 20 }} aria-hidden="true" />
            <Text strong style={{ fontSize: 14 }}>Lihat Semua Fitur</Text>
            <ChevronRight style={{ width: 20, height: 20 }} aria-hidden="true" />
          </Space>
        </Button>
      </Space>
    </Card>
  );
}

interface QuickActionItemProps {
  action: QuickAction;
  isEditMode: boolean;
  isFirst: boolean;
  isLast: boolean;
  onMoveUp: () => void;
  onMoveDown: () => void;
}
function QuickActionItem({
  action,
  isEditMode,
  isFirst,
  isLast,
  onMoveUp,
  onMoveDown,
}: QuickActionItemProps) {
  const Icon = action.icon;

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isEditMode) return;
    if (e.key === "ArrowUp" || e.key === "ArrowLeft") {
      e.preventDefault();
      onMoveUp();
    } else if (e.key === "ArrowDown" || e.key === "ArrowRight") {
      e.preventDefault();
      onMoveDown();
    }
  };

  return (
    <Space direction="vertical" size={8} style={{ width: "100%" }}>
      <a
        href={action.href}
        data-testid={`quick-action-${action.id}`}
        onKeyDown={handleKeyDown}
        aria-label={action.ariaLabel}
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          textAlign: "center",
          padding: 24,
          borderRadius: 16,
          border: isEditMode
            ? "2px dashed var(--ant-color-primary-border)"
            : "1px solid var(--ant-color-border-secondary)",
          backgroundColor: isEditMode
            ? "var(--ant-color-fill-tertiary)"
            : "var(--ant-color-bg-container)",
          textDecoration: "none",
          transition: "border-color 0.2s, box-shadow 0.2s",
        }}
      >
        {isEditMode && (
          <div style={{ position: "absolute", top: 16, right: 16 }}>
            <GripVertical style={{ width: 20, height: 20, color: "var(--ant-color-text-tertiary)" }} aria-hidden="true" />
            <span className="sr-only">Drag untuk mengatur ulang</span>
          </div>
        )}
        <div
          style={{
            width: 64,
            height: 64,
            borderRadius: 16,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            marginBottom: 20,
            backgroundColor: "var(--ant-color-primary-bg)",
            color: "var(--ant-color-primary)",
          }}
        >
          <Icon style={{ width: 32, height: 32 }} aria-hidden="true" />
        </div>
        <Text strong style={{ fontSize: 14, display: "block" }}>
          {action.label}
        </Text>
        {action.description && (
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginTop: 4, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "100%" }}>
            {action.description}
          </Text>
        )}
      </a>
      {isEditMode && (
        <Space size={8} style={{ width: "100%", justifyContent: "center" }}>
          <Button
            size="small"
            disabled={isFirst}
            onClick={onMoveUp}
            aria-label={`Pindahkan ${action.label} ke atas`}
            style={{ minWidth: 44, minHeight: 44 }}
          >
            Naik
          </Button>
          <Button
            size="small"
            disabled={isLast}
            onClick={onMoveDown}
            aria-label={`Pindahkan ${action.label} ke bawah`}
            style={{ minWidth: 44, minHeight: 44 }}
          >
            Turun
          </Button>
        </Space>
      )}
    </Space>
  );
}
