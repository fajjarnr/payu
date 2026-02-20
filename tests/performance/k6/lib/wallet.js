// Wallet/Pocket CRUD Operations for K6 Tests
// ===========================================

import http from 'k6/http';
import { check } from 'k6';
import { getAuthHeaders } from './auth.js';

/**
 * Get wallet list (Wallet READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @returns {Array|null} Array of wallets or null
 */
export function getWallets(gatewayUrl, token) {
  const url = `${gatewayUrl}/api/v1/wallets`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'wallet-list' }
  });

  const success = check(response, {
    'list wallets status is 200': (r) => r.status === 200,
    'list wallets returns array': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body) || Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    }
  });

  if (success) {
    const body = JSON.parse(response.body);
    return body.data || body;
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
export function createPocket(gatewayUrl, token, pocketData) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets`;

  const payload = JSON.stringify({
    name: pocketData.name,
    description: pocketData.description || '',
    currency: pocketData.currency || 'IDR',
    targetAmount: pocketData.targetAmount || null
  });

  const response = http.post(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-create' }
  });

  const success = check(response, {
    'create pocket status is 201': (r) => r.status === 201,
    'create pocket returns id': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined || body.pocketId !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  return {
    success: success,
    response: response,
    body: JSON.parse(response.body || '{}')
  };
}

/**
 * Get pocket details (Pocket READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @returns {Object|null} Pocket data or null
 */
export function getPocket(gatewayUrl, token, pocketId) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-read' }
  });

  const success = check(response, {
    'get pocket status is 200': (r) => r.status === 200,
    'get pocket returns data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined || body.pocketId !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  if (success) {
    return JSON.parse(response.body);
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

  const payload = JSON.stringify({
    amount: amount,
    description: 'K6 Load Test Credit'
  });

  const response = http.post(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-credit' }
  });

  return check(response, {
    'credit pocket status is 200': (r) => r.status === 200,
    'credit pocket updates balance': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.balance !== undefined || body.success === true;
      } catch (e) {
        return false;
      }
    }
  });
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
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}/status`;

  const payload = JSON.stringify({
    status: status
  });

  const response = http.patch(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-update-status' }
  });

  return check(response, {
    'update pocket status is 200': (r) => r.status === 200
  });
}

/**
 * Close/delete pocket (Pocket DELETE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} pocketId - Pocket ID
 * @returns {boolean} Success status
 */
export function closePocket(gatewayUrl, token, pocketId) {
  const url = `${gatewayUrl}/api/v1/wallets/pockets/${pocketId}`;

  const response = http.del(url, null, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'pocket-delete' }
  });

  return check(response, {
    'close pocket status is 204 or 200': (r) => r.status === 204 || r.status === 200
  });
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
