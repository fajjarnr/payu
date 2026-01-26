/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './App.{js,jsx,ts,tsx}',
    './components/**/*.{js,jsx,ts,tsx}',
    './app/**/*.{js,jsx,ts,tsx}',
  ],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        'bank-green': '#10b981',
        'bank-emerald': '#059669',
        'bank-dark': '#047857',
        'background': '#ffffff',
        'card': '#f9fafb',
        'border': '#e5e7eb',
        'text': '#111827',
        'text-secondary': '#6b7280',
      },
      fontFamily: {
        'inter': ['Inter_400Regular', 'Inter_500Medium', 'Inter_600SemiBold', 'Inter_700Bold', 'Inter_900Black'],
        'outfit': ['Outfit_400Regular', 'Outfit_500Medium', 'Outfit_600SemiBold', 'Outfit_700Bold', 'Outfit_900Black'],
      },
      borderRadius: {
        'xl': '1rem',
        '2xl': '1.5rem',
        '3xl': '2rem',
        '4xl': '2.5rem',
      },
    },
  },
  plugins: [],
};
