'use client';

import { useState } from 'react';
import { Trash2, Building2, CreditCard, Plus } from '@/components/icons';
import { useTranslations } from 'next-intl';
import { Alert, Button, Card, Col, Divider, Empty, Input, List, Popconfirm, Row, Skeleton, Space, Tag, Typography } from 'antd';

import { useBeneficiaries, useCreateBeneficiary, useDeleteBeneficiary } from '@/hooks/useBeneficiaries';
import { notify as toast } from '@/lib/notify';

interface BeneficiaryManagerProps {
  accountId: string;
  onSelect?: (accountNumber: string) => void;
}

export default function BeneficiaryManager({ accountId, onSelect }: BeneficiaryManagerProps) {
  const t = useTranslations('beneficiaries');
  const tc = useTranslations('common');
  const { data: beneficiaries, isLoading, isError, refetch } = useBeneficiaries(accountId);
  const createMut = useCreateBeneficiary(accountId);
  const deleteMut = useDeleteBeneficiary(accountId);

  const errorMessage = (err: unknown, fallback: string) => {
    if (err && typeof err === 'object' && 'response' in err) {
      const response = err.response;
      if (response && typeof response === 'object' && 'data' in response) {
        const data = response.data;
        if (data && typeof data === 'object' && 'error' in data) {
          const e = data.error;
          if (e && typeof e === 'object' && 'message' in e && typeof e.message === 'string') return e.message;
        }
      }
    }
    if (err && typeof err === 'object' && 'message' in err && typeof err.message === 'string') return err.message;
    return fallback;
  };
  const [bankCode, setBankCode] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [nickname, setNickname] = useState('');
  const [errors, setErrors] = useState<Record<string,string>>({});
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const validate = () => {
    const e: Record<string,string> = {};
    if (!bankCode.trim() || bankCode.length > 10) e.bankCode = t('errBankCode');
    if (!/^\d{10,20}$/.test(accountNumber)) e.accountNumber = t('errAccount');
    if (nickname && nickname.length > 100) e.nickname = t('errNickname');
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleCreate = async () => {
    if (!validate()) return;
    try {
      await createMut.mutateAsync({ bankCode: bankCode.trim(), accountNumber: accountNumber.trim(), nickname: nickname.trim() || undefined });
      toast.success(t('added'));
      setBankCode(''); setAccountNumber(''); setNickname('');
    } catch (err: unknown) {
      toast.error(errorMessage(err, t('addFailed')));
    }
  };

  const handleDelete = async (id: string) => {
    setDeletingId(id);
    try {
      await deleteMut.mutateAsync(id);
      toast.success(t('removed'));
    } catch (err: unknown) {
      toast.error(errorMessage(err, t('deleteFailed')));
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Card
      data-testid="beneficiary-manager"
      title={
        <Space size={8}>
          <Building2 style={{ fontSize: 20, color: 'var(--ant-color-primary-text)' }} aria-hidden="true" />
          <Typography.Text strong>{t('title')}</Typography.Text>
        </Space>
      }
    >
      <Space orientation="vertical" size={24} style={{ width: '100%' }}>
        <Typography.Text type="secondary">{t('description')}</Typography.Text>
        {isLoading ? (
          <Skeleton active paragraph={{ rows: 2 }} aria-busy="true" />
        ) : isError ? (
          // R-06: a failed load must not masquerade as an empty list.
          <Alert
            type="error"
            showIcon
            message={t('loadError')}
            action={
              <Button size="small" onClick={() => refetch()}>
                {tc('retry')}
              </Button>
            }
          />
        ) : !beneficiaries || beneficiaries.length === 0 ? (
          <Empty description={t('empty')} data-testid="beneficiary-empty" />
        ) : (
          <div role="list" aria-label={t('listLabel')}>
          <List
            dataSource={beneficiaries}
            renderItem={(b) => (
              <List.Item
                key={b.id}
                role="listitem"
                data-testid={`beneficiary-${b.id}`}
                actions={[
                  ...(onSelect
                    ? [<Button key="use" type="link" size="small" onClick={() => onSelect(b.accountNumber)} aria-label={t('useFor', { name: b.nickname || b.accountNumber })} data-testid={`beneficiary-select-${b.id}`}>{t('use')}</Button>]
                    : []),
                  <Popconfirm
                    key="delete-confirm"
                    title={t('deleteConfirmTitle')}
                    description={t('deleteConfirmDesc', { name: b.nickname || b.accountNumber })}
                    okText={t('confirm')}
                    cancelText={t('cancel')}
                    okButtonProps={{ danger: true, loading: deletingId === b.id }}
                    onConfirm={() => handleDelete(b.id)}
                  >
                    <Button
                      key="delete"
                      type="text"
                      danger
                      icon={<Trash2 style={{ fontSize: 16 }} />}
                      loading={deletingId === b.id}
                      aria-label={t('delete', { name: b.nickname || b.accountNumber })}
                      data-testid={`beneficiary-delete-${b.id}`}
                      style={{ minWidth: 44, minHeight: 44 }}
                    />
                  </Popconfirm>,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <span style={{ width: 40, height: 40, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', background: 'var(--ant-color-primary-bg)', border: '1px solid var(--ant-color-border)', borderRadius: 12 }}>
                      <CreditCard style={{ fontSize: 20, color: 'var(--ant-color-primary-text)' }} aria-hidden="true" />
                    </span>
                  }
                  title={
                    <Space size={8}>
                      <Typography.Text strong ellipsis>{b.nickname || b.accountName}</Typography.Text>
                      <Tag>{b.bankCode}</Tag>
                    </Space>
                  }
                  description={<Typography.Text type="secondary" copyable={false}>{b.accountNumber}</Typography.Text>}
                />
              </List.Item>
            )}
          />
          </div>
        )}

        <Divider />

        <Space orientation="vertical" size={16} style={{ width: '100%' }} data-testid="beneficiary-form">
          <Typography.Text strong>{t('addTitle')}</Typography.Text>
          <Row gutter={16}>
            <Col xs={24} sm={8}>
              <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                <label htmlFor="beneficiary-bankCode">{t('bankCode')}</label>
                <Input id="beneficiary-bankCode" data-testid="beneficiary-bankCode" value={bankCode} onChange={(e) => setBankCode(e.target.value)} placeholder="014" maxLength={10} status={errors.bankCode ? 'error' : undefined} />
                {errors.bankCode && <Typography.Text type="danger" role="alert">{errors.bankCode}</Typography.Text>}
              </Space>
            </Col>
            <Col xs={24} sm={16}>
              <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                <label htmlFor="beneficiary-accountNumber">{t('accountNumber')}</label>
                <Input id="beneficiary-accountNumber" data-testid="beneficiary-accountNumber" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value.replace(/\D/g,''))} placeholder="1234567890" inputMode="numeric" status={errors.accountNumber ? 'error' : undefined} />
                {errors.accountNumber && <Typography.Text type="danger" role="alert">{errors.accountNumber}</Typography.Text>}
              </Space>
            </Col>
          </Row>
          <Space orientation="vertical" size={4} style={{ width: '100%' }}>
            <label htmlFor="beneficiary-nickname">{t('nicknameOptional')}</label>
            <Input id="beneficiary-nickname" data-testid="beneficiary-nickname" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="My BCA" maxLength={100} status={errors.nickname ? 'error' : undefined} />
            {errors.nickname && <Typography.Text type="danger" role="alert">{errors.nickname}</Typography.Text>}
          </Space>
          <Button type="primary" size="large" onClick={handleCreate} loading={createMut.isPending} icon={<Plus style={{ fontSize: 16 }} />} data-testid="beneficiary-create" style={{ fontWeight: 600, fontSize: 16, borderRadius: 24 }}>
            {t('add')}
          </Button>
        </Space>
      </Space>
    </Card>
  );
}
