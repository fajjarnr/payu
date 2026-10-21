'use client';

import React, { useState } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuthStore } from '@/stores';
import { useTransactions, useCancelTransaction } from '@/hooks';
import {
  ArrowLeftRight,
  ArrowUpRight,
  ArrowDownLeft,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  MoreHorizontal,
  X,
  RotateCcw,
  Filter
} from '@/components/icons';
import { Button, Card, Col, Divider, Dropdown, Modal, Row, Skeleton, Space, Table, Tag, Typography, theme } from 'antd';
import type { TableColumnsType } from 'antd';
import { notify as toast } from '@/lib/notify';
import clsx from 'clsx';
import { addCurrency, formatCurrency } from '@/lib/currency';
import type { Transaction, TransactionFilters, TransactionStatus, TransactionType } from '@/types';
import { SkipLink } from "@/lib/a11y";

const statusConfig: Record<string, { labelKey: string; color: string; icon: typeof Clock }> = {
  PENDING: { labelKey: 'statusPending', color: 'warning', icon: Clock },
  VALIDATING: { labelKey: 'statusValidating', color: 'warning', icon: Clock },
  PROCESSING: { labelKey: 'statusProcessing', color: 'processing', icon: RotateCcw },
  COMPLETED: { labelKey: 'statusCompleted', color: 'success', icon: CheckCircle2 },
  FAILED: { labelKey: 'statusFailed', color: 'error', icon: XCircle },
  CANCELLED: { labelKey: 'statusCancelled', color: 'default', icon: X },
  // RELAY-014: enum gained these (ADR-0028/0030) — missing entries crashed the
  // whole page (reading 'icon' of undefined) on any held/step-up row.
  PENDING_COMPLIANCE_REVIEW: { labelKey: 'statusComplianceReview', color: 'warning', icon: AlertCircle },
  PENDING_STEP_UP: { labelKey: 'statusStepUp', color: 'warning', icon: AlertCircle },
};

const typeConfig: Record<string, { labelKey: string; icon: typeof ArrowLeftRight }> = {
  INTERNAL_TRANSFER: { labelKey: 'typeTransfer', icon: ArrowLeftRight },
  BIFAST_TRANSFER: { labelKey: 'typeBiFast', icon: ArrowUpRight },
  SKN_TRANSFER: { labelKey: 'typeSkn', icon: ArrowUpRight },
  RTGS_TRANSFER: { labelKey: 'typeRtgs', icon: ArrowUpRight },
  QRIS_PAYMENT: { labelKey: 'typeQris', icon: ArrowUpRight },
  BILL_PAYMENT: { labelKey: 'typeBillPayment', icon: ArrowUpRight },
  TOP_UP: { labelKey: 'typeTopUp', icon: ArrowDownLeft },
};

// Helper to check if transaction is a credit (income) for current account
const isCreditType = (type: string, t?: Transaction, currentAccountId?: string | null): boolean => {
  if (type === 'TOP_UP') return true;
  if (t && currentAccountId && t.recipientAccountId === currentAccountId) return true;
  return false;
};

