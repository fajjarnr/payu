'use client';

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React, { useState, useEffect, useCallback } from 'react';
import { Download, FileText, Calendar, RefreshCw } from '@/components/icons';
import { useTranslations } from 'next-intl';
import { Alert, Button, Card, Select, Space, Tag, Typography, theme } from 'antd';
import StatementService, { Statement } from '@/services/StatementService';
import { getA11yProps } from '@/lib/a11y';
import { useAuthStore } from '@/stores/authStore';

const { Title, Text, Paragraph } = Typography;

export default function StatementDownloader() {
  const t = useTranslations('settings.statements');
  const { token } = theme.useToken();
  const accountId = useAuthStore((state) => state.accountId);
  const [statements, setStatements] = useState<Statement[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  // A-04/F-08: one shared error string made a generation failure render the
  // "failed to load the list" alert too — the same failure shown (and
  // announced) twice. The scope decides which card owns the message.
  const [error, setError] = useState<{ scope: 'generator' | 'list' | 'download'; message: string } | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Decision (monthly-only): the backend generate contract is StatementGenerationRequest
  // {customerId, accountNumber, year, month} -> YearMonth.of(year, month). It has no
  // quarter/year parameter, so quarterly/annual options could only resend the same
  // {year, month} payload. Keep monthly only instead of shipping misleading options.
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1);
  const [page, setPage] = useState<number>(0);
  const [hasMore, setHasMore] = useState<boolean>(false);

  const currentYear = new Date().getFullYear();

  const years = Array.from({ length: currentYear - 2019 }, (_, i) => currentYear - i);

  const loadStatements = useCallback(async (pageNum: number = 0) => {
    if (!accountId) {
      setIsLoading(false);
      return;
    }
    try {
      setIsLoading(true);
      setError(null);
      const result = await StatementService.listStatements(pageNum);
      const data = result.content;
      if (pageNum === 0) {
        setStatements(data);
      } else {
        setStatements(prev => [...prev, ...data]);
      }
      setPage(pageNum);
      setHasMore(!result.last && result.totalPages > pageNum + 1);
    } catch (err) {
      console.error('Failed to load statements:', err instanceof Error ? err.message : 'Unknown error');
      setError({ scope: 'list', message: t('history.errorLoad') });
    } finally {
      setIsLoading(false);
    }
  }, [t, accountId]);

  const handleLoadMore = useCallback(() => {
    if (isLoading || !hasMore) return;
    void loadStatements(page + 1);
  }, [isLoading, hasMore, loadStatements, page]);

  // Reload once the session account arrives after mount (store hydrates async).
  useEffect(() => {
    loadStatements(0);
  }, [loadStatements]);

  // BUG-FE-099: Read from store directly inside async handler to avoid stale closure
  const handleGenerateStatement = useCallback(async () => {
    if (isGenerating) return;
    const { user: currentUser, accountId: currentAccountId } = useAuthStore.getState();
    try {
      setIsGenerating(true);
      setError(null);
      setSuccess(null);

      await StatementService.generateAndDownload({
        customerId: currentUser?.id ?? '',
        accountNumber: currentAccountId ?? '',
        year: selectedYear,
        month: selectedMonth
      });
      setSuccess(t('generator.success'));
      await loadStatements();
    } catch (err) {
      console.error('Failed to generate statement:', err);
      setError({
        scope: 'generator',
        message: err instanceof Error ? err.message : t('generator.errorGen'),
      });
    } finally {
      setIsGenerating(false);
    }
  }, [selectedYear, selectedMonth, loadStatements, t, isGenerating]);

  const handleDownload = useCallback(async (statement: Statement) => {
    if (statement.status !== 'COMPLETED') {
      setError({ scope: 'download', message: t('history.notReady') });
      return;
    }
    if (isDownloading) return;

    try {
      setIsDownloading(statement.id);
      setError(null);
      setSuccess(null);

      await StatementService.downloadStatementWithFilename(statement.id);
      setSuccess(t('generator.success'));
    } catch (err) {
      console.error('Failed to download statement:', err);
      setError({ scope: 'download', message: t('history.errorDownload') });
    } finally {
      setIsDownloading(null);
    }
  }, [t, isDownloading]);

  const statusTagColor = (status: Statement['status']): 'success' | 'processing' | 'error' => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'GENERATING':
        return 'processing';
      case 'FAILED':
        return 'error';
      default:
        return 'processing';
    }
  };

  const statusLabel = (status: Statement['status']) => {
    switch (status) {
      case 'COMPLETED':
        return t('history.statusCompleted');
      case 'GENERATING':
        return t('history.statusGenerating');
      case 'FAILED':
        return t('history.statusFailed');
      default:
        return status;
    }
  };
  return (
    <Space orientation="vertical" size={24} style={{ width: '100%' }}>
      {/* Statement Generator Section */}
      <Card>
        <Space orientation="vertical" size={24} style={{ width: '100%' }}>
          {/* Section Header */}
          <Space size={16} align="start">
            <div
              aria-hidden="true"
              style={{
                width: 48,
                height: 48,
                borderRadius: token.borderRadius,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: token.colorPrimaryBg,
                border: `1px solid ${token.colorPrimaryBorder}`,
              }}
            >
              <FileText style={{ width: 24, height: 24, color: token.colorPrimary }} />
            </div>
            <div>
              <Title level={2} style={{ margin: 0 }}>{t('generator.title')}</Title>
              <Text type="secondary">{t('generator.subtitle')}</Text>
            </div>
          </Space>

          {/* Success Message */}
          {success && (
            <Alert type="success" showIcon role="status" message={t('generator.successLabel')} description={success} />
          )}

          {/* Error Message — generation only; list/download errors belong to the history card */}
          {error?.scope === 'generator' && (
            <Alert type="error" showIcon message={t('generator.errorLabel')} description={error.message} />
          )}

          {/* Generator Form */}
          <Space orientation="vertical" size={16} style={{ width: '100%' }}>
            {/* Year Selector */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>
                <label htmlFor="statement-year">{t('generator.year')}</label>
              </Text>
              <Select
                id="statement-year"
                value={selectedYear}
                onChange={(value) => setSelectedYear(Number(value))}
                options={years.map((year) => ({ value: year, label: String(year) }))}
                style={{ width: '100%' }}
                {...getA11yProps({ label: t('a11y.selectYear') })}
              />
            </div>

            {/* Month Selector (backend generate contract is monthly-only: {year, month}) */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>
                <label htmlFor="statement-month">{t('generator.month')}</label>
              </Text>
              <Select
                id="statement-month"
                value={selectedMonth}
                onChange={(value) => setSelectedMonth(Number(value))}
                options={Array.from({ length: 12 }, (_, i) => i + 1).map((month) => ({
                  value: month,
                  label: t(`months.${month}`),
                }))}
                style={{ width: '100%' }}
                {...getA11yProps({ label: t('a11y.selectMonth') })}
              />
            </div>
          </Space>

          {/* Generate Button */}
          <Button
            type="primary"
            size="large"
            block
            loading={isGenerating}
            disabled={isGenerating}
            onClick={handleGenerateStatement}
            icon={isGenerating ? <RefreshCw style={{ width: 20, height: 20 }} /> : <FileText style={{ width: 20, height: 20 }} />}
            style={{ minHeight: 56, fontWeight: 700, fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}
            {...getA11yProps({ label: t('a11y.generateButton') })}
          >
            {isGenerating ? t('generator.processing') : t('generator.generate')}
          </Button>
        </Space>
      </Card>
      {/* Statement History Section */}
      <Card>
        <Space orientation="vertical" size={24} style={{ width: '100%' }}>
          {/* Section Header */}
          <Space size={16} align="start">
            <div
              aria-hidden="true"
              style={{
                width: 48,
                height: 48,
                borderRadius: token.borderRadius,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: token.colorPrimaryBg,
                border: `1px solid ${token.colorPrimaryBorder}`,
              }}
            >
              <Calendar style={{ width: 24, height: 24, color: token.colorPrimary }} />
            </div>
            <div>
              <Title level={2} style={{ margin: 0 }}>{t('history.title')}</Title>
              <Text type="secondary">{t('history.subtitle')}</Text>
            </div>
          </Space>

          {/* Statements List */}
          {!isLoading && error?.scope === 'list' && (
            <Alert
              type="error"
              showIcon
              message={t('history.errorLoad')}
              action={<Button size="small" onClick={() => loadStatements(0)}>{t('history.retry')}</Button>}
            />
          )}
          {error?.scope === 'download' && (
            <Alert type="error" showIcon message={error.message} />
          )}
          <Space orientation="vertical" size={12} style={{ width: '100%' }}>
            {isLoading ? (
              <Card loading style={{ width: '100%' }} />
            ) : statements.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '48px 0' }}>
                <FileText aria-hidden="true" style={{ width: 48, height: 48, color: token.colorTextTertiary }} />
                <Paragraph type="secondary" strong>{t('history.empty')}</Paragraph>
                <Text type="secondary">{t('history.emptyDesc')}</Text>
              </div>
            ) : (
              statements.map((statement) => (
                <Card key={statement.id} size="small">
                  <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                    <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                      <Space size={12}>
                        <Text strong>{statement.periodFormatted}</Text>
                        <Tag color={statusTagColor(statement.status)}>
                          {statusLabel(statement.status)}
                        </Tag>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {t('history.transactionCount', { count: statement.transactionCount })}
                        </Text>
                      </Space>
                      <Space size={12} align="center">
                        {statement.status === 'COMPLETED' && (
                          <div style={{ textAlign: 'right' }}>
                            <Text strong>{statement.closingBalanceFormatted}</Text>
                            <br />
                            <Text type="secondary" style={{ fontSize: 12 }}>{t('history.closingBalance')}</Text>
                          </div>
                        )}
                        <Button
                          type="primary"
                          size="middle"
                          onClick={() => handleDownload(statement)}
                          disabled={statement.status !== 'COMPLETED' || isDownloading === statement.id}
                          loading={isDownloading === statement.id}
                          icon={<Download style={{ width: 16, height: 16 }} aria-hidden="true" />}
                          style={{ minHeight: 44 }}
                          {...getA11yProps({ label: t('a11y.downloadStatement', { period: statement.periodFormatted }) })}
                        >
                          {t('history.download')}
                        </Button>
                      </Space>
                    </Space>

                    {/* Transaction Summary */}
                    {statement.status === 'COMPLETED' && (
                      <Space size={16} wrap style={{ width: '100%' }}>
                        <div>
                          <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase' }}>{t('history.openingBalance')}</Text>
                          <br />
                          <Text strong>{statement.openingBalanceFormatted}</Text>
                        </div>
                        <div>
                          <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase' }}>{t('history.totalCredits')}</Text>
                          <br />
                          <Text strong style={{ color: token.colorSuccessText }}>+{statement.totalCreditsFormatted}</Text>
                        </div>
                        <div>
                          <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase' }}>{t('history.totalDebits')}</Text>
                          <br />
                          <Text strong style={{ color: token.colorErrorText }}>-{statement.totalDebitsFormatted}</Text>
                        </div>
                        <div>
                          <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase' }}>{t('history.closingBalance')}</Text>
                          <br />
                          <Text strong>{statement.closingBalanceFormatted}</Text>
                        </div>
                      </Space>
                    )}
                  </Space>
                </Card>
              ))
            )}
          </Space>

          {/* Load More Button */}
          {!isLoading && statements.length > 0 && hasMore && (
            <Button
              type="link"
              block
              onClick={handleLoadMore}
              loading={isLoading}
              disabled={isLoading || !hasMore}
              style={{ minHeight: 44, fontWeight: 700, fontSize: 12, textTransform: 'uppercase' }}
              {...getA11yProps({ label: t('a11y.loadMore') })}
            >
              {t('history.loadMore')}
            </Button>
          )}
        </Space>
      </Card>
    </Space>
  );
}
