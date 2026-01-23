const eslintConfig = [
  {
    ignores: [
      "node_modules/**",
      ".next/**",
      "out/**",
      "dist/**",
      "build/**",
      "coverage/**",
      "**/*.config.ts",
      "**/*.config.js",
    ],
  },
  {
    rules: {
      "no-console": "warn",
      "no-unused-vars": "off",
      "prefer-const": "warn",
    },
  },
];

export default eslintConfig;