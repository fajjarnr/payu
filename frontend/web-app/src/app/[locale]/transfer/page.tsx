"use client";

import {
  Search,
  ChevronRight,
  PlusCircle,
  LifeBuoy,
  ArrowRight,
  Clock,
  Calendar as CalendarIcon,
  Zap,
  Truck,
  CheckCircle,
} from "@/components/icons";
import {
  Button,
  DatePicker,
  Form,
  Input,
  Card,
  Space,
  Row,
  Col,
  Divider,
  Typography,
  Radio,
  Segmented,
  Tag,
  Badge,
} from "antd";
import dayjs from "dayjs";
import {
  transferSchema,
  type TransferRequest,
  type TransferType,
  type TransferScheduleType,
} from "@/types";
import { zodFieldRule } from "@/lib/zodForm";
import { compareCurrency, parseCurrencyExact } from "@/lib/currency";
import { useState, useCallback, useMemo, useEffect } from "react";
import { useInitiateTransfer } from "@/hooks";
import { useAuthStore } from "@/stores";
import { useBeneficiaries } from "@/hooks/useBeneficiaries";
import { useUIStore } from "@/stores";
import DashboardLayout from "@/components/DashboardLayout";
import { SkipLink } from "@/lib/a11y";
import { format } from "date-fns";
import { id } from "date-fns/locale";
import { formatCurrencyWithoutSymbol } from "@/lib/currency";
import { useRouter } from "@/lib/navigation";

const TRANSFER_TYPES: {
  type: TransferType;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  fee: string;
  maxLimit: string;
  processingTime: string;
}[] = [
  {
    type: "INTERNAL_TRANSFER",
    label: "Transfer Instan",
    description: "Transfer antar rekening PayU seketika",
    icon: Zap,
    fee: "Gratis",
    maxLimit: "Rp 100.000.000",
    processingTime: "Seketika",
  },
  {
    type: "BIFAST_TRANSFER",
    label: "BI-FAST",
    description: "Transfer real-time antar bank nasional",
    icon: Zap,
    fee: "Gratis",
    maxLimit: "Rp 250.000.000",
    processingTime: "Seketika",
  },
  {
    type: "SKN_TRANSFER",
    label: "SKN",
    description: "Transfer kliring nasional",
    icon: Truck,
    fee: "Gratis",
    maxLimit: "Rp 100.000.000",
    processingTime: "Hari kerja",
  },
  {
    type: "RTGS_TRANSFER",
    label: "RTGS",
    description: "Transfer real-time gross settlement",
    icon: Clock,
    fee: "Gratis",
    maxLimit: "Tidak terbatas",
    processingTime: "Seketika",
  },
];

const SCHEDULE_TYPES: {
  type: TransferScheduleType;
  label: string;
  description: string;
}[] = [
  {
    type: "NOW",
    label: "Sekarang",
    description: "Proses transfer segera",
  },
  {
    type: "SCHEDULED",
    label: "Terjadwal",
    description: "Tentukan tanggal pengiriman",
  },
  {
    type: "RECURRING",
    label: "Berulang",
    description: "Pengiriman rutin bulanan",
  },
];
export interface ReviewContact {
  name: string;
  initial: string;
  color: string;
  accountId: string;
}

/**
 * Resolve the review-screen recipient. Only a saved favorite/beneficiary
 * carries a verified name — a manually typed account id without a match
 * MUST NOT render as a name, or a mistyped digit produces a confident
 * review screen with no beneficiary verification.
 */
export function resolveReviewContact(
  contacts: ReviewContact[],
  selected: string | null,
  typedAccountId: string | null | undefined,
): ReviewContact | undefined {
  const accountId = selected ?? typedAccountId ?? "";
  return contacts.find((c) => c.accountId === accountId);
}

/** Authorize is allowed only when the recipient resolved to a saved contact. */
export function isVerifiedReviewContact(
  contact: ReviewContact | undefined,
): contact is ReviewContact {
  return contact !== undefined;
}

