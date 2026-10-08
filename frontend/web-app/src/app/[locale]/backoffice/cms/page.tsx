"use client";

import React, { useState } from "react";
import type { CSSProperties } from "react";
import Image from "next/image";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Plus,
  Search,
  MoreHorizontal,
  Edit,
  Trash2,
  Eye,
  CheckCircle2,
  Clock,
  AlertCircle,
  Image as ImageIcon,
  ExternalLink,
  FileText,
  Gift,
} from "@/components/icons";
import {
  Button,
  Card,
  Divider,
  Dropdown,
  Flex,
  Input,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  InputNumber,
} from "antd";
import type { MenuProps, TableColumnsType } from "antd";
import type { Content } from "@/services";
import { useActiveContent } from "@/hooks/useCMS";
import { notify as toast } from "@/lib/notify";
import { MutationPresets } from "@/lib/mutation-config";
import CMSService from "@/services/CMSService";
import StatCards from "../_components/StatCards";

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

type ContentTypeOption = "BANNER" | "PROMO" | "ALERT" | "POPUP";
type ActionTypeOption = "LINK" | "DEEP_LINK" | "DISMISS";
type ContentStatus = "ACTIVE" | "PAUSED" | "ARCHIVED";

interface ContentFormState {
  title: string;
  contentType: ContentTypeOption;
  description: string;
  imageUrl: string;
  actionUrl: string;
  actionType: ActionTypeOption;
  startDate: string;
  endDate: string;
  priority: number;
}

const emptyForm: ContentFormState = {
  title: "",
  contentType: "BANNER",
  description: "",
  imageUrl: "",
  actionUrl: "",
  actionType: "LINK",
  startDate: "",
  endDate: "",
  priority: 5,
};

function formToPayload(form: ContentFormState) {
  return {
    title: form.title,
    contentType: form.contentType,
    description: form.description,
    imageUrl: form.imageUrl || undefined,
    actionUrl: form.actionUrl || undefined,
    actionType: form.actionType,
    startDate: form.startDate
      ? new Date(form.startDate).toISOString()
      : undefined,
    endDate: form.endDate ? new Date(form.endDate).toISOString() : undefined,
    priority: form.priority,
  };
}

function contentToForm(content: Content): ContentFormState {
  return {
    title: content.title,
    contentType: (content.contentType as ContentTypeOption) || "BANNER",
    description: content.description,
    imageUrl: content.imageUrl,
    actionUrl: content.actionUrl,
    actionType: (content.actionType as ActionTypeOption) || "LINK",
    startDate: content.startDate
      ? new Date(content.startDate).toISOString().split("T")[0]
      : "",
    endDate: content.endDate
      ? new Date(content.endDate).toISOString().split("T")[0]
      : "",
    priority: content.priority,
  };
}

interface ContentFormModalProps {
  open: boolean;
  isEditing: boolean;
  loading: boolean;
  form: ContentFormState;
  onFormChange: (form: ContentFormState) => void;
  onSubmit: () => void;
  onCancel: () => void;
}

