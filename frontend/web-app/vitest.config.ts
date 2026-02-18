import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    include: ['src/__tests__/**/*.test.{ts,tsx}'],
    server: {
      deps: {
        // Inline next-intl so Vite's resolver handles 'next/navigation' import
        inline: ['next-intl'],
      },
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/__tests__/',
        '**/*.d.ts',
        '**/*.config.{js,ts}',
        'src/**/index.{ts,tsx}'
      ],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      // next/navigation.js exists but ESM can't resolve 'next/navigation' without extension
      // next-intl imports 'next/navigation' using ESM, causing ERR_MODULE_NOT_FOUND in jsdom
      'next/navigation': path.resolve(__dirname, 'node_modules/next/navigation.js'),
    },
  },
});
