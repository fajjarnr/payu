/**
 * Lint-staged Configuration for PayU Mobile
 * Runs linters on git staged files
 *
 * @see https://github.com/okonet/lint-staged
 */

module.exports = {
  // TypeScript and React files
  '*.{ts,tsx}': (filenames) => [
    // ESLint with auto-fix
    `eslint --fix ${filenames.join(' ')}`,
    // Prettier formatting
    `prettier --write ${filenames.join(' ')}`,
  ],

  // JSON and Markdown files
  '*.{json,md}': (filenames) => [
    // Prettier formatting only (no ESLint for these files)
    `prettier --write ${filenames.join(' ')}`,
  ],

  // CSS files
  '*.{css,scss}': (filenames) => [
    // Prettier formatting
    `prettier --write ${filenames.join(' ')}`,
  ],

  // Configuration files
  '*.{yml,yaml}': (filenames) => [
    // Prettier formatting
    `prettier --write ${filenames.join(' ')}`,
  ],

  // Image files (optional: optimize images)
  '*.{png,jpg,jpeg,gif,svg}': () => [
    // Add image optimization commands here if needed
    // 'npx imagemin-lint-staged'
  ],
};
