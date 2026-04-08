// PayU Platform - CRUD Data Consistency Test
// ============================================
// Verifies read-after-write and state-transition consistency for the verified
// onboarding, wallet, pocket, and card flows.

import { sleep, group } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { BASE_URLS, FEATURE_FLAGS, SESSION_SETTINGS } from './config.js';
import { createOnboardedSession, refreshSession, validateSession, generateUserData } from './lib/auth.js';
import {
  createPocket,
  getPocket,
  creditPocket,
  debitPocket,
  freezePocket,
  unfreezePocket,
  closePocket,
  getWalletBalance,
  waitForWalletReady
} from './lib/wallet.js';
import {
  createVirtualCard,
  getCardDetails,
  freezeCard,
  unfreezeCard
} from './lib/card.js';

const readAfterWriteConsistency = new Rate('read_after_write_consistency');
const stateTransitionConsistency = new Rate('state_transition_consistency');
const concurrentUpdateConsistency = new Rate('concurrent_update_consistency');
const consistencyErrors = new Counter('consistency_errors');

let vuSession = null;

function retryCreatePocket(gatewayUrl, token, pocketData, attempts = 3) {
  for (let attempt = 0; attempt < attempts; attempt++) {
    const pocketResult = createPocket(gatewayUrl, token, pocketData, false);
    if (pocketResult.success) {
      return pocketResult;
    }

    if (attempt < attempts - 1) {
      sleep(0.2);
    }
  }

  return { success: false };
}

function waitForPocketState(gatewayUrl, token, pocketId, expectedStatus = null, attempts = 5) {
  for (let attempt = 0; attempt < attempts; attempt++) {
    const pocket = getPocket(gatewayUrl, token, pocketId, false);
    if (pocket !== null && (expectedStatus === null || pocket.status === expectedStatus)) {
      return pocket;
    }

    if (attempt < attempts - 1) {
      sleep(0.2);
    }
  }

  return null;
}

function waitForPocketBalance(gatewayUrl, token, pocketId, expectedBalance, attempts = 8) {
  for (let attempt = 0; attempt < attempts; attempt++) {
    const pocket = getPocket(gatewayUrl, token, pocketId, false);
    if (pocket !== null && Number(pocket.balance) >= expectedBalance) {
      return pocket;
    }

    if (attempt < attempts - 1) {
      sleep(0.25);
    }
  }

  return null;
}

function ensureSession(gatewayUrl) {
  if (!vuSession) {
    vuSession = createOnboardedSession(gatewayUrl, generateUserData('k6consistency'));
    return vuSession.success ? vuSession : null;
  }

  if (Date.now() - vuSession.refreshedAt > SESSION_SETTINGS.tokenRefreshIntervalMs) {
    const refreshedSession = refreshSession(gatewayUrl, vuSession);
    if (!refreshedSession.success) {
      vuSession = null;
      return null;
    }

    vuSession = refreshedSession;
  }

  return vuSession;
}

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
    state_transition_consistency: ['rate>0.99'],
    concurrent_update_consistency: ['rate>0.99'],
    consistency_errors: ['count<10']
  },
  tags: {
    testType: 'data-consistency',
    environment: 'dev'
  }
};

export function setup() {
  console.log('=== PayU CRUD Data Consistency Test - Setup ===');
  console.log('Testing verified pocket/card state transitions under load.');
  console.log('');

  return { startTime: new Date().toISOString() };
}

