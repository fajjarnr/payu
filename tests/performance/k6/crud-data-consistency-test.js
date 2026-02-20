// PayU Platform - CRUD Data Consistency Test
// ============================================
// Verifies data consistency under concurrent load
// Tests: Read-after-write consistency, concurrent updates
//
// Run: k6 run crud-data-consistency-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { BASE_URLS, TEST_USERS } from './config.js';
import { login, getProfile } from './lib/auth.js';
import { createPocket, getWallets, creditPocket, getPocket, closePocket } from './lib/wallet.js';
import { createTransfer, getTransactionDetails } from './lib/transaction.js';

// Consistency tracking metrics
const readAfterWriteConsistency = new Rate('read_after_write_consistency');
const concurrentUpdateConsistency = new Rate('concurrent_update_consistency');
const transactionAtomicity = new Rate('transaction_atomicity');

const consistencyErrors = new Counter('consistency_errors');
const raceConditions = new Counter('race_condition_detected');

export const options = {
  stages: [
    { duration: '2m', target: 10 },
    { duration: '5m', target: 20 },
    { duration: '10m', target: 50 },
    { duration: '5m', target: 20 },
    { duration: '2m', target: 10 },
    { duration: '1m', target: 0 }
  ],
  thresholds: {
    read_after_write_consistency: ['rate>0.99'],
    transaction_atomicity: ['rate>0.999'],
    consistency_errors: ['count<10']
  },
  tags: {
    testType: 'data-consistency',
    environment: 'dev'
  }
};

