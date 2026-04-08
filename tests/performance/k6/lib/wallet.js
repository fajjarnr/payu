// Wallet/Pocket CRUD Operations for K6 Tests
// ===========================================

import { sleep } from 'k6';
import http from 'k6/http';
import { check } from 'k6';
import { getAuthHeaders } from './auth.js';

function parseJson(body) {
  try {
    return JSON.parse(body || '{}');
  } catch (error) {
    return {};
  }
}

function normalizeAmount(amount) {
  if (typeof amount === 'number') {
    return amount.toFixed(2);
  }

  return amount;
}

function logFailure(operation, response, body) {
  const message = body.message || body.error || JSON.stringify(body).slice(0, 240);
  console.error(`${operation} failed: status=${response.status} message=${message}`);
}

const rateLimitRetryEnabled = __ENV.K6_RATE_LIMIT_RETRY === '1';
const rateLimitRetryAttempts = Number(__ENV.K6_RATE_LIMIT_RETRY_ATTEMPTS || 4);
const rateLimitRetryBaseSeconds = Number(__ENV.K6_RATE_LIMIT_RETRY_BASE_SECONDS || 1.5);

function executeWithRateLimitRetry(requestFn) {
  let response = null;

  for (let attempt = 0; attempt < rateLimitRetryAttempts; attempt++) {
    response = requestFn();

    if (!rateLimitRetryEnabled || response.status !== 429 || attempt === rateLimitRetryAttempts - 1) {
      return response;
    }

    const delaySeconds = Math.min(rateLimitRetryBaseSeconds * Math.pow(2, attempt), 8);
    sleep(delaySeconds + Math.random() * 0.5);
  }

  return response;
}

export function createIdempotencyKey(prefix = 'k6') {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
}

function fetchWalletBalance(gatewayUrl, token, accountId, allowNotReady = false) {
  const options = {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'wallet-balance' }
  };

  if (allowNotReady) {
    options.responseCallback = http.expectedStatuses(200, 404, 429);
  }

  const response = http.get(`${gatewayUrl}/api/v1/wallets/${accountId}/balance`, options);

  return {
    response: response,
    body: parseJson(response.body)
  };
}

/**
 * Get wallet balance by account ID
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} accountId - Account ID
 * @returns {Object|null} Wallet balance response or null
 */
export function getWalletBalance(gatewayUrl, token, accountId, recordChecks = true) {
  const result = fetchWalletBalance(gatewayUrl, token, accountId, false);
  const response = result.response;
  const body = result.body;

  const success = response.status === 200 && body.data !== undefined && body.data.accountId !== undefined;

  if (recordChecks) {
    check(response, {
      'wallet balance status is 200': (r) => r.status === 200,
      'wallet balance returns data': () => body.data !== undefined && body.data.accountId !== undefined
    });
  }

  if (success) {
    return body.data;
  }

  return null;
}

export function waitForWalletReady(gatewayUrl, token, accountId, maxAttempts = 10, sleepSeconds = 0.5) {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const result = fetchWalletBalance(gatewayUrl, token, accountId, true);
    const response = result.response;
    const body = result.body;
    const balance = response.status === 200 && body.data !== undefined && body.data.accountId !== undefined
      ? body.data
      : null;

    if (balance !== null) {
      return balance;
    }

    if (attempt < maxAttempts - 1) {
      const delaySeconds = response.status === 429
        ? Math.min(Math.max(sleepSeconds * Math.pow(2, attempt + 2), 2), 16)
        : sleepSeconds;

      sleep(delaySeconds + Math.random() * 0.5);
    }
  }

  return null;
}

/**
 * Backward-compatible wrapper for older scripts.
 */
export function getWallets(gatewayUrl, token, accountId) {
  if (!accountId) {
    return null;
  }

  const balance = getWalletBalance(gatewayUrl, token, accountId);
  return balance ? [Object.assign({ id: accountId }, balance)] : null;
}

export function listPockets(gatewayUrl, token) {
  const response = executeWithRateLimitRetry(() => http.get(`${gatewayUrl}/api/v1/wallets/pockets`, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-list' }
  }));

  const body = parseJson(response.body);
  const success = check(response, {
    'list pockets status is 200': (r) => r.status === 200,
    'list pockets returns array': () => Array.isArray(body.data)
  });

  if (!success) {
    logFailure('list pockets', response, body);
  }

  if (success) {
    return body.data;
  }

  return null;
}

/**
 * Create new pocket (Wallet/CREATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} pocketData - Pocket data
 * @returns {Object} Response with success status and pocket data
 */
