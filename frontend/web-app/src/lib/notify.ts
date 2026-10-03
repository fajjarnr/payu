'use client';

import type { MessageInstance } from 'antd/es/message/interface';

let api: MessageInstance | null = null;

/** Called once by `NotifyRegistrar` inside `<App>` to bind the holder. */
export function registerMessageApi(instance: MessageInstance): void {
  api = instance;
}

function show(
  method: 'success' | 'error' | 'info' | 'warning',
  content: string,
  _opts?: unknown,
): void {
  void _opts;
  api?.[method](content);
}

/**
 * Sonner-compatible `toast` surface backed by antd `message`.
 * The second arg (e.g. sonner `{ duration }`) is accepted and ignored
 * so existing call sites need no changes.
 */
export const notify = {
  success(content: string, _opts?: unknown): void {
    show('success', content, _opts);
  },
  error(content: string, _opts?: unknown): void {
    show('error', content, _opts);
  },
  info(content: string, _opts?: unknown): void {
    show('info', content, _opts);
  },
  warning(content: string, _opts?: unknown): void {
    show('warning', content, _opts);
  },
};
