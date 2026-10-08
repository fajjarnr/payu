'use client';

import React, { useState } from 'react';
import DashboardLayout from '@/components/DashboardLayout';
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
} from '@/components/icons';
import { Button, Card, Col, Input, Row, Space, Tag, Typography, Empty } from 'antd';
import {
  useSplitBills,
  useCreateSplitBill,
  useSettleSplitBill,
  useAddParticipant,
  useActivateSplitBill,
} from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { addCurrency, asMoney, divideCurrency, formatExactDecimal, parseCurrencyExact, type Money } from '@/lib/currency';
import type { SplitBillParticipant } from '@/services/TransactionService';

const { Title, Text } = Typography;

export default function SplitBillPage() {
  const { accountId } = useAuthStore();
  const acctId = accountId ?? '';
  const { data: splitBillsData, isLoading } = useSplitBills(acctId);
  const createSplitBill = useCreateSplitBill();
  const settleBill = useSettleSplitBill();
  const addParticipant = useAddParticipant();
  const activateBill = useActivateSplitBill();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newBillName, setNewBillName] = useState('');
  const [newBillAmount, setNewBillAmount] = useState('');
  const [participants, setParticipants] = useState<Array<{ accountId: string; accountNumber: string; accountName: string }>>([
    { accountId: '', accountNumber: '', accountName: '' },
  ]);

  const updateParticipant = (index: number, field: 'accountId' | 'accountNumber' | 'accountName', value: string) => {
    setParticipants((prev) => prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)));
  };

  const addParticipantRow = () => {
    setParticipants((prev) => [...prev, { accountId: '', accountNumber: '', accountName: '' }]);
  };

  const removeParticipantRow = (index: number) => {
    setParticipants((prev) => prev.filter((_, i) => i !== index));
  };

  const splitBills = ((Array.isArray(splitBillsData) ? splitBillsData : []) as unknown as Array<{
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
  }>);

  const formatCurrency = (amount: Money | number) =>
    formatExactDecimal(amount, 0, 'id-ID');

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Tag bordered={false} color="green">Aktif</Tag>;
      case 'SETTLED':
        return <Tag bordered={false} color="blue">Lunas</Tag>;
      case 'PENDING':
        return <Tag bordered={false} color="orange">Menunggu</Tag>;
      case 'CANCELLED':
        return <Tag bordered={false} color="red">Dibatalkan</Tag>;
      default:
        return <Tag bordered>{status}</Tag>;
    }
  };

  const handleCreate = () => {
    if (!newBillName || !newBillAmount) return;
    const validParticipants = participants
      .map((p) => ({ ...p, accountId: p.accountId.trim(), accountNumber: p.accountNumber.trim(), accountName: p.accountName.trim() }))
      .filter((p) => p.accountId && p.accountNumber && p.accountName);
    if (validParticipants.length === 0) return;
    const totalAmount = parseCurrencyExact(newBillAmount);
    const perHead = divideCurrency(totalAmount, validParticipants.length);
    createSplitBill.mutate(
      {
        title: newBillName,
        totalAmount,
        currency: 'IDR',
        creatorAccountId: acctId,
        splitType: 'EQUAL',
        participants: validParticipants.map((p) => ({ ...p, amountOwed: perHead })),
      },
      {
        onSuccess: () => {
          setShowCreateModal(false);
          setNewBillName('');
          setNewBillAmount('');
          setParticipants([{ accountId: '', accountNumber: '', accountName: '' }]);
        },
      }
    );
  };

  const activeBills = splitBills.filter((b) => b.status === 'ACTIVE' || b.status === 'PENDING');
  const settledBills = splitBills.filter((b) => b.status === 'SETTLED');

  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {/* Header */}
        <Row justify="space-between" align="bottom" gutter={[16, 16]}>
          <Col>
            <Title level={2} style={{ margin: 0 }}>Split Bill</Title>
            <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
              Bagi tagihan dengan teman, keluarga, atau rekan kerja secara adil.
            </Text>
          </Col>
          <Col>
            <Button
              type="primary"
              size="large"
              onClick={() => setShowCreateModal(true)}
            >
              <Plus style={{ marginRight: 8 }} /> Split Bill Baru
            </Button>
          </Col>
        </Row>

        {/* Stats */}
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Card>
              <Space size={20}>
                <div style={{ width: 48, height: 48, backgroundColor: 'var(--ant-color-primary)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Receipt style={{ width: 24, height: 24, color: 'var(--ant-color-text-light-solid)' }} />
                </div>
                <div>
                  <Text type="secondary" strong style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>Aktif</Text>
                  <Title level={3} style={{ margin: '4px 0 0' }}>{activeBills.length}</Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card>
              <Space size={20}>
                <div style={{ width: 48, height: 48, backgroundColor: 'var(--ant-color-primary)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <CheckCircle2 style={{ width: 24, height: 24, color: 'var(--ant-color-text-light-solid)' }} />
                </div>
                <div>
                  <Text type="secondary" strong style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>Lunas</Text>
                  <Title level={3} style={{ margin: '4px 0 0' }}>{settledBills.length}</Title>
                </div>
              </Space>
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card>
              <Space size={20}>
                <div style={{ width: 48, height: 48, backgroundColor: 'var(--ant-color-secondary)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <DollarSign style={{ width: 24, height: 24, color: 'var(--ant-color-text-light-solid)' }} />
                </div>
                <div>
                  <Text type="secondary" strong style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>Total</Text>
                  <Title level={3} style={{ margin: '4px 0 0' }}>
                    {formatCurrency(splitBills.reduce((sum, b) => addCurrency(sum, b.totalAmount), asMoney('0')))}
                  </Title>
                </div>
              </Space>
            </Card>
          </Col>
        </Row>

        {/* Create Modal */}
        {showCreateModal && (
          <Card>
            <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <Title level={3} style={{ margin: 0 }}>Buat Split Bill Baru</Title>
              <Row gutter={[24, 24]}>
                <Col xs={24} md={12}>
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <label htmlFor="splitbill-description" style={{ fontSize: 12, fontWeight: 700, color: 'var(--ant-color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                      Deskripsi
                    </label>
                    <Input id="splitbill-description"
                      placeholder="Makan siang, nonton bareng..."
                      value={newBillName}
                      onChange={(e) => setNewBillName(e.target.value)}
                    />
                  </Space>
                </Col>
                <Col xs={24} md={12}>
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <label htmlFor="splitbill-amount" style={{ fontSize: 12, fontWeight: 700, color: 'var(--ant-color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                      Total Tagihan
                    </label>
                    <Input id="splitbill-amount"
                      type="number"
                      placeholder="150000"
                      value={newBillAmount}
                      onChange={(e) => setNewBillAmount(e.target.value)}
                    />
                  </Space>
                </Col>
              </Row>
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Row justify="space-between" align="middle">
                  <label htmlFor="splitbill-participant-accountId-0" style={{ fontSize: 12, fontWeight: 700, color: 'var(--ant-color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                    Peserta (min. 1)
                  </label>
                  <Button htmlType="button" size="small" onClick={addParticipantRow}>
                    <UserPlus style={{ marginRight: 4 }} /> Tambah Peserta
                  </Button>
                </Row>
                {participants.map((p, i) => (
                  <Row key={i} gutter={[12, 12]} align="middle">
                    <Col xs={24} md={6}>
                      <Input id={`splitbill-participant-accountId-${i}`}
                        aria-label={`ID akun peserta ${i + 1}`}
                        placeholder="Account ID"
                        value={p.accountId}
                        onChange={(e) => updateParticipant(i, 'accountId', e.target.value)}
                      />
                    </Col>
                    <Col xs={24} md={6}>
                      <Input id={`splitbill-participant-accountNumber-${i}`}
                        aria-label={`Nomor rekening peserta ${i + 1}`}
                        placeholder="No. Rekening"
                        value={p.accountNumber}
                        onChange={(e) => updateParticipant(i, 'accountNumber', e.target.value)}
                      />
                    </Col>
                    <Col xs={24} md={8}>
                      <Input id={`splitbill-participant-accountName-${i}`}
                        aria-label={`Nama peserta ${i + 1}`}
                        placeholder="Nama"
                        value={p.accountName}
                        onChange={(e) => updateParticipant(i, 'accountName', e.target.value)}
                      />
                    </Col>
                    <Col xs={24} md={4}>
                      <Button
                        type="text"
                        size="small"
                        disabled={participants.length === 1}
                        onClick={() => removeParticipantRow(i)}
                        style={{ color: 'var(--ant-color-error)' }}
                      >
                        Hapus
                      </Button>
                    </Col>
                  </Row>
                ))}
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Jumlah tiap peserta dibagi rata (split rata).
                </Text>
              </Space>
              <Space size={16}>
                <Button type="primary" onClick={handleCreate} disabled={createSplitBill.isPending}>
                  {createSplitBill.isPending ? <Loader2 style={{ marginRight: 8, animation: 'spin 1s linear infinite' }} /> : null}
                  Buat
                </Button>
                <Button onClick={() => setShowCreateModal(false)}>
                  Batal
                </Button>
              </Space>
            </Space>
          </Card>
        )}

        {/* Active Split Bills */}
        {activeBills.length > 0 && (
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <Title level={3} style={{ margin: 0 }}>Split Bill Aktif</Title>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {activeBills.map((bill) => (
                <Card key={bill.id}>
                  <Row justify="space-between" align="middle" gutter={[16, 16]}>
                    <Col>
                      <Space size={16}>
                        <div style={{ width: 48, height: 48, backgroundColor: 'var(--ant-color-primary-bg)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <Users style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                        </div>
                        <div>
                          <Text strong>{bill.description}</Text>
                          <br />
                          <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                            {new Date(bill.createdAt).toLocaleDateString(undefined)}
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
                    <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 16 }}>
                      {bill.participants.map((p) => (
                        <Row key={p.id} justify="space-between" align="middle" style={{ padding: '12px 16px', backgroundColor: 'var(--ant-color-fill-tertiary)', borderRadius: 12 }}>
                          <Space size={12}>
                            <div style={{ width: 32, height: 32, backgroundColor: 'var(--ant-color-primary-bg)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, color: 'var(--ant-color-primary)' }}>
                              {p.name?.charAt(0) ?? '?'}
                            </div>
                            <Text>{p.name}</Text>
                          </Space>
                          <Space size={12} align="center">
                            <Text strong>{formatCurrency(p.amount)}</Text>
                            {p.status === 'PAID' ? (
                              <CheckCircle2 style={{ width: 16, height: 16, color: 'var(--ant-color-primary)' }} />
                            ) : p.status === 'DECLINED' ? (
                              <XCircle style={{ width: 16, height: 16, color: 'var(--ant-color-error)' }} />
                            ) : (
                              <Clock style={{ width: 16, height: 16, color: 'var(--ant-color-warning)' }} />
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
                          participant: { accountId: '', accountName: 'Teman Baru', amountOwed: divideCurrency(bill.totalAmount, 2), status: 'PENDING' } as SplitBillParticipant,
                        })
                      }
                    >
                      <UserPlus style={{ marginRight: 4 }} /> Tambah Peserta
                    </Button>
                    {bill.status === 'PENDING' && (
                      <Button
                        size="small"
                        type="primary"
                        onClick={() => activateBill.mutate(bill.id)}
                      >
                        Aktifkan
                      </Button>
                    )}
                    <Button
                      size="small"
                      onClick={() => settleBill.mutate(bill.id)}
                    >
                      Selesaikan
                    </Button>
                  </Space>
                </Card>
              ))}
            </Space>
          </Space>
        )}

        {/* Settled Bills */}
        {settledBills.length > 0 && (
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <Title level={3} style={{ margin: 0 }}>Riwayat</Title>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {settledBills.map((bill) => (
                <Card key={bill.id} style={{ opacity: 0.8 }}>
                  <Row justify="space-between" align="middle" gutter={[16, 16]}>
                    <Col>
                      <Space size={16}>
                        <div style={{ width: 48, height: 48, backgroundColor: 'var(--ant-color-fill-tertiary)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <CheckCircle2 style={{ width: 24, height: 24, color: 'var(--ant-color-text-tertiary)' }} />
                        </div>
                        <div>
                          <Text strong>{bill.description}</Text>
                          <br />
                          <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                            {bill.participants?.length ?? 0} peserta
                          </Text>
                        </div>
                      </Space>
                    </Col>
                    <Col>
                      <Text strong style={{ fontSize: 18, color: 'var(--ant-color-text-tertiary)' }}>
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
            <Space direction="vertical" size={8}>
              <Text strong style={{ fontSize: 16 }}>Belum ada Split Bill</Text>
              <Text type="secondary">
                Buat split bill pertama Anda untuk membagi tagihan bersama teman.
              </Text>
            </Space>
          }
        >
          <Button type="primary" onClick={() => setShowCreateModal(true)}>
            <Plus style={{ marginRight: 8 }} /> Mulai Split Bill
          </Button>
        </Empty>
          </Card>
        )}

        {isLoading && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 32 }}>
            <Loader2 style={{ width: 32, height: 32, animation: 'spin 1s linear infinite', color: 'var(--ant-color-primary)' }} />
          </div>
        )}
      </Space>
    </DashboardLayout>
  );
}
