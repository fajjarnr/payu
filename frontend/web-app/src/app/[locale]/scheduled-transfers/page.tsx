'use client';

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import {
  Calendar,
  Clock,
  ArrowRightLeft,
  Pause,
  Play,
  Trash2,
  Edit3,
  Plus,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Repeat
} from '@/components/icons';
import { useLocale, useTranslations } from 'next-intl';
import { Link } from '@/lib/navigation';
import { Alert, Button, Card, Col, Empty, Input, List, Modal, Row, Select, Space, Tag, Typography } from 'antd';
import {
  useScheduledTransfers,
  useUpdateScheduledTransfer,
  useCancelScheduledTransfer,
  usePauseScheduledTransfer,
  useResumeScheduledTransfer,
} from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import type { ScheduledTransfer } from '@/services/TransactionService';
import { formatCurrency, parseCurrencyExact } from '@/lib/currency';
import { SkipLink } from "@/lib/a11y";

const { Title, Text } = Typography;

export default function ScheduledTransfersPage() {
  const t = useTranslations("scheduledTransfers");
  const { accountId: storeAccountId } = useAuthStore();
  const accountId = storeAccountId ?? '';
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';

  const { data: rawTransfers, isLoading } = useScheduledTransfers(accountId);
  const transfers = Array.isArray(rawTransfers) ? rawTransfers : [];
  const updateTransfer = useUpdateScheduledTransfer();
  const cancelTransfer = useCancelScheduledTransfer();
  const pauseTransfer = usePauseScheduledTransfer();
  const resumeTransfer = useResumeScheduledTransfer();

  const [selectedTransfer, setSelectedTransfer] = useState<ScheduledTransfer | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);

  const [editForm, setEditForm] = useState<{
    amount: string;
    description: string;
    scheduleType: 'ONE_TIME' | 'RECURRING_DAILY' | 'RECURRING_WEEKLY' | 'RECURRING_MONTHLY' | 'RECURRING_CUSTOM';
  }>({
    amount: '0.0000',
    description: '',
    scheduleType: 'ONE_TIME',
  });

  const handleOpenEditModal = (transfer: ScheduledTransfer) => {
    setSelectedTransfer(transfer);
    setEditForm({
      amount: transfer.amount,
      description: transfer.description || '',
      scheduleType: transfer.scheduleType,
    });
    setIsEditModalOpen(true);
  };

  const handleOpenCancelModal = (transfer: ScheduledTransfer) => {
    setSelectedTransfer(transfer);
    setIsCancelModalOpen(true);
  };

  const handleUpdate = async () => {
    if (!selectedTransfer) return;

    await updateTransfer.mutateAsync({
      id: selectedTransfer.id,
      data: {
        amount: parseCurrencyExact(editForm.amount),
        description: editForm.description,
        scheduleType: editForm.scheduleType,
      },
    });

    setIsEditModalOpen(false);
  };

  const handleCancel = async () => {
    if (!selectedTransfer) return;

    await cancelTransfer.mutateAsync(selectedTransfer.id);
    setIsCancelModalOpen(false);
  };

  const handlePause = async (transfer: ScheduledTransfer) => {
    await pauseTransfer.mutateAsync(transfer.id);
  };

  const handleResume = async (transfer: ScheduledTransfer) => {
    await resumeTransfer.mutateAsync(transfer.id);
  };

  const STATUS_TAG: Record<string, { color: 'green' | 'orange' | 'red' | 'default' }> = {
    ACTIVE: { color: 'green' },
    PAUSED: { color: 'orange' },
    CANCELLED: { color: 'red' },
    COMPLETED: { color: 'default' },
  };

  const getStatusBadge = (status: string) => {
    const config = STATUS_TAG[status] || STATUS_TAG.ACTIVE;
    return (
      <Tag bordered={false} color={config.color}>
        {status}
      </Tag>
    );
  };

  const getScheduleTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      ONE_TIME: t('scheduleTypeOneTime'),
      RECURRING_DAILY: t('scheduleTypeDaily'),
      RECURRING_WEEKLY: t('scheduleTypeWeekly'),
      RECURRING_MONTHLY: t('scheduleTypeMonthly'),
      RECURRING_CUSTOM: t('scheduleTypeCustom'),
    };
    return labels[type] || type;
  };

  return (
    <DashboardLayout>
      <SkipLink />
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {/* Header */}
        <Row justify="space-between" align="bottom" gutter={[16, 16]}>
          <Col>
            <Title level={1} style={{ margin: 0 }}>{t("title")}</Title>
            <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
              {t("subtitle")}
            </Text>
          </Col>
          <Col>
            <Link href="/transfer">
              <Button type="primary">
                <Plus style={{ marginRight: 8 }} /> {t('newTransfer')}
              </Button>
            </Link>
          </Col>
        </Row>

        {/* Stats Cards */}
        <Row gutter={[16, 16]}>
          {[
            {
              label: t('statTotalTransfer'),
              value: transfers?.length || 0,
              icon: ArrowRightLeft,
              color: 'var(--ant-color-primary-bg)',
              iconColor: 'var(--ant-color-primary)',
            },
            {
              label: t('statActive'),
              value: transfers?.filter((t) => t.status === 'ACTIVE').length || 0,
              icon: CheckCircle2,
              color: 'var(--ant-color-primary-bg)',
              iconColor: 'var(--ant-color-primary)',
            },
            {
              label: t('statPaused'),
              value: transfers?.filter((t) => t.status === 'PAUSED').length || 0,
              icon: Pause,
              color: 'var(--ant-color-warning-bg)',
              iconColor: 'var(--ant-color-warning)',
            },
            {
              label: t('statCompleted'),
              value: transfers?.filter((t) => t.status === 'COMPLETED').length || 0,
              icon: Calendar,
              color: 'var(--ant-color-fill-tertiary)',
              iconColor: 'var(--ant-color-text-tertiary)',
            },
          ].map((stat, i) => (
            <Col xs={24} sm={12} lg={6} key={i}>
              <Card>
                <Row justify="space-between" align="middle">
                  <div>
                    <Text type="secondary" strong style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>{stat.label}</Text>
                    <Title level={2} style={{ margin: '4px 0 0' }}>{stat.value}</Title>
                  </div>
                  <div style={{ width: 48, height: 48, backgroundColor: stat.color, borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <stat.icon style={{ width: 24, height: 24, color: stat.iconColor }} />
                  </div>
                </Row>
              </Card>
            </Col>
          ))}
        </Row>

        {/* Transfers List */}
        <Card>
          <Title level={2} style={{ margin: '0 0 16px' }}>{t('listTitle')}</Title>

          {isLoading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
              <Loader2 style={{ width: 32, height: 32, animation: 'spin 1s linear infinite', color: 'var(--ant-color-primary)' }} />
            </div>
          ) : !transfers || transfers.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Space direction="vertical" size={8}>
                  <Text type="secondary">{t('emptyTitle')}</Text>
                  <Link href="/transfer">
                    <Button type="primary" size="small">{t('emptyAction')}</Button>
                  </Link>
                </Space>
              }
            />
          ) : (
            <List
              dataSource={transfers}
              renderItem={(transfer) => (
                <List.Item
                  key={transfer.id}
                  style={{ padding: '16px 0' }}
                  actions={[
                    transfer.status === 'ACTIVE' && (
                      <Button
                        size="small"
                        onClick={() => handlePause(transfer)}
                        disabled={pauseTransfer.isPending}
                        aria-label={t('ariaPause')}
                      >
                        {pauseTransfer.isPending ? (
                          <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
                        ) : (
                          <Pause style={{ width: 16, height: 16 }} />
                        )}
                      </Button>
                    ),
                    transfer.status === 'ACTIVE' && (
                      <Button
                        size="small"
                        onClick={() => handleOpenEditModal(transfer)}
                        disabled={updateTransfer.isPending}
                        aria-label={t('ariaEdit')}
                      >
                        <Edit3 style={{ width: 16, height: 16 }} />
                      </Button>
                    ),
                    transfer.status === 'PAUSED' && (
                      <Button
                        size="small"
                        onClick={() => handleResume(transfer)}
                        disabled={resumeTransfer.isPending}
                        aria-label={t('ariaResume')}
                      >
                        {resumeTransfer.isPending ? (
                          <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
                        ) : (
                          <Play style={{ width: 16, height: 16 }} />
                        )}
                      </Button>
                    ),
                    (transfer.status === 'ACTIVE' || transfer.status === 'PAUSED') && (
                      <Button
                        danger
                        size="small"
                        onClick={() => handleOpenCancelModal(transfer)}
                        disabled={cancelTransfer.isPending}
                        aria-label={t('ariaDelete')}
                      >
                        {cancelTransfer.isPending ? (
                          <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
                        ) : (
                          <Trash2 style={{ width: 16, height: 16 }} />
                        )}
                      </Button>
                    ),
                  ].filter(Boolean)}
                >
                  <List.Item.Meta
                    avatar={
                      <div style={{ width: 48, height: 48, backgroundColor: 'var(--ant-color-primary-bg)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Repeat style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                      </div>
                    }
                    title={
                      <Space size={8}>
                        <Text strong>{transfer.description || t('defaultDescription')}</Text>
                        {getStatusBadge(transfer.status)}
                      </Space>
                    }
                    description={
                      <Space size={16} style={{ marginTop: 4 }}>
                        <Space size={4}>
                          <ArrowRightLeft style={{ width: 12, height: 12 }} />
                          <Text type="secondary" style={{ fontVariantNumeric: "tabular-nums" }}>{formatCurrency(transfer.amount, { locale: bcp47Locale })}</Text>
                        </Space>
                        <Space size={4}>
                          <Calendar style={{ width: 12, height: 12 }} />
                          <Text type="secondary">{getScheduleTypeLabel(transfer.scheduleType)}</Text>
                        </Space>
                        <Space size={4}>
                          <Clock style={{ width: 12, height: 12 }} />
                          <Text type="secondary">{new Date(transfer.nextExecutionDate).toLocaleDateString(bcp47Locale)}</Text>
                        </Space>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Card>
      </Space>

      {/* Edit Modal */}
      <Modal open={isEditModalOpen} onCancel={() => setIsEditModalOpen(false)} footer={null} centered width={512} title={<Title level={2} style={{ marginBottom: 4 }}>{t('editModalTitle')}</Title>}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Text type="secondary">
            {t('editModalDescription')}
          </Text>

          {updateTransfer.isError && (
            <Alert
              type="error"
              showIcon
              icon={<AlertCircle style={{ width: 16, height: 16, color: 'var(--ant-color-error)' }} />}
              description={<Text type="danger">{t('editError')}</Text>}
            />
          )}

          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <label htmlFor="scheduled-amount" style={{ fontSize: 14, fontWeight: 500 }}>{t('amountLabel')}</label>
              <Input id="scheduled-amount"
                type="number"
                step="any"
                value={editForm.amount}
                onChange={(e) => setEditForm((prev) => ({ ...prev, amount: e.target.value }))}
                placeholder="100000"
              />
            </Space>

            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <label htmlFor="scheduled-description" style={{ fontSize: 14, fontWeight: 500 }}>{t('descriptionLabel')}</label>
              <Input id="scheduled-description"
                type="text"
                value={editForm.description}
                onChange={(e) => setEditForm((prev) => ({ ...prev, description: e.target.value }))}
                placeholder={t('descriptionPlaceholder')}
              />
            </Space>

            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <label htmlFor="scheduled-type" style={{ fontSize: 14, fontWeight: 500 }}>{t('scheduleTypeLabel')}</label>
              <Select id="scheduled-type"
                style={{ width: '100%' }}
                value={editForm.scheduleType}
                onChange={(value: string) => setEditForm((prev) => ({ ...prev, scheduleType: value as typeof prev.scheduleType }))}
                options={[
                  { value: 'ONE_TIME', label: t('scheduleTypeOneTime') },
                  { value: 'RECURRING_DAILY', label: t('scheduleTypeDaily') },
                  { value: 'RECURRING_WEEKLY', label: t('scheduleTypeWeekly') },
                  { value: 'RECURRING_MONTHLY', label: t('scheduleTypeMonthly') },
                ]}
              />
            </Space>
          </Space>

          <Row justify="end" gutter={8}>
            <Col>
              <Button onClick={() => setIsEditModalOpen(false)}>
                {t('cancelButton')}
              </Button>
            </Col>
            <Col>
              <Button type="primary" onClick={handleUpdate} disabled={updateTransfer.isPending}>
                {updateTransfer.isPending ? (
                  <Loader2 style={{ width: 16, height: 16, marginRight: 8, animation: 'spin 1s linear infinite' }} />
                ) : null}
                {t('saveChanges')}
              </Button>
            </Col>
          </Row>
        </Space>
      </Modal>

      {/* Cancel Confirmation Modal */}
      <Modal open={isCancelModalOpen} onCancel={() => setIsCancelModalOpen(false)} footer={null} centered width={512} title={<Title level={2} style={{ marginBottom: 4, color: 'var(--ant-color-error)' }}>{t('cancelModalTitle')}</Title>}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Text type="secondary">
            {t('cancelModalDescription')}
          </Text>

          {cancelTransfer.isError && (
            <Alert
              type="error"
              showIcon
              icon={<AlertCircle style={{ width: 16, height: 16, color: 'var(--ant-color-error)' }} />}
              description={<Text type="danger">{t('cancelError')}</Text>}
            />
          )}

          <Row justify="end" gutter={8}>
            <Col>
              <Button onClick={() => setIsCancelModalOpen(false)}>
                {t('cancelButton')}
              </Button>
            </Col>
            <Col>
              <Button
                danger
                type="primary"
                onClick={handleCancel}
                disabled={cancelTransfer.isPending}
              >
                {cancelTransfer.isPending ? (
                  <Loader2 style={{ width: 16, height: 16, marginRight: 8, animation: 'spin 1s linear infinite' }} />
                ) : (
                  <Trash2 style={{ width: 16, height: 16, marginRight: 8 }} />
                )}
                {t('cancelConfirm')}
              </Button>
            </Col>
          </Row>
        </Space>
      </Modal>
    </DashboardLayout>
  );
}
