#!/usr/bin/env node
/**
 * Mock Biometric Authentication Script for Maestro E2E Tests
 *
 * This script simulates biometric authentication responses for testing purposes.
 * In CI/CD environments, this can be replaced with actual biometric mocking
 * through simulator/emulator commands.
 *
 * Usage:
 *   BIOMETRIC_RESULT=success node mock-biometric.js
 *   BIOMETRIC_RESULT=failure node mock-biometric.js
 */

const result = process.env.BIOMETRIC_RESULT || 'success';

const output = {
  success: result === 'success',
  result: result,
  timestamp: new Date().toISOString(),
};

console.error(`[Mock Biometric] Simulating ${result} result`);

console.log(JSON.stringify(output));

process.exit(result === 'success' ? 0 : 1);
