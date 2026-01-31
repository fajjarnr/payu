# P2: Mobile Data Architecture Improvements

## 🎯 Objective
Fix architecture issues identified in the audit:
- Duplicate State Management (Zustand + React Query)
- Security (Token storage)
- Performance (List rendering)
- Offline-First capability

## 🔄 Execution Status

### P2-C1: Migrate to React Query (State Management)
- [ ] Refactor `cardStore.ts` to use React Query (`hooks/useCardQuery.ts`)
- [ ] Remove manual data fetching logic from `cardStore.ts`
- [ ] Update `(tabs)/cards.tsx` to use new hooks

### P2-C2/C3: Security
- [x] Verify `storage.ts` uses `SecureStore` (Confirmed)
- [x] Verify `authStore.ts` uses `noOpStorage` for persistence (Confirmed)
- [ ] Ensure no other sensitive data is persisted in plain text

### P2-C5/C6: Performance
- [ ] Review `(tabs)/cards.tsx` and replace `ScrollView` with `FlashList` or `FlatList`
- [ ] Review `(tabs)/transfers.tsx` (Confirmed it's a form, `ScrollView` is acceptable)

### P2-C4: Offline-First
- [ ] Configure `PersistQueryClientProvider` in `app/_layout.tsx` or similar
- [ ] Ensure `AsyncStorage` is used for non-sensitive cache persistence
