"use client";

import React, { useState } from "react";
import { useTranslations } from "next-intl";
import DashboardLayout from "@/components/DashboardLayout";
import {
  Users,
  Plus,
  DollarSign,
  CheckCircle2,
  Clock,
  XCircle,
  UserPlus,
  Receipt,
  Loader2,
} from "@/components/icons";
import {
  Button,
  Card,
  Col,
  Input,
  Row,
  Space,
  Tag,
  Typography,
  Empty,
} from "antd";
import {
  useSplitBills,
  useCreateSplitBill,
  useSettleSplitBill,
  useAddParticipant,
  useActivateSplitBill,
} from "@/hooks";
import { useAuthStore } from "@/stores/authStore";
import { notify as toast } from "@/lib/notify";
import {
  addCurrency,
  asMoney,
  divideCurrency,
  formatCurrency,
  parseCurrencyExact,
  type Money,
} from "@/lib/currency";
import type { SplitBillParticipant } from "@/services/TransactionService";
import { SkipLink } from "@/lib/a11y";
import { ON_PRIMARY } from "@/theme/tokens";

const { Title, Text } = Typography;

export default function SplitBillPage() {
  const t = useTranslations("splitBill");
  const { accountId } = useAuthStore();
  const acctId = accountId ?? "";
  const { data: splitBillsData, isLoading } = useSplitBills(acctId);
  const createSplitBill = useCreateSplitBill();
  const settleBill = useSettleSplitBill();
  const addParticipant = useAddParticipant();
  const activateBill = useActivateSplitBill();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newBillName, setNewBillName] = useState("");
  const [newBillAmount, setNewBillAmount] = useState("");
  const [participants, setParticipants] = useState<
    Array<{ accountId: string; accountNumber: string; accountName: string }>
  >([{ accountId: "", accountNumber: "", accountName: "" }]);

  const updateParticipant = (
    index: number,
    field: "accountId" | "accountNumber" | "accountName",
    value: string,
  ) => {
    setParticipants((prev) =>
      prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)),
    );
  };

  const addParticipantRow = () => {
    setParticipants((prev) => [
      ...prev,
      { accountId: "", accountNumber: "", accountName: "" },
    ]);
  };

  const removeParticipantRow = (index: number) => {
    setParticipants((prev) => prev.filter((_, i) => i !== index));
  };

  const splitBills = (Array.isArray(splitBillsData)
    ? splitBillsData
    : []) as unknown as Array<{
    id: string;
    description: string;
    totalAmount: Money;
    currency: string;
    status: string;
    createdAt: string;
    participants: Array<{
      id: string;
      accountId: string;
      name: string;
      amount: Money;
      status: string;
      paidAmount: Money;
    }>;
  }>;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return (
          <Tag variant="filled" color="green">
            {t("statusActive")}
          </Tag>
        );
      case "SETTLED":
        return (
          <Tag variant="filled" color="blue">
            {t("statusSettled")}
          </Tag>
        );
      case "PENDING":
        return (
          <Tag variant="filled" color="orange">
            {t("statusPending")}
          </Tag>
        );
      case "DRAFT":
        return (
          <Tag variant="filled" color="default">
            {t("statusDraft")}
          </Tag>
        );
      case "CANCELLED":
        return (
          <Tag variant="filled" color="red">
            {t("statusCancelled")}
          </Tag>
        );
      default:
        return <Tag bordered>{status}</Tag>;
    }
  };

  const handleCreate = () => {
    if (!newBillName.trim()) {
      toast.warning(t("validationNameRequired"));
      return;
    }
    if (!newBillAmount.trim()) {
      toast.warning(t("validationAmountRequired"));
      return;
    }
    if (!acctId) {
      toast.warning(t("validationAccountNotLoaded"));
      return;
    }
    const validParticipants = participants
      .map((p) => ({
        ...p,
        accountId: p.accountId.trim(),
        accountNumber: p.accountNumber.trim(),
        accountName: p.accountName.trim(),
      }))
      .filter((p) => p.accountId && p.accountNumber && p.accountName);
    if (validParticipants.length === 0) {
      toast.warning(t("validationParticipantRequired"));
      return;
    }
    const totalAmount = parseCurrencyExact(newBillAmount);
    const perHead = divideCurrency(totalAmount, validParticipants.length);
    createSplitBill.mutate(
      {
        title: newBillName,
        totalAmount,
        currency: "IDR",
        creatorAccountId: acctId,
        splitType: "EQUAL",
        participants: validParticipants.map((p) => ({
          ...p,
          amountOwed: perHead,
        })),
      },
      {
        onSuccess: () => {
          setShowCreateModal(false);
          setNewBillName("");
          setNewBillAmount("");
          setParticipants([
            { accountId: "", accountNumber: "", accountName: "" },
          ]);
        },
      },
    );
  };

  // Newly created bills are stored as DRAFT; filtering them out hid them from
  // the list entirely (the create form appeared to succeed with no result).
  const activeBills = splitBills.filter(
    (b) =>
      b.status === "ACTIVE" || b.status === "PENDING" || b.status === "DRAFT",
  );
  const settledBills = splitBills.filter((b) => b.status === "SETTLED");

  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size={24} style={{ width: "100%" }}>
        {/* Header */}
        <Row justify="space-between" align="bottom" gutter={[16, 16]}>
          <Col>
            <Title level={1} style={{ margin: 0 }}>
              {t("title")}
            </Title>
            <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
              {t("subtitle")}
            </Text>
          </Col>
          <Col>
            <Button
              type="primary"
              size="large"
              onClick={() => setShowCreateModal(true)}
            >
              <Plus style={{ marginRight: 8 }} /> {t("newSplitBill")}
            </Button>
          </Col>
        </Row>

        {/* Stats */}
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Card>
              <Space size={20}>
                <div
                  style={{
                    width: 48,
                    height: 48,
                    backgroundColor: "var(--ant-color-primary)",
                    borderRadius: 24,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Receipt
                    style={{
                      width: 24,
                      height: 24,
                      color: ON_PRIMARY,
                    }}
                  />
                </div>
                <div>
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      textTransform: "uppercase",
                      letterSpacing: "0.1em",
                    }}
                  >
                    {t("statActive")}
                  </Text>
                  <Title level={2} style={{ margin: "4px 0 0" }}>
                    {activeBills.length}
                  </Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card>
              <Space size={20}>
                <div
                  style={{
                    width: 48,
                    height: 48,
                    backgroundColor: "var(--ant-color-primary)",
                    borderRadius: 24,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <CheckCircle2
                    style={{
                      width: 24,
                      height: 24,
                      color: ON_PRIMARY,
                    }}
                  />
                </div>
                <div>
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      textTransform: "uppercase",
                      letterSpacing: "0.1em",
                    }}
                  >
                    {t("statSettled")}
                  </Text>
                  <Title level={2} style={{ margin: "4px 0 0" }}>
                    {settledBills.length}
                  </Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card>
              <Space size={20}>
                <div
                  style={{
                    width: 48,
                    height: 48,
                    backgroundColor: "var(--ant-color-primary-bg)",
                    borderRadius: 24,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <DollarSign
                    style={{
                      width: 24,
                      height: 24,
                      color: "var(--ant-color-primary-text)",
                    }}
                  />
                </div>
                <div>
                  <Text
                    type="secondary"
                    strong
                    style={{
                      fontSize: 12,
                      textTransform: "uppercase",
                      letterSpacing: "0.1em",
                    }}
                  >
                    {t("statTotal")}
                  </Text>
                  <Title level={2} style={{ margin: "4px 0 0" }}>
                    {formatCurrency(
                      splitBills.reduce(
                        (sum, b) => addCurrency(sum, b.totalAmount),
                        asMoney("0"),
                      ),
                    )}
                  </Title>
                </div>
              </Space>
            </Card>
          </Col>
        </Row>

        {/* Create Modal */}
        {showCreateModal && (
          <Card>
            <Space orientation="vertical" size={24} style={{ width: "100%" }}>
              <Title level={2} style={{ margin: 0 }}>
                {t("createTitle")}
              </Title>
              <Row gutter={[24, 24]}>
                <Col xs={24} md={12}>
                  <Space
                    orientation="vertical"
                    size={8}
                    style={{ width: "100%" }}
                  >
                    <label
                      htmlFor="splitbill-description"
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        color: "var(--ant-color-text-secondary)",
                        textTransform: "uppercase",
                        letterSpacing: "0.1em",
                      }}
                    >
                      {t("descriptionLabel")}
                    </label>
                    <Input
                      id="splitbill-description"
                      placeholder={t("descriptionPlaceholder")}
                      value={newBillName}
                      onChange={(e) => setNewBillName(e.target.value)}
                    />
                  </Space>
                </Col>
                <Col xs={24} md={12}>
                  <Space
                    orientation="vertical"
                    size={8}
                    style={{ width: "100%" }}
                  >
                    <label
                      htmlFor="splitbill-amount"
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        color: "var(--ant-color-text-secondary)",
                        textTransform: "uppercase",
                        letterSpacing: "0.1em",
                      }}
                    >
                      {t("totalLabel")}
                    </label>
                    <Input
                      id="splitbill-amount"
                      type="number"
                      placeholder="150000"
                      value={newBillAmount}
                      onChange={(e) => setNewBillAmount(e.target.value)}
                    />
                  </Space>
                </Col>
              </Row>
              <Space orientation="vertical" size={12} style={{ width: "100%" }}>
                <Row justify="space-between" align="middle">
                  <label
                    htmlFor="splitbill-participant-accountId-0"
                    style={{
                      fontSize: 12,
                      fontWeight: 600,
                      color: "var(--ant-color-text-secondary)",
                      textTransform: "uppercase",
                      letterSpacing: "0.1em",
                    }}
                  >
                    {t("participantsLabel")}
                  </label>
                  <Button
                    htmlType="button"
                    size="small"
                    onClick={addParticipantRow}
                  >
                    <UserPlus style={{ marginRight: 4 }} /> {t("addParticipant")}
                  </Button>
                </Row>
                {participants.map((p, i) => (
                  <Row key={i} gutter={[12, 12]} align="middle">
                    <Col xs={24} md={6}>
                      <Input
                        id={`splitbill-participant-accountId-${i}`}
                        aria-label={t("participantAccountIdLabel", { index: i + 1 })}
                        placeholder={t("accountIdPlaceholder")}
                        value={p.accountId}
                        onChange={(e) =>
                          updateParticipant(i, "accountId", e.target.value)
                        }
                      />
                    </Col>
                    <Col xs={24} md={6}>
                      <Input
                        id={`splitbill-participant-accountNumber-${i}`}
                        aria-label={t("participantAccountNumberLabel", { index: i + 1 })}
                        placeholder={t("accountNumberPlaceholder")}
                        value={p.accountNumber}
                        onChange={(e) =>
                          updateParticipant(i, "accountNumber", e.target.value)
                        }
                      />
                    </Col>
                    <Col xs={24} md={8}>
                      <Input
                        id={`splitbill-participant-accountName-${i}`}
                        aria-label={t("participantAccountNameLabel", { index: i + 1 })}
                        placeholder={t("namePlaceholder")}
                        value={p.accountName}
                        onChange={(e) =>
                          updateParticipant(i, "accountName", e.target.value)
                        }
                      />
                    </Col>
                    <Col xs={24} md={4}>
                      <Button
                        type="text"
                        size="small"
                        disabled={participants.length === 1}
                        onClick={() => removeParticipantRow(i)}
                        style={{ color: "var(--ant-color-error)" }}
                      >
                        {t("removeParticipant")}
                      </Button>
                    </Col>
                  </Row>
                ))}
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {t("equalSplitNote")}
                </Text>
              </Space>
              <Space size={16}>
                <Button
                  type="primary"
                  onClick={handleCreate}
                  disabled={createSplitBill.isPending}
                >
                  {createSplitBill.isPending ? (
                    <Loader2
                      style={{
                        marginRight: 8,
                        animation: "spin 1s linear infinite",
                      }}
                    />
                  ) : null}
                  {t("createButton")}
                </Button>
                <Button onClick={() => setShowCreateModal(false)}>{t("cancelButton")}</Button>
              </Space>
            </Space>
          </Card>
        )}

        {/* Active Split Bills */}
        {activeBills.length > 0 && (
          <Space orientation="vertical" size={24} style={{ width: "100%" }}>
            <Title level={2} style={{ margin: 0 }}>
              {t("activeBillsTitle")}
            </Title>
            <Space orientation="vertical" size={16} style={{ width: "100%" }}>
              {activeBills.map((bill) => (
                <Card key={bill.id}>
                  <Row justify="space-between" align="middle" gutter={[16, 16]}>
                    <Col>
                      <Space size={16}>
                        <div
                          style={{
                            width: 48,
                            height: 48,
                            backgroundColor: "var(--ant-color-primary-bg)",
                            borderRadius: 24,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                          }}
                        >
                          <Users
                            style={{
                              width: 24,
                              height: 24,
                              color: "var(--ant-color-primary-text)",
                            }}
                          />
                        </div>
                        <div>
                          <Text strong>{bill.description}</Text>
                          <br />
                          <Text
                            type="secondary"
                            style={{
                              fontSize: 12,
                              textTransform: "uppercase",
                              letterSpacing: "0.1em",
                            }}
                          >
                            {new Date(bill.createdAt).toLocaleDateString(
                              undefined,
                            )}
                          </Text>
                        </div>
                      </Space>
                    </Col>
                    <Col>
                      <Space size={12} align="center">
                        {getStatusBadge(bill.status)}
                        <Text strong style={{ fontSize: 18 }}>
                          {formatCurrency(bill.totalAmount)}
                        </Text>
                      </Space>
                    </Col>
                  </Row>

                  {/* Participants */}
                  {bill.participants?.length > 0 && (
                    <Space
                      orientation="vertical"
                      size={8}
                      style={{ width: "100%", marginTop: 16 }}
                    >
                      {bill.participants.map((p) => (
                        <Row
                          key={p.id}
                          justify="space-between"
                          align="middle"
                          style={{
                            padding: "12px 16px",
                            backgroundColor: "var(--ant-color-fill-tertiary)",
                            borderRadius: 24,
                          }}
                        >
                          <Space size={12}>
                            <div
                              style={{
                                width: 32,
                                height: 32,
                                backgroundColor: "var(--ant-color-primary-bg)",
                                borderRadius: "50%",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                fontSize: 12,
                                fontWeight: 600,
                                color: "var(--ant-color-primary-text)",
                              }}
                            >
                              {p.name?.charAt(0) ?? "?"}
                            </div>
                            <Text>{p.name}</Text>
                          </Space>
                          <Space size={12} align="center">
                            <Text strong>{formatCurrency(p.amount)}</Text>
                            {p.status === "PAID" ? (
                              <CheckCircle2
                                style={{
                                  width: 16,
                                  height: 16,
                                  color: "var(--ant-color-success-text)",
                                }}
                              />
                            ) : p.status === "DECLINED" ? (
                              <XCircle
                                style={{
                                  width: 16,
                                  height: 16,
                                  color: "var(--ant-color-error)",
                                }}
                              />
                            ) : (
                              <Clock
                                style={{
                                  width: 16,
                                  height: 16,
                                  color: "var(--ant-color-warning)",
                                }}
                              />
                            )}
                          </Space>
                        </Row>
                      ))}
                    </Space>
                  )}

                  {/* Actions */}
                  <Space size={8} style={{ marginTop: 16 }}>
                    <Button
                      size="small"
                      onClick={() =>
                        addParticipant.mutate({
                          id: bill.id,
                          participant: {
                            accountId: "",
                            accountName: t("defaultParticipantName"),
                            amountOwed: divideCurrency(bill.totalAmount, 2),
                            status: "PENDING",
                          } as SplitBillParticipant,
                        })
                      }
                    >
                      <UserPlus style={{ marginRight: 4 }} /> {t("addParticipant")}
                    </Button>
                    {bill.status === "PENDING" && (
                      <Button
                        size="small"
                        type="primary"
                        onClick={() => activateBill.mutate(bill.id)}
                      >
                        {t("activateButton")}
                      </Button>
                    )}
                    <Button
                      size="small"
                      onClick={() => settleBill.mutate(bill.id)}
                    >
                      {t("settleButton")}
                    </Button>
                  </Space>
                </Card>
              ))}
            </Space>
          </Space>
        )}

        {/* Settled Bills */}
        {settledBills.length > 0 && (
          <Space orientation="vertical" size={24} style={{ width: "100%" }}>
            <Title level={2} style={{ margin: 0 }}>
              {t("historyTitle")}
            </Title>
            <Space orientation="vertical" size={16} style={{ width: "100%" }}>
              {settledBills.map((bill) => (
                <Card key={bill.id} style={{ opacity: 0.8 }}>
                  <Row justify="space-between" align="middle" gutter={[16, 16]}>
                    <Col>
                      <Space size={16}>
                        <div
                          style={{
                            width: 48,
                            height: 48,
                            backgroundColor: "var(--ant-color-fill-tertiary)",
                            borderRadius: 24,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                          }}
                        >
                          <CheckCircle2
                            style={{
                              width: 24,
                              height: 24,
                              color: "var(--ant-color-text-tertiary)",
                            }}
                          />
                        </div>
                        <div>
                          <Text strong>{bill.description}</Text>
                          <br />
                          <Text
                            type="secondary"
                            style={{
                              fontSize: 12,
                              textTransform: "uppercase",
                              letterSpacing: "0.1em",
                            }}
                          >
                            {t("participantCount", {
                              count: bill.participants?.length ?? 0,
                            })}
                          </Text>
                        </div>
                      </Space>
                    </Col>
                    <Col>
                      <Text
                        strong
                        style={{
                          fontSize: 18,
                          color: "var(--ant-color-text-tertiary)",
                        }}
                      >
                        {formatCurrency(bill.totalAmount)}
                      </Text>
                    </Col>
                  </Row>
                </Card>
              ))}
            </Space>
          </Space>
        )}

        {/* Empty State */}
        {!isLoading && splitBills.length === 0 && (
          <Card>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Space orientation="vertical" size={8}>
                  <Text strong style={{ fontSize: 16 }}>
                    {t("emptyTitle")}
                  </Text>
                  <Text type="secondary">
                    {t("emptyDescription")}
                  </Text>
                </Space>
              }
            >
              <Button type="primary" onClick={() => setShowCreateModal(true)}>
                <Plus style={{ marginRight: 8 }} /> {t("startSplitBill")}
              </Button>
            </Empty>
          </Card>
        )}

        {isLoading && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: 32,
            }}
          >
            <Loader2
              style={{
                width: 32,
                height: 32,
                animation: "spin 1s linear infinite",
                color: "var(--ant-color-primary-text)",
              }}
            />
          </div>
        )}
      </Space>
    </DashboardLayout>
  );
}
