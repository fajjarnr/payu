// PayU CMS Service - CRUD Baseline Performance Test
// ====================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list, del } from '../lib/crud-helper.js';

// Service-specific metrics
const cmsMetrics = {
  createContentDuration: new Trend('cms_create_content_duration'),
  getContentsDuration: new Trend('cms_get_contents_duration'),
  getContentDetailDuration: new Trend('cms_get_content_detail_duration'),
  updateContentDuration: new Trend('cms_update_content_duration'),
  publishContentDuration: new Trend('cms_publish_duration'),
  unpublishContentDuration: new Trend('cms_unpublish_duration'),
  deleteContentDuration: new Trend('cms_delete_content_duration'),
  getBannersDuration: new Trend('cms_get_banners_duration'),
  uploadMediaDuration: new Trend('cms_upload_media_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'cms-service',
    testType: 'baseline-crud'
  }
};

// Content types and categories
const CONTENT_TYPES = ['ARTICLE', 'BANNER', 'FAQ', 'ANNOUNCEMENT', 'TUTORIAL'];
const CATEGORIES = ['PROMO', 'FEATURE', 'SECURITY', 'HELP', 'NEWS'];
const PLATFORMS = ['MOBILE', 'WEB', 'BOTH'];

// Test data generators
function generateContentData(uniqueId) {
  const contentType = CONTENT_TYPES[Math.floor(Math.random() * CONTENT_TYPES.length)];
  return {
    title: `Content ${uniqueId}`,
    slug: `content-${uniqueId.toLowerCase()}`,
    type: contentType,
    category: CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)],
    platform: PLATFORMS[Math.floor(Math.random() * PLATFORMS.length)],
    content: `<p>This is test content for ${uniqueId}. Generated during baseline testing.</p>`,
    summary: `Summary for ${uniqueId}`,
    featuredImage: `https://cdn.payu.test/images/${uniqueId}.jpg`,
    tags: ['test', 'baseline', contentType.toLowerCase()],
    priority: Math.floor(1 + Math.random() * 10),
    startDate: new Date().toISOString(),
    endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
  };
}

function generateBannerData(uniqueId) {
  const positions = ['HOME_TOP', 'HOME_MIDDLE', 'DASHBOARD', 'PAYMENT_FLOW'];
  return {
    title: `Banner ${uniqueId}`,
    position: positions[Math.floor(Math.random() * positions.length)],
    imageUrl: `https://cdn.payu.test/banners/${uniqueId}.jpg`,
    targetUrl: `https://payu.test/promo/${uniqueId}`,
    platform: PLATFORMS[Math.floor(Math.random() * PLATFORMS.length)],
    priority: Math.floor(1 + Math.random() * 10),
    startDate: new Date().toISOString(),
    endDate: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString()
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

  let contentId = null;

  group('CMS Service - CRUD Operations', () => {

    // ===== CREATE: Create Content =====
    group('CREATE: Create Content', () => {
      const contentData = generateContentData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.cms}`, contentData, auth.token);
      cmsMetrics.createContentDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        contentId = result.body.contentId || result.body.id;
        console.log(`Content created: ${contentId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Contents =====
    group('READ: List Contents', () => {
      const params = {
        page: 0,
        size: 10,
        status: 'PUBLISHED'
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.cms}`, params, auth.token);
      cmsMetrics.getContentsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (contentId) {
      // ===== READ: Get Content Detail =====
      group('READ: Get Content Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.cms}/${contentId}`, auth.token);
        cmsMetrics.getContentDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Update Content =====
      group('UPDATE: Update Content', () => {
        const updateData = {
          title: `Updated Content ${Date.now()}`,
          content: `<p>Updated content at ${Date.now()}</p>`,
          priority: Math.floor(1 + Math.random() * 10)
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.cms}/${contentId}`, updateData, auth.token);
        cmsMetrics.updateContentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Publish Content =====
      group('UPDATE: Publish Content', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.cms}/${contentId}/publish`, {}, auth.token);
        cmsMetrics.publishContentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Unpublish Content =====
      group('UPDATE: Unpublish Content', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.cms}/${contentId}/unpublish`, {}, auth.token);
        cmsMetrics.unpublishContentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

    // ===== READ: Get Banners =====
    group('READ: Get Banners', () => {
      const params = {
        position: 'HOME_TOP',
        platform: 'MOBILE'
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.cms}/banners`, params, auth.token);
      cmsMetrics.getBannersDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Create Banner =====
    group('CREATE: Create Banner', () => {
      const bannerData = generateBannerData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.cms}/banners`, bannerData, auth.token);
      cmsMetrics.uploadMediaDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    if (contentId) {
      // ===== DELETE: Delete Content =====
      group('DELETE: Delete Content', () => {
        const startTime = Date.now();
        const result = del(`${SERVICE_ENDPOINTS.cms}/${contentId}`, auth.token);
        cmsMetrics.deleteContentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('CMS Service CRUD Baseline Test - Starting');
  console.log('==========================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.cms}/health`);
  console.log(`CMS Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'cms-service-crud'
  };
}

export function teardown(data) {
  console.log('\n==========================================');
  console.log('CMS Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
