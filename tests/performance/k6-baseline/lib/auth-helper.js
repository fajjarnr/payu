import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URLS, TEST_USERS, CHECKS } from '../config/baseline-config.js';

export function login(userIndex = 0) {
  const user = TEST_USERS[userIndex % TEST_USERS.length];

  const payload = {
    username: user.username,
    password: user.password
  };

  const randomIp = `13.104.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;
  const response = http.post(
    `${BASE_URLS.gateway}/api/v1/auth/login`,
    JSON.stringify(payload),
    { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': randomIp } }
  );

  const success = check(response, {
    'login: status is 200 or 202': (r) => r.status === 200 || r.status === 202,
    'login: response has token or mfaRequired': (r) => {
      try {
        const body = JSON.parse(r.body || '{}');
        const data = body.data || body;
        return !!(data.accessToken || data.access_token || data.mfa_required || data.mfaRequired || data.token);
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    console.error(`Login failed: ${response.status} - ${response.body}`);
    return null;
  }

  const parsedBody = JSON.parse(response.body);
  const body = parsedBody.data || parsedBody;

  // Handle MFA flow
  if (body.mfa_required || body.mfaRequired) {
    return verifyMFA(body.mfa_token || body.tempToken || body.sessionId, userIndex);
  }

  return {
    token: body.accessToken || body.access_token || body.token,
    refreshToken: body.refreshToken || body.refresh_token,
    userId: body.userId || body.id || (body.user && body.user.id)
  };
}

/**
 * Verify MFA (simplified - returns mock token for testing)
 */
export function verifyMFA(tempToken, userIndex = 0) {
  // For baseline testing, we use a mock approach
  // In real scenario, this would submit OTP
  const payload = {
    tempToken: tempToken,
    otpCode: '123456' // Mock OTP
  };

  const randomIp = `13.104.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;
  const response = http.post(
    `${BASE_URLS.gateway}/api/v1/auth/verify-mfa`,
    JSON.stringify(payload),
    { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': randomIp } }
  );

  const success = check(response, {
    'MFA verify: status 200': (r) => r.status === 200
  });

  if (!success) {
    console.warn('MFA verify failed, using temp token');
    return { token: tempToken, userId: `user-${userIndex}` };
  }

  const parsedBody = JSON.parse(response.body);
  const body = parsedBody.data || parsedBody;
  return {
    token: body.accessToken || body.access_token || body.token,
    refreshToken: body.refreshToken || body.refresh_token,
    userId: body.userId || (body.user && body.user.id)
  };
}

export function registerUser(uniqueId) {
  const payload = {
    username: `testuser_${uniqueId}`,
    email: `test_${uniqueId}@payu.test`,
    password: 'TestPassword123!',
    phoneNumber: `+6281${Math.floor(10000000 + Math.random() * 90000000)}`,
    nik: `3175${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    fullName: `Test User ${uniqueId}`
  };

  const randomIp = `13.104.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;
  const response = http.post(
    `${BASE_URLS.gateway}/api/v1/accounts/register`,
    JSON.stringify(payload),
    { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': randomIp } }
  );

  check(response, {
    'register: status 201 or 200': (r) => r.status === 201 || r.status === 200,
    'register: response has userId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.userId || body.id || body.accountId;
      } catch {
        return false;
      }
    }
  });

  try {
    const body = JSON.parse(response.body);
    return {
      userId: body.userId || body.id || body.accountId,
      username: payload.username,
      email: payload.email
    };
  } catch {
    return { userId: `mock-${uniqueId}`, username: payload.username };
  }
}

export function getAuthHeaders(token) {
  const randomIp = `13.104.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Forwarded-For': randomIp
  };
}

export function getProfile(token) {
  const response = http.get(
    `${BASE_URLS.gateway}/api/v1/accounts/profile`,
    { headers: getAuthHeaders(token) }
  );

  check(response, {
    'getProfile: status 200': (r) => r.status === 200
  });

  try {
    return JSON.parse(response.body);
  } catch {
    return {};
  }
}

export function refreshToken(refreshToken) {
  const randomIp = `13.104.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;
  const response = http.post(
    `${BASE_URLS.gateway}/api/v1/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': randomIp } }
  );

  check(response, {
    'refreshToken: status 200': (r) => r.status === 200
  });

  try {
    const body = JSON.parse(response.body);
    return {
      token: body.accessToken || body.token,
      refreshToken: body.refreshToken
    };
  } catch {
    return null;
  }
}

export function logout(token) {
  const response = http.post(
    `${BASE_URLS.gateway}/api/v1/auth/logout`,
    null,
    { headers: getAuthHeaders(token) }
  );

  check(response, {
    'logout: status 200 or 204': (r) => r.status === 200 || r.status === 204
  });
}
