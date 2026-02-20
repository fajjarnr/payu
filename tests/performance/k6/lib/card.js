// Card CRUD Operations for K6 Tests
// ==================================

import http from 'k6/http';
import { check } from 'k6';
import { getAuthHeaders } from './auth.js';

/**
 * Get card list (Card READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @returns {Array|null} Array of cards or null
 */
export function getCards(gatewayUrl, token) {
  const url = `${gatewayUrl}/api/v1/cards`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-list' }
  });

  const success = check(response, {
    'list cards status is 200': (r) => r.status === 200,
    'list cards returns array': (r) => {
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
 * Create virtual card (Card CREATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} cardData - Card creation data
 * @returns {Object} Response with success status and card data
 */
export function createVirtualCard(gatewayUrl, token, cardData) {
  const url = `${gatewayUrl}/api/v1/cards/virtual`;

  const payload = JSON.stringify({
    cardHolderName: cardData.cardHolderName,
    dailyLimit: cardData.dailyLimit || 10000000,
    monthlyLimit: cardData.monthlyLimit || 100000000,
    walletId: cardData.walletId
  });

  const response = http.post(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-create' }
  });

  const success = check(response, {
    'create card status is 201': (r) => r.status === 201,
    'create card returns id': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.cardId !== undefined || body.id !== undefined;
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
 * Get card details (Card READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} cardId - Card ID
 * @returns {Object|null} Card details or null
 */
export function getCardDetails(gatewayUrl, token, cardId) {
  const url = `${gatewayUrl}/api/v1/cards/${cardId}`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-read' }
  });

  const success = check(response, {
    'get card details status is 200': (r) => r.status === 200,
    'get card details returns data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.cardId !== undefined || body.id !== undefined;
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
 * Freeze card (Card UPDATE - status)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} cardId - Card ID
 * @returns {boolean} Success status
 */
export function freezeCard(gatewayUrl, token, cardId) {
  const url = `${gatewayUrl}/api/v1/cards/${cardId}/freeze`;

  const response = http.post(url, null, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-freeze' }
  });

  return check(response, {
    'freeze card status is 200': (r) => r.status === 200
  });
}

/**
 * Unfreeze card (Card UPDATE - status)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} cardId - Card ID
 * @returns {boolean} Success status
 */
export function unfreezeCard(gatewayUrl, token, cardId) {
  const url = `${gatewayUrl}/api/v1/cards/${cardId}/unfreeze`;

  const response = http.post(url, null, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-unfreeze' }
  });

  return check(response, {
    'unfreeze card status is 200': (r) => r.status === 200
  });
}

/**
 * Update card limits (Card UPDATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} cardId - Card ID
 * @param {Object} limits - New limits { dailyLimit, monthlyLimit }
 * @returns {boolean} Success status
 */
export function updateCardLimits(gatewayUrl, token, cardId, limits) {
  const url = `${gatewayUrl}/api/v1/cards/${cardId}/limits`;

  const payload = JSON.stringify({
    dailyLimit: limits.dailyLimit,
    monthlyLimit: limits.monthlyLimit
  });

  const response = http.patch(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-update-limits' }
  });

  return check(response, {
    'update card limits status is 200': (r) => r.status === 200
  });
}

/**
 * Get card transactions
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {string} cardId - Card ID
 * @returns {Array|null} Transaction list or null
 */
export function getCardTransactions(gatewayUrl, token, cardId) {
  const url = `${gatewayUrl}/api/v1/cards/${cardId}/transactions`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'card-transactions' }
  });

  const success = check(response, {
    'get card transactions status is 200': (r) => r.status === 200,
    'get card transactions returns array': (r) => {
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