export default function TransferPage() {
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const transferMutation = useInitiateTransfer();
  const router = useRouter();

  const { data: beneficiaries } = useBeneficiaries(accountId || undefined);
  const recentContacts: Array<{
    name: string;
    initial: string;
    color: string;
    accountId: string;
  }> = (beneficiaries ?? []).map((b) => ({
    name: b.nickname || b.accountName || b.accountNumber.slice(-4),
    initial: (b.nickname || b.accountName || "B").charAt(0).toUpperCase(),
    color: "bg-primary-light text-primary-dark",
    accountId: b.accountNumber,
  }));

  const [form] = Form.useForm<TransferRequest>();
  const rule = (field: string) => zodFieldRule(form, transferSchema, field);
  const onValid = (values: TransferRequest) =>
    onSubmit(transferSchema.parse(values) as TransferRequest);

  const amount = Form.useWatch("amount", form) ?? "0";
  const transferType =
    Form.useWatch("transferType", form) ?? "INTERNAL_TRANSFER";
  const scheduleType = Form.useWatch("scheduleType", form) ?? "NOW";
  const toAccountId = Form.useWatch("toAccountId", form);
  const description = Form.useWatch("description", form);
  const scheduledAt = Form.useWatch("scheduledAt", form);
  const recurringDay = Form.useWatch("recurringDay", form);
  const recurringMonth = Form.useWatch("recurringMonth", form);

  // TRF-SUBMIT-001: sender must default from the session account so typing a
  // recipient manually (no favorite contact) still passes schema validation.
  useEffect(() => {
    if (accountId) form.setFieldsValue({ fromAccountId: accountId });
  }, [accountId, form]);

  const handleContactSelect = (contact: {
    name: string;
    accountId: string;
  }) => {
    setSelectedContact(contact.accountId);
    form.setFieldsValue({
      toAccountId: contact.accountId,
      fromAccountId: accountId || "",
    });
  };

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value.replace(/\D/g, "");
    form.setFieldsValue({ amount: rawValue || "0" });
  };

  const formattedAmount =
    amount === "0" ? "" : formatCurrencyWithoutSymbol(amount);

  const onSubmit = (data: TransferRequest) => {
    let scheduledAt = undefined;
    let recurringDay = undefined;
    let recurringMonth = undefined;

    if (data.scheduleType === "SCHEDULED" && data.scheduledAt) {
      scheduledAt = data.scheduledAt;
    } else if (data.scheduleType === "RECURRING") {
      recurringDay = data.recurringDay;
      recurringMonth = data.recurringMonth;
    }

    transferMutation.mutate(
      {
        senderAccountId: data.fromAccountId || accountId || "",
        recipientAccountNumber: data.toAccountId,
        amount: parseCurrencyExact(data.amount),
        description: data.description || "",
        type: data.transferType || "INTERNAL_TRANSFER",
        scheduledAt,
        recurringDay,
        recurringMonth,
      },
      {
        onSuccess: () => {
          const message =
            data.scheduleType === "NOW"
              ? "Transfer berhasil!"
              : data.scheduleType === "SCHEDULED"
                ? "Transfer terjadwal berhasil diset!"
                : "Transfer berulang berhasil diset!";
          addToast(message, "success");
          setShowSuccess(true);
        },
        onError: () => {
          addToast("Transfer gagal. Silakan coba lagi.", "error");
        },
      },
    );
  };

  // Memoize form values for review to prevent React Compiler warnings
  const formValues = useMemo(
    () => ({
      amount,
      toAccountId,
      description,
      scheduleType,
      scheduledAt,
      recurringDay,
      recurringMonth,
      fromAccountId: accountId || "",
    }),
    [
      amount,
      toAccountId,
      description,
      scheduleType,
      scheduledAt,
      recurringDay,
      recurringMonth,
      accountId,
    ],
  );

  const handleReview = useCallback(() => {
    if (
      !formValues.toAccountId ||
      compareCurrency((formValues.amount ?? "0") as string, "0" as string) <= 0
    ) {
      addToast(
        "Silakan pilih penerima dan masukkan jumlah transfer",
        "warning",
      );
      return;
    }

    if (formValues.scheduleType === "SCHEDULED" && !formValues.scheduledAt) {
      addToast("Silakan tentukan tanggal transfer", "warning");
      return;
    }

    if (
      formValues.scheduleType === "RECURRING" &&
      (!formValues.recurringDay || !formValues.recurringMonth)
    ) {
      addToast("Silakan tentukan tanggal dan bulan transfer", "warning");
      return;
    }

    setShowReview(true);
  }, [formValues, addToast]);

  const selectedTransferType = TRANSFER_TYPES.find(
    (t) => t.type === transferType,
  );

  if (showReview) {
    const reviewAccountId = selectedContact ?? formValues.toAccountId ?? "";
    const selectedContactData = resolveReviewContact(
      recentContacts,
      selectedContact,
      formValues.toAccountId,
    );
    const isRecipientVerified = isVerifiedReviewContact(selectedContactData);
    const selectedScheduleType = SCHEDULE_TYPES.find(
      (s) => s.type === scheduleType,
    );
    const TransferTypeIcon = selectedTransferType?.icon || Zap;

    return (
      <DashboardLayout>
        <SkipLink href="#main-content" />
        <main id="main-content" className="overflow-x-hidden">
          <Space direction="vertical" size={16}>
            <Space size={16}>
              <Button
                type="default"
                data-testid="back-from-review-button"
                onClick={() => setShowReview(false)}
                size="large"
                aria-label="Kembali"
              >
                <ChevronRight className="h-6 w-6 rotate-180" />
              </Button>
            </Space>

            <Card>
              <Row gutter={[16, 16]} justify="space-between" align="middle" style={{ marginBottom: 32, paddingBottom: 32 }}>
                <Col xs={24} md={12}>
                  <Space size={16}>
                    <div
                      style={{
                        width: 80,
                        height: 80,
                        borderRadius: 16,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontWeight: "bold",
                        fontSize: "1.875rem",
                        boxShadow: "0 10px 15px -3px rgba(0, 0, 0, 0.1)",
                        ...(selectedContactData?.color
                          ? {}
                          : { backgroundColor: "hsl(var(--muted))", color: "hsl(var(--muted-foreground))" }),
                      }}
                      aria-hidden={!isRecipientVerified}
                    >
                      {selectedContactData?.initial ?? "?"}
                    </div>
                    <div>
                      <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 4 }}>
                        Kepada Penerima
                      </Typography.Text>
                      {isRecipientVerified ? (
                        <>
                          <Typography.Title level={3} style={{ marginBottom: 0 }}>
                            {selectedContactData.name}
                          </Typography.Title>
                          <Typography.Text strong style={{ display: "block", marginTop: 4 }}>
                            ID Akun: {reviewAccountId}
                          </Typography.Text>
                        </>
                      ) : (
                        <>
                          <Typography.Title level={3} style={{ marginBottom: 0 }}>
                            Penerima belum terverifikasi
                          </Typography.Title>
                          <Typography.Text
                            strong
                            type="danger"
                            style={{ display: "block", marginTop: 4 }}
                            id="recipient-verification-alert"
                            role="alert"
                          >
                            ID Akun {reviewAccountId || "-"} tidak cocok dengan
                            penerima tersimpan. Cek kembali nomornya atau
                            tambahkan sebagai penerima favorit.
                          </Typography.Text>
                        </>
                      )}
                    </div>
                  </Space>
                </Col>
                <Col xs={24} md={12} style={{ textAlign: "right" }}>
                  <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 4 }}>
                    Jumlah Transfer
                  </Typography.Text>
                  <Typography.Text strong style={{ display: "block", fontSize: "2.25rem" }} className="animate-amount-settle">
                    Rp {formatCurrencyWithoutSymbol(amount)}
                  </Typography.Text>
                  <Typography.Text type="secondary" strong style={{ display: "block", marginTop: 8 }}>
                    Mata Uang IDR
                  </Typography.Text>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                <Col xs={24} md={8}>
                  <Card>
                    <Space size={8} style={{ display: "block", marginBottom: 8 }}>
                      <TransferTypeIcon className="h-4 w-4 text-primary" />
                      <Typography.Text type="secondary" strong>
                        Tipe Transfer
                      </Typography.Text>
                    </Space>
                    <Typography.Text strong style={{ display: "block", fontSize: "0.875rem" }}>
                      {selectedTransferType?.label}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ display: "block", fontSize: "0.75rem", marginTop: 4 }}>
                      {selectedTransferType?.processingTime}
                    </Typography.Text>
                  </Card>
                </Col>
                <Col xs={24} md={8}>
                  <Card>
                    <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 8 }}>
                      Biaya Transfer
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", fontSize: "0.875rem" }}>
                      {selectedTransferType?.fee}
                    </Typography.Text>
                  </Card>
                </Col>
                <Col xs={24} md={8}>
                  <Card>
                    <Space size={8} style={{ display: "block", marginBottom: 8 }}>
                      {scheduleType !== "NOW" && (
                        <CalendarIcon className="h-4 w-4 text-primary" />
                      )}
                      <Typography.Text type="secondary" strong>
                        Jadwal
                      </Typography.Text>
                    </Space>
                    <Typography.Text strong style={{ display: "block", fontSize: "0.875rem" }}>
                      {selectedScheduleType?.label}
                    </Typography.Text>
                    {scheduleType === "SCHEDULED" && scheduledAt && (
                      <Typography.Text type="secondary" style={{ display: "block", fontSize: "0.75rem", marginTop: 4 }}>
                        {format(new Date(scheduledAt), "PPP", { locale: id })}
                      </Typography.Text>
                    )}
                    {scheduleType === "RECURRING" && (
                      <Typography.Text type="secondary" style={{ display: "block", fontSize: "0.75rem", marginTop: 4 }}>
                        Tanggal {recurringDay || "-"}-
                        {recurringMonth || "setiap bulan"}
                      </Typography.Text>
                    )}
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]}>
                <Col xs={24} md={12}>
                  <Card>
                    <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 8 }}>
                      Kantong Sumber
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", fontSize: "1.125rem" }}>
                      Kantong Utama Cair
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", color: "hsl(var(--primary))", marginTop: 8 }}>
                      Saldo: Rp 86.353.000
                    </Typography.Text>
                  </Card>
                </Col>
                {description && (
                  <Col xs={24} md={12}>
                    <Card>
                      <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 8 }}>
                        Pesan Konfirmasi
                      </Typography.Text>
                      <Typography.Text strong style={{ display: "block", fontSize: "1.125rem" }}>
                        &quot;{description}&quot;
                      </Typography.Text>
                    </Card>
                  </Col>
                )}
              </Row>
            </Card>

            <Button
              type="primary"
              onClick={() => form.submit()}
              data-testid="confirm-transfer-button"
              disabled={transferMutation.isPending || !isRecipientVerified}
              aria-describedby={
                isRecipientVerified
                  ? undefined
                  : "recipient-verification-alert"
              }
              block
              size="large"
            >
              {transferMutation.isPending
                ? "Memvalidasi Transaksi..."
                : "Otorisasi Transfer Sekarang"}
            </Button>
          </Space>
        </main>
      </DashboardLayout>
    );
  }
  if (showSuccess) {
    const successContact = resolveReviewContact(
      recentContacts,
      selectedContact,
      formValues.toAccountId,
    );
    const successSchedule = SCHEDULE_TYPES.find((s) => s.type === scheduleType);
    const now = new Date();

    const handleTransferAgain = () => {
      form.setFieldsValue({ amount: "0", description: "" });
      setSelectedContact(null);
      setShowSuccess(false);
    };

    return (
      <DashboardLayout>
        <SkipLink href="#main-content" />
        <main id="main-content" className="overflow-x-hidden">
          <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", minHeight: "60vh", padding: "48px 16px" }}>
            <div className="animate-seal-lock will-change-transform" style={{ marginBottom: 32 }}>
              <div style={{ width: 96, height: 96, borderRadius: "50%", backgroundColor: "hsl(var(--primary) / 0.1)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <CheckCircle className="w-14 h-14 text-primary" />
              </div>
            </div>

            <Typography.Title level={2} style={{ textAlign: "center", marginBottom: 8 }}>
              Transfer Berhasil!
            </Typography.Title>
            <Typography.Text type="secondary" style={{ textAlign: "center", display: "block", marginBottom: 32 }}>
              {successSchedule?.label === "Sekarang"
                ? "Dana telah dikirim ke penerima"
                : successSchedule?.label === "Terjadwal"
                  ? "Transfer terjadwal telah diatur"
                  : "Transfer berulang telah diatur"}
            </Typography.Text>

            <Card className="animate-receipt-unfold" style={{ width: "100%", maxWidth: 448 }}>
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Space size={16} style={{ paddingBottom: 24 }}>
                  <div
                    style={{
                      width: 56,
                      height: 56,
                      borderRadius: 16,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontWeight: "bold",
                      fontSize: "1.25rem",
                      boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
                      ...(successContact?.color
                        ? {}
                        : { backgroundColor: "hsl(var(--muted))", color: "hsl(var(--muted-foreground))" }),
                    }}
                  >
                    {successContact?.initial || "?"}
                  </div>
                  <div>
                    <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 4 }}>
                      Penerima
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", fontSize: "1.125rem" }}>
                      {successContact?.name || "-"}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ display: "block", fontSize: "0.75rem" }}>
                      {formValues.toAccountId || "-"}
                    </Typography.Text>
                  </div>
                </Space>

                <div style={{ textAlign: "center" }}>
                  <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 8 }}>
                    Jumlah Transfer
                  </Typography.Text>
                  <Typography.Text strong style={{ display: "block", fontSize: "2.25rem" }} className="animate-amount-settle">
                    Rp {formatCurrencyWithoutSymbol(amount)}
                  </Typography.Text>
                </div>

                <Row gutter={[16, 16]} style={{ paddingTop: 16 }}>
                  <Col span={12}>
                    <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 4 }}>
                      Tipe Transfer
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", fontSize: "0.875rem" }}>
                      {selectedTransferType?.label || "-"}
                    </Typography.Text>
                  </Col>
                  <Col span={12} style={{ textAlign: "right" }}>
                    <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 4 }}>
                      Waktu
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", fontSize: "0.875rem" }}>
                      {format(now, "dd MMM yyyy, HH:mm", { locale: id })}
                    </Typography.Text>
                  </Col>
                </Row>
              </Space>
            </Card>

            <Space direction="vertical" size={16} style={{ width: "100%", maxWidth: 448, marginTop: 32 }}>
              <Button
                type="primary"
                onClick={handleTransferAgain}
                data-testid="transfer-again-button"
                block
                size="large"
              >
                Transfer Lagi
              </Button>
              <Button
                type="default"
                onClick={() => router.push("/dashboard")}
                data-testid="back-to-dashboard-button"
                block
                size="large"
              >
                Kembali ke Dashboard
              </Button>
            </Space>
          </div>
        </main>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content" className="overflow-x-hidden">
        <Space direction="vertical" size={16}>
          <div>
            <Typography.Title level={1} style={{ marginBottom: 0 }}>
              Transfer Instan
            </Typography.Title>
            <Typography.Text type="secondary" style={{ display: "block", marginTop: 4 }}>
              Kirim dana secara aman dalam hitungan detik.
            </Typography.Text>
          </div>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Space direction="vertical" size={16}>
                <Form
                  form={form}
                  onFinish={onValid}
                  initialValues={{
                    amount: "0",
                    transferType: "INTERNAL_TRANSFER",
                    scheduleType: "NOW",
                  }}
                  layout="vertical"
                >
                  <Form.Item
                    name="transferType"
                    rules={[rule("transferType")]}
                    noStyle
                  >
                    <Input type="hidden" />
                  </Form.Item>
                  <Form.Item
                    name="scheduleType"
                    rules={[rule("scheduleType")]}
                    noStyle
                  >
                    <Input type="hidden" />
                  </Form.Item>
                  <Form.Item
                    name="fromAccountId"
                    rules={[rule("fromAccountId")]}
                    noStyle
                  >
                    <Input type="hidden" />
                  </Form.Item>
                  <Card>
                    <Typography.Text strong style={{ display: "block", marginBottom: 16 }}>
                      Pilih Metode Transfer
                    </Typography.Text>
                    <Row gutter={[16, 16]} role="radiogroup" aria-label="Pilih Metode Transfer">
                      {TRANSFER_TYPES.map((t) => {
                        const Icon = t.icon;
                        const isSelected = transferType === t.type;
                        return (
                          <Col xs={24} sm={12} key={t.type}>
                            <Button
                              type="text"
                              htmlType="button"
                              role="radio"
                              aria-checked={isSelected}
                              onClick={() =>
                                form.setFieldsValue({ transferType: t.type })
                              }
                              data-testid={`transfer-type-${t.type.toLowerCase()}`}
                              style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: 16,
                                padding: 24,
                                borderRadius: 12,
                                border: isSelected
                                  ? "2px solid hsl(var(--primary))"
                                  : "2px solid transparent",
                                transition: "all 0.2s",
                                ...(isSelected
                                  ? { backgroundColor: "hsl(var(--primary) / 0.05)", boxShadow: "0 10px 15px -3px hsl(var(--primary) / 0.1)" }
                                  : { backgroundColor: "hsl(var(--muted))" }),
                              }}
                            >
                              <Row justify="space-between" align="top">
                                <div
                                  style={{
                                    width: 48,
                                    height: 48,
                                    borderRadius: 12,
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    ...(isSelected
                                      ? { backgroundColor: "hsl(var(--primary) / 0.1)", color: "hsl(var(--primary))" }
                                      : { backgroundColor: "hsl(var(--muted) / 0.5)", color: "hsl(var(--muted-foreground))" }),
                                  }}
                                >
                                  <Icon className="h-6 w-6" />
                                </div>
                                {isSelected && (
                                  <div style={{ width: 8, height: 8, backgroundColor: "hsl(var(--primary))", borderRadius: "50%" }} className="animate-pulse" />
                                )}
                              </Row>
                              <div style={{ textAlign: "left" }}>
                                <Typography.Text strong style={{ display: "block", fontSize: "0.875rem", marginBottom: 4 }}>
                                  {t.label}
                                </Typography.Text>
                                <Typography.Text type="secondary" style={{ display: "block", fontSize: "0.75rem", marginBottom: 8 }}>
                                  {t.description}
                                </Typography.Text>
                                <Space size={8}>
                                  <Tag>{t.fee}</Tag>
                                  <Tag>{t.processingTime}</Tag>
                                </Space>
                              </div>
                            </Button>
                          </Col>
                        );
                      })}
                    </Row>
                  </Card>

                  <Card>
                    <Typography.Text strong style={{ display: "block", marginBottom: 16 }}>
                      Jadwal Transfer
                    </Typography.Text>
                    <Row gutter={[16, 16]} role="radiogroup" aria-label="Jadwal Transfer">
                      {SCHEDULE_TYPES.map((s) => {
                        const isSelected = scheduleType === s.type;
                        return (
                          <Col xs={24} sm={8} key={s.type}>
                            <Button
                              type="text"
                              htmlType="button"
                              role="radio"
                              aria-checked={isSelected}
                              onClick={() =>
                                form.setFieldsValue({ scheduleType: s.type })
                              }
                              data-testid={`schedule-type-${s.type.toLowerCase()}`}
                              style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: 12,
                                padding: 24,
                                borderRadius: 12,
                                border: isSelected
                                  ? "2px solid hsl(var(--primary))"
                                  : "2px solid transparent",
                                transition: "all 0.2s",
                                ...(isSelected
                                  ? { backgroundColor: "hsl(var(--primary) / 0.05)", boxShadow: "0 10px 15px -3px hsl(var(--primary) / 0.1)" }
                                  : { backgroundColor: "hsl(var(--muted))" }),
                              }}
                            >
                              <Row justify="space-between" align="middle">
                                {s.type !== "NOW" && (
                                  <div
                                    style={{
                                      width: 40,
                                      height: 40,
                                      borderRadius: 8,
                                      display: "flex",
                                      alignItems: "center",
                                      justifyContent: "center",
                                      ...(isSelected
                                        ? { backgroundColor: "hsl(var(--primary) / 0.1)", color: "hsl(var(--primary))" }
                                        : { backgroundColor: "hsl(var(--muted) / 0.5)", color: "hsl(var(--muted-foreground))" }),
                                    }}
                                  >
                                    {s.type === "SCHEDULED" ? (
                                      <CalendarIcon className="h-5 w-5" />
                                    ) : (
                                      <Clock className="h-5 w-5" />
                                    )}
                                  </div>
                                )}
                                {isSelected && (
                                  <div style={{ marginLeft: "auto", width: 8, height: 8, backgroundColor: "hsl(var(--primary))", borderRadius: "50%" }} className="animate-pulse" />
                                )}
                              </Row>
                              <div style={{ textAlign: "left" }}>
                                <Typography.Text strong style={{ display: "block", fontSize: "0.875rem", marginBottom: 4 }}>
                                  {s.label}
                                </Typography.Text>
                                <Typography.Text type="secondary" style={{ display: "block", fontSize: "0.75rem" }}>
                                  {s.description}
                                </Typography.Text>
                              </div>
                            </Button>
                          </Col>
                        );
                      })}
                    </Row>
                  </Card>

                  {scheduleType === "SCHEDULED" && (
                    <Card style={{ marginTop: 24 }}>
                      <Form.Item
                        name="scheduledAt"
                        rules={[rule("scheduledAt")]}
                        style={{ marginBottom: 0 }}
                        label={
                          <label
                            htmlFor="transfer-scheduled-at"
                            style={{ fontSize: "0.75rem", fontWeight: "bold", color: "hsl(var(--muted-foreground))", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 12, display: "block" }}
                          >
                            Tanggal Transfer
                          </label>
                        }
                      >
                        <DatePicker
                          id="transfer-scheduled-at"
                          style={{ width: "100%", height: 64, borderRadius: 12, fontWeight: "bold" }}
                          placeholder="Pilih Tanggal Transfer"
                          format="DD MMM YYYY"
                          disabledDate={(current) =>
                            current && current < dayjs().startOf("day")
                          }
                          onChange={(date) =>
                            form.setFieldsValue({
                              scheduledAt: date
                                ? date.toDate().toISOString()
                                : undefined,
                            })
                          }
                        />
                      </Form.Item>
                    </Card>
                  )}

                  {scheduleType === "RECURRING" && (
                    <Space direction="vertical" size={16} className="animate-fade-in" style={{ marginTop: 24 }}>
                      <Space direction="vertical" size={16}>
                        <Typography.Text type="secondary" strong style={{ display: "block", marginLeft: 8 }} id="recurring-day-label">
                          Pilih Tanggal Tagihan / Transfer
                        </Typography.Text>
                        <Form.Item
                          name="recurringDay"
                          rules={[rule("recurringDay")]}
                          style={{ marginBottom: 0 }}
                          noStyle
                        >
                          <Input type="hidden" />
                        </Form.Item>
                        <Row gutter={[8, 8]} style={{ backgroundColor: "hsl(var(--muted) / 0.3)", padding: 16, borderRadius: 12, border: "1px solid hsl(var(--border))" }} aria-labelledby="recurring-day-label">
                          {Array.from({ length: 31 }, (_, i) => i + 1).map(
                            (d) => (
                              <Col span={3} key={d}>
                                <Button
                                  type="text"
                                  htmlType="button"
                                  onClick={() =>
                                    form.setFieldsValue({ recurringDay: d })
                                  }
                                  style={{
                                    aspectRatio: "1",
                                    minHeight: 44,
                                    minWidth: 44,
                                    borderRadius: 12,
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    fontWeight: "bold",
                                    fontSize: "0.875rem",
                                    transition: "all 0.2s",
                                    ...(recurringDay === d
                                      ? { backgroundColor: "hsl(var(--primary))", color: "hsl(var(--surface))", boxShadow: "0 10px 15px -3px hsl(var(--primary) / 0.3)", transform: "scale(1.05)" }
                                      : { backgroundColor: "hsl(var(--card))", color: "hsl(var(--foreground) / 0.6)" }),
                                  }}
                                >
                                  {d}
                                </Button>
                              </Col>
                            ),
                          )}
                        </Row>
                      </Space>

                      <Space direction="vertical" size={16}>
                        <Row justify="space-between" align="middle" style={{ marginLeft: 8 }}>
                          <Typography.Text type="secondary" strong id="recurring-month-label">
                            Pilih Bulan (Opsional)
                          </Typography.Text>
                          <Button
                            type="link"
                            htmlType="button"
                            onClick={() =>
                              form.setFieldsValue({ recurringMonth: undefined })
                            }
                            style={{ fontSize: "0.75rem", fontWeight: "bold", color: "hsl(var(--primary-dark))", letterSpacing: "0.1em", textTransform: "uppercase" }}
                          >
                            Reset ke Setiap Bulan
                          </Button>
                        </Row>
                        <Form.Item
                          name="recurringMonth"
                          rules={[rule("recurringMonth")]}
                          style={{ marginBottom: 0 }}
                          noStyle
                        >
                          <Input type="hidden" />
                        </Form.Item>
                        <Row gutter={[8, 8]} style={{ backgroundColor: "hsl(var(--muted) / 0.3)", padding: 16, borderRadius: 16, border: "1px solid hsl(var(--border))" }} aria-labelledby="recurring-month-label">
                          {[
                            "JAN",
                            "FEB",
                            "MAR",
                            "APR",
                            "MEI",
                            "JUN",
                            "JUL",
                            "AGU",
                            "SEP",
                            "OKT",
                            "NOV",
                            "DES",
                          ].map((m, idx) => {
                            const val = idx + 1;
                            return (
                              <Col span={4} key={m}>
                                <Button
                                  type="text"
                                  htmlType="button"
                                  onClick={() =>
                                    form.setFieldsValue({ recurringMonth: val })
                                  }
                                  style={{
                                    padding: "16px 0",
                                    borderRadius: 12,
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    fontWeight: "bold",
                                    fontSize: "0.75rem",
                                    letterSpacing: "0.1em",
                                    transition: "all 0.2s",
                                    ...(recurringMonth === val
                                      ? { backgroundColor: "hsl(var(--primary))", color: "hsl(var(--surface))", boxShadow: "0 10px 15px -3px hsl(var(--primary) / 0.3)" }
                                      : { backgroundColor: "hsl(var(--card))", color: "hsl(var(--foreground) / 0.6)" }),
                                  }}
                                >
                                  {m}
                                </Button>
                              </Col>
                            );
                          })}
                        </Row>
                      </Space>
                    </Space>
                  )}

                  <div style={{ position: "relative" }}>
                    <Form.Item
                      name="toAccountId"
                      rules={[rule("toAccountId")]}
                      style={{ marginBottom: 0 }}
                      noStyle
                    >
                      <Input
                        data-testid="recipient-account-input"
                        type="text"
                        placeholder="Masukkan ID Akun atau Nomor Rekening"
                        style={{ paddingLeft: 64, height: 64, fontSize: "1.125rem" }}
                        aria-label="Nomor Rekening Penerima"
                      />
                    </Form.Item>
                    <Search style={{ position: "absolute", left: 24, top: "50%", transform: "translateY(-50%)", height: 24, width: 24, color: "hsl(var(--muted-foreground))", zIndex: 10, pointerEvents: "none" }} />
                  </div>

                  <Card>
                    <div style={{ position: "absolute", top: 0, right: 0, width: 192, height: 192, backgroundColor: "hsl(var(--primary) / 0.05)", borderRadius: "50%", filter: "blur(48px)", zIndex: 0 }} />

                    <Row justify="space-between" align="middle" gutter={[8, 8]} style={{ marginBottom: 24, position: "relative", zIndex: 10 }}>
                      <Typography.Text type="secondary" strong>
                        Nominal Transfer
                      </Typography.Text>
                      <div style={{ display: "flex", alignItems: "center", gap: 8, backgroundColor: "hsl(var(--success-light))", padding: "6px 12px", borderRadius: "9999px", border: "1px solid hsl(var(--primary) / 0.1)", boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)", flexShrink: 0 }}>
                        <div style={{ width: 6, height: 6, backgroundColor: "hsl(var(--primary))", borderRadius: "50%" }} className="animate-pulse" />
                        <Typography.Text strong style={{ fontSize: "0.75rem" }}>
                          Secured IDR
                        </Typography.Text>
                      </div>
                    </Row>

                    <Space size={16} style={{ marginBottom: 24, position: "relative", zIndex: 10, display: "block" }}>
                      <Form.Item
                        name="amount"
                        rules={[rule("amount")]}
                        style={{ marginBottom: 0 }}
                        noStyle
                      >
                        <Input type="hidden" data-testid="amount-input" />
                      </Form.Item>
                      <Input
                        value={formattedAmount}
                        onChange={handleAmountChange}
                        placeholder="0"
                        aria-label="Nominal Transfer"
                        style={{ width: "100%", backgroundColor: "transparent", border: "none", padding: 0, fontSize: "2.25rem", fontWeight: "bold", color: "hsl(var(--foreground))" }}
                        variant="borderless"
                      />
                    </Space>

                    <Card style={{ position: "relative", zIndex: 10 }}>
                      <Typography.Text type="secondary" strong style={{ display: "block", marginBottom: 12 }}>
                        Memo Transaksi
                      </Typography.Text>
                      <Form.Item
                        name="description"
                        rules={[rule("description")]}
                        style={{ marginBottom: 0 }}
                        noStyle
                      >
                        <Input
                          data-testid="description-input"
                          type="text"
                          placeholder="Apa tujuan transfer ini?"
                          style={{ width: "100%", fontSize: "1rem", fontWeight: "bold", backgroundColor: "transparent", border: "none", padding: 0 }}
                          aria-label="Memo Transaksi"
                        />
                      </Form.Item>
                    </Card>
                  </Card>

                  <Button
                    type="primary"
                    htmlType="button"
                    onClick={handleReview}
                    data-testid="review-transfer-button"
                    block
                    size="large"
                  >
                    Tinjau Ringkasan Transfer
                    <ArrowRight className="h-5 w-5 ml-2 group-hover:translate-x-2 transition-transform" />
                  </Button>
                </Form>
              </Space>
            </Col>

            <Col xs={24} lg={8}>
              <Space direction="vertical" size={16}>
                <Card>
                  <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
                    <Typography.Text strong>
                      Penerima Favorit
                    </Typography.Text>
                    <div style={{ height: 4, width: 32, backgroundColor: "hsl(var(--primary))", borderRadius: "9999px" }} />
                  </Row>

                  <Row gutter={[16, 16]}>
                    {recentContacts.map((c) => (
                      <Col xs={12} sm={8} lg={12} key={c.accountId}>
                        <Button
                          type="text"
                          htmlType="button"
                          onClick={() => handleContactSelect(c)}
                          data-testid={`favorite-contact-${c.name.toLowerCase()}`}
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            gap: 16,
                            padding: 24,
                            borderRadius: 12,
                            border: selectedContact === c.accountId
                              ? "1px solid hsl(var(--primary))"
                              : "1px solid transparent",
                            transition: "all 0.2s",
                            ...(selectedContact === c.accountId
                              ? { backgroundColor: "hsl(var(--primary) / 0.05)", boxShadow: "0 10px 15px -3px hsl(var(--primary) / 0.1)" }
                              : { backgroundColor: "hsl(var(--muted))" }),
                          }}
                        >
                          <div
                            style={{
                              width: 56,
                              height: 56,
                              borderRadius: 16,
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              fontWeight: "bold",
                              fontSize: "1.5rem",
                              boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
                            }}
                            className={c.color}
                          >
                            {c.initial}
                          </div>
                          <Typography.Text type="secondary" strong>
                            {c.name}
                          </Typography.Text>
                        </Button>
                      </Col>
                    ))}
                    <Col xs={12} sm={8} lg={12}>
                      <Button
                        type="text"
                        htmlType="button"
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          alignItems: "center",
                          gap: 16,
                          padding: 24,
                          borderRadius: 12,
                          border: "1px dashed hsl(var(--border))",
                          transition: "all 0.2s",
                        }}
                      >
                        <div style={{ width: 56, height: 56, borderRadius: 16, backgroundColor: "hsl(var(--muted))", display: "flex", alignItems: "center", justifyContent: "center", color: "hsl(var(--muted-foreground))" }}>
                          <PlusCircle className="h-6 w-6" />
                        </div>
                        <Typography.Text type="secondary" strong>
                          Tambah
                        </Typography.Text>
                      </Button>
                    </Col>
                  </Row>

                  <div style={{ marginTop: "auto", paddingTop: 40 }}>
                    <div style={{ background: "linear-gradient(135deg, hsl(var(--text-primary)), hsl(var(--text-primary)))", borderRadius: 12, padding: "20px 24px", color: "hsl(var(--surface))", position: "relative", overflow: "hidden", boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)" }}>
                      <div style={{ position: "relative", zIndex: 10 }}>
                        <Typography.Title level={4} style={{ marginBottom: 8, color: "hsl(var(--surface))" }}>
                          Bantuan?
                        </Typography.Title>
                        <Typography.Text type="secondary" style={{ display: "block", marginBottom: 32, lineHeight: 1.5 }}>
                          Proteksi & panduan transaksi aman.
                        </Typography.Text>
                        <Button
                          type="default"
                          htmlType="button"
                          style={{ fontSize: "0.75rem", fontWeight: "bold", letterSpacing: "0.1em", textTransform: "uppercase", backgroundColor: "hsl(var(--surface) / 0.1)", padding: "12px 24px", borderRadius: 12, border: "1px solid hsl(var(--surface) / 0.1)", color: "hsl(var(--surface))" }}
                        >
                          Hubungi Kami
                        </Button>
                      </div>
                      <LifeBuoy style={{ position: "absolute", bottom: -30, right: -30, height: 192, width: 192, color: "hsl(var(--surface) / 0.05)", transform: "rotate(12deg)" }} />
                    </div>
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>
        </Space>
      </main>
    </DashboardLayout>
  );
}
