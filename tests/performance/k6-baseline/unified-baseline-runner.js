// PayU Platform - Unified K6 Baseline Test Runner
// ==================================================
// This script runs baseline tests for multiple services
// Usage: k6 run unified-baseline-runner.js --env SERVICES=wallet,transaction

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';

// Import all service tests
import accountTest from './core-services/account-service-crud.js';
import authTest from './core-services/auth-service-crud.js';
import walletTest from './core-services/wallet-service-crud.js';
import transactionTest from './core-services/transaction-service-crud.js';

import investmentTest from './financial-services/investment-service-crud.js';
import lendingTest from './financial-services/lending-service-crud.js';
import fxTest from './financial-services/fx-service-crud.js';
import billingTest from './financial-services/billing-service-crud.js';
import statementTest from './financial-services/statement-service-crud.js';

import notificationTest from './supporting-services/notification-service-crud.js';
import partnerTest from './supporting-services/partner-service-crud.js';
import promotionTest from './supporting-services/promotion-service-crud.js';
import supportTest from './supporting-services/support-service-crud.js';
import complianceTest from './supporting-services/compliance-service-crud.js';
import backofficeTest from './supporting-services/backoffice-service-crud.js';
import cmsTest from './supporting-services/cms-service-crud.js';
import abTestingTest from './supporting-services/ab-testing-service-crud.js';
import apiPortalTest from './supporting-services/api-portal-service-crud.js';
import kycTest from './supporting-services/kyc-service-crud.js';
import analyticsTest from './supporting-services/analytics-service-crud.js';

const { BASELINE_THRESHOLDS, BASELINE_STAGES } = require('./config/baseline-config.js');

// Service registry
const SERVICES = {
  // Core Services
  account: accountTest,
  auth: authTest,
  wallet: walletTest,
  transaction: transactionTest,

  // Financial Services
  investment: investmentTest,
  lending: lendingTest,
  fx: fxTest,
  billing: billingTest,
  statement: statementTest,

  // Supporting Services
  notification: notificationTest,
  partner: partnerTest,
  promotion: promotionTest,
  support: supportTest,
  compliance: complianceTest,
  backoffice: backofficeTest,
  cms: cmsTest,
  abTesting: abTestingTest,
  apiPortal: apiPortalTest,
  kyc: kycTest,
  analytics: analyticsTest
};

// Parse environment variable for services to test
function getServicesToTest() {
  const envServices = __ENV.SERVICES;
  if (!envServices) {
    return Object.keys(SERVICES); // Test all by default
  }
  return envServices.split(',').map(s => s.trim());
}

// Get service group
function getServiceGroup(serviceName) {
  const core = ['account', 'auth', 'wallet', 'transaction'];
  const financial = ['investment', 'lending', 'fx', 'billing', 'statement'];

  if (core.includes(serviceName)) return 'core';
  if (financial.includes(serviceName)) return 'financial';
  return 'supporting';
}

export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    testType: 'unified-baseline',
    runner: 'unified'
  }
};

export function setup() {
  console.log('╔══════════════════════════════════════════════════════════════╗');
  console.log('║         PayU Platform - Unified K6 Baseline Runner           ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');
  console.log('');

  const servicesToTest = getServicesToTest();
  console.log(`Services to test: ${servicesToTest.join(', ')}`);
  console.log(`Total services: ${servicesToTest.length}`);
  console.log('');

  return {
    servicesToTest,
    startTime: Date.now()
  };
}

export default function (data) {
  const servicesToTest = data.servicesToTest;

  // Distribute services across VUs
  const serviceIndex = (__VU - 1) % servicesToTest.length;
  const serviceName = servicesToTest[serviceIndex];
  const test = SERVICES[serviceName];

  if (!test) {
    console.error(`Unknown service: ${serviceName}`);
    return;
  }

  group(`Service: ${serviceName} (${getServiceGroup(serviceName)})`, () => {
    // Call the service's default function
    try {
      test();
    } catch (error) {
      console.error(`Error in ${serviceName} test: ${error.message}`);
    }
  });

  sleep(1);
}

export function teardown(data) {
  console.log('');
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('                    BASELINE TESTS COMPLETE');
  console.log('═══════════════════════════════════════════════════════════════');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
  console.log(`Services tested: ${data.servicesToTest.length}`);
  console.log('');
}
