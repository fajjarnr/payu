// PayU Investment Service - CRUD Baseline Performance Test
// =========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, del, list } from '../lib/crud-helper.js';

// Service-specific metrics
const investmentMetrics = {
  createPortfolioDuration: new Trend('investment_create_portfolio_duration'),
  getPortfolioDuration: new Trend('investment_get_portfolio_duration'),
  listPortfoliosDuration: new Trend('investment_list_portfolios_duration'),
  buyFundDuration: new Trend('investment_buy_fund_duration'),
  sellFundDuration: new Trend('investment_sell_fund_duration'),
  getFundListDuration: new Trend('investment_get_fund_list_duration'),
  getPerformanceDuration: new Trend('investment_get_performance_duration'),
  updatePortfolioDuration: new Trend('investment_update_portfolio_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'investment-service',
    testType: 'baseline-crud'
  }
};

// Test data generators
function generatePortfolioData(uniqueId) {
  const portfolioTypes = ['MUTUAL_FUND', 'STOCK', 'BONDS', 'MIXED'];
  return {
    name: `Portfolio ${uniqueId}`,
    type: portfolioTypes[Math.floor(Math.random() * portfolioTypes.length)],
    description: `Test portfolio ${uniqueId}`,
    initialInvestment: Math.floor(1000000 + Math.random() * 9000000),
    riskProfile: ['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE'][Math.floor(Math.random() * 3)],
    investmentGoal: 'LONG_TERM_GROWTH'
  };
}

function generateBuyFundData() {
  return {
    fundCode: `FUND${Math.floor(100 + Math.random() * 900)}`,
    amount: Math.floor(100000 + Math.random() * 900000),
    paymentMethod: 'WALLET'
  };
}

function generateSellFundData() {
  return {
    fundCode: `FUND${Math.floor(100 + Math.random() * 900)}`,
    units: Math.floor(10 + Math.random() * 90),
    sellAll: false
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

  let portfolioId = null;

  group('Investment Service - CRUD Operations', () => {

    // ===== READ: Get Fund List =====
    group('READ: Get Fund List', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.investment}/funds`, { page: 0, size: 20 }, auth.token);
      investmentMetrics.getFundListDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Create Portfolio =====
    group('CREATE: Create Portfolio', () => {
      const portfolioData = generatePortfolioData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.investment}/portfolios`, portfolioData, auth.token);
      investmentMetrics.createPortfolioDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        portfolioId = result.body.portfolioId || result.body.id;
        console.log(`Portfolio created: ${portfolioId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Portfolios =====
    group('READ: List Portfolios', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.investment}/portfolios`, { page: 0, size: 10 }, auth.token);
      investmentMetrics.listPortfoliosDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (portfolioId) {
      // ===== READ: Get Portfolio =====
      group('READ: Get Portfolio', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.investment}/portfolios/${portfolioId}`, auth.token);
        investmentMetrics.getPortfolioDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== READ: Get Performance =====
      group('READ: Get Performance', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.investment}/portfolios/${portfolioId}/performance`, auth.token);
        investmentMetrics.getPerformanceDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Buy Fund =====
      group('CREATE: Buy Fund', () => {
        const buyData = generateBuyFundData();

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.investment}/portfolios/${portfolioId}/buy`, buyData, auth.token);
        investmentMetrics.buyFundDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== CREATE: Sell Fund =====
      group('CREATE: Sell Fund', () => {
        const sellData = generateSellFundData();

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.investment}/portfolios/${portfolioId}/sell`, sellData, auth.token);
        investmentMetrics.sellFundDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== UPDATE: Update Portfolio =====
      group('UPDATE: Update Portfolio', () => {
        const updateData = {
          name: `Updated Portfolio ${Date.now()}`,
          description: 'Updated during baseline test'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.investment}/portfolios/${portfolioId}`, updateData, auth.token);
        investmentMetrics.updatePortfolioDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Investment Service CRUD Baseline Test - Starting');
  console.log('=================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.investment}/health`);
  console.log(`Investment Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'investment-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=================================================');
  console.log('Investment Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
