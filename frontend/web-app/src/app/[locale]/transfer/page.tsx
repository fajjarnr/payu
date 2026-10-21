"use client";

import {
  Search,
  ChevronRight,
  PlusCircle,
  ArrowRight,
  Clock,
  Calendar as CalendarIcon,
  Zap,
  Truck,
  CheckCircle,
  AlertTriangle,
} from "@/components/icons";
import { useTranslations } from "next-intl";
import {
  Button,
  DatePicker,
  Form,
  Input,
  Card,
  Modal,
  Space,
  Row,
  Col,
  Typography,
  Tag,
  theme,
} from "antd";
import PageHeader from "@/components/PageHeader";
import MoneyText from "@/components/MoneyText";
import dayjs from "dayjs";
import {
  transferSchema,
  type TransferRequest,
  type TransferType,
  type TransferScheduleType,
} from "@/types";
import { zodFieldRule } from "@/lib/zodForm";
import { compareCurrency, parseCurrencyExact } from "@/lib/currency";
import { generateUUID } from "@/lib/utils";
import { useState, useCallback, useMemo, useEffect, useRef } from "react";
import { useBalance, useInitiateTransfer } from "@/hooks";
import { useAuthStore } from "@/stores";
import {
  useBeneficiaries,
  useCreateBeneficiary,
} from "@/hooks/useBeneficiaries";
import { useUIStore } from "@/stores";
import DashboardLayout from "@/components/DashboardLayout";
import { format } from "date-fns";
import { id } from "date-fns/locale";
import { formatCurrencyWithoutSymbol } from "@/lib/currency";
import { useRouter } from "@/lib/navigation";

type TransferTypeMeta = {
  type: TransferType;
  icon: React.ComponentType<{ className?: string }>;
};

const TRANSFER_TYPE_META: TransferTypeMeta[] = [
  { type: "INTERNAL_TRANSFER", icon: Zap },
  { type: "BIFAST_TRANSFER", icon: Zap },
  { type: "SKN_TRANSFER", icon: Truck },
  { type: "RTGS_TRANSFER", icon: Clock },
];

const TRANSFER_TYPE_I18N: Record<
  TransferType,
  { labelKey: string; descKey: string; timeKey: string }
> = {
  INTERNAL_TRANSFER: {
    labelKey: "typeInternalLabel",
    descKey: "typeInternalDesc",
    timeKey: "typeTimeInstant",
  },
  BIFAST_TRANSFER: {
    labelKey: "typeBifastLabel",
    descKey: "typeBifastDesc",
    timeKey: "typeTimeInstant",
  },
  SKN_TRANSFER: {
    labelKey: "typeSknLabel",
    descKey: "typeSknDesc",
    timeKey: "typeTimeBusinessDay",
  },
  RTGS_TRANSFER: {
    labelKey: "typeRtgsLabel",
    descKey: "typeRtgsDesc",
    timeKey: "typeTimeInstant",
  },
};

const SCHEDULE_TYPE_META: { type: TransferScheduleType }[] = [
  { type: "NOW" },
  { type: "SCHEDULED" },
  { type: "RECURRING" },
];

const SCHEDULE_TYPE_I18N: Record<
  TransferScheduleType,
  { labelKey: string; descKey: string }
> = {
  NOW: { labelKey: "scheduleNowLabel", descKey: "scheduleNowDesc" },
  SCHEDULED: {
    labelKey: "scheduleScheduledLabel",
    descKey: "scheduleScheduledDesc",
  },
  RECURRING: {
    labelKey: "scheduleRecurringLabel",
    descKey: "scheduleRecurringDesc",
  },
};
export interface ReviewContact {
  name: string;
  initial: string;
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

/**
 * Values captured when the user opens the review step. The form branch
 * unmounts at that point, so the review UI and the final submit read
 * from this snapshot instead of the live AntD store.
 */
export interface TransferReviewSnapshot {
  amount: string;
  toAccountId: string;
  description: string;
  scheduleType: string;
  scheduledAt?: string;
  recurringDay?: number;
  recurringMonth?: number;
  fromAccountId: string;
}

export default function TransferPage() {
  const t = useTranslations("transfers");
  const { token } = theme.useToken();
  const TRANSFER_TYPES = TRANSFER_TYPE_META.map((m) => ({
    ...m,
    label: t(TRANSFER_TYPE_I18N[m.type].labelKey),
    description: t(TRANSFER_TYPE_I18N[m.type].descKey),
    fee: t("typeFeeFree"),
    processingTime: t(TRANSFER_TYPE_I18N[m.type].timeKey),
  }));
  const SCHEDULE_TYPES = SCHEDULE_TYPE_META.map((m) => ({
    ...m,
    label: t(SCHEDULE_TYPE_I18N[m.type].labelKey),
    description: t(SCHEDULE_TYPE_I18N[m.type].descKey),
  }));
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showBeneficiaryModal, setShowBeneficiaryModal] = useState(false);
  const [beneficiaryNickname, setBeneficiaryNickname] = useState("");
  const [beneficiaryAccountNumber, setBeneficiaryAccountNumber] = useState("");
  // Snapshot of form values captured when the review step opens. The form
  // branch unmounts on review, and the AntD store does not survive the
  // swap reliably, so the review tree and submit read from this snapshot.
  const [reviewSnapshot, setReviewSnapshot] =
    useState<TransferReviewSnapshot | null>(null);
  // TRF-J-003: the confirm button is re-enabled for one frame once the
  // mutation settles, so a second click could fire a second transfer POST.
  // `isSubmittingRef` is the synchronous guard (isPending only flips on the
  // next render, so two clicks in the same tick both pass a state check);
  // `isSubmitLocked` mirrors it for the button's disabled state.
  const isSubmittingRef = useRef(false);
  const [isSubmitLocked, setIsSubmitLocked] = useState(false);
  // One transfer attempt = one idempotency key, scoped to the payload: a
  // duplicate submit of the same payload is deduped server-side, while a
  // changed amount/recipient gets a fresh key (the server rejects a reused
  // key with a different body).
  const transferAttemptRef = useRef<{ key: string; fingerprint: string } | null>(
    null,
  );
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const transferMutation = useInitiateTransfer();
  const createBeneficiary = useCreateBeneficiary(accountId ?? "");
  const router = useRouter();

