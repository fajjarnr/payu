'use client';

import DashboardLayout from '@/components/DashboardLayout';
import { Button, Space, Row, Col, Card, Typography } from 'antd';
import { useInvestmentAccount, useBuyDeposit, useSellInvestment, useCreateInvestmentAccount } from '@/hooks';
import { useTranslations } from 'next-intl';
import { asMoney, formatCurrency } from '@/lib/currency';
import { notify as toast } from '@/lib/notify';

export default function InvestmentsPage() {
  const t = useTranslations('investments');
  const { data: account, isLoading: loadingAccount, isError: accountError } = useInvestmentAccount();
  const buyDeposit = useBuyDeposit();
  const sellInvestment = useSellInvestment();
  const createAccount = useCreateInvestmentAccount();

  const hasAccount = Boolean(account) && !accountError;

  // ponytail: minimal wiring for I1-I5 — buy/sell via real mutations, no extra modal abstraction
  const handleBuy = async () => {
    try {
      let acc = account;
      if (!acc) {
        acc = await createAccount.mutateAsync();
      }
      await buyDeposit.mutateAsync({ accountId: acc.id, amount: asMoney('1000000'), tenure: 12 });
      toast.success('Pembelian deposit berhasil');
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal membeli produk');
    }
  };

  const handleSell = async () => {
    try {
      if (!account) {
        toast.error('Belum ada akun investasi');
        return;
      }
      await sellInvestment.mutateAsync({ accountId: account.id, transactionId: account.id, amount: asMoney('500000') });
      toast.success('Penjualan berhasil');
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal menjual produk');
    }
  };

  return (
    <DashboardLayout>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div>
          <Typography.Title level={2} style={{ marginBottom: 4 }}>{t('title')}</Typography.Title>
          <Typography.Text type="secondary">{t('subtitle')}</Typography.Text>
        </div>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={16}>
            <Card data-testid="portfolio-overview-card">
              <Typography.Text type="secondary">{t('accountBalance')}</Typography.Text>
              <Typography.Title level={3} style={{ marginTop: 8 }}>
                {loadingAccount
                  ? '...'
                  : account && !accountError
                    ? formatCurrency(account.balance, { withDecimals: false })
                    : t('accountUnavailable')}
              </Typography.Title>
              {hasAccount && <Typography.Text type="secondary" style={{ marginTop: 8 }}>{t('accountSource')}</Typography.Text>}
              <Space size={16} style={{ marginTop: 24 }}>
                <Button
                  type="primary"
                  onClick={handleBuy}
                  disabled={buyDeposit.isPending || createAccount.isPending}
                  data-testid="invest-buy-button"
                >
                  {buyDeposit.isPending ? 'Memproses...' : 'Beli Produk'}
                </Button>
                <Button
                  onClick={handleSell}
                  disabled={sellInvestment.isPending}
                  data-testid="invest-sell-button"
                >
                  {sellInvestment.isPending ? 'Memproses...' : 'Jual Produk'}
                </Button>
              </Space>
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card data-testid="investment-performance-empty" style={{ height: '100%' }}>
              <Typography.Title level={4}>{t('performance')}</Typography.Title>
              <Typography.Text type="secondary" style={{ marginTop: 8 }}>{t('performanceUnavailable')}</Typography.Text>
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card data-testid="investment-risk-empty" style={{ height: '100%' }}>
              <Typography.Title level={4}>{t('riskLevel')}</Typography.Title>
              <Typography.Text type="secondary" style={{ marginTop: 8 }}>{t('riskUnavailable')}</Typography.Text>
            </Card>
          </Col>

          <Col xs={24} lg={16}>
            <Card data-testid="investment-products-empty" style={{ height: '100%' }}>
              <Typography.Title level={4}>{t('portfolio')}</Typography.Title>
              <Typography.Text type="secondary" style={{ marginTop: 8 }}>{t('productsUnavailable')}</Typography.Text>
            </Card>
          </Col>

          <Col xs={24} lg={24}>
            <Card data-testid="investment-advice-empty">
              <Typography.Title level={4}>{t('advice')}</Typography.Title>
              <Typography.Text type="secondary" style={{ marginTop: 8 }}>{t('adviceUnavailable')}</Typography.Text>
            </Card>
          </Col>
        </Row>
      </Space>
    </DashboardLayout>
  );
}
