// Authentication Utilities for K6 Tests
// ======================================

import http from 'k6/http';
import { check } from 'k6';

/**
 * Login and obtain access token
 * @param {string} keycloakUrl - Keycloak base URL
 * @param {string} username - Username
 * @param {string} password - Password
 * @returns {string|null} Access token or null if failed
 */
export function login(keycloakUrl, username, password) {
  const loginUrl = `${keycloakUrl}/auth/realms/payu/protocol/openid-connect/token`;

  const payload = {
    grant_type: 'password',
    client_id: 'payu-backend',
    username: username,
    password: password
  };

  const response = http.post(loginUrl, payload, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    tags: { endpoint: 'auth-login' }
  });

  const success = check(response, {
    'login status is 200': (r) => r.status === 200,
    'login returns access_token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.access_token !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  if (success) {
    const body = JSON.parse(response.body);
    return body.access_token;
  }

  return null;
}

/**
 * Get auth headers with bearer token
 * @param {string} token - Access token
 * @returns {Object} Headers object
 */
export function getAuthHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
}

/**
 * Register new user (Account CREATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {Object} userData - User registration data
 * @returns {Object} Response object with success status and user data
 */
export function registerUser(gatewayUrl, userData) {
  const url = `${gatewayUrl}/api/v1/auth/register`;

  const payload = JSON.stringify({
    username: userData.username,
    email: userData.email,
    password: userData.password,
    fullName: userData.fullName,
    phoneNumber: userData.phoneNumber,
    nik: userData.nik
  });

  const response = http.post(url, payload, {
    headers: {
      'Content-Type': 'application/json'
    },
    tags: { endpoint: 'account-create' }
  });

  const success = check(response, {
    'register status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    'register returns user data or error': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined || body.error !== undefined || body.message !== undefined;
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
 * Get user profile (Account READ)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @returns {Object} Profile data or null
 */
export function getProfile(gatewayUrl, token) {
  const url = `${gatewayUrl}/api/v1/accounts/me`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'account-read' }
  });

  const success = check(response, {
    'get profile status is 200': (r) => r.status === 200,
    'get profile returns data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined || body.username !== undefined;
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
 * Update user profile (Account UPDATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} updateData - Data to update
 * @returns {boolean} Success status
 */
export function updateProfile(gatewayUrl, token, updateData) {
  const url = `${gatewayUrl}/api/v1/accounts/me`;

  const payload = JSON.stringify(updateData);

  const response = http.put(url, payload, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'account-update' }
  });

  return check(response, {
    'update profile status is 200': (r) => r.status === 200
  });
}
