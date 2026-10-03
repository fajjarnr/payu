/**
 * Lint-staged Configuration for PayU Mobile
 * Runs linters on git staged files
 *
 * @see https://github.com/okonet/lint-staged
 */

module.exports = {
  '*.{ts,tsx}': (filenames) => [
    `eslint --fix ${filenames.join(' ')}`,
    `prettier --write ${filenames.join(' ')}`,
  ],

  '*.{json,md}': (filenames) => [
    `prettier --write ${filenames.join(' ')}`,
  ],

  '*.{css,scss}': (filenames) => [
    `prettier --write ${filenames.join(' ')}`,
  ],

  '*.{yml,yaml}': (filenames) => [
    `prettier --write ${filenames.join(' ')}`,
  ],

  '*.{png,jpg,jpeg,gif,svg}': () => [
    // Add image optimization commands here if needed
    // 'npx imagemin-lint-staged'
  ],
};
