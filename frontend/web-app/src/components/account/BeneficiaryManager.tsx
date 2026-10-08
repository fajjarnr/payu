'use client';

import { useState } from 'react';
import { Trash2, Building2, CreditCard, Plus } from '@/components/icons';
import { useTranslations } from 'next-intl';
import { Button, Card, Col, Divider, Empty, Input, List, Row, Skeleton, Space, Tag, Typography } from 'antd';

import { useBeneficiaries, useCreateBeneficiary, useDeleteBeneficiary } from '@/hooks/useBeneficiaries';
import { notify as toast } from '@/lib/notify';

interface BeneficiaryManagerProps {
  accountId: string;
  onSelect?: (accountNumber: string) => void;
}

export default function BeneficiaryManager({ accountId, onSelect }: BeneficiaryManagerProps) {
  const t = useTranslations('beneficiaries');
  const { data: beneficiaries, isLoading } = useBeneficiaries(accountId);
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
    try {
      await deleteMut.mutateAsync(id);
      toast.success(t('removed'));
    } catch (err: unknown) {
      toast.error(errorMessage(err, t('deleteFailed')));
    }
  };

  return (
    <Card
      data-testid="beneficiary-manager"
      title={
        <Space size={8}>
          <Building2 style={{ fontSize: 20, color: 'var(--ant-color-primary)' }} aria-hidden="true" />
          <Typography.Text strong>{t('title')}</Typography.Text>
        </Space>
      }
    >
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        <Typography.Text type="secondary">{t('description')}</Typography.Text>
        {isLoading ? (
          <Skeleton active paragraph={{ rows: 2 }} aria-busy="true" />
        ) : !beneficiaries || beneficiaries.length === 0 ? (
          <Empty description={t('empty')}>
            <Typography.Text data-testid="beneficiary-empty" type="secondary">{t('empty')}</Typography.Text>
          </Empty>
        ) : (
          <div role="list" aria-label="Beneficiary list">
          <List
            dataSource={beneficiaries}
            renderItem={(b) => (
              <List.Item
                key={b.id}
                role="listitem"
                data-testid={`beneficiary-${b.id}`}
                actions={[
                  ...(onSelect
                    ? [<Button key="use" type="link" size="small" onClick={() => onSelect(b.accountNumber)} data-testid={`beneficiary-select-${b.id}`}>{t('use')}</Button>]
                    : []),
                  <Button
                    key="delete"
                    type="text"
                    danger
                    icon={<Trash2 style={{ fontSize: 16 }} />}
                    loading={deleteMut.isPending}
                    onClick={() => handleDelete(b.id)}
                    aria-label={t('delete', { name: b.nickname || b.accountNumber })}
                    data-testid={`beneficiary-delete-${b.id}`}
                  />,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <span style={{ width: 40, height: 40, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', background: 'var(--ant-color-primary-bg)', border: '1px solid var(--ant-color-border)' }}>
                      <CreditCard style={{ fontSize: 20, color: 'var(--ant-color-primary)' }} aria-hidden="true" />
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

        <Space direction="vertical" size={16} style={{ width: '100%' }} data-testid="beneficiary-form">
          <Typography.Text strong>{t('addTitle')}</Typography.Text>
          <Row gutter={16}>
            <Col span={8}>
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Typography.Text>{t('bankCode')}</Typography.Text>
                <Input id="beneficiary-bankCode" data-testid="beneficiary-bankCode" value={bankCode} onChange={(e) => setBankCode(e.target.value)} placeholder="014" maxLength={10} status={errors.bankCode ? 'error' : undefined} />
                {errors.bankCode && <Typography.Text type="danger" role="alert">{errors.bankCode}</Typography.Text>}
              </Space>
            </Col>
            <Col span={16}>
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Typography.Text>{t('accountNumber')}</Typography.Text>
                <Input id="beneficiary-accountNumber" data-testid="beneficiary-accountNumber" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value.replace(/\D/g,''))} placeholder="1234567890" inputMode="numeric" status={errors.accountNumber ? 'error' : undefined} />
                {errors.accountNumber && <Typography.Text type="danger" role="alert">{errors.accountNumber}</Typography.Text>}
              </Space>
            </Col>
          </Row>
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <Typography.Text>{t('nicknameOptional')}</Typography.Text>
            <Input id="beneficiary-nickname" data-testid="beneficiary-nickname" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="My BCA" maxLength={100} status={errors.nickname ? 'error' : undefined} />
            {errors.nickname && <Typography.Text type="danger" role="alert">{errors.nickname}</Typography.Text>}
          </Space>
          <Button type="primary" size="large" onClick={handleCreate} loading={createMut.isPending} icon={<Plus style={{ fontSize: 16 }} />} data-testid="beneficiary-create">
            {t('add')}
          </Button>
        </Space>
      </Space>
    </Card>
  );
}
