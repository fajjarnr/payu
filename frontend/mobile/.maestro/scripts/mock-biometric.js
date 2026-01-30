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

// Output format expected by Maestro
const output = {
  success: result === 'success',
  result: result,
  timestamp: new Date().toISOString(),
};

// Log for debugging
console.error(`[Mock Biometric] Simulating ${result} result`);

// Output JSON for Maestro to consume
console.log(JSON.stringify(output));

// Exit with appropriate code
process.exit(result === 'success' ? 0 : 1);
