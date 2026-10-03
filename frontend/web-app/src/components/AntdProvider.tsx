'use client';

import { StyleProvider } from '@ant-design/cssinjs';
import { App, ConfigProvider, theme as antdTheme } from 'antd';
import { registerMessageApi } from '@/lib/notify';
import type { ThemeConfig } from 'antd';
import type { ReactNode } from 'react';
import { getAntdLocale } from '@/lib/antd-locale';

const FONT_FAMILY = 'Inter, system-ui, sans-serif';

const baseTokens: ThemeConfig['token'] = {
  colorPrimary: '#0a6b48',
  colorSuccess: '#10b981',
  colorError: '#ef4444',
  colorWarning: '#f59e0b',
  colorInfo: '#3b82f6',
  borderRadius: 12,
  borderRadiusLG: 16,
  // antd v6 dropped the `borderRadiusXL` alias token; set 24px per-component
  // (e.g. Modal/Sheet) when those migrations land.
  fontFamily: FONT_FAMILY,
  fontSize: 14,
};

const componentTokens: ThemeConfig['components'] = {
  Button: {
    controlHeight: 48,
    controlHeightLG: 56,
    controlHeightSM: 36,
    fontWeight: 700,
  },
  Card: {
    borderRadiusLG: 16,
    boxShadowTertiary:
      '0 2px 8px -2px rgba(0, 0, 0, 0.05), 0 4px 16px -4px rgba(0, 0, 0, 0.08)',
  },
  Input: {
    controlHeight: 48,
    borderRadius: 12,
  },
  Table: {
    headerBg: 'transparent',
    headerColor: 'rgba(0,0,0,0.45)',
  },
};

export interface AntdProviderProps {
  children: ReactNode;
  /** next-intl locale code (`id` | `en`). */
  locale?: string;
  /** Whether the app is currently in dark mode. */
  isDark?: boolean;
}
function NotifyRegistrar() {
  const { message } = App.useApp();
  registerMessageApi(message);
  return null;
}

/**
 * Ant Design root provider. Renders no DOM wrapper (`App component={false}`)
 * so wrapping the tree does not change the existing layout.
 */
export function AntdProvider({ children, locale = 'en', isDark = false }: AntdProviderProps) {
  return (
    <StyleProvider layer>
      <ConfigProvider
        locale={getAntdLocale(locale)}
        theme={{
          algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
          token: baseTokens,
          components: componentTokens,
        }}
      >
        <App component={false}>
          <NotifyRegistrar />
          {children}
        </App>
      </ConfigProvider>
    </StyleProvider>
  );
}