export default function () {
  const session = ensureSession(BASE_URLS.gateway);

  if (!session) {
    consistencyErrors.add(1);
    console.error('Failed to create or refresh consistency-test session');
    sleep(1);
    return;
  }

  const { token, accountId, userData } = session;
  const walletBalance = waitForWalletReady(
    BASE_URLS.gateway,
    token,
    accountId,
    SESSION_SETTINGS.walletReadyMaxAttempts,
    SESSION_SETTINGS.walletReadySleepSeconds
  );

  if (walletBalance === null) {
    readAfterWriteConsistency.add(0);
    consistencyErrors.add(1);
    sleep(1);
    return;
  }

  group('Wallet Lookup Consistency', () => {
    const validation = validateSession(BASE_URLS.gateway, token);
    const balance = getWalletBalance(BASE_URLS.gateway, token, accountId);
    const consistent = validation !== null && balance !== null && balance.accountId === accountId;

    readAfterWriteConsistency.add(consistent);
    if (!consistent) {
      consistencyErrors.add(1);
    }
  });

  group('Pocket Read-After-Write', () => {
    const pocketResult = retryCreatePocket(BASE_URLS.gateway, token, {
      name: `Consistency Pocket ${Date.now()}`,
      description: 'Testing read-after-write',
      currency: 'IDR'
    });

    if (!pocketResult.success) {
      readAfterWriteConsistency.add(0);
      consistencyErrors.add(1);
      return;
    }

    const pocketId = pocketResult.body.data.id;
    const pocket = waitForPocketState(BASE_URLS.gateway, token, pocketId);
    const consistent = pocket !== null && pocket.id === pocketId;

    readAfterWriteConsistency.add(consistent);
    if (!consistent) {
      consistencyErrors.add(1);
      return;
    }

    closePocket(BASE_URLS.gateway, token, pocketId);
  });

  group('Pocket State Transitions', () => {
    const pocketResult = retryCreatePocket(BASE_URLS.gateway, token, {
      name: `Transition Pocket ${Date.now()}`,
      description: 'Testing freeze/unfreeze/close',
      currency: 'IDR'
    });

    if (!pocketResult.success) {
      stateTransitionConsistency.add(0);
      consistencyErrors.add(1);
      return;
    }

    const pocketId = pocketResult.body.data.id;
    const creditResult = creditPocket(BASE_URLS.gateway, token, pocketId, 20000);
    const freezeResult = freezePocket(BASE_URLS.gateway, token, pocketId);
    const frozenPocket = waitForPocketState(BASE_URLS.gateway, token, pocketId, 'FROZEN');
    const unfreezeResult = unfreezePocket(BASE_URLS.gateway, token, pocketId);
    const debitResult = debitPocket(BASE_URLS.gateway, token, pocketId, 20000);
    const closeResult = closePocket(BASE_URLS.gateway, token, pocketId);
    const closedPocket = waitForPocketState(BASE_URLS.gateway, token, pocketId, 'CLOSED');

    const consistent = creditResult && freezeResult && frozenPocket !== null && frozenPocket.status === 'FROZEN' &&
      unfreezeResult && debitResult && closeResult && closedPocket !== null && closedPocket.status === 'CLOSED';

    stateTransitionConsistency.add(consistent);
    if (!consistent) {
      consistencyErrors.add(1);
    }
  });

  group('Repeated Balance Updates', () => {
    const pocketResult = retryCreatePocket(BASE_URLS.gateway, token, {
      name: `Balance Pocket ${Date.now()}`,
      description: 'Testing repeated credits',
      currency: 'IDR'
    });

    if (!pocketResult.success) {
      concurrentUpdateConsistency.add(0);
      consistencyErrors.add(1);
      return;
    }

    const pocketId = pocketResult.body.data.id;
    const credits = [
      creditPocket(BASE_URLS.gateway, token, pocketId, 10000),
      creditPocket(BASE_URLS.gateway, token, pocketId, 10000),
      creditPocket(BASE_URLS.gateway, token, pocketId, 10000)
    ];
    const pocket = waitForPocketBalance(BASE_URLS.gateway, token, pocketId, 30000);
    const consistent = credits.every((value) => value) && pocket !== null && Number(pocket.balance) >= 30000;

    concurrentUpdateConsistency.add(consistent);
    if (!consistent) {
      consistencyErrors.add(1);
      return;
    }

    debitPocket(BASE_URLS.gateway, token, pocketId, 30000);
    closePocket(BASE_URLS.gateway, token, pocketId);
  });

  if (FEATURE_FLAGS.enableCardCrud) {
    group('Card Read-After-Write', () => {
      const cardResult = createVirtualCard(BASE_URLS.gateway, token, {
        accountId: accountId,
        cardHolderName: userData.fullName,
        dailyLimit: 5000000
      });

      if (!cardResult.success) {
        stateTransitionConsistency.add(0);
        consistencyErrors.add(1);
        return;
      }

      const cardId = cardResult.body.data.id;
      const card = getCardDetails(BASE_URLS.gateway, token, cardId);
      const freezeResult = freezeCard(BASE_URLS.gateway, token, cardId);
      const frozenCard = getCardDetails(BASE_URLS.gateway, token, cardId);
      const unfreezeResult = unfreezeCard(BASE_URLS.gateway, token, cardId);
      const activeCard = getCardDetails(BASE_URLS.gateway, token, cardId);

      const consistent = card !== null && card.id === cardId && freezeResult && frozenCard !== null &&
        frozenCard.status === 'FROZEN' && unfreezeResult && activeCard !== null && activeCard.status === 'ACTIVE';

      stateTransitionConsistency.add(consistent);
      if (!consistent) {
        consistencyErrors.add(1);
      }
    });
  }

  sleep(2);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Data Consistency Test - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('');
}
