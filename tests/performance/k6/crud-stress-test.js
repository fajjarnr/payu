// PayU Platform - CRUD Stress Test
// ==================================
// Stress profile for the verified onboarding, wallet/pocket, and card flows.

import http from 'k6/http';
import { sleep, group } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URLS, FEATURE_FLAGS, LOAD_STAGES, SESSION_SETTINGS } from './config.js';
import { createOnboardedSession, refreshSession, validateSession, generateUserData } from './lib/auth.js';
import {
  createPocket,
  listPockets,
  creditPocket,
  debitPocket,
  closePocket,
  getWalletBalance,
  waitForWalletReady
} from './lib/wallet.js';
import {
  createVirtualCard,
  getCards,
  getCardDetails,
  freezeCard,
  unfreezeCard
} from './lib/card.js';

const stressThresholds = {
  http_req_duration: [
    { threshold: 'p(95)<5000', abortOnFail: false },
    { threshold: 'p(99)<10000', abortOnFail: false }
  ],
  crud_create_success: ['rate>0.80'],
  crud_read_success: ['rate>0.90'],
  crud_update_success: ['rate>0.80'],
  crud_delete_success: ['rate>0.70']
};

const crudCreateSuccess = new Rate('crud_create_success');
const crudReadSuccess = new Rate('crud_read_success');
const crudUpdateSuccess = new Rate('crud_update_success');
const crudDeleteSuccess = new Rate('crud_delete_success');

let vuSession = null;
let vuCardId = null;

function ensureSession(gatewayUrl) {
  if (!vuSession) {
    vuSession = createOnboardedSession(gatewayUrl, generateUserData('k6stress'));
    return vuSession.success ? vuSession : null;
  }

  if (Date.now() - vuSession.refreshedAt > SESSION_SETTINGS.tokenRefreshIntervalMs) {
    const refreshedSession = refreshSession(gatewayUrl, vuSession);
    if (!refreshedSession.success) {
      vuSession = null;
      vuCardId = null;
      return null;
    }

    vuSession = refreshedSession;
  }

  return vuSession;
}

export const options = {
  stages: LOAD_STAGES.stress,
  thresholds: stressThresholds,
  tags: {
    testType: 'crud-stress',
    environment: 'dev'
  }
};

export function setup() {
  console.log('=== PayU CRUD Stress Test - Setup ===');
  console.log(`Gateway: ${BASE_URLS.gateway}`);
  console.log('WARNING: This test uses the verified CRUD flow under sustained stress.');
  console.log('');

  const healthCheck = http.get(`${BASE_URLS.gateway}/q/health`);
  if (healthCheck.status !== 200) {
    console.error(`WARNING: Gateway health check failed (status: ${healthCheck.status})`);
  }

  return {
    startTime: new Date().toISOString()
  };
}

export default function () {
  const session = ensureSession(BASE_URLS.gateway);

  if (!session) {
    crudCreateSuccess.add(0);
    console.error('Failed to create or refresh stress-test session');
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
    crudReadSuccess.add(0);
    sleep(1);
    return;
  }

  const operation = Math.random();

  if (operation < 0.50) {
    group('Stress: Read Operations', () => {
      const validation = validateSession(BASE_URLS.gateway, token);
      crudReadSuccess.add(validation !== null);

      const balance = getWalletBalance(BASE_URLS.gateway, token, accountId);
      crudReadSuccess.add(balance !== null);

      const pockets = listPockets(BASE_URLS.gateway, token);
      crudReadSuccess.add(pockets !== null);

      if (FEATURE_FLAGS.enableCardCrud) {
        const cards = getCards(BASE_URLS.gateway, token, accountId);
        crudReadSuccess.add(cards !== null);
      }
    });
  } else if (operation < 0.85) {
    group('Stress: Pocket Lifecycle', () => {
      const pocketResult = createPocket(BASE_URLS.gateway, token, {
        name: `Stress Pocket ${Date.now()}`,
        description: 'Stress CRUD pocket',
        currency: 'IDR'
      });
      crudCreateSuccess.add(pocketResult.success);

      if (!pocketResult.success) {
        return;
      }

      const pocketId = pocketResult.body.data.id;
      const creditResult = creditPocket(BASE_URLS.gateway, token, pocketId, 10000);
      crudUpdateSuccess.add(creditResult);

      const debitResult = debitPocket(BASE_URLS.gateway, token, pocketId, 10000);
      crudUpdateSuccess.add(debitResult);

      const closeResult = closePocket(BASE_URLS.gateway, token, pocketId);
      crudDeleteSuccess.add(closeResult);
    });
  } else if (FEATURE_FLAGS.enableCardCrud) {
    group('Stress: Card Lifecycle', () => {
      const cards = getCards(BASE_URLS.gateway, token, accountId);
      crudReadSuccess.add(cards !== null);

      if (!vuCardId) {
        const cardResult = createVirtualCard(BASE_URLS.gateway, token, {
          accountId: accountId,
          cardHolderName: userData.fullName,
          dailyLimit: 1000000
        });
        crudCreateSuccess.add(cardResult.success);

        if (cardResult.success) {
          vuCardId = cardResult.body.data.id;
        }
      }

      if (!vuCardId) {
        return;
      }

      const card = getCardDetails(BASE_URLS.gateway, token, vuCardId);
      crudReadSuccess.add(card !== null);

      if (!card) {
        vuCardId = null;
        return;
      }

      const freezeResult = freezeCard(BASE_URLS.gateway, token, vuCardId);
      crudUpdateSuccess.add(freezeResult);

      const unfreezeResult = unfreezeCard(BASE_URLS.gateway, token, vuCardId);
      crudUpdateSuccess.add(unfreezeResult);
    });
  } else {
    group('Stress: Fallback Read', () => {
      const balance = getWalletBalance(BASE_URLS.gateway, token, accountId);
      crudReadSuccess.add(balance !== null);
    });
  }

  sleep(Math.random() * 2 + 1);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Stress Test - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('');
}