export function createPocket(gatewayUrl, token, pocketData, recordChecks = true) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets`;
  const idempotencyKey = pocketData.idempotencyKey || createIdempotencyKey('pocket-create');

  const payload = JSON.stringify({
    name: pocketData.name,
    description: pocketData.description || '',
    currency: pocketData.currency || 'IDR'
  });

  const response = executeWithRateLimitRetry(() => http.post(url, payload, {
    headers: getAuthHeaders(token, { 'Idempotency-Key': idempotencyKey }),
    tags: { endpoint: 'pocket-create' }
  }));

  const body = parseJson(response.body);

  const success = response.status === 200 && body.data !== undefined && body.data.id !== undefined;

  if (recordChecks) {
    check(response, {
      'create pocket status is 200': (r) => r.status === 200,
      'create pocket returns id': () => body.data !== undefined && body.data.id !== undefined
    });
  }

  if (!success) {
    logFailure('create pocket', response, body);
  }

  return {
    success: success,
    response: response,
    body: body
  };
}

/**
 * Get pocket details (Pocket READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @returns {Object|null} Pocket data or null
 */
export function getPocket(gatewayUrl, token, pocketId, recordChecks = true) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}`;

  const response = executeWithRateLimitRetry(() => http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-read' }
  }));

  const body = parseJson(response.body);

  const success = response.status === 200 && body.data !== undefined && body.data.id !== undefined;

  if (recordChecks) {
    check(response, {
      'get pocket status is 200': (r) => r.status === 200,
      'get pocket returns data': () => body.data !== undefined && body.data.id !== undefined
    });
  }

  if (!success) {
    logFailure('get pocket', response, body);
  }

  if (success) {
    return body.data;
  }

  return null;
}

/**
 * Credit pocket (Pocket UPDATE - add funds)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @param {number} amount - Amount to credit
 * @returns {boolean} Success status
 */
export function creditPocket(gatewayUrl, token, pocketId, amount) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/credit`;
  const idempotencyKey = createIdempotencyKey('pocket-credit');

  const payload = JSON.stringify({
    amount: normalizeAmount(amount),
    referenceId: createIdempotencyKey('credit-ref')
  });

  const response = executeWithRateLimitRetry(() => http.post(url, payload, {
    headers: getAuthHeaders(token, { 'Idempotency-Key': idempotencyKey }),
    tags: { endpoint: 'pocket-credit' }
  }));

  const success = check(response, {
    'credit pocket status is 200': (r) => r.status === 200
  });

  if (!success) {
    logFailure('credit pocket', response, parseJson(response.body));
  }

  return success;
}

export function debitPocket(gatewayUrl, token, pocketId, amount) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/debit`;
  const idempotencyKey = createIdempotencyKey('pocket-debit');

  const payload = JSON.stringify({
    amount: normalizeAmount(amount),
    referenceId: createIdempotencyKey('debit-ref')
  });

  const response = executeWithRateLimitRetry(() => http.post(url, payload, {
    headers: getAuthHeaders(token, { 'Idempotency-Key': idempotencyKey }),
    tags: { endpoint: 'pocket-debit' }
  }));

  const success = check(response, {
    'debit pocket status is 200': (r) => r.status === 200
  });

  if (!success) {
    logFailure('debit pocket', response, parseJson(response.body));
  }

  return success;
}

export function freezePocket(gatewayUrl, token, pocketId) {
  const response = executeWithRateLimitRetry(() => http.post(`${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/freeze`, null, {
    headers: getAuthHeaders(token, { 'Idempotency-Key': createIdempotencyKey('pocket-freeze') }),
    tags: { endpoint: 'pocket-freeze' }
  }));

  const success = check(response, {
    'freeze pocket status is 200': (r) => r.status === 200
  });

  if (!success) {
    logFailure('freeze pocket', response, parseJson(response.body));
  }

  return success;
}

export function unfreezePocket(gatewayUrl, token, pocketId) {
  const response = executeWithRateLimitRetry(() => http.post(`${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/unfreeze`, null, {
    headers: getAuthHeaders(token, { 'Idempotency-Key': createIdempotencyKey('pocket-unfreeze') }),
    tags: { endpoint: 'pocket-unfreeze' }
  }));

  const success = check(response, {
    'unfreeze pocket status is 200': (r) => r.status === 200
  });

  if (!success) {
    logFailure('unfreeze pocket', response, parseJson(response.body));
  }

  return success;
}

/**
 * Update pocket status (freeze/unfreeze)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @param {string} status - 'FROZEN' or 'ACTIVE'
 * @returns {boolean} Success status
 */
export function updatePocketStatus(gatewayUrl, token, pocketId, status) {
  if (status === 'FROZEN') {
    return freezePocket(gatewayUrl, token, pocketId);
  }

  if (status === 'ACTIVE') {
    return unfreezePocket(gatewayUrl, token, pocketId);
  }

  return false;
}

/**
 * Close/delete pocket (Pocket DELETE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @returns {boolean} Success status
 */
export function closePocket(gatewayUrl, token, pocketId) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/close`;

  const response = executeWithRateLimitRetry(() => http.post(url, null, {
    headers: getAuthHeaders(token, { 'Idempotency-Key': createIdempotencyKey('pocket-close') }),
    tags: { endpoint: 'pocket-close' }
  }));

  const success = check(response, {
    'close pocket status is 200': (r) => r.status === 200
  });

  if (!success) {
    logFailure('close pocket', response, parseJson(response.body));
  }

  return success;
}

/**
 * Get pocket transactions
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @returns {Array|null} Transaction list or null
 */
export function getPocketTransactions(gatewayUrl, token, pocketId) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/transactions`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-transactions-read' }
  });

  const success = check(response, {
    'get pocket transactions status is 200': (r) => r.status === 200,
    'get pocket transactions returns array': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body) || Array.isArray(body.data) || Array.isArray(body.transactions);
      } catch (e) {
        return false;
      }
    }
  });

  if (success) {
    const body = JSON.parse(response.body);
    return body.transactions || body.data || body;
  }

  return null;
}
