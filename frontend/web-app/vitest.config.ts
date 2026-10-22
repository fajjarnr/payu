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
    // threads pool starves heavy suites (TransferActivity axe runs) under full
    // load: 1-3 timeout flakes per run. forks isolates workers, 2103/2103 green.
    pool: 'forks',
    maxWorkers: 4,
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
      'next/navigation': path.resolve(__dirname, 'node_modules/next/navigation.js'),
    },
  },
});