function ContentFormModal({
  open,
  isEditing,
  loading,
  form,
  onFormChange,
  onSubmit,
  onCancel,
}: ContentFormModalProps) {
  const update = <K extends keyof ContentFormState>(
    key: K,
    value: ContentFormState[K],
  ) => onFormChange({ ...form, [key]: value });

  return (
    <Modal
      open={open}
      title={isEditing ? "Edit Content" : "New Content"}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={isEditing ? "Save" : "Create"}
      confirmLoading={loading}
      width={560}
    >
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Title
          </Typography.Text>
          <Input
            value={form.title}
            onChange={(e) => update("title", e.target.value)}
            placeholder="Content title"
          />
        </div>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Type
          </Typography.Text>
          <Select
            value={form.contentType}
            onChange={(v) => update("contentType", v)}
            style={{ width: "100%" }}
            options={["BANNER", "PROMO", "ALERT", "POPUP"].map((t) => ({
              value: t,
              label: t,
            }))}
          />
        </div>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Description
          </Typography.Text>
          <Input.TextArea
            value={form.description}
            onChange={(e) => update("description", e.target.value)}
            rows={3}
            placeholder="Content description"
          />
        </div>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Image URL
          </Typography.Text>
          <Input
            value={form.imageUrl}
            onChange={(e) => update("imageUrl", e.target.value)}
            placeholder="https://..."
          />
        </div>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Action URL
          </Typography.Text>
          <Input
            value={form.actionUrl}
            onChange={(e) => update("actionUrl", e.target.value)}
            placeholder="https://..."
          />
        </div>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Action Type
          </Typography.Text>
          <Select
            value={form.actionType}
            onChange={(v) => update("actionType", v)}
            style={{ width: "100%" }}
            options={["LINK", "DEEP_LINK", "DISMISS"].map((t) => ({
              value: t,
              label: t,
            }))}
          />
        </div>
        <Flex gap={16}>
          <div style={{ flex: 1 }}>
            <Typography.Text
              strong
              style={{ fontSize: 12, display: "block", marginBottom: 4 }}
            >
              Start Date
            </Typography.Text>
            <Input
              type="date"
              value={form.startDate}
              onChange={(e) => update("startDate", e.target.value)}
              style={{ width: "100%" }}
            />
          </div>
          <div style={{ flex: 1 }}>
            <Typography.Text
              strong
              style={{ fontSize: 12, display: "block", marginBottom: 4 }}
            >
              End Date
            </Typography.Text>
            <Input
              type="date"
              value={form.endDate}
              onChange={(e) => update("endDate", e.target.value)}
              style={{ width: "100%" }}
            />
          </div>
        </Flex>
        <div>
          <Typography.Text
            strong
            style={{ fontSize: 12, display: "block", marginBottom: 4 }}
          >
            Priority (1-10)
          </Typography.Text>
          <InputNumber
            value={form.priority}
            onChange={(v) => update("priority", v ?? 1)}
            min={1}
            max={10}
            style={{ width: "100%" }}
          />
        </div>
      </Space>
    </Modal>
  );
}

