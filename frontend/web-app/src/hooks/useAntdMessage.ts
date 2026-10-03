'use client';

import { App } from 'antd';

/**
 * Imperative antd feedback APIs bound to the nearest `App` context
 * (see `AntdProvider`). Replaces static `message`/`notification`/`modal`
 * imports so they inherit theme + locale, and replaces sonner toasts.
 *
 * Must be called from a component rendered inside `AntdProvider`.
 */
export function useAntdMessage() {
  const { message, notification, modal } = App.useApp();

  return { message, notification, modal };
}
