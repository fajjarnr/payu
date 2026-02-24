// PayU Auth Service - CRUD Baseline Performance Test
// ===================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, registerUser, logout, refreshToken, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read } from '../lib/crud-helper.js';

// Service-specific metrics
const authMetrics = {
  loginDuration: new Trend('auth_login_duration'),
  registerDuration: new Trend('auth_register_duration'),
  logoutDuration: new Trend('auth_logout_duration'),
  refreshDuration: new Trend('auth_refresh_duration'),
  mfaVerifyDuration: new Trend('auth_mfa_verify_duration'),
  passwordResetDuration: new Trend('auth_password_reset_duration'),
  sessionCheckDuration: new Trend('auth_session_check_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'auth-service',
    testType: 'baseline-crud'
  }
};

// Test data generators
function generateUserData(uniqueId) {
  return {
    username: `auth_test_${uniqueId}`,
    email: `auth_${uniqueId}@payu.test`,
    password: 'AuthTestPass123!',
    phoneNumber: `+6281${Math.floor(10000000 + Math.random() * 90000000)}`,
    nik: `3175${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    fullName: `Auth Test User ${uniqueId}`
  };
}

// Main test scenario
export default function () {
  const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;

  group('Auth Service - CRUD Operations', () => {

    // ===== CREATE: Register new user =====
    group('CREATE: Register', () => {
      const userData = generateUserData(uniqueId);

      const startTime = Date.now();
      const result = create(SERVICE_ENDPOINTS.auth + '/register', userData, null);
      authMetrics.registerDuration.add(Date.now() - startTime);

      if (result.success) {
        console.log(`User registered: ${result.body.userId || result.body.id}`);
      }

      sleep(0.3);
    });

    // ===== CREATE: Login =====
    let auth = null;
    group('CREATE: Login', () => {
      const startTime = Date.now();
      auth = login(__VU % 5);
      authMetrics.loginDuration.add(Date.now() - startTime);

      if (auth && auth.token) {
        console.log(`Login successful, token received`);
      }

      sleep(0.3);
    });

    if (!auth || !auth.token) {
      console.error('Login failed, skipping authenticated operations');
      return;
    }

    // ===== READ: Verify session/token =====
    group('READ: Verify Session', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.auth}/verify`, auth.token);
      authMetrics.sessionCheckDuration.add(Date.now() - startTime);

      sleep(0.2);
    });

    // ===== READ: Get user info =====
    group('READ: Get User Info', () => {
      const result = read(`${SERVICE_ENDPOINTS.auth}/me`, auth.token);

      sleep(0.2);
    });

    // ===== CREATE: Request password reset =====
    group('CREATE: Password Reset Request', () => {
      const resetData = {
        email: `auth_${uniqueId}@payu.test`,
        username: `auth_test_${uniqueId}`
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.auth}/password-reset`, resetData, null);
      authMetrics.passwordResetDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== UPDATE: Refresh token =====
    if (auth.refreshToken) {
      group('UPDATE: Refresh Token', () => {
        const startTime = Date.now();
        const newTokens = refreshToken(auth.refreshToken);
        authMetrics.refreshDuration.add(Date.now() - startTime);

        if (newTokens) {
          auth.token = newTokens.token;
        }

        sleep(0.2);
      });
    }

    // ===== DELETE: Logout =====
    group('DELETE: Logout', () => {
      const startTime = Date.now();
      logout(auth.token);
      authMetrics.logoutDuration.add(Date.now() - startTime);

      sleep(0.2);
    });

  });

  sleep(0.5);
}

// Setup function
export function setup() {
  console.log('Auth Service CRUD Baseline Test - Starting');
  console.log('===========================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.auth}/health`);
  console.log(`Auth Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'auth-service-crud'
  };
}

// Teardown function
export function teardown(data) {
  console.log('\n===========================================');
  console.log('Auth Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
