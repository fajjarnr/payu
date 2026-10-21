'use client';

import { theme } from 'antd';
import { ArrowDownLeft, ArrowUpRight, MoreHorizontal } from '@/components/icons';
import { formatCurrency } from '@/lib/currency';

export interface MoneyTextProps {
  amount: string | number;
  currency?: string;
  /** Credit/debit direction cue — rendered as icon + sr text, never color-only. */
  direction?: 'in' | 'out' | 'neutral';
  size?: 'body' | 'display';
}

/** Tabular-nums money with an icon cue for credit/debit direction. */
export default function MoneyText({
  amount,
  currency = 'Rp',
  direction = 'neutral',
  size = 'body',
}: MoneyTextProps) {
  const { token } = theme.useToken();
  const Icon = direction === 'in' ? ArrowUpRight : direction === 'out' ? ArrowDownLeft : MoreHorizontal;
  const color =
    direction === 'in'
      ? token.colorSuccessText ?? token.colorSuccess
      : direction === 'out'
        ? token.colorErrorText ?? token.colorError
        : undefined;
  const srLabel = direction === 'in' ? 'masuk' : direction === 'out' ? 'keluar' : null;
  return (
    <span
      style={{
        fontVariantNumeric: 'tabular-nums',
        fontSize: size === 'display' ? token.fontSizeXL : undefined,
        fontWeight: size === 'display' ? 700 : undefined,
        color,
      }}
    >
      <Icon aria-hidden="true" style={{ marginRight: 4 }} />
      {srLabel ? <span className="sr-only">{srLabel} </span> : null}
      {formatCurrency(amount, { symbol: currency })}
    </span>
  );
}
