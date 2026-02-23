// CRUD Operations Helper for K6 Baseline Tests
// =============================================
import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { getAuthHeaders } from './auth-helper.js';
import { BASE_URLS, SERVICE_ENDPOINTS, CHECKS } from '../config/baseline-config.js';

// Custom metrics for CRUD operations
const crudMetrics = {
  createDuration: new Trend('crud_create_duration'),
  readDuration: new Trend('crud_read_duration'),
  updateDuration: new Trend('crud_update_duration'),
  deleteDuration: new Trend('crud_delete_duration'),

  createSuccess: new Rate('crud_create_success'),
  readSuccess: new Rate('crud_read_success'),
  updateSuccess: new Rate('crud_update_success'),
  deleteSuccess: new Rate('crud_delete_success'),

  entitiesCreated: new Counter('entities_created_total'),
  entitiesRead: new Counter('entities_read_total'),
  entitiesUpdated: new Counter('entities_updated_total'),
  entitiesDeleted: new Counter('entities_deleted_total')
};

/**
 * Perform CREATE operation
 * @param {string} endpoint - API endpoint
 * @param {Object} payload - Request body
 * @param {string} token - Auth token
 * @param {Object} options - Additional options
 * @returns {Object} - Response and parsed body
 */
export function create(endpoint, payload, token, options = {}) {
  const startTime = new Date();

  const response = http.post(
    `${BASE_URLS.gateway}${endpoint}`,
    JSON.stringify(payload),
    {
      headers: getAuthHeaders(token),
      tags: { operation: 'create', ...options.tags }
    }
  );

  const duration = new Date() - startTime;
  crudMetrics.createDuration.add(duration);

  const success = check(response, {
    'create: status 201 or 200': (r) => r.status === 201 || r.status === 200,
    'create: valid JSON response': CHECKS.hasValidJson
  });

  crudMetrics.createSuccess.add(success);

  if (success) {
    crudMetrics.entitiesCreated.add(1);
    try {
      return { response, body: JSON.parse(response.body), success: true };
    } catch {
      return { response, body: {}, success: true };
    }
  }

  return { response, body: {}, success: false };
}

/**
 * Perform READ operation (single entity)
 * @param {string} endpoint - API endpoint with ID
 * @param {string} token - Auth token
 * @param {Object} options - Additional options
 * @returns {Object} - Response and parsed body
 */
export function read(endpoint, token, options = {}) {
  const startTime = new Date();

  const response = http.get(
    `${BASE_URLS.gateway}${endpoint}`,
    {
      headers: getAuthHeaders(token),
      tags: { operation: 'read', ...options.tags }
    }
  );

  const duration = new Date() - startTime;
  crudMetrics.readDuration.add(duration);

  const success = check(response, {
    'read: status 200': (r) => r.status === 200,
    'read: valid JSON response': CHECKS.hasValidJson
  });

  crudMetrics.readSuccess.add(success);

  if (success) {
    crudMetrics.entitiesRead.add(1);
    try {
      return { response, body: JSON.parse(response.body), success: true };
    } catch {
      return { response, body: {}, success: true };
    }
  }

  return { response, body: {}, success: false };
}

/**
 * Perform READ operation (list/query)
 * @param {string} endpoint - API endpoint
 * @param {Object} queryParams - Query parameters
 * @param {string} token - Auth token
 * @param {Object} options - Additional options
 * @returns {Object} - Response and parsed body
 */
export function list(endpoint, queryParams, token, options = {}) {
  const startTime = new Date();

  const queryString = queryParams
    ? '?' + Object.entries(queryParams).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
    : '';

  const response = http.get(
    `${BASE_URLS.gateway}${endpoint}${queryString}`,
    {
      headers: getAuthHeaders(token),
      tags: { operation: 'list', ...options.tags }
    }
  );

  const duration = new Date() - startTime;
  crudMetrics.readDuration.add(duration);

  const success = check(response, {
    'list: status 200': (r) => r.status === 200,
    'list: valid JSON response': CHECKS.hasValidJson
  });

  crudMetrics.readSuccess.add(success);

  if (success) {
    crudMetrics.entitiesRead.add(1);
    try {
      return { response, body: JSON.parse(response.body), success: true };
    } catch {
      return { response, body: {}, success: true };
    }
  }

  return { response, body: {}, success: false };
}

