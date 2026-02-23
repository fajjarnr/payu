// PayU KYC Service - CRUD Baseline Performance Test
// ====================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const kycMetrics = {
  submitKycDuration: new Trend('kyc_submit_duration'),
  getKycStatusDuration: new Trend('kyc_get_status_duration'),
  uploadDocumentDuration: new Trend('kyc_upload_doc_duration'),
  getDocumentsDuration: new Trend('kyc_get_docs_duration'),
  verifyDocumentDuration: new Trend('kyc_verify_doc_duration'),
  livenessCheckDuration: new Trend('kyc_liveness_duration'),
  ocrCheckDuration: new Trend('kyc_ocr_duration'),
  faceMatchDuration: new Trend('kyc_face_match_duration'),
  approveKycDuration: new Trend('kyc_approve_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'kyc-service',
    testType: 'baseline-crud'
  }
};

// Document types and verification methods
const DOCUMENT_TYPES = ['KTP', 'PASSPORT', 'SIM', 'NPWP'];
const VERIFICATION_METHODS = ['OCR', 'MANUAL', 'BIOMETRIC'];

// Test data generators
function generateKycData(uniqueId) {
  return {
    fullName: `Test User ${uniqueId}`,
    birthDate: '1990-01-01',
    birthPlace: 'Jakarta',
    address: {
      street: `Jl. Test ${uniqueId} No. 1`,
      city: 'Jakarta Selatan',
      province: 'DKI Jakarta',
      postalCode: '12000'
    },
    nik: `3175${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    verificationMethod: VERIFICATION_METHODS[Math.floor(Math.random() * VERIFICATION_METHODS.length)]
  };
}

function generateDocumentData(uniqueId) {
  return {
    type: DOCUMENT_TYPES[Math.floor(Math.random() * DOCUMENT_TYPES.length)],
    documentNumber: `DOC${Math.floor(100000000 + Math.random() * 900000000)}`,
    expiryDate: new Date(Date.now() + 5 * 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    documentImageUrl: `https://cdn.payu.test/kyc/${uniqueId}_doc.jpg`
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

  let kycId = null;

  group('KYC Service - CRUD Operations', () => {

    // ===== CREATE: Submit KYC =====
    group('CREATE: Submit KYC', () => {
      const kycData = generateKycData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.kyc}`, kycData, auth.token);
      kycMetrics.submitKycDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        kycId = result.body.kycId || result.body.id;
        console.log(`KYC submitted: ${kycId}`);
      }

      sleep(0.5);
    });

    // ===== READ: Get KYC Status =====
    group('READ: Get KYC Status', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.kyc}/status`, auth.token);
      kycMetrics.getKycStatusDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (kycId) {
      // ===== READ: Get KYC Detail =====
      group('READ: Get KYC Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.kyc}/${kycId}`, auth.token);
        kycMetrics.getKycStatusDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

    // ===== CREATE: Upload Document =====
    group('CREATE: Upload Document', () => {
      const docData = generateDocumentData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.kyc}/documents`, docData, auth.token);
      kycMetrics.uploadDocumentDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== READ: List Documents =====
    group('READ: List Documents', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.kyc}/documents`, {}, auth.token);
      kycMetrics.getDocumentsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: OCR Check =====
    group('CREATE: OCR Check', () => {
      const ocrData = {
        documentUrl: `https://cdn.payu.test/kyc/${uniqueId}_ktp.jpg`,
        documentType: 'KTP'
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.kyc}/ocr`, ocrData, auth.token);
      kycMetrics.ocrCheckDuration.add(Date.now() - startTime);

      sleep(1);
    });

    // ===== CREATE: Liveness Check =====
    group('CREATE: Liveness Check', () => {
      const livenessData = {
        videoUrl: `https://cdn.payu.test/kyc/${uniqueId}_liveness.mp4`,
        challengeType: ['BLINK', 'TURN_LEFT', 'TURN_RIGHT'][Math.floor(Math.random() * 3)]
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.kyc}/liveness`, livenessData, auth.token);
      kycMetrics.livenessCheckDuration.add(Date.now() - startTime);

      sleep(1);
    });

    // ===== CREATE: Face Match =====
    group('CREATE: Face Match', () => {
      const faceMatchData = {
        documentImageUrl: `https://cdn.payu.test/kyc/${uniqueId}_ktp.jpg`,
        selfieImageUrl: `https://cdn.payu.test/kyc/${uniqueId}_selfie.jpg`,
        threshold: 0.85
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.kyc}/face-match`, faceMatchData, auth.token);
      kycMetrics.faceMatchDuration.add(Date.now() - startTime);

      sleep(1);
    });

    // ===== CREATE: Verify Document =====
    group('CREATE: Verify Document', () => {
      const verifyData = {
        documentId: `DOC${Math.floor(100000000 + Math.random() * 900000000)}`,
        verificationMethod: 'OCR'
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.kyc}/verify`, verifyData, auth.token);
      kycMetrics.verifyDocumentDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    if (kycId) {
      // ===== UPDATE: Approve KYC =====
      group('UPDATE: Approve KYC', () => {
        const approveData = {
          status: 'APPROVED',
          verifiedBy: 'system',
          notes: 'Approved during baseline test'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.kyc}/${kycId}/status`, approveData, auth.token);
        kycMetrics.approveKycDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('KYC Service CRUD Baseline Test - Starting');
  console.log('==========================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.kyc}/health`);
  console.log(`KYC Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'kyc-service-crud'
  };
}

export function teardown(data) {
  console.log('\n==========================================');
  console.log('KYC Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
