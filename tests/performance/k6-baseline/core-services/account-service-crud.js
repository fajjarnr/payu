// PayU Account Service - CRUD Baseline Performance Test
// ======================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS } from '../config/baseline-config.js';
import { login, registerUser, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, del, list } from '../lib/crud-helper.js';

// Service-specific metrics
const accountMetrics = {
  createProfileDuration: new Trend('account_create_profile_duration'),
  getProfileDuration: new Trend('account_get_profile_duration'),
  updateProfileDuration: new Trend('account_update_profile_duration'),
  listAccountsDuration: new Trend('account_list_duration'),
  kycSubmitDuration: new Trend('account_kyc_submit_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'account-service',
    testType: 'baseline-crud'
  }
};

// Test data generators
function generateUserData(uniqueId) {
  return {
    username: `baseline_user_${uniqueId}`,
    email: `baseline_${uniqueId}@payu.test`,
    password: 'BaselinePass123!',
    phoneNumber: `+6281${Math.floor(10000000 + Math.random() * 90000000)}`,
    nik: `3175${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    fullName: `Baseline User ${uniqueId}`,
    dateOfBirth: '1990-01-01',
    address: {
      street: 'Jl. Baseline Test',
      city: 'Jakarta',
      postalCode: '12345',
      country: 'ID'
    }
  };
}

function generateKycData() {
  return {
    idType: 'KTP',
    idNumber: `3175${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    idPhotoUrl: 'https://storage.payu.id/kyc/id-photo.jpg',
    selfiePhotoUrl: 'https://storage.payu.id/kyc/selfie.jpg'
  };
}

function generateUpdateData() {
  return {
    fullName: `Updated User ${Date.now()}`,
    phoneNumber: `+6282${Math.floor(10000000 + Math.random() * 90000000)}`,
    address: {
      street: 'Jl. Updated Address',
      city: 'Bandung',
      postalCode: '40123',
      country: 'ID'
    }
  };
}

// Main test scenario
export default function () {
  const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;

  group('Account Service - CRUD Operations', () => {

    // ===== CREATE: Register new account =====
    group('CREATE: Register Account', () => {
      const userData = generateUserData(uniqueId);

      const startTime = Date.now();
      const result = create(SERVICE_ENDPOINTS.account, userData, null);
      accountMetrics.createProfileDuration.add(Date.now() - startTime);

      if (result.success) {
        console.log(`Account created: ${result.body.userId || result.body.id}`);
      }

      sleep(0.5);
    });

    // Login untuk operasi selanjutnya
    const auth = login(__VU % 5);
    if (!auth || !auth.token) {
      console.error('Login failed, skipping authenticated operations');
      return;
    }

    // ===== READ: Get profile =====
    group('READ: Get Profile', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.account}/profile`, auth.token);
      accountMetrics.getProfileDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: List accounts =====
    group('READ: List Accounts', () => {
      const startTime = Date.now();
      const result = list(SERVICE_ENDPOINTS.account, { page: 0, size: 10 }, auth.token);
      accountMetrics.listAccountsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== UPDATE: Update profile =====
    group('UPDATE: Update Profile', () => {
      const updateData = generateUpdateData();

      const startTime = Date.now();
      const result = update(`${SERVICE_ENDPOINTS.account}/profile`, updateData, auth.token);
      accountMetrics.updateProfileDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Submit KYC =====
    group('CREATE: Submit KYC', () => {
      const kycData = generateKycData();

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.account}/kyc`, kycData, auth.token);
      accountMetrics.kycSubmitDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== READ: Get KYC status =====
    group('READ: Get KYC Status', () => {
      const result = read(`${SERVICE_ENDPOINTS.account}/kyc/status`, auth.token);

      sleep(0.3);
    });

  });

  sleep(1);
}

// Setup function - runs once at test start
export function setup() {
  console.log('Account Service CRUD Baseline Test - Starting');
  console.log('============================================');

  // Verify service health
  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.account}/health`);
  console.log(`Account Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'account-service-crud'
  };
}

// Teardown function - runs once at test end
export function teardown(data) {
  console.log('\n============================================');
  console.log('Account Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
