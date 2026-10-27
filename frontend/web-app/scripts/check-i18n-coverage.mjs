#!/usr/bin/env node
/**
 * i18n key coverage check — prevents MISSING_MESSAGE crashes (L-057).
 *
 * Single-locale app (Indonesian only): validates messages/id.json —
 * valid JSON, non-empty key set, every leaf a non-empty string.
 * Fails CI if:
 *   - id.json is unreadable or invalid JSON
 *   - id.json has zero keys or any empty/non-string leaf
 *
 * Usage:
 *   node scripts/check-i18n-coverage.mjs
 *
 * Exit codes:
 *   0 = catalog valid
 *   1 = catalog invalid
 *   2 = file read error
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const messagesDir = resolve(__dirname, '../messages');

/**
 * Flatten nested JSON object to dot-path entries.
 * { common: { loading: "..." } } → [ ["common.loading", "..."] ]
 */
function flatten(obj, prefix = '') {
  const entries = [];
  for (const [k, v] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${k}` : k;
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      entries.push(...flatten(v, path));
    } else {
      entries.push([path, v]);
    }
  }
  return entries;
}

function main() {
  let idRaw;
  try {
    idRaw = readFileSync(resolve(messagesDir, 'id.json'), 'utf-8');
  } catch (e) {
    console.error(`ERROR: Failed to read locale file: ${e.message}`);
    process.exit(2);
  }

  let id;
  try {
    id = JSON.parse(idRaw);
  } catch (e) {
    console.error(`ERROR: Invalid JSON in id.json: ${e.message}`);
    process.exit(2);
  }

  const entries = flatten(id);
  console.warn(`id.json: ${entries.length} keys`);

  const bad = entries.filter(
    ([, v]) => typeof v !== 'string' || v.trim() === '',
  );

  if (entries.length === 0 || bad.length > 0) {
    console.error('');
    console.error('FAIL: id.json catalog invalid');
    for (const [k, v] of bad.slice(0, 20)) {
      console.error(`    - ${k}: ${JSON.stringify(v)}`);
    }
    if (bad.length > 20) console.error(`    ... and ${bad.length - 20} more`);
    console.error('');
    console.error('Fix: every leaf in id.json must be a non-empty string.');
    console.error('Ref: L-057 — MISSING_MESSAGE causes silent translation fallback in production.');
    process.exit(1);
  }

  console.warn('OK: id.json catalog valid (single-locale app)');
  process.exit(0);
}

main();
