import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      "@typescript-eslint/no-explicit-any": "warn",
      "no-console": ["warn", { allow: ["warn", "error"] }],
      "@typescript-eslint/no-unused-vars": ["warn", {
        "args": "none",
        "argsIgnorePattern": "^_",
        "varsIgnorePattern": "^_",
        "caughtErrorsIgnorePattern": "^_"
      }],
      // ADR-0047: forbid float corruption of Money / branded Ids — warn to allow gradual migration
      "no-restricted-syntax": ["warn",
        { "selector": "CallExpression[callee.name='Number']", "message": "Use asMoney/asAccountId/compareCurrency — Number() corrupts Money/branded Id (ADR-0047)" },
        { "selector": "CallExpression[callee.name='parseFloat']", "message": "Use parseCurrencyExact/addCurrency/compareCurrency — parseFloat corrupts Money (ADR-0047)" }
      ],
    },
  },
  {
    files: ["src/lib/currency.ts", "src/lib/validation.ts", "src/lib/utils.ts", "src/app/api/**", "e2e/**", "src/__tests__/**"],
    rules: { "no-restricted-syntax": "off" },
  },
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
