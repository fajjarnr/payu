#!/bin/bash
# E2E Test Runner - Runs tests and generates a summary report
set -e

echo "========================================="
echo "E2E Test Suite - Final Run"
echo "========================================="
echo ""

cd "$(dirname "$0")/.."

# Clean previous results
rm -rf playwright-results test-results

echo "Running E2E tests..."
echo ""

# Run tests with list reporter for real-time output
npx playwright test --reporter=list 2>&1 | tee /tmp/e2e-run-output.log

# Extract summary from output
echo ""
echo "========================================="
echo "Test Results Summary"
echo "========================================="
echo ""

# Parse the output for pass/fail counts
passed=$(grep -o "passed [0-9]*" /tmp/e2e-run-output.log | tail -1 | grep -o "[0-9]*" || echo "0")
failed=$(grep -o "failed [0-9]*" /tmp/e2e-run-output.log | tail -1 | grep -o "[0-9]*" || echo "0")
total=$((passed + failed))

echo "Total Tests: $total"
echo "Passed: $passed"
echo "Failed: $failed"

if [ $total -gt 0 ]; then
    pass_rate=$((passed * 100 / total))
    echo "Pass Rate: $pass_rate%"

    if [ $pass_rate -ge 95 ]; then
        echo "✅ PASS RATE TARGET ACHIEVED!"
    elif [ $pass_rate -ge 85 ]; then
        echo "⚠️  Pass rate good, but below 95% target"
    else
        echo "❌ Pass rate below 85% target"
    fi
fi

echo ""
echo "Full report available at: playwright-report/index.html"
echo "========================================="
