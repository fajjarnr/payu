// PayU Platform - CRUD Load Test (Short Version for Validation)
// ================================================================
// Verified flow:
// - Account onboarding via /api/v1/accounts/register
// - Session validation via /api/v1/auth/login + /api/v1/auth/validate
// - Wallet readiness via /api/v1/wallets/{accountId}/balance
// - Pocket CRUD lifecycle
// - Card CRUD lifecycle

import http from 'k6/http';
import { sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URLS, FEATURE_FLAGS, LOAD_STAGES, SESSION_SETTINGS, THRESHOLDS } from './config.js';
import { createOnboardedSession, refreshSession, validateSession, generateUserData } from './lib/auth.js';
import {
  createPocket,
  listPockets,
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
  getCards,
  getCardDetails,
  freezeCard,
  unfreezeCard
} from './lib/card.js';

const crudCreateDuration = new Trend('crud_create_duration');
const crudReadDuration = new Trend('crud_read_duration');
const crudUpdateDuration = new Trend('crud_update_duration');
const crudDeleteDuration = new Trend('crud_delete_duration');

const crudCreateSuccess = new Rate('crud_create_success');
const crudReadSuccess = new Rate('crud_read_success');
const crudUpdateSuccess = new Rate('crud_update_success');
const crudDeleteSuccess = new Rate('crud_delete_success');

const pocketCreated = new Counter('pocket_created_total');
const cardCreated = new Counter('card_created_total');

let vuSession = null;

const shortThresholds = Object.assign({}, THRESHOLDS, {
  http_req_duration: ['p(95)<500', 'p(99)<1500'],
  crud_create_success: ['rate>0.95'],
  crud_read_success: ['rate>0.99'],
  crud_update_success: ['rate>0.95'],
  crud_delete_success: ['rate>0.90']
});

function ensureSession(gatewayUrl) {
  if (!vuSession) {
    vuSession = createOnboardedSession(gatewayUrl, generateUserData('k6short'));
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
  stages: LOAD_STAGES.smoke,
  thresholds: shortThresholds,
  tags: {
    testType: 'crud-load-short',
    environment: 'dev'
  }
};

export function setup() {
  console.log('=== PayU CRUD Load Test (Short) - Setup ===');
  console.log(`Gateway: ${BASE_URLS.gateway}`);
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
    console.error('Failed to create or refresh short-test session');
    sleep(1);
    return;
  }

  const { token, accountId, userData } = session;
  let walletBalance = null;

  group('Account & Wallet Readiness', () => {
    const startValidate = Date.now();
    const validation = validateSession(BASE_URLS.gateway, token);
    crudReadDuration.add(Date.now() - startValidate);
    crudReadSuccess.add(validation !== null);

    const startBalance = Date.now();
    walletBalance = waitForWalletReady(
      BASE_URLS.gateway,
      token,
      accountId,
      SESSION_SETTINGS.walletReadyMaxAttempts,
      SESSION_SETTINGS.walletReadySleepSeconds
    );
    if (walletBalance !== null) {
      walletBalance = getWalletBalance(BASE_URLS.gateway, token, accountId);
    }
    crudReadDuration.add(Date.now() - startBalance);
    crudReadSuccess.add(walletBalance !== null);
  });

  if (walletBalance === null) {
    sleep(1);
    return;
  }

  group('Pocket CRUD', () => {
    const startList = Date.now();
    const pocketsBefore = listPockets(BASE_URLS.gateway, token);
    crudReadDuration.add(Date.now() - startList);
    crudReadSuccess.add(pocketsBefore !== null);

    const startCreate = Date.now();
    const pocketResult = createPocket(BASE_URLS.gateway, token, {
      name: `K6 Pocket ${Date.now()}`,
      description: 'Short CRUD validation pocket',
      currency: 'IDR'
    });
    crudCreateDuration.add(Date.now() - startCreate);
    crudCreateSuccess.add(pocketResult.success);

    if (!pocketResult.success) {
      return;
    }

    pocketCreated.add(1);
    const pocketId = pocketResult.body.data.id;

    const startRead = Date.now();
    const pocket = getPocket(BASE_URLS.gateway, token, pocketId);
    crudReadDuration.add(Date.now() - startRead);
    crudReadSuccess.add(pocket !== null);

    const startCredit = Date.now();
    const creditResult = creditPocket(BASE_URLS.gateway, token, pocketId, 25000);
    crudUpdateDuration.add(Date.now() - startCredit);
    crudUpdateSuccess.add(creditResult);

    const startFreeze = Date.now();
    const freezeResult = freezePocket(BASE_URLS.gateway, token, pocketId);
    crudUpdateDuration.add(Date.now() - startFreeze);
    crudUpdateSuccess.add(freezeResult);

    const startUnfreeze = Date.now();
    const unfreezeResult = unfreezePocket(BASE_URLS.gateway, token, pocketId);
    crudUpdateDuration.add(Date.now() - startUnfreeze);
    crudUpdateSuccess.add(unfreezeResult);

    const startDebit = Date.now();
    const debitResult = debitPocket(BASE_URLS.gateway, token, pocketId, 25000);
    crudUpdateDuration.add(Date.now() - startDebit);
    crudUpdateSuccess.add(debitResult);

    const startDelete = Date.now();
    const closeResult = closePocket(BASE_URLS.gateway, token, pocketId);
    crudDeleteDuration.add(Date.now() - startDelete);
    crudDeleteSuccess.add(closeResult);

    const startAfter = Date.now();
    const pocketsAfter = listPockets(BASE_URLS.gateway, token);
    crudReadDuration.add(Date.now() - startAfter);
    crudReadSuccess.add(pocketsAfter !== null);
  });

  if (FEATURE_FLAGS.enableCardCrud) {
    group('Card CRUD', () => {
      const startList = Date.now();
      const cards = getCards(BASE_URLS.gateway, token, accountId);
      crudReadDuration.add(Date.now() - startList);
      crudReadSuccess.add(cards !== null);

      const startCreate = Date.now();
      const cardResult = createVirtualCard(BASE_URLS.gateway, token, {
        accountId: accountId,
        cardHolderName: userData.fullName,
        dailyLimit: 5000000
      });
      crudCreateDuration.add(Date.now() - startCreate);
      crudCreateSuccess.add(cardResult.success);

      if (!cardResult.success) {
        return;
      }

      cardCreated.add(1);
      const cardId = cardResult.body.data.id;

      const startRead = Date.now();
      const card = getCardDetails(BASE_URLS.gateway, token, cardId);
      crudReadDuration.add(Date.now() - startRead);
      crudReadSuccess.add(card !== null);

      const startFreeze = Date.now();
      const freezeResult = freezeCard(BASE_URLS.gateway, token, cardId);
      crudUpdateDuration.add(Date.now() - startFreeze);
      crudUpdateSuccess.add(freezeResult);

      const startUnfreeze = Date.now();
      const unfreezeResult = unfreezeCard(BASE_URLS.gateway, token, cardId);
      crudUpdateDuration.add(Date.now() - startUnfreeze);
      crudUpdateSuccess.add(unfreezeResult);
    });
  }

  sleep(1);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Load Test (Short) - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('');
}
