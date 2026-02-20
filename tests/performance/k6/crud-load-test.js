// PayU Platform - CRUD Load Test
// ================================
// Full CRUD operations load test covering:
// - Account: CREATE, READ, UPDATE
// - Wallet/Pocket: CREATE, READ, UPDATE, DELETE
// - Transaction: CREATE, READ (transfer, QRIS)
// - Card: CREATE, READ, UPDATE
//
// Run: k6 run crud-load-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URLS, THRESHOLDS, LOAD_STAGES, TEST_USERS } from './config.js';
import { login, registerUser, getProfile, updateProfile } from './lib/auth.js';
import { createPocket, getWallets, creditPocket, getPocket, updatePocketStatus, closePocket, getPocketTransactions } from './lib/wallet.js';
import { createTransfer, getTransactionHistory, getTransactionDetails } from './lib/transaction.js';
import { createVirtualCard, getCards, freezeCard, unfreezeCard, getCardDetails } from './lib/card.js';

// Custom metrics for CRUD operations
const crudCreateDuration = new Trend('crud_create_duration');
const crudReadDuration = new Trend('crud_read_duration');
const crudUpdateDuration = new Trend('crud_update_duration');
const crudDeleteDuration = new Trend('crud_delete_duration');

const crudCreateSuccess = new Rate('crud_create_success');
const crudReadSuccess = new Rate('crud_read_success');
const crudUpdateSuccess = new Rate('crud_update_success');
const crudDeleteSuccess = new Rate('crud_delete_success');

const transferAmount = new Counter('transfer_amount_total');
const pocketCreated = new Counter('pocket_created_total');
const cardCreated = new Counter('card_created_total');

export const options = {
  stages: LOAD_STAGES.load,
  thresholds: {
    ...THRESHOLDS,
    'crud_create_success': ['rate>0.95'],
    'crud_read_success': ['rate>0.99'],
    'crud_update_success': ['rate>0.95'],
    'crud_delete_success': ['rate>0.90']
  },
  tags: {
    testType: 'crud-load',
    environment: 'dev'
  }
};

