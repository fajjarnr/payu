module.exports = function(api) {
  api.cache(true);

  const presets = ['babel-preset-expo'];
  const plugins = [];

  // Only add nativewind for non-test environments
  if (process.env.NODE_ENV !== 'test') {
    plugins.push('nativewind/babel');
  }

  return {
    presets,
    plugins,
  };
};
