"use client";

import React, { useState } from "react";
import type { CSSProperties } from "react";
import {
  Store,
  Search,
  Plus,
  CheckCircle2,
  AlertCircle,
  Globe,
  ShieldCheck,
} from "@/components/icons";
import {
  Avatar,
  Button,
  Card,
  Divider,
  Flex,
  Input,
  Modal,
  Form,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { usePartners, useRegisterPartner } from "@/hooks";
import type { Partner } from "@/services";
import { notify } from "@/lib/notify";
import StatCards from "../_components/StatCards";

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

type PartnerRow = {
  id: number;
  name: string;
  type: string;
  status: "ACTIVE" | "UNDER_REVIEW";
  apiLevel: string;
  transactions: string;
  volume: string;
};

function toPartnerRow(partner: Partner): PartnerRow {
  return {
    id: partner.id,
    name: partner.name,
    type: partner.type,
    status: partner.active ? "ACTIVE" : "UNDER_REVIEW",
    apiLevel: partner.publicKey ? "SNAP BI Ready" : "Pending Setup",
    transactions: "--",
    volume: "N/A",
  };
}

export default function PartnersPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();
  const {
    data: partnersData,
    isLoading,
    isError: partnersError,
  } = usePartners();
  const registerPartner = useRegisterPartner();

  const partners = (
    Array.isArray(partnersData) ? partnersData.map(toPartnerRow) : []
  ).filter((p) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      p.name?.toLowerCase().includes(term) ||
      String(p.id ?? "")
        .toLowerCase()
        .includes(term)
    );
  });

  const activeCount = partners.filter((p) => p.status === "ACTIVE").length;
  const pendingCount = partners.filter(
    (p) => p.status === "UNDER_REVIEW",
  ).length;

  const handleRegister = async (values: {
    name: string;
    email: string;
    type: string;
    phone: string;
  }) => {
    await registerPartner.mutateAsync(values);
    setIsModalOpen(false);
    form.resetFields();
  };

  const columns: TableColumnsType<PartnerRow> = [
    {
      key: "org",
      title: "Partner Org",
      render: (_, partner) => (
        <Flex align="center" gap={12}>
          <Avatar
            shape="square"
            size={40}
            style={{
              background: "var(--ant-color-fill-tertiary)",
              color: "var(--ant-color-text-secondary)",
            }}
            icon={<Store style={{ fontSize: 20 }} />}
          />
          <Space direction="vertical" size={0}>
            <Typography.Text strong style={{ fontSize: 14 }}>
              {partner.name}
            </Typography.Text>
            <Typography.Text type="secondary" strong style={labelStyle}>
              {partner.id}
            </Typography.Text>
          </Space>
        </Flex>
      ),
    },
    {
      key: "type",
      title: "Type",
      render: (_, partner) => (
        <Typography.Text strong style={labelStyle}>
          {partner.type}
        </Typography.Text>
      ),
    },
    {
      key: "status",
      title: "Status",
      render: (_, partner) => partnerStatusBadge(partner.status),
    },
    {
      key: "api",
      title: "API Integration",
      render: (_, partner) => (
        <Tag color="blue" icon={<ShieldCheck style={{ fontSize: 12 }} />}>
          {partner.apiLevel}
        </Tag>
      ),
    },
    {
      key: "volume",
      title: "Volume",
      render: (_, partner) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong style={{ fontSize: 12 }}>
            {partner.volume}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {partner.transactions} txns
          </Typography.Text>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: "100%" }}>
      <StatCards
        stats={[
          {
            label: "Total Partners",
            value: isLoading ? "…" : String(partners.length),
            icon: Store,
          },
          {
            label: "Active Merchants",
            value: isLoading ? "…" : String(activeCount),
            icon: CheckCircle2,
          },
          {
            label: "Pending Apps",
            value: isLoading ? "…" : String(pendingCount),
            icon: AlertCircle,
          },
          { label: "SNAP BI Volume", value: "—", icon: Globe },
        ]}
      />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Input
            aria-label="Cari mitra berdasarkan nama atau ID"
            placeholder="Search partners by name or ID..."
            prefix={
              <Search
                style={{
                  fontSize: 16,
                  color: "var(--ant-color-text-secondary)",
                }}
              />
            }
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
          />
          <Flex gap={16} wrap>
            <Button
              type="primary"
              icon={<Plus style={{ fontSize: 16 }} />}
              onClick={() => setIsModalOpen(true)}
            >
              Register New Partner
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: "hidden" } }}>
        <Table<PartnerRow>
          columns={columns}
          dataSource={partners}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{
            emptyText: partnersError
              ? "Akses ditolak: butuh role PARTNER_* atau ADMIN"
              : "No partners found",
          }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex
          align="center"
          justify="space-between"
          wrap
          gap={16}
          style={{ padding: 24 }}
        >
          <Typography.Text
            type={partnersError ? "danger" : "secondary"}
            strong={!partnersError}
            style={labelStyle}
          >
            {partnersError
              ? "Registry partner tidak tersedia"
              : "Partner Portal & SNAP BI Registry Syncing"}
          </Typography.Text>
        </Flex>
      </Card>

      <Modal
        title="Register New Partner"
        open={isModalOpen}
        onCancel={() => {
          setIsModalOpen(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        confirmLoading={registerPartner.isPending}
        okText="Register"
        cancelText="Cancel"
      >
        <Form form={form} layout="vertical" onFinish={handleRegister}>
          <Form.Item
            name="name"
            label="Partner Name"
            rules={[{ required: true, message: "Name is required" }]}
          >
            <Input placeholder="Enter partner name" />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[
              {
                required: true,
                type: "email",
                message: "Valid email is required",
              },
            ]}
          >
            <Input placeholder="Enter email address" />
          </Form.Item>
          <Form.Item
            name="type"
            label="Type"
            rules={[{ required: true, message: "Type is required" }]}
          >
            <Select
              options={[
                { value: "MERCHANT", label: "Merchant" },
                { value: "PAYMENT_GATEWAY", label: "Payment Gateway" },
                { value: "BANK", label: "Bank" },
                { value: "FINTECH", label: "Fintech" },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="phone"
            label="Phone"
            rules={[{ required: true, message: "Phone is required" }]}
          >
            <Input placeholder="Enter phone number" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

function partnerStatusBadge(status: string) {
  switch (status) {
    case "ACTIVE":
      return <Tag color="green">Active</Tag>;
    case "UNDER_REVIEW":
      return <Tag color="gold">Reviewing</Tag>;
    case "SUSPENDED":
      return <Tag color="red">Suspended</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}