  const { data: beneficiaries } = useBeneficiaries(accountId || undefined);
  const { data: balance } = useBalance(accountId || undefined);
  const recentContacts: Array<{
    name: string;
    initial: string;
    accountId: string;
  }> = (beneficiaries ?? []).map((b) => ({
    name: b.nickname || b.accountName || b.accountNumber.slice(-4),
    initial: (b.nickname || b.accountName || "B").charAt(0).toUpperCase(),
    accountId: b.accountNumber,
  }));

  const [form] = Form.useForm<TransferRequest>();
  const rule = (field: string) => zodFieldRule(form, transferSchema, field);
  // Review submit happens with the form branch unmounted, so the store is
  // empty there; prefer the snapshot captured when review opened. Enter-key
  // submit from the form branch still falls back to the store values.
  const onValid = (values: TransferRequest) =>
    onSubmit(
      (reviewSnapshot ?? transferSchema.parse(values)) as TransferRequest,
    );

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

  const handleBeneficiarySubmit = () => {
    const accountNumber = beneficiaryAccountNumber.trim();
    if (!/^\d{10,20}$/.test(accountNumber)) {
      addToast(t("toastBeneficiaryInvalid"), "warning");
      return;
    }
    createBeneficiary.mutate(
      {
        bankCode: "PAYU",
        accountNumber,
        nickname: beneficiaryNickname.trim() || undefined,
      },
      {
        onSuccess: () => {
          addToast(t("toastBeneficiaryAdded"), "success");
          setBeneficiaryNickname("");
          setBeneficiaryAccountNumber("");
          setShowBeneficiaryModal(false);
        },
        onError: () => addToast(t("toastBeneficiaryAddFailed"), "error"),
      },
    );
  };

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value.replace(/\D/g, "");
    form.setFieldsValue({ amount: rawValue || "0" });
  };

  const formattedAmount =
    amount === "0" ? "" : formatCurrencyWithoutSymbol(amount);

  const onSubmit = (data: TransferRequest) => {
    // TRF-J-003: a second submit of the same attempt must never reach the API.
    if (isSubmittingRef.current) return;
    const src = reviewSnapshot ?? data;
    // Scheduled/recurring are handled by the dedicated scheduled-transfers
    // endpoint; the transfer contract rejects those fields, so they are not
    // forwarded here (scheduleType only drives the success message).
    const senderAccountId = src.fromAccountId || accountId || "";
    const amount = parseCurrencyExact(src.amount);
    const type = data.transferType || "INTERNAL_TRANSFER";
    const fingerprint = `${senderAccountId}|${src.toAccountId}|${amount}|${type}`;
    if (
      !transferAttemptRef.current ||
      transferAttemptRef.current.fingerprint !== fingerprint
    ) {
      transferAttemptRef.current = { key: generateUUID(), fingerprint };
    }
    isSubmittingRef.current = true;
    setIsSubmitLocked(true);
    transferMutation.mutate(
      {
        senderAccountId,
        recipientAccountNumber: src.toAccountId,
        amount,
        description: src.description || "",
        type,
        idempotencyKey: transferAttemptRef.current.key,
      },
      {
        onSuccess: () => {
          const message =
            src.scheduleType === "NOW"
              ? t("toastSuccessNow")
              : src.scheduleType === "SCHEDULED"
                ? t("toastSuccessScheduled")
                : t("toastSuccessRecurring");
          addToast(message, "success");
          // The review branch renders before the success branch, so the
          // review flag must be cleared or the success screen never shows.
          setShowReview(false);
          setShowSuccess(true);
          // The attempt is done: the next transfer is a new logical operation
          // and must get its own idempotency key.
          transferAttemptRef.current = null;
        },
        onError: () => {
          // Retry is allowed: the same key is reused, so a transfer that did
          // reach the server is returned instead of executed twice.
          isSubmittingRef.current = false;
          setIsSubmitLocked(false);
          addToast(t("toastFailed"), "error");
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
      addToast(t("toastRecipientAmountRequired"), "warning");
      return;
    }

    if (formValues.scheduleType === "SCHEDULED" && !formValues.scheduledAt) {
      addToast(t("toastDateRequired"), "warning");
      return;
    }

    if (
      formValues.scheduleType === "RECURRING" &&
      (!formValues.recurringDay || !formValues.recurringMonth)
    ) {
      addToast(t("toastRecurringRequired"), "warning");
      return;
    }

    setReviewSnapshot(formValues as TransferReviewSnapshot);
    setShowReview(true);
  }, [formValues, addToast, t]);

  const selectedTransferType = TRANSFER_TYPES.find(
    (t) => t.type === transferType,
  );

  if (showReview) {
    const rv: TransferReviewSnapshot = reviewSnapshot ?? {
      amount,
      toAccountId: toAccountId ?? "",
      description: description ?? "",
      scheduleType,
      scheduledAt,
      recurringDay,
      recurringMonth,
      fromAccountId: accountId || "",
    };
    const reviewAccountId = selectedContact ?? rv.toAccountId ?? "";
    const selectedContactData = resolveReviewContact(
      recentContacts,
      selectedContact,
      rv.toAccountId,
    );
    const isRecipientVerified = isVerifiedReviewContact(selectedContactData);
    const selectedScheduleType = SCHEDULE_TYPES.find(
      (s) => s.type === scheduleType,
    );
    const TransferTypeIcon = selectedTransferType?.icon || Zap;

    return (
      <DashboardLayout>
        <Form
          form={form}
          onFinish={onValid}
          layout="vertical"
          style={{ width: "100%" }}
        >
          <div className="overflow-x-hidden">
            <Space direction="vertical" size={16}>
              <Space size={16}>
                <Button
                  type="default"
                  data-testid="back-from-review-button"
                  onClick={() => setShowReview(false)}
                  size="large"
                  aria-label="Kembali"
                >
                  <ChevronRight
                    style={{
                      width: 24,
                      height: 24,
                      transform: "rotate(180deg)",
                    }}
                  />
                </Button>
              </Space>

              <Card style={{ width: "100%" }}>
                <Row
                  gutter={[16, 16]}
                  justify="space-between"
                  align="middle"
                  style={{ marginBottom: 32, paddingBottom: 32 }}
                >
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
                          // A-01: antd tint pairs (guard: antd-theme.test.tsx), not legacy hsl vars
                          backgroundColor: isRecipientVerified
                            ? token.colorPrimaryBg
                            : token.colorFillTertiary,
                          color: isRecipientVerified
                            ? token.colorText
                            : token.colorTextSecondary,
                        }}
                        aria-hidden={!isRecipientVerified}
                      >
                        {selectedContactData?.initial ?? "?"}
                      </div>
                      <div>
                        <Typography.Text
                          type="secondary"
                          strong
                          style={{ display: "block", marginBottom: 4 }}
                        >
                          Kepada Penerima
                        </Typography.Text>
                        {isRecipientVerified ? (
                          <>
                            <Typography.Title
                              level={3}
                              style={{ marginBottom: 0 }}
                            >
                              {selectedContactData.name}
                            </Typography.Title>
                            <Typography.Text
                              strong
                              style={{ display: "block", marginTop: 4 }}
                            >
                              ID Akun: {reviewAccountId}
                            </Typography.Text>
                          </>
                        ) : (
                          <>
                            <Typography.Title
                              level={3}
                              style={{ marginBottom: 0 }}
                            >
                              Penerima belum terverifikasi
                            </Typography.Title>
                            <Typography.Text
                              strong
                              type="danger"
                              style={{ display: "block", marginTop: 4 }}
                              id="recipient-verification-alert"
                              role="alert"
                            >
                              <AlertTriangle
                                aria-hidden
                                style={{ marginRight: 8, verticalAlign: -2 }}
                              />
                              ID Akun {reviewAccountId || "-"} tidak cocok
                              dengan penerima tersimpan. Cek kembali nomornya
                              atau tambahkan sebagai penerima favorit.
                            </Typography.Text>
                          </>
                        )}
                      </div>
                    </Space>
                  </Col>
                  <Col xs={24} md={12} style={{ textAlign: "right" }}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{ display: "block", marginBottom: 4 }}
                    >
                      Jumlah Transfer
                    </Typography.Text>
                    <MoneyText amount={rv.amount} size="display" direction="out" />
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{ display: "block", marginTop: 8 }}
                    >
                      Mata Uang IDR
                    </Typography.Text>
                  </Col>
                </Row>
              </Card>

              <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                  <Col xs={24} md={8}>
                    <Card>
                      <Space
                        size={8}
                        style={{ display: "block", marginBottom: 8 }}
                      >
                        <TransferTypeIcon
                          style={{
                            width: 16,
                            height: 16,
                            color: "hsl(var(--primary))",
                          }}
                        />
                        <Typography.Text type="secondary" strong>
                          Tipe Transfer
                        </Typography.Text>
                      </Space>
                      <Typography.Text
                        strong
                        style={{ display: "block", fontSize: "0.875rem" }}
                      >
                        {selectedTransferType?.label}
                      </Typography.Text>
                      <Typography.Text
                        type="secondary"
                        style={{
                          display: "block",
                          fontSize: "0.75rem",
                          marginTop: 4,
                        }}
                      >
                        {selectedTransferType?.processingTime}
                      </Typography.Text>
                    </Card>
                  </Col>
                  <Col xs={24} md={8}>
                    <Card>
                      <Typography.Text
                        type="secondary"
                        strong
                        style={{ display: "block", marginBottom: 8 }}
                      >
                        Biaya Transfer
                      </Typography.Text>
                      <Typography.Text
                        strong
                        style={{ display: "block", fontSize: "0.875rem" }}
                      >
                        {selectedTransferType?.fee}
                      </Typography.Text>
                    </Card>
                  </Col>
                  <Col xs={24} md={8}>
                    <Card>
                      <Space
                        size={8}
                        style={{ display: "block", marginBottom: 8 }}
                      >
                        {scheduleType !== "NOW" && (
                          <CalendarIcon
                            style={{
                              width: 16,
                              height: 16,
                              color: "hsl(var(--primary))",
                            }}
                          />
                        )}
                        <Typography.Text type="secondary" strong>
                          Jadwal
                        </Typography.Text>
                      </Space>
                      <Typography.Text
                        strong
                        style={{ display: "block", fontSize: "0.875rem" }}
                      >
                        {selectedScheduleType?.label}
                      </Typography.Text>
                      {scheduleType === "SCHEDULED" && scheduledAt && (
                        <Typography.Text
                          type="secondary"
                          style={{
                            display: "block",
                            fontSize: "0.75rem",
                            marginTop: 4,
                          }}
                        >
                          {format(new Date(scheduledAt), "PPP", { locale: id })}
                        </Typography.Text>
                      )}
                      {scheduleType === "RECURRING" && (
                        <Typography.Text
                          type="secondary"
                          style={{
                            display: "block",
                            fontSize: "0.75rem",
                            marginTop: 4,
                          }}
                        >
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
                      <Typography.Text
                        type="secondary"
                        strong
                        style={{ display: "block", marginBottom: 8 }}
                      >
                        Kantong Sumber
                      </Typography.Text>
                      <Typography.Text
                        strong
                        style={{ display: "block", fontSize: "1.125rem" }}
                      >
                        Kantong Utama Cair
                      </Typography.Text>
                      <Typography.Text
                        strong
                        style={{
                          display: "block",
                          color: "hsl(var(--primary))",
                          marginTop: 8,
                        }}
                      >
                        Saldo:{" "}
                        {balance ? `Rp ${balance.availableBalance}` : "—"}
                      </Typography.Text>
                    </Card>
                  </Col>
                  {rv.description && (
                    <Col xs={24} md={12}>
                      <Card>
                        <Typography.Text
                          type="secondary"
                          strong
                          style={{ display: "block", marginBottom: 8 }}
                        >
                          Pesan Konfirmasi
                        </Typography.Text>
                        <Typography.Text
                          strong
                          style={{ display: "block", fontSize: "1.125rem" }}
                        >
                          &quot;{description}&quot;
                        </Typography.Text>
                      </Card>
                    </Col>
                  )}
                </Row>

              <Button
                type="primary"
                htmlType="submit"
                data-testid="confirm-transfer-button"
                disabled={
                  transferMutation.isPending ||
                  isSubmitLocked ||
                  !isRecipientVerified
                }
                aria-describedby={
                  isRecipientVerified
                    ? undefined
                    : "recipient-verification-alert"
                }
                block
                size="large"
              >
                {transferMutation.isPending ? t("validating") : t("confirmButton")}
              </Button>
            </Space>
          </div>
        </Form>
      </DashboardLayout>
    );
  }
  if (showSuccess) {
    const successSnapshot = reviewSnapshot;
    const successContact = resolveReviewContact(
      recentContacts,
      selectedContact,
      successSnapshot?.toAccountId ?? formValues.toAccountId,
    );
    const successSchedule = SCHEDULE_TYPES.find((s) => s.type === scheduleType);
    const now = new Date();

    const handleTransferAgain = () => {
      form.setFieldsValue({ amount: "0", description: "" });
      setSelectedContact(null);
      setShowSuccess(false);
      isSubmittingRef.current = false;
      setIsSubmitLocked(false);
      transferAttemptRef.current = null;
    };

    return (
      <DashboardLayout>
        <div className="overflow-x-hidden">
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              minHeight: "60vh",
              padding: "48px 16px",
            }}
          >
            <div
              className="animate-seal-lock will-change-transform"
              style={{ marginBottom: 32 }}
            >
              <div
                style={{
                  width: 96,
                  height: 96,
                  borderRadius: "50%",
                  backgroundColor: "hsl(var(--primary) / 0.1)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <CheckCircle className="w-14 h-14 text-primary" />
              </div>
            </div>

            <Typography.Title
              level={2}
              style={{ textAlign: "center", marginBottom: 8 }}
            >
              Transfer Berhasil!
            </Typography.Title>
            <Typography.Text
              type="secondary"
              style={{
                textAlign: "center",
                display: "block",
                marginBottom: 32,
              }}
            >
              {successSchedule?.label === "Sekarang"
                ? "Dana telah dikirim ke penerima"
                : successSchedule?.label === "Terjadwal"
                  ? "Transfer terjadwal telah diatur"
                  : "Transfer berulang telah diatur"}
            </Typography.Text>

            <Card
              className="animate-receipt-unfold"
              style={{ width: "100%", maxWidth: 448 }}
            >
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
                      backgroundColor: successContact
                        ? token.colorPrimaryBg
                        : token.colorFillTertiary,
                      color: successContact
                        ? token.colorText
                        : token.colorTextSecondary,
                    }}
                  >
                    {successContact?.initial || "?"}
                  </div>
                  <div>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{ display: "block", marginBottom: 4 }}
                    >
                      Penerima
                    </Typography.Text>
                    <Typography.Text
                      strong
                      style={{ display: "block", fontSize: "1.125rem" }}
                    >
                      {successContact?.name || "-"}
                    </Typography.Text>
                    <Typography.Text
                      type="secondary"
                      style={{ display: "block", fontSize: "0.75rem" }}
                    >
                      {successSnapshot?.toAccountId || "-"}
                    </Typography.Text>
                  </div>
                </Space>

                <div style={{ textAlign: "center" }}>
                  <Typography.Text
                    type="secondary"
                    strong
                    style={{ display: "block", marginBottom: 8 }}
                  >
                    Jumlah Transfer
                  </Typography.Text>
                  <MoneyText
                    amount={successSnapshot?.amount ?? "0"}
                    size="display"
                    direction="out"
                  />
                </div>

                <Row gutter={[16, 16]} style={{ paddingTop: 16 }}>
                  <Col span={12}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{ display: "block", marginBottom: 4 }}
                    >
                      Tipe Transfer
                    </Typography.Text>
                    <Typography.Text
                      strong
                      style={{ display: "block", fontSize: "0.875rem" }}
                    >
                      {selectedTransferType?.label || "-"}
                    </Typography.Text>
                  </Col>
                  <Col span={12} style={{ textAlign: "right" }}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{ display: "block", marginBottom: 4 }}
                    >
                      Waktu
                    </Typography.Text>
                    <Typography.Text
                      strong
                      style={{ display: "block", fontSize: "0.875rem" }}
                    >
                      {format(now, "dd MMM yyyy, HH:mm", { locale: id })}
                    </Typography.Text>
                  </Col>
                </Row>
              </Space>
            </Card>

            <Space
              direction="vertical"
              size={16}
              style={{ width: "100%", maxWidth: 448, marginTop: 32 }}
            >
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
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="overflow-x-hidden">
        <Space direction="vertical" size={16}>
          <PageHeader title={t("title")} subtitle={t("subtitle")} />

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
                    validateTrigger="onBlur"
                    style={{ marginBottom: 0 }}
                  >
                    <Input type="hidden" aria-hidden tabIndex={-1} />
                  </Form.Item>
                  <Form.Item
                    name="scheduleType"
                    rules={[rule("scheduleType")]}
                    validateTrigger="onBlur"
                    style={{ marginBottom: 0 }}
                  >
                    <Input type="hidden" aria-hidden tabIndex={-1} />
                  </Form.Item>
                  <Form.Item
                    name="fromAccountId"
                    rules={[rule("fromAccountId")]}
                    validateTrigger="onBlur"
                    style={{ marginBottom: 0 }}
                  >
                    <Input type="hidden" aria-hidden tabIndex={-1} />
                  </Form.Item>
                  <Card>
                    <Typography.Text
                      strong
                      style={{ display: "block", marginBottom: 16 }}
                    >
                      Pilih Metode Transfer
                    </Typography.Text>
                    <Row
                      gutter={[16, 16]}
                      role="radiogroup"
                      aria-label="Pilih Metode Transfer"
                    >
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
                                width: "100%",
                                height: "auto",
                                border: isSelected
                                  ? "2px solid hsl(var(--primary))"
                                  : "2px solid transparent",
                                transition: "all 0.2s",
                                ...(isSelected
                                  ? {
                                      backgroundColor:
                                        "hsl(var(--primary) / 0.05)",
                                      boxShadow:
                                        "0 10px 15px -3px hsl(var(--primary) / 0.1)",
                                    }
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
                                    ...(isSelected
                                      ? {
                                          backgroundColor:
                                            "hsl(var(--primary) / 0.1)",
                                          color: "hsl(var(--primary))",
                                        }
                                      : {
                                          backgroundColor:
                                            "hsl(var(--muted) / 0.5)",
                                          color: "hsl(var(--muted-foreground))",
                                        }),
                                  }}
                                >
                                  <div
                                    style={{
                                      width: 24,
                                      height: 24,
                                      display: "flex",
                                      alignItems: "center",
                                      justifyContent: "center",
                                    }}
                                  >
                                    <Icon className="h-6 w-6" />
                                  </div>
                                </div>
                                {isSelected && (
                                  <div
                                    style={{
                                      width: 8,
                                      height: 8,
                                      backgroundColor: "hsl(var(--primary))",
                                      borderRadius: "50%",
                                    }}
                                    className="animate-pulse"
                                  />
                                )}
                              </Row>
                              <div style={{ textAlign: "left" }}>
                                <Typography.Text
                                  strong
                                  style={{
                                    display: "block",
                                    fontSize: "0.875rem",
                                    marginBottom: 4,
                                  }}
                                >
                                  {t.label}
                                </Typography.Text>
                                <Typography.Text
                                  type="secondary"
                                  style={{
                                    display: "block",
                                    fontSize: "0.75rem",
                                    marginBottom: 8,
                                  }}
                                >
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
                    <Typography.Text
                      strong
                      style={{ display: "block", marginBottom: 16 }}
                    >
                      Jadwal Transfer
                    </Typography.Text>
                    <Row
                      gutter={[16, 16]}
                      role="radiogroup"
                      aria-label="Jadwal Transfer"
                    >
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
                                width: "100%",
                                height: "auto",
                                border: isSelected
                                  ? "2px solid hsl(var(--primary))"
                                  : "2px solid transparent",
                                transition: "all 0.2s",
                                ...(isSelected
                                  ? {
                                      backgroundColor:
                                        "hsl(var(--primary) / 0.05)",
                                      boxShadow:
                                        "0 10px 15px -3px hsl(var(--primary) / 0.1)",
                                    }
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
                                        ? {
                                            backgroundColor:
                                              "hsl(var(--primary) / 0.1)",
                                            color: "hsl(var(--primary))",
                                          }
                                        : {
                                            backgroundColor:
                                              "hsl(var(--muted) / 0.5)",
                                            color:
                                              "hsl(var(--muted-foreground))",
                                          }),
                                    }}
                                  >
                                    {s.type === "SCHEDULED" ? (
                                      <CalendarIcon
                                        style={{ width: 20, height: 20 }}
                                      />
                                    ) : (
                                      <Clock
                                        style={{ width: 20, height: 20 }}
                                      />
                                    )}
                                  </div>
                                )}
                                {isSelected && (
                                  <div
                                    style={{
                                      marginLeft: "auto",
                                      width: 8,
                                      height: 8,
                                      backgroundColor: "hsl(var(--primary))",
                                      borderRadius: "50%",
                                    }}
                                    className="animate-pulse"
                                  />
                                )}
                              </Row>
                              <div style={{ textAlign: "left" }}>
                                <Typography.Text
                                  strong
                                  style={{
                                    display: "block",
                                    fontSize: "0.875rem",
                                    marginBottom: 4,
                                  }}
                                >
                                  {s.label}
                                </Typography.Text>
                                <Typography.Text
                                  type="secondary"
                                  style={{
                                    display: "block",
                                    fontSize: "0.75rem",
                                  }}
                                >
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
                            style={{
                              fontSize: "0.75rem",
                              fontWeight: "bold",
                              color: "hsl(var(--muted-foreground))",
                              letterSpacing: "0.1em",
                              textTransform: "uppercase",
                              marginBottom: 12,
                              display: "block",
                            }}
                          >
                            Tanggal Transfer
                          </label>
                        }
                      >
                        <DatePicker
                          id="transfer-scheduled-at"
                          style={{
                            width: "100%",
                            height: 64,
                            borderRadius: 12,
                            fontWeight: "bold",
                          }}
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
                    <Space
                      direction="vertical"
                      size={16}
                      className="animate-fade-in"
                      style={{ marginTop: 24 }}
                    >
                      <Space direction="vertical" size={16}>
                        <Typography.Text
                          type="secondary"
                          strong
                          style={{ display: "block", marginLeft: 8 }}
                          id="recurring-day-label"
                        >
                          Pilih Tanggal Tagihan / Transfer
                        </Typography.Text>
                        <Form.Item
                          name="recurringDay"
                          rules={[rule("recurringDay")]}
                          validateTrigger="onBlur"
                          style={{ marginBottom: 0 }}
                        >
                          <Input type="hidden" aria-hidden tabIndex={-1} />
                        </Form.Item>
                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns:
                              "repeat(auto-fill, minmax(44px, 1fr))",
                            gap: 8,
                            backgroundColor: "hsl(var(--muted) / 0.3)",
                            padding: 16,
                            borderRadius: 12,
                            border: "1px solid hsl(var(--border))",
                            maxHeight: 220,
                            overflowY: "auto",
                          }}
                          role="radiogroup"
                          aria-labelledby="recurring-day-label"
                        >
                          {Array.from({ length: 31 }, (_, i) => i + 1).map(
                            (d) => (
                              <Button
                                key={d}
                                type="text"
                                htmlType="button"
                                role="radio"
                                aria-checked={recurringDay === d}
                                aria-label={`Tanggal ${d}`}
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
                                  ...(recurringDay === d
                                    ? {
                                        backgroundColor: "hsl(var(--primary))",
                                        color: "hsl(var(--surface))",
                                        boxShadow:
                                          "0 10px 15px -3px hsl(var(--primary) / 0.3)",
                                      }
                                    : {
                                        backgroundColor: "hsl(var(--card))",
                                        color: "hsl(var(--foreground) / 0.6)",
                                      }),
                                }}
                              >
                                {d}
                              </Button>
                            ),
                          )}
                        </div>
                      </Space>

                      <Space direction="vertical" size={16}>
                        <Row
                          justify="space-between"
                          align="middle"
                          style={{ marginLeft: 8 }}
                        >
                          <Typography.Text
                            type="secondary"
                            strong
                            id="recurring-month-label"
                          >
                            Pilih Bulan (Opsional)
                          </Typography.Text>
                          <Button
                            type="link"
                            htmlType="button"
                            onClick={() =>
                              form.setFieldsValue({ recurringMonth: undefined })
                            }
                            style={{
                              fontSize: "0.75rem",
                              fontWeight: "bold",
                              color: "hsl(var(--primary-dark))",
                              letterSpacing: "0.1em",
                              textTransform: "uppercase",
                            }}
                          >
                            Reset ke Setiap Bulan
                          </Button>
                        </Row>
                        <Form.Item
                          name="recurringMonth"
                          rules={[rule("recurringMonth")]}
                          validateTrigger="onBlur"
                          style={{ marginBottom: 0 }}
                        >
                          <Input type="hidden" aria-hidden tabIndex={-1} />
                        </Form.Item>
                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns:
                              "repeat(auto-fill, minmax(64px, 1fr))",
                            gap: 8,
                            backgroundColor: "hsl(var(--muted) / 0.3)",
                            padding: 16,
                            borderRadius: 12,
                            border: "1px solid hsl(var(--border))",
                          }}
                          role="radiogroup"
                          aria-labelledby="recurring-month-label"
                        >
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
                              <Button
                                key={m}
                                type="text"
                                htmlType="button"
                                role="radio"
                                aria-checked={recurringMonth === val}
                                aria-label={`Bulan ${m}`}
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
                                  ...(recurringMonth === val
                                    ? {
                                        backgroundColor: "hsl(var(--primary))",
                                        color: "hsl(var(--surface))",
                                        boxShadow:
                                          "0 10px 15px -3px hsl(var(--primary) / 0.3)",
                                      }
                                    : {
                                        backgroundColor: "hsl(var(--card))",
                                        color: "hsl(var(--foreground) / 0.6)",
                                      }),
                                }}
                              >
                                {m}
                              </Button>
                            );
                          })}
                        </div>
                      </Space>
                    </Space>
                  )}

                  <Form.Item
                    name="toAccountId"
                    label={
                      <label htmlFor="transfer-recipient">
                        {t("recipientAccount")}
                      </label>
                    }
                    rules={[rule("toAccountId")]}
                    validateTrigger="onBlur"
                    required
                  >
                    <Input
                      id="transfer-recipient"
                      data-testid="recipient-account-input"
                      type="text"
                      placeholder="Masukkan ID Akun atau Nomor Rekening"
                      aria-required="true"
                      prefix={<Search aria-hidden />}
                      style={{
                        height: 64,
                        fontSize: "1.125rem",
                      }}
                    />
                  </Form.Item>

                  <Card style={{ position: "relative" }}>

                    <Row
                      justify="space-between"
                      align="middle"
                      gutter={[8, 8]}
                      style={{
                        marginBottom: 24,
                        position: "relative",
                        zIndex: 10,
                      }}
                    >
                      <Typography.Text type="secondary" strong>
                        Nominal Transfer
                      </Typography.Text>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 8,
                          backgroundColor: "hsl(var(--success-light))",
                          padding: "6px 12px",
                          borderRadius: "9999px",
                          border: "1px solid hsl(var(--primary) / 0.1)",
                          boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
                          flexShrink: 0,
                        }}
                      >
                        <div
                          style={{
                            width: 6,
                            height: 6,
                            backgroundColor: "hsl(var(--primary))",
                            borderRadius: "50%",
                          }}
                          className="animate-pulse"
                        />
                        <Typography.Text strong style={{ fontSize: "0.75rem" }}>
                          {t("securedIdr")}
                        </Typography.Text>
                      </div>
                    </Row>

                    <Space
                      size={16}
                      style={{
                        marginBottom: 24,
                        position: "relative",
                        zIndex: 10,
                        display: "block",
                      }}
                    >
                      <Form.Item
                        name="amount"
                        label={
                          <label htmlFor="transfer-amount">
                            {t("amount")}
                          </label>
                        }
                        rules={[rule("amount")]}
                        validateTrigger="onBlur"
                        required
                      >
                        <Input
                          id="transfer-amount"
                          data-testid="amount-input"
                          value={formattedAmount}
                          onChange={handleAmountChange}
                          placeholder="0"
                          inputMode="numeric"
                          aria-required="true"
                          style={{
                            width: "100%",
                            fontSize: "2rem",
                            fontWeight: "bold",
                          }}
                          variant="borderless"
                        />
                      </Form.Item>

                    <Card style={{ position: "relative", zIndex: 10 }}>
                      <Typography.Text
                        type="secondary"
                        strong
                        style={{ display: "block", marginBottom: 12 }}
                      >
                        Memo Transaksi
                      </Typography.Text>
                      <Form.Item
                        name="description"
                        label={
                          <label htmlFor="transfer-memo">{t("memo")}</label>
                        }
                        rules={[rule("description")]}
                        validateTrigger="onBlur"
                      >
                        <Input
                          id="transfer-memo"
                          data-testid="description-input"
                          type="text"
                          placeholder="Apa tujuan transfer ini?"
                          style={{
                            width: "100%",
                            fontSize: "1rem",
                          }}
                        />
                      </Form.Item>
                    </Card>
                    </Space>
                  </Card>

                  <Button
                    type="primary"
                    htmlType="button"
                    onClick={handleReview}
                    data-testid="review-transfer-button"
                    block
                    size="large"
                  >
                    {t("reviewButton")}
                    <ArrowRight
                      style={{ width: 20, height: 20, marginLeft: 8 }}
                    />
                  </Button>
                </Form>
              </Space>
            </Col>

            <Col xs={24} lg={8}>
              <Space direction="vertical" size={16}>
                <Card>
                  <Row
                    justify="space-between"
                    align="middle"
                    style={{ marginBottom: 24 }}
                  >
                    <Typography.Text strong>Penerima Favorit</Typography.Text>
                    <div
                      style={{
                        height: 4,
                        width: 32,
                        backgroundColor: "hsl(var(--primary))",
                        borderRadius: "9999px",
                      }}
                    />
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
                            border:
                              selectedContact === c.accountId
                                ? "1px solid hsl(var(--primary))"
                                : "1px solid transparent",
                            transition: "all 0.2s",
                            ...(selectedContact === c.accountId
                              ? {
                                  backgroundColor: "hsl(var(--primary) / 0.05)",
                                  boxShadow:
                                    "0 10px 15px -3px hsl(var(--primary) / 0.1)",
                                }
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
                              backgroundColor: token.colorPrimaryBg,
                              color: token.colorText,
                            }}
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
                        onClick={() => setShowBeneficiaryModal(true)}
                        aria-label="Tambah penerima"
                        data-testid="add-beneficiary-button"
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
                        <div
                          style={{
                            width: 56,
                            height: 56,
                            borderRadius: 16,
                            backgroundColor: "hsl(var(--muted))",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            color: "hsl(var(--muted-foreground))",
                          }}
                        >
                          <PlusCircle style={{ width: 24, height: 24 }} />
                        </div>
                        <Typography.Text type="secondary" strong>
                          Tambah
                        </Typography.Text>
                      </Button>
                    </Col>
                  </Row>

                  <div style={{ marginTop: "auto", paddingTop: 40 }}>
                    <div
                      style={{
                        background:
                          "linear-gradient(135deg, hsl(var(--text-primary)), hsl(var(--text-primary)))",
                        borderRadius: 12,
                        padding: "20px 24px",
                        color: "hsl(var(--surface))",
                        position: "relative",
                        overflow: "hidden",
                        boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)",
                      }}
                    >
                      <div style={{ position: "relative", zIndex: 10 }}>
                        <Typography.Title
                          level={4}
                          style={{
                            marginBottom: 8,
                            color: "hsl(var(--surface))",
                          }}
                        >
                          Bantuan?
                        </Typography.Title>
                        <Typography.Text
                          type="secondary"
                          style={{
                            display: "block",
                            marginBottom: 32,
                            lineHeight: 1.5,
                          }}
                        >
                          Proteksi & panduan transaksi aman.
                        </Typography.Text>
                        <Button
                          type="default"
                          htmlType="button"
                          onClick={() => router.push("/support")}
                          data-testid="contact-support-button"
                          style={{
                            fontSize: "0.75rem",
                            fontWeight: "bold",
                            letterSpacing: "0.1em",
                            textTransform: "uppercase",
                            backgroundColor: "hsl(var(--surface) / 0.1)",
                            padding: "12px 24px",
                            borderRadius: 12,
                            border: "1px solid hsl(var(--surface) / 0.1)",
                            color: "hsl(var(--surface))",
                          }}
                        >
                          Hubungi Kami
                        </Button>
                      </div>
                    </div>
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>
        </Space>
        <Modal
          title="Tambah Penerima"
          open={showBeneficiaryModal}
          onOk={handleBeneficiarySubmit}
          onCancel={() => setShowBeneficiaryModal(false)}
          confirmLoading={createBeneficiary.isPending}
          okText="Simpan"
          cancelText="Batal"
          destroyOnHidden
        >
          <Space
            direction="vertical"
            size={16}
            style={{ width: "100%", marginTop: 16 }}
          >
            <Space direction="vertical" size={4} style={{ width: "100%" }}>
              <label
                htmlFor="new-beneficiary-name"
                style={{
                  fontSize: 12,
                  fontWeight: 700,
                  color: "var(--ant-color-text-secondary)",
                  textTransform: "uppercase",
                  letterSpacing: "0.1em",
                }}
              >
                Nama Penerima (opsional)
              </label>
              <Input
                id="new-beneficiary-name"
                data-testid="new-beneficiary-name"
                value={beneficiaryNickname}
                onChange={(e) => setBeneficiaryNickname(e.target.value)}
                placeholder="Nama panggilan"
                maxLength={100}
              />
            </Space>
            <Space direction="vertical" size={4} style={{ width: "100%" }}>
              <label
                htmlFor="new-beneficiary-account"
                style={{
                  fontSize: 12,
                  fontWeight: 700,
                  color: "var(--ant-color-text-secondary)",
                  textTransform: "uppercase",
                  letterSpacing: "0.1em",
                }}
              >
                Nomor Rekening
              </label>
              <Input
                id="new-beneficiary-account"
                data-testid="new-beneficiary-account"
                value={beneficiaryAccountNumber}
                onChange={(e) =>
                  setBeneficiaryAccountNumber(e.target.value.replace(/\D/g, ""))
                }
                placeholder="1234567890"
                inputMode="numeric"
                maxLength={20}
              />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Bank: PAYU — 10-20 digit
              </Typography.Text>
            </Space>
          </Space>
        </Modal>
      </div>
    </DashboardLayout>
  );
}
