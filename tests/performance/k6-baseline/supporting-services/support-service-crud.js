// PayU Support Service - CRUD Baseline Performance Test
// ========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const supportMetrics = {
  createTicketDuration: new Trend('support_create_ticket_duration'),
  getTicketsDuration: new Trend('support_get_tickets_duration'),
  getTicketDetailDuration: new Trend('support_get_ticket_detail_duration'),
  updateTicketDuration: new Trend('support_update_ticket_duration'),
  addCommentDuration: new Trend('support_add_comment_duration'),
  getFaqsDuration: new Trend('support_get_faqs_duration'),
  searchKnowledgeBaseDuration: new Trend('support_search_kb_duration'),
  getCategoriesDuration: new Trend('support_get_categories_duration'),
  rateSupportDuration: new Trend('support_rate_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'support-service',
    testType: 'baseline-crud'
  }
};

// Ticket categories and priorities
const CATEGORIES = ['ACCOUNT', 'TRANSACTION', 'PAYMENT', 'SECURITY', 'TECHNICAL', 'BILLING'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

// Test data generators
function generateTicketData(uniqueId) {
  return {
    subject: `Support Request ${uniqueId}`,
    description: `This is a test support ticket created during baseline testing. Issue ID: ${uniqueId}`,
    category: CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)],
    priority: PRIORITIES[Math.floor(Math.random() * PRIORITIES.length)],
    relatedTransactionId: `TXN${Math.floor(1000000 + Math.random() * 9000000)}`
  };
}

function generateCommentData(uniqueId) {
  return {
    message: `Comment added during test ${uniqueId}. This is a follow-up message.`,
    isInternal: false
  };
}

function generateRatingData() {
  return {
    rating: Math.floor(3 + Math.random() * 3),
    feedback: ['Good service', 'Satisfactory', 'Excellent support', 'Quick resolution'][Math.floor(Math.random() * 4)]
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

  let ticketId = null;

  group('Support Service - CRUD Operations', () => {

    // ===== READ: Get Categories =====
    group('READ: Get Categories', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.support}/categories`, {}, auth.token);
      supportMetrics.getCategoriesDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get FAQs =====
    group('READ: Get FAQs', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.support}/faqs`, { category: 'GENERAL' }, auth.token);
      supportMetrics.getFaqsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Search Knowledge Base =====
    group('READ: Search KB', () => {
      const searchQueries = ['payment', 'account', 'transfer', 'password', 'limit'];
      const query = searchQueries[Math.floor(Math.random() * searchQueries.length)];

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.support}/kb/search`, { q: query }, auth.token);
      supportMetrics.searchKnowledgeBaseDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Create Ticket =====
    group('CREATE: Create Ticket', () => {
      const ticketData = generateTicketData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.support}/tickets`, ticketData, auth.token);
      supportMetrics.createTicketDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        ticketId = result.body.ticketId || result.body.id;
        console.log(`Ticket created: ${ticketId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Tickets =====
    group('READ: List Tickets', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.support}/tickets`, { page: 0, size: 10 }, auth.token);
      supportMetrics.getTicketsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (ticketId) {
      // ===== READ: Get Ticket Detail =====
      group('READ: Get Ticket Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.support}/tickets/${ticketId}`, auth.token);
        supportMetrics.getTicketDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Add Comment =====
      group('CREATE: Add Comment', () => {
        const commentData = generateCommentData(uniqueId);

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.support}/tickets/${ticketId}/comments`, commentData, auth.token);
        supportMetrics.addCommentDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== UPDATE: Update Ticket =====
      group('UPDATE: Update Ticket', () => {
        const updateData = {
          priority: 'MEDIUM',
          description: `Updated description ${Date.now()}`
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.support}/tickets/${ticketId}`, updateData, auth.token);
        supportMetrics.updateTicketDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Rate Support =====
      group('CREATE: Rate Support', () => {
        const ratingData = generateRatingData();

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.support}/tickets/${ticketId}/rate`, ratingData, auth.token);
        supportMetrics.rateSupportDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Support Service CRUD Baseline Test - Starting');
  console.log('===============================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.support}/health`);
  console.log(`Support Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'support-service-crud'
  };
}

export function teardown(data) {
  console.log('\n===============================================');
  console.log('Support Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
