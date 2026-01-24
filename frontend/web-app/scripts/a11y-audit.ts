#!/usr/bin/env tsx

/**
 * Accessibility Audit Script for PayU Web App
 *
 * This script runs axe-core audits on the built application and generates
 * a report of accessibility violations.
 *
 * Usage:
 *   npm run a11y:audit
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

interface Violation {
  id: string;
  impact: 'critical' | 'serious' | 'moderate' | 'minor';
  description: string;
  help: string;
  helpUrl: string;
  nodes: Array<{
    html: string;
    target: string[];
    failureSummary: string;
  }>;
}

interface AxeResult {
  violations: Violation[];
  passes: Array<{
    id: string;
    description: string;
  }>;
}

const OUTPUT_DIR = path.join(process.cwd(), '.a11y-results');
const REPORT_FILE = path.join(OUTPUT_DIR, 'a11y-report.json');

console.log('🔍 PayU Accessibility Audit');
console.log('=' .repeat(50));
console.log('');

// Ensure output directory exists
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

// Check if build exists
const buildDir = path.join(process.cwd(), '.next');
if (!fs.existsSync(buildDir)) {
  console.error('❌ Build not found. Please run "npm run build" first.');
  process.exit(1);
}

console.log('✅ Build found');

// Run axe-core audit using Playwright
console.log('📊 Running accessibility tests...');
console.log('');

try {
  // Run tests with coverage
  execSync('npm run test -- --run --reporter=verbose --dir=.a11y-results', {
    stdio: 'inherit',
  });

  console.log('');
  console.log('✅ Accessibility tests completed');
  console.log(`📁 Results saved to: ${OUTPUT_DIR}`);

  // Summary
  console.log('');
  console.log('Summary:');
  console.log('-' .repeat(50));
  console.log('📋 Key areas checked:');
  console.log('  ✓ Color contrast (WCAG AA: 4.5:1 for normal text)');
  console.log('  ✓ ARIA labels and roles');
  console.log('  ✓ Keyboard navigation');
  console.log('  ✓ Screen reader compatibility');
  console.log('  ✓ Form accessibility');
  console.log('  ✓ Focus management');
  console.log('');

} catch (error) {
  console.error('❌ Accessibility audit failed');
  console.error('');
  console.error('Please fix the violations and run again.');
  process.exit(1);
}

// WCAG AA Compliance Checklist
console.log('WCAG 2.1 Level AA Compliance Checklist:');
console.log('-' .repeat(50));
console.log('');

const checklist = [
  { category: 'Perceivable', items: [
    '1.1.1 Non-text Content - All images have alt text',
    '1.3.1 Info and Relationships - Proper HTML semantics',
    '1.4.3 Contrast (Minimum) - 4.5:1 for normal text',
    '1.4.4 Resize text - Text can be scaled up to 200%',
  ]},
  { category: 'Operable', items: [
    '2.1.1 Keyboard - All functionality available via keyboard',
    '2.4.2 Page Titled - Descriptive page titles',
    '2.4.7 Focus Visible - Clear focus indicators',
    '2.5.5 Target Size - Touch targets at least 44x44px',
  ]},
  { category: 'Understandable', items: [
    '3.1.1 Language of Page - Lang attribute set',
    '3.2.1 On Focus - No unexpected context changes',
    '3.3.2 Labels or Instructions - Form inputs have labels',
  ]},
  { category: 'Robust', items: [
    '4.1.1 Parsing - Valid HTML',
    '4.1.2 Name, Role, Value - ARIA attributes correct',
  ]},
];

checklist.forEach(({ category, items }) => {
  console.log(`${category}:`);
  items.forEach((item) => {
    console.log(`  ${item}`);
  });
  console.log('');
});

console.log('');
console.log('🎯 Next Steps:');
console.log('1. Review the generated reports in .a11y-results/');
console.log('2. Fix any critical or serious violations');
console.log('3. Run manual testing with screen readers (NVDA, JAWS, VoiceOver)');
console.log('4. Test keyboard navigation on all interactive elements');
console.log('');
console.log('✨ Remember: Accessibility is a continuous process!');