export default function CMSPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [activeTab, setActiveTab] = useState<string>("ALL");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingContent, setEditingContent] = useState<Content | null>(null);
  const [form, setForm] = useState<ContentFormState>(emptyForm);

  const queryClient = useQueryClient();

  const {
    data: banners,
    isLoading: bannersLoading,
    error: bannersError,
  } = useActiveContent("BANNER");
  const { data: promos, isLoading: promosLoading } = useActiveContent("PROMO");
  const { data: alerts, isLoading: alertsLoading } = useActiveContent("ALERT");
  const { data: popups, isLoading: popupsLoading } = useActiveContent("POPUP");
  const isLoading =
    bannersLoading || promosLoading || alertsLoading || popupsLoading;
  const error = bannersError;
  const allContent = [
    ...(banners ?? []),
    ...(promos ?? []),
    ...(alerts ?? []),
    ...(popups ?? []),
  ];
  const filteredContent = allContent.filter((content) => {
    const matchesSearch =
      content.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      content.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesTab = activeTab === "ALL" || content.contentType === activeTab;
    return matchesSearch && matchesTab;
  });

  const createMutation = useMutation({
    mutationFn: () => CMSService.createContent(formToPayload(form)),
    onSuccess: () => {
      toast.success("Content created successfully");
      queryClient.invalidateQueries({ queryKey: ["cms-content"] });
      setIsModalOpen(false);
    },
    onError: () => toast.error("Failed to create content"),
    ...MutationPresets.nonFinancial,
  });

  const updateMutation = useMutation({
    mutationFn: () => {
      if (!editingContent)
        return Promise.reject(new Error("No content to update"));
      return CMSService.updateContent(editingContent.id, formToPayload(form));
    },
    onSuccess: () => {
      toast.success("Content updated successfully");
      queryClient.invalidateQueries({ queryKey: ["cms-content"] });
      setIsModalOpen(false);
    },
    onError: () => toast.error("Failed to update content"),
    ...MutationPresets.nonFinancial,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => CMSService.deleteContent(id),
    onSuccess: () => {
      toast.success("Content deleted successfully");
      queryClient.invalidateQueries({ queryKey: ["cms-content"] });
    },
    onError: () => toast.error("Failed to delete content"),
    ...MutationPresets.nonFinancial,
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ContentStatus }) =>
      CMSService.updateContentStatus(id, status),
    onSuccess: () => {
      toast.success("Content status updated");
      queryClient.invalidateQueries({ queryKey: ["cms-content"] });
    },
    onError: () => toast.error("Failed to update content status"),
    ...MutationPresets.nonFinancial,
  });

  const openCreateModal = () => {
    setEditingContent(null);
    setForm(emptyForm);
    setIsModalOpen(true);
  };

  const openEditModal = (content: Content) => {
    setEditingContent(content);
    setForm(contentToForm(content));
    setIsModalOpen(true);
  };

  const handleDelete = (content: Content) => {
    Modal.confirm({
      title: "Delete Content",
      content: `Are you sure you want to delete "${content.title}"?`,
      okText: "Delete",
      okButtonProps: { danger: true },
      cancelText: "Cancel",
      onOk: () => deleteMutation.mutate(content.id),
    });
  };

  const handleStatusChange = (content: Content, status: ContentStatus) => {
    statusMutation.mutate({ id: content.id, status });
  };

  const handleViewDetail = (content: Content) => {
    const url = content.imageUrl || content.actionUrl;
    if (url) window.open(url, "_blank", "noopener,noreferrer");
  };

  const handleSubmit = () => {
    if (editingContent) {
      updateMutation.mutate();
    } else {
      createMutation.mutate();
    }
  };

  const columns: TableColumnsType<Content> = [
    {
      key: "content",
      title: "Content",
      render: (_, item) => (
        <Flex align="flex-start" gap={16}>
          <div
            style={{
              width: 64,
              height: 64,
              borderRadius: 8,
              background: "var(--ant-color-fill-tertiary)",
              overflow: "hidden",
              position: "relative",
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {item.imageUrl ? (
              <Image
                src={item.imageUrl}
                alt={item.title}
                fill
                sizes="64px"
                style={{ objectFit: "cover" }}
                loading="lazy"
              />
            ) : (
              contentTypeIcon(item.contentType)
            )}
          </div>
          <Space direction="vertical" size={4}>
            <Typography.Text strong style={{ fontSize: 14 }}>
              {item.title}
            </Typography.Text>
            <Typography.Paragraph
              type="secondary"
              style={{ fontSize: 12, maxWidth: 200, margin: 0 }}
              ellipsis={{ rows: 2 }}
            >
              {item.description}
            </Typography.Paragraph>
          </Space>
        </Flex>
      ),
    },
    {
      key: "type",
      title: "Type",
      render: (_, item) => (
        <Space>
          {contentTypeIcon(item.contentType)}
          <Typography.Text strong style={labelStyle}>
            {item.contentType}
          </Typography.Text>
        </Space>
      ),
    },
    {
      key: "status",
      title: "Status",
      render: (_, item) => statusBadge(item.status),
    },
    {
      key: "schedule",
      title: "Schedule",
      render: (_, item) => (
        <Space direction="vertical" size={4}>
          <Typography.Text style={{ fontSize: 12 }}>
            S: {new Date(item.startDate).toLocaleDateString()}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            E: {new Date(item.endDate).toLocaleDateString()}
          </Typography.Text>
        </Space>
      ),
    },
    {
      key: "priority",
      title: "Priority",
      render: (_, item) => (
        <Flex align="center" gap={8}>
          <Progress
            percent={item.priority * 10}
            showInfo={false}
            size="small"
            style={{ width: 48 }}
          />
          <Typography.Text strong style={{ fontSize: 12 }}>
            {item.priority}
          </Typography.Text>
        </Flex>
      ),
    },
    {
      key: "actions",
      title: "Actions",
      align: "right",
      render: (_, item) => {
        const menuItems: MenuProps["items"] = [
          {
            key: "activate",
            label: (
              <Space>
                <CheckCircle2
                  style={{ fontSize: 16, color: "var(--ant-color-primary)" }}
                />
                <Typography.Text strong style={labelStyle}>
                  Activate
                </Typography.Text>
              </Space>
            ),
            onClick: () => handleStatusChange(item, "ACTIVE"),
          },
          {
            key: "pause",
            label: (
              <Space>
                <Clock
                  style={{ fontSize: 16, color: "var(--ant-color-warning)" }}
                />
                <Typography.Text strong style={labelStyle}>
                  Pause
                </Typography.Text>
              </Space>
            ),
            onClick: () => handleStatusChange(item, "PAUSED"),
          },
          {
            key: "archive",
            label: (
              <Space>
                <Trash2
                  style={{ fontSize: 16, color: "var(--ant-color-error)" }}
                />
                <Typography.Text
                  strong
                  style={{ ...labelStyle, color: "var(--ant-color-error)" }}
                >
                  Archive
                </Typography.Text>
              </Space>
            ),
            onClick: () => handleStatusChange(item, "ARCHIVED"),
          },
        ];

        return (
          <Flex align="center" justify="flex-end" gap={8}>
            {(item.imageUrl || item.actionUrl) && (
              <Button
                type="text"
                icon={<Eye style={{ fontSize: 16 }} />}
                aria-label="Lihat detail"
                onClick={() => handleViewDetail(item)}
              />
            )}
            <Button
              type="text"
              icon={<Edit style={{ fontSize: 16 }} />}
              aria-label="Edit konten"
              onClick={() => openEditModal(item)}
            />
            <Button
              type="text"
              icon={<Trash2 style={{ fontSize: 16 }} />}
              aria-label="Hapus konten"
              onClick={() => handleDelete(item)}
            />
            <Dropdown
              trigger={["click"]}
              placement="bottomRight"
              menu={{ items: menuItems }}
            >
              <Button
                type="text"
                icon={<MoreHorizontal style={{ fontSize: 16 }} />}
                aria-label="Aksi lainnya"
              />
            </Dropdown>
          </Flex>
        );
      },
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: "100%" }}>
      <StatCards
        stats={[
          {
            label: "Total Content",
            value: String(allContent.length),
            icon: FileText,
          },
          {
            label: "Active Now",
            value: String(
              allContent.filter((c) => c.status === "ACTIVE").length,
            ),
            icon: CheckCircle2,
          },
          {
            label: "Scheduled",
            value: String(
              allContent.filter((c) => c.status === "SCHEDULED").length,
            ),
            icon: Clock,
          },
          {
            label: "Pending Review",
            value: String(
              allContent.filter((c) => c.status === "DRAFT").length,
            ),
            icon: AlertCircle,
          },
        ]}
      />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Select
            aria-label="Pilih tab konten"
            value={activeTab}
            onChange={(value: string) => setActiveTab(value)}
            style={{ minWidth: 160 }}
            options={["ALL", "BANNER", "PROMO", "ALERT", "POPUP"].map(
              (tab) => ({ value: tab, label: tab }),
            )}
          />
          <Flex gap={16} wrap style={{ flex: 1 }} justify="flex-end">
            <Input
              aria-label="Cari konten"
              placeholder="Search content..."
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
              style={{ flex: 1, minWidth: 200, maxWidth: 320 }}
            />
            <Button
              type="primary"
              icon={<Plus style={{ fontSize: 16 }} />}
              onClick={openCreateModal}
            >
              New Content
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: "hidden" } }}>
        <Table<Content>
          columns={columns}
          dataSource={filteredContent}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{
            emptyText: error ? "Failed to load content" : "No content found",
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
          <Typography.Text type="secondary" strong style={labelStyle}>
            Showing{" "}
            <Typography.Text strong>{filteredContent.length}</Typography.Text>{" "}
            results
          </Typography.Text>
        </Flex>
      </Card>

      <ContentFormModal
        open={isModalOpen}
        isEditing={!!editingContent}
        loading={createMutation.isPending || updateMutation.isPending}
        form={form}
        onFormChange={setForm}
        onSubmit={handleSubmit}
        onCancel={() => setIsModalOpen(false)}
      />
    </Space>
  );
}

function statusBadge(status: string) {
  switch (status) {
    case "ACTIVE":
      return <Tag color="green">Active</Tag>;
    case "SCHEDULED":
      return <Tag color="gold">Scheduled</Tag>;
    case "DRAFT":
      return <Tag>DRAFT</Tag>;
    case "ARCHIVED":
      return <Tag color="red">Archived</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

function contentTypeIcon(type: string) {
  switch (type) {
    case "BANNER":
      return (
        <ImageIcon
          style={{ fontSize: 16, color: "var(--ant-color-primary)" }}
        />
      );
    case "PROMO":
      return (
        <Gift style={{ fontSize: 16, color: "var(--ant-color-primary)" }} />
      );
    case "ALERT":
      return (
        <AlertCircle
          style={{ fontSize: 16, color: "var(--ant-color-error)" }}
        />
      );
    case "POPUP":
      return <ExternalLink style={{ fontSize: 16 }} />;
    default:
      return (
        <FileText
          style={{ fontSize: 16, color: "var(--ant-color-text-secondary)" }}
        />
      );
  }
}
