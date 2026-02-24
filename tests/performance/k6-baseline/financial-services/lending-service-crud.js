// PayU Lending Service - CRUD Baseline Performance Test
// ======================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const lendingMetrics = {
  applyLoanDuration: new Trend('lending_apply_loan_duration'),
  getLoanDuration: new Trend('lending_get_loan_duration'),
  listLoansDuration: new Trend('lending_list_loans_duration'),
  calculateEMIDuration: new Trend('lending_calculate_emi_duration'),
  makePaymentDuration: new Trend('lending_make_payment_duration'),
  checkEligibilityDuration: new Trend('lending_check_eligibility_duration'),
  updateLoanStatusDuration: new Trend('lending_update_status_duration'),
  getScheduleDuration: new Trend('lending_get_schedule_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'lending-service',
    testType: 'baseline-crud'
  }
};

// Test data generators
function generateLoanApplicationData(uniqueId) {
  const loanTypes = ['PERSONAL', 'BUSINESS', 'PAYLATER', 'EMERGENCY'];
  return {
    loanType: loanTypes[Math.floor(Math.random() * loanTypes.length)],
    amount: Math.floor(5000000 + Math.random() * 45000000),
    tenure: [6, 12, 24, 36][Math.floor(Math.random() * 4)],
    purpose: 'Business expansion',
    employmentType: 'SALARIED',
    monthlyIncome: Math.floor(5000000 + Math.random() * 15000000)
  };
}

function generatePaymentData() {
  return {
    amount: Math.floor(500000 + Math.random() * 1000000),
    paymentMethod: 'WALLET',
    notes: 'Monthly installment payment'
  };
}

// Main test scenario
export default function () {
  const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;

  const auth = login(__VU % 5);
  if (!auth || !auth.token) {
    console.error('Login failed, skipping test');
    return;
  }

  let loanId = null;

  group('Lending Service - CRUD Operations', () => {

    // ===== READ: Check Eligibility =====
    group('READ: Check Eligibility', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.lending}/eligibility`, auth.token);
      lendingMetrics.checkEligibilityDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Calculate EMI =====
    group('CREATE: Calculate EMI', () => {
      const emiData = {
        amount: 10000000,
        tenure: 12,
        interestRate: 10.5
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.lending}/calculate-emi`, emiData, auth.token);
      lendingMetrics.calculateEMIDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Apply for Loan =====
    group('CREATE: Apply Loan', () => {
      const loanData = generateLoanApplicationData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.lending}/applications`, loanData, auth.token);
      lendingMetrics.applyLoanDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        loanId = result.body.loanId || result.body.id;
        console.log(`Loan application created: ${loanId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Loans =====
    group('READ: List Loans', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.lending}/loans`, { page: 0, size: 10 }, auth.token);
      lendingMetrics.listLoansDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (loanId) {
      // ===== READ: Get Loan Detail =====
      group('READ: Get Loan', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.lending}/loans/${loanId}`, auth.token);
        lendingMetrics.getLoanDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== READ: Get Repayment Schedule =====
      group('READ: Get Schedule', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.lending}/loans/${loanId}/schedule`, auth.token);
        lendingMetrics.getScheduleDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Make Payment =====
      group('CREATE: Make Payment', () => {
        const paymentData = generatePaymentData();

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.lending}/loans/${loanId}/payments`, paymentData, auth.token);
        lendingMetrics.makePaymentDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== UPDATE: Update Loan Status =====
      group('UPDATE: Update Status', () => {
        const statusData = {
          status: 'ACTIVE',
          remarks: 'Loan approved'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.lending}/loans/${loanId}/status`, statusData, auth.token);
        lendingMetrics.updateLoanStatusDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Lending Service CRUD Baseline Test - Starting');
  console.log('==============================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.lending}/health`);
  console.log(`Lending Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'lending-service-crud'
  };
}

export function teardown(data) {
  console.log('\n==============================================');
  console.log('Lending Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
