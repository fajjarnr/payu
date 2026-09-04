import { describe, it, expect } from 'vitest';
import { resolveReviewContact, type ReviewContact } from '@/app/[locale]/transfer/page';

const favorites: ReviewContact[] = [
  { name: 'Budi', initial: 'B', color: 'bg-emerald-100', accountId: '1002003001' },
];

describe('resolveReviewContact', () => {
  it('prefers the selected favorite contact', () => {
    expect(resolveReviewContact(favorites, '1002003001', '1002003001')).toEqual(favorites[0]);
  });

  it('falls back to a manually typed account id', () => {
    expect(resolveReviewContact(favorites, null, '1001001002')).toEqual({
      name: '1001001002',
      initial: '1',
      color: 'bg-muted text-muted-foreground',
      accountId: '1001001002',
    });
  });

  it('returns undefined when nothing is known', () => {
    expect(resolveReviewContact(favorites, null, null)).toBeUndefined();
    expect(resolveReviewContact([], null, '')).toBeUndefined();
  });
});
