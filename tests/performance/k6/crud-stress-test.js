// PayU Platform - CRUD Stress Test
// ==================================
// Breaking point analysis with full CRUD operations
// Progressive load up to 1000 users
//
// Run: k6 run crud-stress-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URLS, THRESHOLDS, LOAD_STAGES, TEST_USERS } from './config.js';
import { login, getProfile } from './lib/auth.js';
import { createPocket, getWallets, creditPocket, closePocket } from './lib/wallet.js';
import { createTransfer, getTransactionHistory } from './lib/transaction.js';
import { createVirtualCard, getCards } from './lib/card.js';

// Relaxed thresholds for stress testing
const stressThresholds = {
  http_req_duration: [
    { threshold: 'p(95)<5000', abortOnFail: false },
    { threshold: 'p(99)<10000', abortOnFail: false }
  ],
  http_req_failed: ['rate<0.50'],
  crud_create_success: ['rate>0.80'],
  crud_read_success: ['rate>0.90']
};

// Custom metrics
const crudCreateSuccess = new Rate('crud_create_success');
const crudReadSuccess = new Rate('crud_read_success');
const dataInconsistencyCounter = new Counter('data_inconsistency_errors');

const activeUsers = new Counter('active_users');
const concurrentOperations = new Counter('concurrent_operations');

export const options = {
  stages: LOAD_STAGES.stress,
  thresholds: stressThresholds,
  tags: {
    testType: 'crud-stress',
    environment: 'dev'
  }
};

function generateUniqueId() {
  return `stress-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
}

export function setup() {
  console.log('=== PayU CRUD Stress Test - Setup ===');
  console.log('WARNING: This test will push the system to its breaking point!');
  console.log(`Max virtual users: 1000`);
  console.log(`Duration: ~40 minutes`);
  console.log('');

  return {
    startTime: new Date().toISOString(),
    testType: 'stress'
  };
}

export default function () {
  const gatewayUrl = BASE_URLS.gateway;
  const keycloakUrl = BASE_URLS.keycloak;

  activeUsers.add(1);

  // Rotate through test users
  const userIndex = __VU % TEST_USERS.length;
  const testUser = TEST_USERS[userIndex];

  // Authenticate
  const token = login(keycloakUrl, testUser.username, testUser.password);

  if (!token) {
    console.error(`Auth failed for user: ${testUser.username}`);
    return;
  }

  concurrentOperations.add(1);

  // ==========================================
  // MIXED CRUD OPERATIONS UNDER STRESS
  // ==========================================

  // Weighted operations based on real usage patterns
  const operation = Math.random();

  if (operation < 0.40) {
    // 40% - Read operations (most common)
    group('Stress: Read Operations', () => {
      const profile = getProfile(gatewayUrl, token);
      crudReadSuccess.add(profile !== null);

      if (profile) {
        const wallets = getWallets(gatewayUrl, token);
        crudReadSuccess.add(wallets !== null);

        const history = getTransactionHistory(gatewayUrl, token, { page: 0, size: 20 });
        crudReadSuccess.add(history !== null);
      }
    });

  } else if (operation < 0.65) {
    // 25% - Create operations (pockets)
    group('Stress: Create Pockets', () => {
      const wallets = getWallets(gatewayUrl, token);

      if (wallets && wallets.length > 0) {
        const pocketResult = createPocket(gatewayUrl, token, {
          name: `Stress Pocket ${generateUniqueId()}`,
          description: 'Stress test pocket',
          currency: 'IDR'
        });
        crudCreateSuccess.add(pocketResult.success);

        if (pocketResult.success) {
          const pocketId = pocketResult.body.pocketId || pocketResult.body.id;

          // Credit and immediately close
          creditPocket(gatewayUrl, token, pocketId, 50000);
          sleep(0.5);
          closePocket(gatewayUrl, token, pocketId);
        }
      }
    });

  } else if (operation < 0.85) {
    // 20% - Transfer operations (high impact)
    group('Stress: Transfers', () => {
      const wallets = getWallets(gatewayUrl, token);

      if (wallets && wallets.length > 0) {
        const transferResult = createTransfer(gatewayUrl, token, {
          sourceWalletId: wallets[0].id,
          destinationAccountId: TEST_USERS[(userIndex + 1) % TEST_USERS.length].username,
          amount: Math.floor(Math.random() * 50000) + 10000,
          description: 'Stress test transfer'
        });
        crudCreateSuccess.add(transferResult.success);
      }
    });

  } else {
    // 15% - Card operations
    group('Stress: Card Operations', () => {
      const wallets = getWallets(gatewayUrl, token);

      if (wallets && wallets.length > 0) {
        const cardResult = createVirtualCard(gatewayUrl, token, {
          cardHolderName: 'Stress Test',
          dailyLimit: 1000000,
          walletId: wallets[0].id
        });
        crudCreateSuccess.add(cardResult.success);
      }

      const cards = getCards(gatewayUrl, token);
      crudReadSuccess.add(cards !== null);
    });
  }

  concurrentOperations.add(-1);
  sleep(Math.random() * 2 + 1);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Stress Test - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('Check metrics for breaking point identification');
  console.log('');
}
