// DX-CI-COMMITS-001: enforce Conventional Commits on every commit + PR title.
// Matches AGENTS.md rule 13 (`type(scope): msg`, type ∈ feat|fix|docs|chore|refactor|test|build|ci|perf|revert|style).
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'header-max-length': [2, 'always', 100],
    'scope-case': [2, 'always', ['lower-case', 'camel-case', 'kebab-case']],
    'type-enum': [2, 'always', [
      'feat', 'fix', 'docs', 'chore', 'refactor', 'test',
      'build', 'ci', 'perf', 'revert', 'style',
    ]],
  },
};
