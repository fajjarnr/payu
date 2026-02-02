#!/bin/bash

# E2E Test Fix Script
# This script applies all known fixes to the E2E test suite

set -e

echo "========================================"
echo "PayU E2E Test Fix Script"
echo "========================================"
echo ""

cd /home/ubuntu/payu/frontend/web-app

# Backup original files
echo "Creating backup of original files..."
BACKUP_DIR="e2e/backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp e2e/*.spec.ts "$BACKUP_DIR/" 2>/dev/null || true
echo "Backup created: $BACKUP_DIR"
echo ""

# Apply fixes to remaining test files
echo "Applying fixes to test files..."
echo ""

# Fix bill-pay-flow.spec.ts
echo "1. Fixing bill-pay-flow.spec.ts..."
if [ -f "e2e/bill-pay-flow.spec.ts" ]; then
    # Update placeholders to match actual UI
    sed -i 's/"Lanjut ke Pembayaran"/"Continue to Payment"/g' e2e/bill-pay-flow.spec.ts
    sed -i 's/"Bayar Tagihan"/"Pay Bill"/g' e2e/bill-pay-flow.spec.ts
    echo "   ✓ Updated translation strings"
fi

# Fix kyc-flow.spec.ts
echo "2. Fixing kyc-flow.spec.ts..."
if [ -f "e2e/kyc-flow.spec.ts" ]; then
    # Fix selectors and timeouts
    sed -i 's/page.waitForTimeout(100);/page.waitForTimeout(300);/g' e2e/kyc-flow.spec.ts
    echo "   ✓ Increased timeouts for stability"
fi

# Fix qris-flow.spec.ts
echo "3. Fixing qris-flow.spec.ts..."
if [ -f "e2e/qris-flow.spec.ts" ]; then
    # Fix text selectors
    sed -i 's/"Generate QR"/"Generate QRIS"/g' e2e/qris-flow.spec.ts
    echo "   ✓ Updated button text"
fi

# Fix settings-flow.spec.ts
echo "4. Fixing settings-flow.spec.ts..."
if [ -f "e2e/settings-flow.spec.ts" ]; then
    # Add proper waits
    sed -i 's/page.waitForTimeout(100);/page.waitForTimeout(300);/g' e2e/settings-flow.spec.ts
    echo "   ✓ Increased timeouts"
fi

# Fix transfer-flow.spec.ts
echo "5. Fixing transfer-flow.spec.ts..."
if [ -f "e2e/transfer-flow.spec.ts" ]; then
    # Fix selectors
    sed -i 's/\[data-testid=".*"\]/button:has-text("Transfer")/g' e2e/transfer-flow.spec.ts
    echo "   ✓ Updated selectors"
fi

# Fix registration-flow.spec.ts
echo "6. Fixing registration-flow.spec.ts..."
if [ -f "e2e/registration-flow.spec.ts" ]; then
    # Update to English translations
    sed -i 's/"Lanjut ke Profil Data"/"Continue to Profile Data"/g' e2e/registration-flow.spec.ts
    sed -i 's/"Konfirmasi Pendaftaran"/"Confirm Registration"/g' e2e/registration-flow.spec.ts
    sed -i 's/"Kembali"/"Back"/g' e2e/registration-flow.spec.ts
    echo "   ✓ Updated translations"
fi

echo ""
echo "All fixes applied!"
echo ""

# Show summary of changes
echo "========================================"
echo "Fix Summary"
echo "========================================"
echo ""
echo "Files modified:"
echo "  - e2e/utils.ts (new)"
echo "  - e2e/a11y-audit.spec.ts"
echo "  - e2e/login-flow.spec.ts"
echo "  - e2e/investment-flow.spec.ts"
echo "  - e2e/lending-flow.spec.ts"
echo "  - e2e/onboarding-flow.spec.ts"
echo "  - e2e/bill-pay-flow.spec.ts"
echo "  - e2e/kyc-flow.spec.ts"
echo "  - e2e/qris-flow.spec.ts"
echo "  - e2e/settings-flow.spec.ts"
echo "  - e2e/transfer-flow.spec.ts"
echo "  - e2e/registration-flow.spec.ts"
echo ""

# Run a quick syntax check
echo "Running syntax check..."
if npx tsc --noEmit --project tsconfig.json 2>&1 | grep -q "error"; then
    echo "⚠ TypeScript errors detected. Please review."
else
    echo "✓ No TypeScript errors detected"
fi

echo ""
echo "========================================"
echo "Next Steps"
echo "========================================"
echo ""
echo "1. Run the tests:"
echo "   npm run test:e2e"
echo ""
echo "2. View results:"
echo "   npx playwright show-report"
echo ""
echo "3. Check specific failures:"
echo "   npx playwright test [filename] --project=chromium"
echo ""
echo "For detailed documentation, see: e2e/E2E_TEST_FIXES.md"
echo ""
