// PayU AB Testing Service - CRUD Baseline Performance Test
// ===========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const abTestingMetrics = {
  createExperimentDuration: new Trend('abtest_create_experiment_duration'),
  getExperimentsDuration: new Trend('abtest_get_experiments_duration'),
  getExperimentDetailDuration: new Trend('abtest_get_experiment_detail_duration'),
  updateExperimentDuration: new Trend('abtest_update_experiment_duration'),
  startExperimentDuration: new Trend('abtest_start_duration'),
  stopExperimentDuration: new Trend('abtest_stop_duration'),
  assignVariantDuration: new Trend('abtest_assign_variant_duration'),
  recordEventDuration: new Trend('abtest_record_event_duration'),
  getResultsDuration: new Trend('abtest_get_results_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'ab-testing-service',
    testType: 'baseline-crud'
  }
};

// Experiment types and platforms
const EXPERIMENT_TYPES = ['UI', 'FEATURE', 'ALGORITHM', 'PRICING'];
const PLATFORMS = ['MOBILE', 'WEB', 'API'];
const METRICS = ['CONVERSION', 'ENGAGEMENT', 'RETENTION', 'REVENUE'];

// Test data generators
function generateExperimentData(uniqueId) {
  return {
    name: `Experiment ${uniqueId}`,
    description: `A/B test experiment created during baseline testing ${uniqueId}`,
    type: EXPERIMENT_TYPES[Math.floor(Math.random() * EXPERIMENT_TYPES.length)],
    platform: PLATFORMS[Math.floor(Math.random() * PLATFORMS.length)],
    targetMetric: METRICS[Math.floor(Math.random() * METRICS.length)],
    trafficAllocation: Math.floor(10 + Math.random() * 40),
    variants: [
      { name: 'control', allocation: 50 },
      { name: 'treatment', allocation: 50 }
    ],
    startDate: new Date().toISOString(),
    endDate: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString(),
    targetAudience: {
      userSegments: ['NEW_USER', 'ACTIVE_USER'],
      minAppVersion: '1.0.0'
    }
  };
}

function generateEventData(experimentId, variant) {
  return {
    experimentId: experimentId,
    variant: variant || 'control',
    eventType: ['VIEW', 'CLICK', 'CONVERSION', 'DISMISS'][Math.floor(Math.random() * 4)],
    timestamp: new Date().toISOString(),
    metadata: {
      screen: 'home',
      element: 'cta_button'
    }
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

  let experimentId = null;

  group('AB Testing Service - CRUD Operations', () => {

    // ===== CREATE: Create Experiment =====
    group('CREATE: Create Experiment', () => {
      const experimentData = generateExperimentData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.abTesting}/experiments`, experimentData, auth.token);
      abTestingMetrics.createExperimentDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        experimentId = result.body.experimentId || result.body.id;
        console.log(`Experiment created: ${experimentId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Experiments =====
    group('READ: List Experiments', () => {
      const params = {
        page: 0,
        size: 10,
        status: 'DRAFT'
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.abTesting}/experiments`, params, auth.token);
      abTestingMetrics.getExperimentsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (experimentId) {
      // ===== READ: Get Experiment Detail =====
      group('READ: Get Experiment Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.abTesting}/experiments/${experimentId}`, auth.token);
        abTestingMetrics.getExperimentDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Update Experiment =====
      group('UPDATE: Update Experiment', () => {
        const updateData = {
          name: `Updated Experiment ${Date.now()}`,
          trafficAllocation: Math.floor(20 + Math.random() * 30),
          description: 'Updated during baseline test'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.abTesting}/experiments/${experimentId}`, updateData, auth.token);
        abTestingMetrics.updateExperimentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Start Experiment =====
      group('UPDATE: Start Experiment', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.abTesting}/experiments/${experimentId}/start`, {}, auth.token);
        abTestingMetrics.startExperimentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Assign Variant =====
      group('CREATE: Assign Variant', () => {
        const assignmentData = {
          userId: `USR${Math.floor(100000 + Math.random() * 900000)}`,
          experimentId: experimentId
        };

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.abTesting}/assign`, assignmentData, auth.token);
        abTestingMetrics.assignVariantDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Record Event =====
      group('CREATE: Record Event', () => {
        const eventData = generateEventData(experimentId, 'treatment');

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.abTesting}/events`, eventData, auth.token);
        abTestingMetrics.recordEventDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== READ: Get Experiment Results =====
      group('READ: Get Results', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.abTesting}/experiments/${experimentId}/results`, auth.token);
        abTestingMetrics.getResultsDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Stop Experiment =====
      group('UPDATE: Stop Experiment', () => {
        const stopData = {
          reason: 'Baseline test completion'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.abTesting}/experiments/${experimentId}/stop`, stopData, auth.token);
        abTestingMetrics.stopExperimentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('AB Testing Service CRUD Baseline Test - Starting');
  console.log('=================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.abTesting}/health`);
  console.log(`AB Testing Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'ab-testing-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=================================================');
  console.log('AB Testing Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
