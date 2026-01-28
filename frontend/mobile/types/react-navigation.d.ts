// Global type augmentation for React Navigation theme
import { Theme } from '@react-navigation/native';

declare module '@react-navigation/native' {
  export interface DefaultTheme extends Theme {
    colors: {
      primary: string;
      background: string;
      card: string;
      text: string;
      border: string;
      notification: string;
      textSecondary: string;
    };
  }

  export function useTheme(): {
    colors: {
      primary: string;
      background: string;
      card: string;
      text: string;
      border: string;
      notification: string;
      textSecondary: string;
    };
    dark: boolean;
  };
}
