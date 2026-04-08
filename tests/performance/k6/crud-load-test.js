// PayU Platform - CRUD Load Test
// ================================
// Verified coverage by default:
// - Account onboarding and session validation
// - Wallet balance reads
// - Pocket lifecycle (create/read/update/delete)
// - Card lifecycle (create/read/update)
// Transactions are intentionally excluded from the default flow because they
// require funded accounts and recipient account numbers that are not derivable
// from the standard onboarding contract.

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
let vuCardId = null;

const loadThresholds = Object.assign({}, THRESHOLDS, {
  http_req_duration: ['p(95)<500', 'p(99)<1500'],
  crud_create_success: ['rate>0.95'],
  crud_read_success: ['rate>0.99'],
  crud_update_success: ['rate>0.95'],
  crud_delete_success: ['rate>0.90']
});

delete loadThresholds.http_req_failed;

function ensureSession(gatewayUrl) {
  if (!vuSession) {
    vuSession = createOnboardedSession(gatewayUrl, generateUserData('k6load'));
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
  stages: LOAD_STAGES.load,
  thresholds: loadThresholds,
  tags: {
    testType: 'crud-load',
    environment: 'dev'
  }
};

export function setup() {
  console.log('=== PayU CRUD Load Test - Setup ===');
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
    console.error('Failed to create or refresh load-test session');
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
    sleep(1);
    return;
  }

  group('Account & Wallet Readiness', () => {
    const startValidate = Date.now();
    const validation = validateSession(BASE_URLS.gateway, token);
    crudReadDuration.add(Date.now() - startValidate);
    crudReadSuccess.add(validation !== null);

    const startBalance = Date.now();
    const currentBalance = getWalletBalance(BASE_URLS.gateway, token, accountId);
    crudReadDuration.add(Date.now() - startBalance);
    crudReadSuccess.add(currentBalance !== null);
  });

  group('Pocket CRUD', () => {
    const startList = Date.now();
    const pocketsBefore = listPockets(BASE_URLS.gateway, token);
    crudReadDuration.add(Date.now() - startList);
    crudReadSuccess.add(pocketsBefore !== null);

    const startCreate = Date.now();
    const pocketResult = createPocket(BASE_URLS.gateway, token, {
      name: `K6 Pocket ${Date.now()}`,
      description: 'CRUD load pocket',
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
    const creditResult = creditPocket(BASE_URLS.gateway, token, pocketId, 10000);
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
    const debitResult = debitPocket(BASE_URLS.gateway, token, pocketId, 10000);
    crudUpdateDuration.add(Date.now() - startDebit);
    crudUpdateSuccess.add(debitResult);

    const startDelete = Date.now();
    const closeResult = closePocket(BASE_URLS.gateway, token, pocketId);
    crudDeleteDuration.add(Date.now() - startDelete);
    crudDeleteSuccess.add(closeResult);
  });

  if (FEATURE_FLAGS.enableCardCrud) {
    group('Card CRUD', () => {
      const startList = Date.now();
      const cards = getCards(BASE_URLS.gateway, token, accountId);
      crudReadDuration.add(Date.now() - startList);
      crudReadSuccess.add(cards !== null);

      if (!vuCardId) {
        const startCreate = Date.now();
        const cardResult = createVirtualCard(BASE_URLS.gateway, token, {
          accountId: accountId,
          cardHolderName: userData.fullName,
          dailyLimit: 5000000
        });
        crudCreateDuration.add(Date.now() - startCreate);
        crudCreateSuccess.add(cardResult.success);

        if (cardResult.success) {
          vuCardId = cardResult.body.data.id;
          cardCreated.add(1);
        }
      }

      if (!vuCardId) {
        return;
      }

      const startRead = Date.now();
      const card = getCardDetails(BASE_URLS.gateway, token, vuCardId);
      crudReadDuration.add(Date.now() - startRead);
      crudReadSuccess.add(card !== null);

      if (!card) {
        vuCardId = null;
        return;
      }

      const startFreeze = Date.now();
      const freezeResult = freezeCard(BASE_URLS.gateway, token, vuCardId);
      crudUpdateDuration.add(Date.now() - startFreeze);
      crudUpdateSuccess.add(freezeResult);

      const startUnfreeze = Date.now();
      const unfreezeResult = unfreezeCard(BASE_URLS.gateway, token, vuCardId);
      crudUpdateDuration.add(Date.now() - startUnfreeze);
      crudUpdateSuccess.add(unfreezeResult);
    });
  }

  sleep(2);
}

export function teardown(data) {
  console.log('');
  console.log('=== PayU CRUD Load Test - Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('');
}
