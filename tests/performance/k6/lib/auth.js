// Authentication Utilities for K6 Tests
// ======================================

import { sleep } from 'k6';
import http from 'k6/http';
import { check } from 'k6';

const sessionRetryAttempts = Number(__ENV.K6_SESSION_RETRY_ATTEMPTS || 10);
const sessionRetrySleepSeconds = Number(__ENV.K6_SESSION_RETRY_SLEEP_SECONDS || 0.5);

function parseJson(body) {
  try {
    return JSON.parse(body || '{}');
  } catch (error) {
    return {};
  }
}

function getSyntheticForwardedFor() {
  const vu = Math.max(1, typeof __VU === 'number' ? __VU : 1);
  const thirdOctet = Math.floor((vu - 1) / 250) % 250;
  const fourthOctet = ((vu - 1) % 250) + 1;

  return `10.200.${thirdOctet}.${fourthOctet}`;
}

function getOptionalTestHeaders() {
  if (!__ENV.K6_E2E_TEST_HEADER) {
    return {};
  }

  return {
    'X-E2E-Test': __ENV.K6_E2E_TEST_HEADER
  };
}

function uniqueDigits() {
  const vu = typeof __VU === 'number' ? __VU : 0;
  const iteration = typeof __ITER === 'number' ? __ITER : 0;
  return `${Date.now()}${vu}${iteration}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`;
}

export function generateUserData(prefix = 'k6user') {
  const digits = uniqueDigits();
  const username = `${prefix}${digits}`.slice(0, 32);

  return {
    externalId: `ext-${digits}`,
    username: username,
    email: `${username}@test.com`,
    password: __ENV.K6_TEST_PASSWORD || 'TestPassword123!',
    fullName: `K6 User ${digits}`,
    phoneNumber: `081${digits.slice(-9).padStart(9, '0')}`,
    nik: `3${digits.slice(-15).padStart(15, '7')}`
  };
}

/**
 * Login and obtain access token
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} username - Username
 * @param {string} password - Password
 * @returns {string|null} Access token or null if failed
 */
export function login(gatewayUrl, username, password) {
  const loginUrl = `${gatewayUrl}/api/v1/auth/login`;

  const response = http.post(loginUrl, JSON.stringify({ username, password }), {
    headers: Object.assign({
      'Content-Type': 'application/json',
      'X-Forwarded-For': getSyntheticForwardedFor()
    }, getOptionalTestHeaders()),
    tags: { endpoint: 'auth-login' }
  });

  const body = parseJson(response.body);

  const success = check(response, {
    'login status is 200': (r) => r.status === 200,
    'login returns access token': () => body.data !== undefined && body.data.access_token !== undefined
  });

  if (success) {
    return body.data.access_token;
  }

  return null;
}

export function loginWithRetry(gatewayUrl, username, password, attempts = sessionRetryAttempts, sleepSeconds = sessionRetrySleepSeconds) {
  for (let attempt = 0; attempt < attempts; attempt++) {
    const token = login(gatewayUrl, username, password);
    if (token) {
      return token;
    }

    if (attempt < attempts - 1) {
      sleep(sleepSeconds);
    }
  }

  return null;
}

/**
 * Get auth headers with bearer token
 * @param {string} token - Access token
 * @param {Object} extraHeaders - Extra headers
 * @returns {Object} Headers object
 */
export function getAuthHeaders(token, extraHeaders = {}) {
  return Object.assign({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
    'X-Forwarded-For': getSyntheticForwardedFor()
  }, getOptionalTestHeaders(), extraHeaders);
}

/**
 * Register new user (Account CREATE)
 * @param {string} gatewayUrl - Gateway base URL
 * @param {Object} userData - User registration data
 * @returns {Object} Response object with success status and user data
 */
export function registerUser(gatewayUrl, userData) {
  const url = `${gatewayUrl}/api/v1/accounts/register`;

  const payload = JSON.stringify({
    externalId: userData.externalId,
    username: userData.username,
    email: userData.email,
    fullName: userData.fullName,
    phoneNumber: userData.phoneNumber,
    nik: userData.nik,
    password: userData.password
  });

  const response = http.post(url, payload, {
    headers: Object.assign({
      'Content-Type': 'application/json',
      'X-Forwarded-For': getSyntheticForwardedFor()
    }, getOptionalTestHeaders()),
    tags: { endpoint: 'account-register' }
  });

  const body = parseJson(response.body);

  const success = check(response, {
    'register status is 200': (r) => r.status === 200,
    'register returns account data': () => body.id !== undefined && body.externalId !== undefined
  });

  return {
    success: success,
    response: response,
    body: body
  };
}

/**
 * Validate current session and return auth metadata
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @returns {Object|null} Session data or null
 */
export function validateSession(gatewayUrl, token) {
  const url = `${gatewayUrl}/api/v1/auth/validate`;

  const response = http.get(url, {
    headers: getAuthHeaders(token),
    tags: { endpoint: 'auth-validate' }
  });

  const body = parseJson(response.body);

  const success = check(response, {
    'validate session status is 200': (r) => r.status === 200,
    'validate session returns user id': () => body.data !== undefined && body.data.user_id !== undefined
  });

  if (success) {
    return body.data;
  }

  return null;
}

export function validateSessionWithRetry(gatewayUrl, token, attempts = sessionRetryAttempts, sleepSeconds = sessionRetrySleepSeconds) {
  for (let attempt = 0; attempt < attempts; attempt++) {
    const session = validateSession(gatewayUrl, token);
    if (session) {
      return session;
    }

    if (attempt < attempts - 1) {
      sleep(sleepSeconds);
    }
  }

  return null;
}

/**
 * Backward-compatible alias for older k6 scripts.
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @returns {Object|null} Session data or null
 */
export function getProfile(gatewayUrl, token) {
  return validateSession(gatewayUrl, token);
}

/**
 * This endpoint is not part of the verified dev-cluster CRUD contract.
 * @param {string} gatewayUrl - Gateway base URL
 * @param {string} token - Access token
 * @param {Object} updateData - Data to update
 * @returns {boolean} Success status
 */
export function updateProfile(gatewayUrl, token, updateData) {
  return false;
}

export function createOnboardedSession(gatewayUrl, userData = generateUserData()) {
  const registration = registerUser(gatewayUrl, userData);
  if (!registration.success) {
    return { success: false, userData, registerResult: registration };
  }

  const token = loginWithRetry(gatewayUrl, userData.username, userData.password);
  if (!token) {
    return { success: false, userData, registerResult: registration };
  }

  const session = validateSessionWithRetry(gatewayUrl, token);
  if (!session) {
    return { success: false, userData, registerResult: registration, token };
  }

  return {
    success: true,
    token: token,
    accountId: session.user_id,
    userData: userData,
    registerResult: registration,
    session: session,
    refreshedAt: Date.now()
  };
}

export function refreshSession(gatewayUrl, currentSession) {
  const token = loginWithRetry(gatewayUrl, currentSession.userData.username, currentSession.userData.password);
  if (!token) {
    return Object.assign({}, currentSession, { success: false });
  }

  const session = validateSessionWithRetry(gatewayUrl, token);
  if (!session) {
    return Object.assign({}, currentSession, { success: false, token: token });
  }

  return Object.assign({}, currentSession, {
    success: true,
    token: token,
    accountId: session.user_id,
    session: session,
    refreshedAt: Date.now()
  });
}
