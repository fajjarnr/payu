import { describe, it, expect } from 'vitest';
import {
  isVerifiedReviewContact,
  resolveReviewContact,
  type ReviewContact,
} from '@/app/[locale]/transfer/page';

const favorites: ReviewContact[] = [
  { name: 'Budi', initial: 'B', color: 'bg-emerald-100', accountId: '1002003001' },
];

describe('resolveReviewContact', () => {
  it('prefers the selected favorite contact', () => {
    expect(resolveReviewContact(favorites, '1002003001', '1002003001')).toEqual(favorites[0]);
  });

  it('returns undefined for a typed account id with no verified name', () => {
    expect(resolveReviewContact(favorites, null, '1001001002')).toBeUndefined();
  });

  it('returns undefined when nothing is known', () => {
    expect(resolveReviewContact(favorites, null, null)).toBeUndefined();
    expect(resolveReviewContact([], null, '')).toBeUndefined();
  });
});

describe('isVerifiedReviewContact', () => {
  it('accepts a favorite contact', () => {
    expect(isVerifiedReviewContact(favorites[0])).toBe(true);
  });

  it('rejects missing contacts', () => {
    expect(isVerifiedReviewContact(undefined)).toBe(false);
  });
});
