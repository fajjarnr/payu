import { describe, it, expect } from 'vitest';
import { cn } from '@/lib/utils';

describe('Utility Functions', () => {
  describe('cn', () => {
    it('should merge class names correctly', () => {
      expect(cn('class1', 'class2')).toBe('class1 class2');
    });

    it('should handle conditional classes', () => {
      expect(cn('base', false && 'conditional')).toBe('base');
    });

    it('should merge tailwind classes correctly', () => {
      expect(cn('px-4 py-2', 'bg-red-500')).toBe('px-4 py-2 bg-red-500');
    });
  });
});
