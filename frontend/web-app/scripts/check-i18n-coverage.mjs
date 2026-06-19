#!/usr/bin/env node
/**
 * i18n key coverage check — prevents MISSING_MESSAGE crashes (L-057).
 *
 * Compares key paths in messages/en.json vs messages/id.json.
 * Fails CI if:
 *   - Any key exists in en.json but not id.json (Indonesian users see English fallback)
 *   - Any key exists in id.json but not en.json (English users see missing key)
 *
 * Usage:
 *   node scripts/check-i18n-coverage.mjs
 *
 * Exit codes:
 *   0 = key sets match
 *   1 = key mismatch
 *   2 = file read error
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const messagesDir = resolve(__dirname, '../messages');

/**
 * Flatten nested JSON object to dot-path set.
 * { common: { loading: "..." } } → Set { "common.loading" }
 */
function flatten(obj, prefix = '') {
  const keys = new Set();
  for (const [k, v] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === 'object' && !Array.isArray(v)) {
      for (const nested of flatten(v, path)) keys.add(nested);
    } else {
      keys.add(path);
    }
  }
  return keys;
}

/**
 * Check key parity between two locale files.
 * Returns { ok: boolean, missing: Set, extra: Set }.
 */
function checkParity(enKeys, idKeys) {
  const missing = new Set([...enKeys].filter((k) => !idKeys.has(k)));
  const extra = new Set([...idKeys].filter((k) => !enKeys.has(k)));
  return { ok: missing.size === 0 && extra.size === 0, missing, extra };
}

function main() {
  let enRaw, idRaw;
  try {
    enRaw = readFileSync(resolve(messagesDir, 'en.json'), 'utf-8');
    idRaw = readFileSync(resolve(messagesDir, 'id.json'), 'utf-8');
  } catch (e) {
    console.error(`ERROR: Failed to read locale files: ${e.message}`);
    process.exit(2);
  }

  let en, id;
  try {
    en = JSON.parse(enRaw);
    id = JSON.parse(idRaw);
  } catch (e) {
    console.error(`ERROR: Invalid JSON in locale files: ${e.message}`);
    process.exit(2);
  }

  const enKeys = flatten(en);
  const idKeys = flatten(id);

  console.log(`en.json: ${enKeys.size} keys`);
  console.log(`id.json: ${idKeys.size} keys`);

  const { ok, missing, extra } = checkParity(enKeys, idKeys);

  if (ok) {
    console.log('OK: key parity maintained across locales');
    process.exit(0);
  }

  console.error('');
  console.error('FAIL: i18n key mismatch detected');
  if (missing.size > 0) {
    console.error(`  Missing in id.json (${missing.size}):`);
    for (const k of [...missing].sort()) console.error(`    - ${k}`);
  }
  if (extra.size > 0) {
    console.error(`  Extra in id.json (${extra.size}):`);
    for (const k of [...extra].sort()) console.error(`    + ${k}`);
  }
  console.error('');
  console.error('Fix: add missing keys to BOTH en.json and id.json, or remove extras.');
  console.error('Ref: L-057 — MISSING_MESSAGE causes silent translation fallback in production.');
  process.exit(1);
}

main();