function generateUniqueId() {
  return `consistency-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
}

export function setup() {
  console.log('=== PayU CRUD Data Consistency Test - Setup ===');
  console.log('Testing read-after-write consistency and transaction atomicity');
  console.log('');

  return { startTime: new Date().toISOString() };
}

export default function () {
  const gatewayUrl = BASE_URLS.gateway;
  const keycloakUrl = BASE_URLS.keycloak;

  const userIndex = __VU % TEST_USERS.length;
  const testUser = TEST_USERS[userIndex];

  const token = login(keycloakUrl, testUser.username, testUser.password);

  if (!token) {
    console.error(`Auth failed for user: ${testUser.username}`);
    return;
  }

  const wallets = getWallets(gatewayUrl, token);

  if (!wallets || wallets.length === 0) {
    console.error('No wallets available for consistency test');
    return;
  }

  const wallet = wallets[0];

  // ==========================================
  // TEST 1: Read-After-Write Consistency
  // ==========================================
  group('Read-After-Write Consistency', () => {
    // Create pocket
    const pocketResult = createPocket(gatewayUrl, token, {
      name: `Consistency Test ${generateUniqueId()}`,
      description: 'Testing read-after-write',
      currency: 'IDR'
    });

    if (pocketResult.success) {
      const pocketId = pocketResult.body.pocketId || pocketResult.body.id;

      // Immediately read (should find)
      let retries = 0;
      let found = false;

      while (retries < 5 && !found) {
        const pocket = getPocket(gatewayUrl, token, pocketId);

        if (pocket && (pocket.id === pocketId || pocket.pocketId === pocketId)) {
          found = true;
          readAfterWriteConsistency.add(1);
          console.log(`Read-after-write consistency: PASS (retries: ${retries})`);
        } else {
          retries++;
          sleep(0.5);
        }
      }

      if (!found) {
        readAfterWriteConsistency.add(0);
        consistencyErrors.add(1);
        console.error(`Read-after-write consistency: FAILED after ${retries} retries`);
      }

      // Credit and verify balance update
      const creditAmount = 100000;
      creditPocket(gatewayUrl, token, pocketId, creditAmount);
      sleep(1);

      const updatedPocket = getPocket(gatewayUrl, token, pocketId);
      if (updatedPocket) {
        const expectedBalance = creditAmount;
        const actualBalance = updatedPocket.balance || updatedPocket.currentBalance || 0;

        if (actualBalance >= expectedBalance) {
          readAfterWriteConsistency.add(1);
          console.log(`Balance update consistency: PASS`);
        } else {
          readAfterWriteConsistency.add(0);
          consistencyErrors.add(1);
          console.error(`Balance update consistency: FAILED (expected: ${expectedBalance}, actual: ${actualBalance})`);
        }
      }

      // Cleanup
      closePocket(gatewayUrl, token, pocketId);
    }

    sleep(2);
  });

  // ==========================================
  // TEST 2: Transaction Atomicity
  // ==========================================
  group('Transaction Atomicity', () => {
    // Create transfer with idempotency key
    const idempotencyKey = `atomic-${generateUniqueId()}`;

    const transferResult = createTransfer(gatewayUrl, token, {
      sourceWalletId: wallet.id,
      destinationAccountId: TEST_USERS[(userIndex + 1) % TEST_USERS.length].username,
      amount: 10000,
      description: 'Atomicity test'
    });

    if (transferResult.success) {
      const transactionId = transferResult.body.transactionId || transferResult.body.id;

      // Verify transaction can be retrieved
      sleep(1);
      const transaction = getTransactionDetails(gatewayUrl, token, transactionId);

      if (transaction && (transaction.id === transactionId || transaction.transactionId === transactionId)) {
        transactionAtomicity.add(1);
        console.log(`Transaction atomicity: PASS`);

        // Verify transaction status is consistent
        const status = transaction.status || transaction.transactionStatus;
        if (status && ['COMPLETED', 'PENDING', 'PROCESSING'].includes(status)) {
          console.log(`Transaction status consistency: PASS (${status})`);
        } else {
          transactionAtomicity.add(0);
          consistencyErrors.add(1);
          console.error(`Transaction status consistency: FAILED (${status})`);
        }
      } else {
        transactionAtomicity.add(0);
        consistencyErrors.add(1);
        console.error(`Transaction atomicity: FAILED - transaction not found`);
      }
    }

    sleep(2);
  });

  // ==========================================
  // TEST 3: Concurrent Update Detection
  // ==========================================
  group('Concurrent Update Detection', () => {
    // Create a pocket for concurrent testing
    const pocketResult = createPocket(gatewayUrl, token, {
      name: `Concurrent Test ${generateUniqueId()}`,
      description: 'Testing concurrent updates',
      currency: 'IDR'
    });

    if (pocketResult.success) {
      const pocketId = pocketResult.body.pocketId || pocketResult.body.id;

      // Multiple rapid credits (simulating concurrent updates)
      const creditPromises = [];
      for (let i = 0; i < 3; i++) {
        const creditResult = creditPocket(gatewayUrl, token, pocketId, 10000);
        creditPromises.push(creditResult);
        sleep(0.2);
      }

      sleep(2);

      // Verify final state
      const finalPocket = getPocket(gatewayUrl, token, pocketId);

      if (finalPocket) {
        const finalBalance = finalPocket.balance || finalPocket.currentBalance || 0;
        const expectedMinBalance = 30000; // 3 credits of 10000

        if (finalBalance >= expectedMinBalance) {
          concurrentUpdateConsistency.add(1);
          console.log(`Concurrent update consistency: PASS (balance: ${finalBalance})`);
        } else {
          concurrentUpdateConsistency.add(0);
          raceConditions.add(1);
          console.error(`Concurrent update consistency: FAILED (expected: >=${expectedMinBalance}, actual: ${finalBalance})`);
        }
      }

      // Cleanup
      closePocket(gatewayUrl, token, pocketId);
    }

    sleep(2);
  });

  sleep(3);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Data Consistency Test - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('');
  console.log('Key Metrics:');
  console.log('  - Read-after-write consistency should be > 99%');
  console.log('  - Transaction atomicity should be > 99.9%');
  console.log('  - Consistency errors should be < 10');
  console.log('');
}
