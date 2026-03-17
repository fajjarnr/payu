// Transaction CRUD Operations for K6 Tests
// =========================================

import http from 'k6/http';
import { check } from 'k6';
import { getAuthHeaders } from './auth.js';

/**
 * Create transfer transaction (Transaction CREATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} transferData - Transfer data
 * @returns {Object} Response with success status and transaction data
 */
export function createTransfer(gatewayUrl, token, transferData) {
  const url = `${gatewayUrl}/api/v1/transactions/transfer`;

  const payload = JSON.stringify({
    sourceAccountId: transferData.sourceAccountId,
    destinationAccountId: transferData.destinationAccountId,
    amount: transferData.amount,
    description: transferData.description || 'K6 Load Test Transfer',
    idempotencyKey: `k6-${Date.now()}-${Math.random().toString(36).substring(7)}`
  });

  const response = http.post(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-transfer-create' }
  });

  const success = check(response, {
    'transfer status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    'transfer returns transaction id': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.transactionId !== undefined || body.id !== undefined;
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
 * Create QRIS payment (Transaction CREATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} qrisData - QRIS payment data
 * @returns {Object} Response with success status
 */
export function createQrisPayment(gatewayUrl, token, qrisData) {
  const url = `${gatewayUrl}/api/v1/transactions/qris/pay`;

  const payload = JSON.stringify({
    qrCode: qrisData.qrCode,
    walletId: qrisData.walletId,
    amount: qrisData.amount,
    idempotencyKey: `k6-qris-${Date.now()}-${Math.random().toString(36).substring(7)}`
  });

  const response = http.post(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-qris-create' }
  });

  const success = check(response, {
    'qris payment status is 201 or 200': (r) => r.status === 201 || r.status === 200
  });

  return {
    success: success,
    response: response,
    body: JSON.parse(response.body || '{}')
  };
}

/**
 * Get transaction history (Transaction READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} params - Query params (page, size, startDate, endDate)
 * @returns {Object|null} Transaction list or null
 */
export function getTransactionHistory(gatewayUrl, token, params = {}) {
  const queryParams = new URLSearchParams();
  if (params.page) queryParams.append('page', params.page);
  if (params.size) queryParams.append('size', params.size);
  if (params.startDate) queryParams.append('startDate', params.startDate);
  if (params.endDate) queryParams.append('endDate', params.endDate);
  if (params.type) queryParams.append('type', params.type);

  const url = `${gatewayUrl}/api/v1/transactions?${queryParams.toString()}`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-list' }
  });

  const success = check(response, {
    'get transaction history status is 200': (r) => r.status === 200,
    'get transaction history returns data': (r) => {
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
    return {
      transactions: body.transactions || body.data || body,
      total: body.total || body.totalElements || (body.transactions || body.data || body).length
    };
  }

  return null;
}

/**
 * Get transaction details (Transaction READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} transactionId - Transaction ID
 * @returns {Object|null} Transaction details or null
 */
export function getTransactionDetails(gatewayUrl, token, transactionId) {
  const url = `${gatewayUrl}/api/v1/transactions/${transactionId}`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-read' }
  });

  const success = check(response, {
    'get transaction details status is 200': (r) => r.status === 200,
    'get transaction details returns data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.transactionId !== undefined || body.id !== undefined;
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
 * Cancel pending transaction (Transaction DELETE/Cancel)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} transactionId - Transaction ID
 * @returns {boolean} Success status
 */
export function cancelTransaction(gatewayUrl, token, transactionId) {
  const url = `${gatewayUrl}/api/v1/transactions/${transactionId}/cancel`;

  const response = http.post(url, null, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-cancel' }
  });

  return check(response, {
    'cancel transaction status is 200 or 204': (r) => r.status === 200 || r.status === 204
  });
}

/**
 * Add transaction note (Transaction UPDATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} transactionId - Transaction ID
 * @param {string} note - Note text
 * @returns {boolean} Success status
 */
export function addTransactionNote(gatewayUrl, token, transactionId, note) {
  const url = `${gatewayUrl}/api/v1/transactions/${transactionId}/note`;

  const payload = JSON.stringify({
    note: note
  });

  const response = http.patch(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-update-note' }
  });

  return check(response, {
    'add transaction note status is 200': (r) => r.status === 200
  });
}

/**
 * Get transfer receipt/confirmation
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} transactionId - Transaction ID
 * @returns {Object|null} Receipt data or null
 */
export function getTransferReceipt(gatewayUrl, token, transactionId) {
  const url = `${gatewayUrl}/api/v1/transactions/${transactionId}/receipt`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'transaction-receipt' }
  });

  const success = check(response, {
    'get receipt status is 200': (r) => r.status === 200
  });

  if (success) {
    return JSON.parse(response.body);
  }

  return null;
}