/**
 * Perform UPDATE operation
 * @param {string} endpoint - API endpoint with ID
 * @param {Object} payload - Request body
 * @param {string} token - Auth token
 * @param {Object} options - Additional options
 * @returns {Object} - Response and parsed body
 */
export function update(endpoint, payload, token, options = {}) {
  const startTime = new Date();

  const response = http.put(
    `${BASE_URLS.gateway}${endpoint}`,
    JSON.stringify(payload),
    {
      headers: getAuthHeaders(token),
      tags: { operation: 'update', ...options.tags }
    }
  );

  const duration = new Date() - startTime;
  crudMetrics.updateDuration.add(duration);

  const success = check(response, {
    'update: status 200 or 204': (r) => r.status === 200 || r.status === 204,
    'update: valid JSON response if 200': (r) =>
      r.status !== 200 || CHECKS.hasValidJson(r)
  });

  crudMetrics.updateSuccess.add(success);

  if (success) {
    crudMetrics.entitiesUpdated.add(1);
    try {
      return { response, body: JSON.parse(response.body), success: true };
    } catch {
      return { response, body: {}, success: true };
    }
  }

  return { response, body: {}, success: false };
}

/**
 * Perform PATCH operation (partial update)
 * @param {string} endpoint - API endpoint with ID
 * @param {Object} payload - Request body
 * @param {string} token - Auth token
 * @param {Object} options - Additional options
 * @returns {Object} - Response and parsed body
 */
export function patch(endpoint, payload, token, options = {}) {
  const startTime = new Date();

  const response = http.patch(
    `${BASE_URLS.gateway}${endpoint}`,
    JSON.stringify(payload),
    {
      headers: getAuthHeaders(token),
      tags: { operation: 'patch', ...options.tags }
    }
  );

  const duration = new Date() - startTime;
  crudMetrics.updateDuration.add(duration);

  const success = check(response, {
    'patch: status 200 or 204': (r) => r.status === 200 || r.status === 204
  });

  crudMetrics.updateSuccess.add(success);

  if (success) {
    crudMetrics.entitiesUpdated.add(1);
  }

  return { response, success };
}

/**
 * Perform DELETE operation
 * @param {string} endpoint - API endpoint with ID
 * @param {string} token - Auth token
 * @param {Object} options - Additional options
 * @returns {Object} - Response
 */
export function del(endpoint, token, options = {}) {
  const startTime = new Date();

  const response = http.del(
    `${BASE_URLS.gateway}${endpoint}`,
    null,
    {
      headers: getAuthHeaders(token),
      tags: { operation: 'delete', ...options.tags }
    }
  );

  const duration = new Date() - startTime;
  crudMetrics.deleteDuration.add(duration);

  const success = check(response, {
    'delete: status 200 or 204': (r) => r.status === 200 || r.status === 204
  });

  crudMetrics.deleteSuccess.add(success);

  if (success) {
    crudMetrics.entitiesDeleted.add(1);
  }

  return { response, success };
}

/**
 * Health check for a service
 * @param {string} servicePath - Service health endpoint path
 * @returns {boolean} - True if healthy
 */
export function healthCheck(servicePath) {
  const response = http.get(`${BASE_URLS.gateway}${servicePath}/health`);

  return check(response, {
    'health: status 200': (r) => r.status === 200,
    'health: UP status': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.status === 'UP';
      } catch {
        return false;
      }
    }
  });
}

export { crudMetrics };
