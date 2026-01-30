module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' } }],
    '@babel/preset-typescript',
    '@babel/preset-react',
  ],
  plugins: [
    // Strip Flow types from node_modules (especially react-native)
    ['@babel/plugin-transform-flow-strip-types', { requireDirective: false }],
  ],
  overrides: [
    {
      test: /node_modules[/\\]react-native[/\\]/,
      plugins: [
        ['@babel/plugin-transform-flow-strip-types', { requireDirective: false }],
      ],
    },
  ],
};