// Test data generators
function generateUniqueId() {
  return `k6-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
}

function generateUserData() {
  const uniqueId = generateUniqueId();
  return {
    username: `user_${uniqueId}`,
    email: `user_${uniqueId}@test.com`,
    password: 'TestPassword123!',
    fullName: `Test User ${uniqueId}`,
    phoneNumber: `08${Math.floor(Math.random() * 1000000000).toString().padStart(9, '0')}`,
    nik: `${Math.floor(Math.random() * 1000000000000000).toString().padStart(16, '0')}`
  };
}

export function setup() {
  console.log('=== PayU CRUD Load Test - Setup ===');
  console.log(`Gateway: ${BASE_URLS.gateway}`);
  console.log(`Keycloak: ${BASE_URLS.keycloak}`);
  console.log('');

  // Verify platform health before starting
  const healthCheck = http.get(`${BASE_URLS.gateway}/actuator/health`);
  if (healthCheck.status !== 200) {
    console.error(`WARNING: Gateway health check failed (status: ${healthCheck.status})`);
  }

  // Pre-register a test user for login flow tests
  const testUser = TEST_USERS[0];
  const registerResult = registerUser(BASE_URLS.gateway, {
    username: testUser.username,
    email: `${testUser.username}@test.com`,
    password: testUser.password,
    fullName: 'K6 Test User',
    phoneNumber: '081234567890',
    nik: '1234567890123456'
  });

  return {
    startTime: new Date().toISOString(),
    testUserRegistered: registerResult.success
  };
}

export default function () {
  const gatewayUrl = BASE_URLS.gateway;
  const keycloakUrl = BASE_URLS.keycloak;

  // Get test user credentials
  const userIndex = __VU % TEST_USERS.length;
  const testUser = TEST_USERS[userIndex];

  // ==========================================
  // AUTHENTICATION
  // ==========================================
  let token = null;
  group('Authentication', () => {
    // Login
    token = login(keycloakUrl, testUser.username, testUser.password);
    sleep(1);
  });

  if (!token) {
    console.error(`Failed to login as ${testUser.username}`);
    return;
  }

  // ==========================================
  // ACCOUNT CRUD
  // ==========================================
  group('Account CRUD', () => {
    // READ - Get Profile
    const startRead = Date.now();
    const profile = getProfile(gatewayUrl, token);
    crudReadDuration.add(Date.now() - startRead);
    crudReadSuccess.add(profile !== null);

    if (profile) {
      console.log(`Profile loaded for: ${profile.username || profile.email}`);
    }

    sleep(1);

    // UPDATE - Update Profile
    const startUpdate = Date.now();
    const updateResult = updateProfile(gatewayUrl, token, {
      fullName: `Updated Name ${generateUniqueId()}`
    });
    crudUpdateDuration.add(Date.now() - startUpdate);
    crudUpdateSuccess.add(updateResult);

    sleep(1);
  });

  // ==========================================
  // WALLET/POCKET CRUD
  // ==========================================
  group('Wallet & Pocket CRUD', () => {
    // READ - Get Wallets
    const startRead = Date.now();
    const wallets = getWallets(gatewayUrl, token);
    crudReadDuration.add(Date.now() - startRead);
    crudReadSuccess.add(wallets !== null && wallets.length > 0);

    if (wallets && wallets.length > 0) {
      const wallet = wallets[0];
      console.log(`Using wallet: ${wallet.id}`);

      // CREATE - Create Pocket
      const startCreate = Date.now();
      const pocketResult = createPocket(gatewayUrl, token, {
        name: `K6 Pocket ${generateUniqueId()}`,
        description: 'Load test pocket',
        currency: 'IDR',
        targetAmount: 1000000
      });
      crudCreateDuration.add(Date.now() - startCreate);
      crudCreateSuccess.add(pocketResult.success);

      if (pocketResult.success) {
        pocketCreated.add(1);
        const pocketId = pocketResult.body.pocketId || pocketResult.body.id;
        console.log(`Created pocket: ${pocketId}`);

        // UPDATE - Credit Pocket
        const startUpdate = Date.now();
        const creditResult = creditPocket(gatewayUrl, token, pocketId, 100000);
        crudUpdateDuration.add(Date.now() - startUpdate);
        crudUpdateSuccess.add(creditResult);

        sleep(1);

        // READ - Get Pocket Details
        const startReadPocket = Date.now();
        const pocketDetails = getPocket(gatewayUrl, token, pocketId);
        crudReadDuration.add(Date.now() - startReadPocket);
        crudReadSuccess.add(pocketDetails !== null);

        sleep(1);

        // UPDATE - Freeze Pocket
        const startFreeze = Date.now();
        const freezeResult = updatePocketStatus(gatewayUrl, token, pocketId, 'FROZEN');
        crudUpdateDuration.add(Date.now() - startFreeze);
        crudUpdateSuccess.add(freezeResult);

        sleep(1);

        // UPDATE - Unfreeze Pocket
        const startUnfreeze = Date.now();
        const unfreezeResult = updatePocketStatus(gatewayUrl, token, pocketId, 'ACTIVE');
        crudUpdateDuration.add(Date.now() - startUnfreeze);
        crudUpdateSuccess.add(unfreezeResult);

        sleep(1);

        // DELETE - Close Pocket
        const startDelete = Date.now();
        const closeResult = closePocket(gatewayUrl, token, pocketId);
        crudDeleteDuration.add(Date.now() - startDelete);
        crudDeleteSuccess.add(closeResult);
      }
    }

    sleep(2);
  });

  // ==========================================
  // TRANSACTION CRUD
  // ==========================================
  group('Transaction CRUD', () => {
    const wallets = getWallets(gatewayUrl, token);

    if (wallets && wallets.length > 0) {
      const sourceWallet = wallets[0];

      // CREATE - Transfer
      const startCreate = Date.now();
      const transferResult = createTransfer(gatewayUrl, token, {
        sourceWalletId: sourceWallet.id,
        destinationAccountId: 'customer2', // Test destination
        amount: 10000,
        description: 'K6 Load Test Transfer'
      });
      crudCreateDuration.add(Date.now() - startCreate);
      crudCreateSuccess.add(transferResult.success);

      if (transferResult.success) {
        transferAmount.add(10000);
        const transactionId = transferResult.body.transactionId || transferResult.body.id;
        console.log(`Created transfer: ${transactionId}`);

        // READ - Get Transaction Details
        const startRead = Date.now();
        const transactionDetails = getTransactionDetails(gatewayUrl, token, transactionId);
        crudReadDuration.add(Date.now() - startRead);
        crudReadSuccess.add(transactionDetails !== null);

        sleep(1);
      }

      // READ - Get Transaction History
      const startList = Date.now();
      const history = getTransactionHistory(gatewayUrl, token, {
        page: 0,
        size: 10
      });
      crudReadDuration.add(Date.now() - startList);
      crudReadSuccess.add(history !== null);

      if (history) {
        console.log(`Retrieved ${history.transactions.length} transactions`);
      }
    }

    sleep(2);
  });

  // ==========================================
  // CARD CRUD
  // ==========================================
  group('Card CRUD', () => {
    // READ - Get Cards
    const startRead = Date.now();
    const cards = getCards(gatewayUrl, token);
    crudReadDuration.add(Date.now() - startRead);
    crudReadSuccess.add(cards !== null);

    // CREATE - Create Virtual Card
    const wallets = getWallets(gatewayUrl, token);
    if (wallets && wallets.length > 0) {
      const startCreate = Date.now();
      const cardResult = createVirtualCard(gatewayUrl, token, {
        cardHolderName: 'K6 Test User',
        dailyLimit: 5000000,
        monthlyLimit: 50000000,
        walletId: wallets[0].id
      });
      crudCreateDuration.add(Date.now() - startCreate);
      crudCreateSuccess.add(cardResult.success);

      if (cardResult.success) {
        cardCreated.add(1);
        const cardId = cardResult.body.cardId || cardResult.body.id;
        console.log(`Created card: ${cardId}`);

        // READ - Get Card Details
        const startReadCard = Date.now();
        const cardDetails = getCardDetails(gatewayUrl, token, cardId);
        crudReadDuration.add(Date.now() - startReadCard);
        crudReadSuccess.add(cardDetails !== null);

        sleep(1);

        // UPDATE - Freeze Card
        const startFreeze = Date.now();
        const freezeResult = freezeCard(gatewayUrl, token, cardId);
        crudUpdateDuration.add(Date.now() - startFreeze);
        crudUpdateSuccess.add(freezeResult);

        sleep(1);

        // UPDATE - Unfreeze Card
        const startUnfreeze = Date.now();
        const unfreezeResult = unfreezeCard(gatewayUrl, token, cardId);
        crudUpdateDuration.add(Date.now() - startUnfreeze);
        crudUpdateSuccess.add(unfreezeResult);
      }
    }

    sleep(2);
  });

  sleep(5);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Load Test - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log(`Test user registered: ${data.testUserRegistered}`);
  console.log('');
}
