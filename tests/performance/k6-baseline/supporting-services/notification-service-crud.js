// PayU Notification Service - CRUD Baseline Performance Test
// =============================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list, del } from '../lib/crud-helper.js';

// Service-specific metrics
const notificationMetrics = {
  sendNotificationDuration: new Trend('notif_send_duration'),
  getNotificationsDuration: new Trend('notif_get_list_duration'),
  getNotificationDetailDuration: new Trend('notif_get_detail_duration'),
  markAsReadDuration: new Trend('notif_mark_read_duration'),
  getPreferencesDuration: new Trend('notif_preferences_duration'),
  updatePreferencesDuration: new Trend('notif_update_prefs_duration'),
  registerDeviceDuration: new Trend('notif_register_device_duration'),
  unregisterDeviceDuration: new Trend('notif_unregister_device_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'notification-service',
    testType: 'baseline-crud'
  }
};

// Notification channels
const CHANNELS = ['PUSH', 'SMS', 'EMAIL', 'WHATSAPP'];
const CATEGORIES = ['TRANSACTION', 'PROMOTION', 'SECURITY', 'SYSTEM'];

// Test data generators
function generateNotificationData(uniqueId) {
  return {
    channel: CHANNELS[Math.floor(Math.random() * CHANNELS.length)],
    category: CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)],
    title: `Test Notification ${uniqueId}`,
    message: `This is a test notification message for ${uniqueId}`,
    priority: ['HIGH', 'MEDIUM', 'LOW'][Math.floor(Math.random() * 3)],
    data: { testId: uniqueId }
  };
}

function generateDeviceData(uniqueId) {
  return {
    deviceToken: `token_${uniqueId}_${Math.random().toString(36).substring(7)}`,
    deviceType: Math.random() > 0.5 ? 'ANDROID' : 'IOS',
    deviceName: `Test Device ${uniqueId}`,
    appVersion: '1.0.0'
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

  let notificationId = null;
  let deviceId = null;

  group('Notification Service - CRUD Operations', () => {

    // ===== CREATE: Register Device =====
    group('CREATE: Register Device', () => {
      const deviceData = generateDeviceData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.notification}/devices`, deviceData, auth.token);
      notificationMetrics.registerDeviceDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        deviceId = result.body.deviceId || result.body.id;
        console.log(`Device registered: ${deviceId}`);
      }

      sleep(0.5);
    });

    // ===== CREATE: Send Notification =====
    group('CREATE: Send Notification', () => {
      const notificationData = generateNotificationData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.notification}/send`, notificationData, auth.token);
      notificationMetrics.sendNotificationDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        notificationId = result.body.notificationId || result.body.id;
        console.log(`Notification sent: ${notificationId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Notifications =====
    group('READ: List Notifications', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.notification}`, { page: 0, size: 20 }, auth.token);
      notificationMetrics.getNotificationsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (notificationId) {
      // ===== READ: Get Notification Detail =====
      group('READ: Get Notification', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.notification}/${notificationId}`, auth.token);
        notificationMetrics.getNotificationDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Mark as Read =====
      group('UPDATE: Mark as Read', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.notification}/${notificationId}/read`, {}, auth.token);
        notificationMetrics.markAsReadDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

    // ===== READ: Get Preferences =====
    group('READ: Get Preferences', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.notification}/preferences`, auth.token);
      notificationMetrics.getPreferencesDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== UPDATE: Update Preferences =====
    group('UPDATE: Update Preferences', () => {
      const prefsData = {
        channels: {
          PUSH: true,
          SMS: Math.random() > 0.5,
          EMAIL: true,
          WHATSAPP: Math.random() > 0.5
        },
        categories: {
          TRANSACTION: true,
          PROMOTION: Math.random() > 0.5,
          SECURITY: true,
          SYSTEM: true
        }
      };

      const startTime = Date.now();
      const result = update(`${SERVICE_ENDPOINTS.notification}/preferences`, prefsData, auth.token);
      notificationMetrics.updatePreferencesDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (deviceId) {
      // ===== DELETE: Unregister Device =====
      group('DELETE: Unregister Device', () => {
        const startTime = Date.now();
        const result = del(`${SERVICE_ENDPOINTS.notification}/devices/${deviceId}`, auth.token);
        notificationMetrics.unregisterDeviceDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Notification Service CRUD Baseline Test - Starting');
  console.log('===================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.notification}/health`);
  console.log(`Notification Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'notification-service-crud'
  };
}

export function teardown(data) {
  console.log('\n===================================================');
  console.log('Notification Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
