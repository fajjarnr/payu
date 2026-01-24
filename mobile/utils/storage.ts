import * as SecureStore from 'expo-secure-store';

export const storage = {
  async get<T>(key: string): Promise<T | null> {
    try {
      const value = await SecureStore.getItemAsync(key);
      return value ? JSON.parse(value) : null;
    } catch (error) {
      console.error(`Error reading ${key} from secure store:`, error);
      return null;
    }
  },

  async set<T>(key: string, value: T): Promise<boolean> {
    try {
      await SecureStore.setItemAsync(key, JSON.stringify(value));
      return true;
    } catch (error) {
      console.error(`Error writing ${key} to secure store:`, error);
      return false;
    }
  },

  async remove(key: string): Promise<boolean> {
    try {
      await SecureStore.deleteItemAsync(key);
      return true;
    } catch (error) {
      console.error(`Error deleting ${key} from secure store:`, error);
      return false;
    }
  },

  async clear(): Promise<boolean> {
    try {
      // Note: SecureStore doesn't have a clear method
      // You would need to track keys separately
      return true;
    } catch (error) {
      console.error('Error clearing secure store:', error);
      return false;
    }
  },
};
