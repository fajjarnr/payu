import React, { createContext, useContext, ReactNode, useEffect } from 'react';
import { useColorScheme } from 'react-native';
import { Colors } from '@/constants/theme';
import { useUIStore, selectIsDark, selectColorScheme } from '@/store/uiStore';

const ThemeContext = createContext({
  isDark: false,
  colors: Colors,
  toggleTheme: () => {},
});

export const useTheme = () => useContext(ThemeContext);

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const systemColorScheme = useColorScheme();
  const isDark = useUIStore(selectIsDark);
  const colorScheme = useUIStore(selectColorScheme);
  const toggleTheme = useUIStore((state) => state.toggleTheme);

  // Update isDark based on system color scheme when set to 'system'
  useEffect(() => {
    if (colorScheme === 'system') {
      const shouldBeDark = systemColorScheme === 'dark';
      useUIStore.getState().setIsDark(shouldBeDark);
    }
  }, [systemColorScheme, colorScheme]);

  const colors = Colors;

  return (
    <ThemeContext.Provider value={{ isDark, colors, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
