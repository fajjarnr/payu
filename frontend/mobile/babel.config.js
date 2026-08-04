module.exports = function(api) {
  api.cache(true);

  const presets = ['babel-preset-expo'];

  // NativeWind 4.0.1 exposes a Babel preset, not a plugin.
  if (process.env.NODE_ENV !== 'test') {
    presets.push('nativewind/babel');
  }

  return {
    presets,
  };
};