export default function TransactionsPage() {
  const t = useTranslations('transactions');
  const { token } = theme.useToken();
  const accountId = useAuthStore((state) => state.accountId);
  const [page, setPage] = useState(0);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [isCancelDialogOpen, setIsCancelDialogOpen] = useState(false);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';

  const { data: transactions, isLoading } = useTransactions(accountId || undefined, page, 20, filters);
  const cancelTransaction = useCancelTransaction();

  // Compute stats from actual transaction data
  const totalIn = transactions?.filter((t: Transaction) => isCreditType(t.type, t, accountId)).reduce((sum: string, t: Transaction) => addCurrency(sum, t.amount), '0') ?? '0';
  const totalOut = transactions?.filter((t: Transaction) => !isCreditType(t.type, t, accountId)).reduce((sum: string, t: Transaction) => addCurrency(sum, t.amount), '0') ?? '0';
  const pendingCount = transactions?.filter((t: Transaction) => t.status === 'PENDING' || t.status === 'PROCESSING').length ?? 0;
  const completedCount = transactions?.filter((t: Transaction) => t.status === 'COMPLETED').length ?? 0;

  const handleCancelClick = (transaction: Transaction) => {
    setSelectedTransaction(transaction);
    setIsCancelDialogOpen(true);
  };

  const handleConfirmCancel = async () => {
    if (!selectedTransaction) return;

    try {
      await cancelTransaction.mutateAsync(selectedTransaction.id);
      toast.success(t('cancelSuccess'));
      setIsCancelDialogOpen(false);
      setSelectedTransaction(null);
    } catch {
      toast.error(t('cancelError'));
    }
  };

  const formatAmount = (amount: string, currency: string) => {
    return formatCurrency(amount, { symbol: currency || 'Rp', locale: bcp47Locale });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(bcp47Locale, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const canCancel = (status: string) => {
    return status === 'PENDING' || status === 'PROCESSING';
  };

  const columns: TableColumnsType<Transaction> = [
    {
      key: 'date',
      title: t('date'),
      render: (_, transaction) => (
        <div>
          <Typography.Text strong>{formatDate(transaction.createdAt)}</Typography.Text>
          <div>
            <Typography.Text type="secondary" className="font-mono text-xs">{transaction.referenceNumber}</Typography.Text>
          </div>
        </div>
      ),
    },
    {
      key: 'type',
      title: t('type'),
      render: (_, transaction) => {
        const type = typeConfig[transaction.type] || typeConfig.INTERNAL_TRANSFER;
        const TypeIcon = type.icon;
        const credit = isCreditType(transaction.type, transaction, accountId);
        return (
          <Space size={12}>
            <div style={{
              width: 40,
              height: 40,
              borderRadius: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid',
              ...(credit
                ? { backgroundColor: token.colorPrimaryBg, borderColor: token.colorPrimaryBorder }
                : { backgroundColor: token.colorFillTertiary, borderColor: token.colorBorderSecondary }
              )
            }}>
              <TypeIcon style={{ width: 20, height: 20, color: credit ? token.colorPrimary : token.colorTextTertiary }} />
            </div>
            <Typography.Text strong>{t(type.labelKey)}</Typography.Text>
          </Space>
        );
      },
    },
    {
      key: 'description',
      title: t('description'),
      render: (_, transaction) => (
        <Typography.Text ellipsis={{ tooltip: transaction.description }} style={{ maxWidth: 200 }}>
          {transaction.description}
        </Typography.Text>
      ),
    },
    {
      key: 'status',
      title: t('status'),
      align: 'center',
      render: (_, transaction) => {
        const status = statusConfig[transaction.status];
        const StatusIcon = status.icon;
        return (
          <Tag bordered={false} color={status.color} className="font-bold text-xs">
            <StatusIcon style={{ width: 12, height: 12, marginRight: 4 }} />
            {t(status.labelKey)}
          </Tag>
        );
      },
    },
    {
      key: 'amount',
      title: t('amount'),
      align: 'right',
      render: (_, transaction) => {
        const credit = isCreditType(transaction.type, transaction, accountId);
        return (
          <Typography.Text strong className="tabular-nums" type={credit ? undefined : 'secondary'}>
            {credit ? '+' : '-'}{formatAmount(transaction.amount, transaction.currency)}
          </Typography.Text>
        );
      },
    },
    {
      key: 'actions',
      title: '',
      align: 'right',
      render: (_, transaction) => (
        <Dropdown
          trigger={['click']}
          placement="bottomRight"
          menu={{
            items: [
              { key: 'detail', label: t('viewDetails') },
              ...(canCancel(transaction.status)
                ? [{ key: 'cancel', danger: true, label: t('cancelTransaction'), onClick: () => handleCancelClick(transaction) }]
                : []),
            ],
          }}
        >
          <Button type="text" shape="circle" style={{ minWidth: 44, minHeight: 44, borderRadius: 12 }} icon={<MoreHorizontal style={{ width: 16, height: 16 }} />} aria-label={t('transactionOptionsAria')} />
        </Dropdown>
      ),
    },
  ];

  return (
    <DashboardLayout>
      <SkipLink />
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        {/* Header */}
        <Row gutter={[16, 16]} align="middle" justify="space-between">
          <Col>
            <Space direction="vertical" size={4}>
              <Typography.Title level={1} style={{ margin: 0 }}>{t('transactionHistory')}</Typography.Title>
              <Typography.Text type="secondary">{t('pageSubtitle')}</Typography.Text>
            </Space>
          </Col>
          <Col>
            <Space size={12}>
              {(filters.status || filters.type) && (
                <Button
                  type="text"
                  size="small"
                  onClick={() => setFilters({})}
                  className="text-xs font-bold"
                >
                  {t('clearFilters')}
                </Button>
              )}
              <Dropdown
                trigger={['click']}
                placement="bottomRight"
                menu={{
                  selectedKeys: filters.status ? [filters.status] : [],
                  items: [
                    { key: 'all', label: t('allStatuses'), onClick: () => setFilters({ ...filters, status: undefined }) },
                    ...Object.keys(statusConfig).map((status) => ({
                      key: status,
                      label: t(statusConfig[status].labelKey),
                      onClick: () => setFilters({ ...filters, status: status as TransactionStatus }),
                    })),
                  ],
                }}
              >
                <Button>
                  <Space size={8}>
                    <Filter style={{ width: 16, height: 16 }} />
                    {filters.status ? t(statusConfig[filters.status].labelKey) : t('status')}
                  </Space>
                </Button>
              </Dropdown>

              <Dropdown
                trigger={['click']}
                placement="bottomRight"
                menu={{
                  selectedKeys: filters.type ? [filters.type] : [],
                  items: [
                    { key: 'all', label: t('allTypes'), onClick: () => setFilters({ ...filters, type: undefined }) },
                    ...Object.keys(typeConfig).map((type) => ({
                      key: type,
                      label: t(typeConfig[type].labelKey),
                      onClick: () => setFilters({ ...filters, type: type as TransactionType }),
                    })),
                  ],
                }}
              >
                <Button>
                  <Space size={8}>
                    <ArrowLeftRight style={{ width: 16, height: 16 }} />
                    {filters.type ? t(typeConfig[filters.type]?.labelKey ?? 'typeTransfer') : t('type')}
                  </Space>
                </Button>
              </Dropdown>
            </Space>
          </Col>
        </Row>

        {/* Stats Cards */}
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Space size={16}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  backgroundColor: token.colorPrimaryBg,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: `1px solid ${token.colorPrimaryBorder}`
                }}>
                  <ArrowDownLeft style={{ width: 24, height: 24, color: token.colorPrimary }} />
                </div>
                <div>
                  <Typography.Text strong type="secondary" className="text-xs tracking-widest uppercase">{t('totalIn')}</Typography.Text>
                  <Typography.Title level={2} style={{ margin: 0, fontVariantNumeric: "tabular-nums" }}>
                    {isLoading ? <Skeleton.Input active size="small" style={{ height: 28, width: 96 }} /> : formatAmount(totalIn, 'IDR')}
                  </Typography.Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Space size={16}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  backgroundColor: token.colorErrorBg,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <ArrowUpRight style={{ width: 24, height: 24, color: token.colorError }} />
                </div>
                <div>
                  <Typography.Text strong type="secondary" className="text-xs tracking-widest uppercase">{t('totalOut')}</Typography.Text>
                  <Typography.Title level={2} style={{ margin: 0, fontVariantNumeric: "tabular-nums" }}>
                    {isLoading ? <Skeleton.Input active size="small" style={{ height: 28, width: 96 }} /> : formatAmount(totalOut, 'IDR')}
                  </Typography.Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Space size={16}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  backgroundColor: token.colorPrimaryBg,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <Clock style={{ width: 24, height: 24, color: token.colorPrimary }} />
                </div>
                <div>
                  <Typography.Text strong type="secondary" className="text-xs tracking-widest uppercase">{t('statusPending')}</Typography.Text>
                  <Typography.Title level={2} style={{ margin: 0 }}>
                    {isLoading ? <Skeleton.Input active size="small" style={{ height: 28, width: 96 }} /> : String(pendingCount)}
                  </Typography.Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Space size={16}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  backgroundColor: token.colorInfoBg,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <CheckCircle2 style={{ width: 24, height: 24, color: token.colorInfo }} />
                </div>
                <div>
                  <Typography.Text strong type="secondary" className="text-xs tracking-widest uppercase">{t('statusCompleted')}</Typography.Text>
                  <Typography.Title level={2} style={{ margin: 0 }}>
                    {isLoading ? <Skeleton.Input active size="small" style={{ height: 28, width: 96 }} /> : String(completedCount)}
                  </Typography.Title>
                </div>
              </Space>
            </Card>
          </Col>
        </Row>

        {/* Transactions Table */}
        <Card
          title={<Typography.Text strong className="text-lg tracking-widest uppercase">{t('transactionList')}</Typography.Text>}
          extra={<Tag bordered className="font-mono">{t('pageNumber', { page: page + 1 })}</Tag>}
        >
          {isLoading ? (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton.Input key={i} active style={{ height: 64, width: '100%' }} />
              ))}
            </Space>
          ) : (
            <>
              <div className="hidden md:block">
                <Table<Transaction>
                  columns={columns}
                  dataSource={transactions ?? []}
                  rowKey="id"
                  pagination={false}
                  loading={isLoading}
                  locale={{ emptyText: t('emptyTable') }}
                />
              </div>

              {/* Mobile Layout */}
              <div className="md:hidden">
                <Space direction="vertical" size={16} style={{ width: '100%' }}>
                  {transactions?.map((transaction: Transaction) => {
                    const status = statusConfig[transaction.status];
                    const type = typeConfig[transaction.type] || typeConfig.INTERNAL_TRANSFER;
                    const StatusIcon = status.icon;
                    const TypeIcon = type.icon;

                    return (
                      <div key={transaction.id} style={{ borderBottom: "1px solid var(--ant-color-border-secondary)", paddingBottom: 16 }}>
                        <Space direction="vertical" size={12} style={{ width: '100%' }}>
                          <Row justify="space-between" align="middle">
                            <Space size={12}>
                              <div style={{
                                width: 40,
                                height: 40,
                                borderRadius: 12,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                border: '1px solid',
                                ...(isCreditType(transaction.type)
                                  ? { backgroundColor: token.colorPrimaryBg, borderColor: token.colorPrimaryBorder }
                                  : { backgroundColor: token.colorFillTertiary, borderColor: token.colorBorderSecondary }
                                )
                              }}>
                                <TypeIcon style={{
                                  width: 20, height: 20,
                                  color: isCreditType(transaction.type) ? token.colorPrimary : token.colorTextTertiary
                                }} />
                              </div>
                              <div>
                                <Typography.Text strong>{t(type.labelKey)}</Typography.Text>
                                <div>
                                  <Typography.Text type="secondary" className="text-xs">{formatDate(transaction.createdAt)}</Typography.Text>
                                </div>
                              </div>
                            </Space>
                            <Tag bordered={false} color={status.color} className="font-bold text-xs">
                              <StatusIcon style={{ width: 12, height: 12, marginRight: 4 }} />
                              {t(status.labelKey)}
                            </Tag>
                          </Row>
                          <Divider style={{ margin: 0 }} />
                          <Row justify="space-between" align="middle">
                            <Typography.Text type="secondary" ellipsis={{ tooltip: transaction.description }} style={{ maxWidth: 150 }}>
                              {transaction.description}
                            </Typography.Text>
                            <Typography.Text strong className="tabular-nums" type={isCreditType(transaction.type) ? undefined : 'secondary'}>
                              {isCreditType(transaction.type) ? '+' : '-'}{formatAmount(transaction.amount, transaction.currency)}
                            </Typography.Text>
                          </Row>
                          {canCancel(transaction.status) && (
                            <>
                              <Divider style={{ margin: 0 }} />
                              <Button
                                type="text"
                                danger
                                size="small"
                                style={{ width: '100%' }}
                                onClick={() => handleCancelClick(transaction)}
                              >
                                <Space size={8}>
                                  <X style={{ width: 16, height: 16 }} />
                                  {t('cancelTransaction')}
                                </Space>
                              </Button>
                            </>
                          )}
                        </Space>
                      </div>
                    );
                  })}
                </Space>
              </div>

              {!transactions || transactions.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '32px 0' }}>
                  <div style={{
                    width: 64,
                    height: 64,
                    backgroundColor: token.colorFillTertiary,
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 16px'
                  }}>
                    <AlertCircle style={{ width: 32, height: 32, color: token.colorTextTertiary }} />
                  </div>
                  <Typography.Title level={3} style={{ margin: 0 }}>{t('emptyTitle')}</Typography.Title>
                  <Typography.Text type="secondary">
                    {t('emptyDescription')}
                  </Typography.Text>
                </div>
              ) : null}

              {/* Pagination */}
              {transactions && transactions.length > 0 && (
                <>
                  <Divider style={{ margin: '24px 0' }} />
                  <Row justify="space-between" align="middle">
                    <Button
                      size="small"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                    >
                      {t('previousPage')}
                    </Button>
                    <Typography.Text type="secondary">
                      {t('pageNumber', { page: page + 1 })}
                    </Typography.Text>
                    <Button
                      size="small"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={!transactions || transactions.length < 20}
                    >
                      {t('nextPage')}
                    </Button>
                  </Row>
                </>
              )}
            </>
          )}
        </Card>
      </Space>

      {/* Cancel Confirmation Dialog */}
      <Modal open={isCancelDialogOpen} onCancel={() => setIsCancelDialogOpen(false)} footer={null} centered width={512} title={<Typography.Title level={4} style={{ marginBottom: 4 }}><Space size={8}><AlertCircle style={{ width: 20, height: 20, color: token.colorError }} />{t('cancelTransactionTitle')}</Space></Typography.Title>}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            {t('cancelConfirm')}
          </Typography.Text>
          {selectedTransaction && (
            <Card size="small">
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Row justify="space-between">
                  <Typography.Text type="secondary">{t('reference')}</Typography.Text>
                  <Typography.Text className="font-mono">{selectedTransaction.referenceNumber}</Typography.Text>
                </Row>
                <Row justify="space-between">
                  <Typography.Text type="secondary">{t('description')}</Typography.Text>
                  <Typography.Text ellipsis={{ tooltip: selectedTransaction.description }} style={{ maxWidth: 150 }}>{selectedTransaction.description}</Typography.Text>
                </Row>
                <Row justify="space-between">
                  <Typography.Text type="secondary">{t('amount')}</Typography.Text>
                  <Typography.Text strong style={{ fontVariantNumeric: "tabular-nums" }}>
                    {formatAmount(selectedTransaction.amount, selectedTransaction.currency)}
                  </Typography.Text>
                </Row>
              </Space>
            </Card>
          )}
          <Row justify="end" gutter={8}>
            <Col>
              <Button onClick={() => setIsCancelDialogOpen(false)}>
                {t('cancel')}
              </Button>
            </Col>
            <Col>
              <Button
                danger
                type="primary"
                onClick={handleConfirmCancel}
                disabled={cancelTransaction.isPending}
              >
                {cancelTransaction.isPending ? t('cancelling') : t('yesCancel')}
              </Button>
            </Col>
          </Row>
        </Space>
      </Modal>
    </DashboardLayout>
  );
}
