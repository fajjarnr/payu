# PayU — TODO List

> Terakhir diperbarui: 2026-02-11  
> Status saat ini: **37/38 container healthy** (kyc-service masih butuh fix image)  
> Last commit: `235262f` — fix: end-to-end auth pipeline and BFF login flow

---

## 🔴 P0 — Belum Di-commit (Harus Commit Segera)

Perubahan ini sudah diterapkan dan berfungsi, tapi belum di-commit ke git.

### 0.1 — Commit fix container: partner-service, api-portal-service, compose

- **File yang berubah:**
  - `backend/partner-service/src/main/resources/application-container.yml` — tambah `spring.data.redis.host/port`, management health probes
  - `backend/api-portal-service/src/main/resources/application.yaml` — port jadi `${QUARKUS_HTTP_PORT:8080}`, Keycloak URL default ke `http://keycloak:8080`
  - `backend/api-portal-service/Containerfile` — ganti `target/*.jar` → `target/*-runner.jar` (Quarkus uber-jar)
  - `infrastructure/local-podman/podman-compose.yml` — tambah `PAYU_CACHE_REDIS_HOST/PORT` untuk partner, `QUARKUS_HTTP_PORT/KEYCLOAK_URL` untuk api-portal, fix healthcheck endpoint
- **Command:** `git add -A && git commit -m "fix: container fixes for partner-service and api-portal-service"`

---

## 🔴 P0 — Container Image Fixes

### 0.2 — Fix Python container images: analytics-service & kyc-service

- **Problem:** Setiap kali container di-restart dari image, `setuptools` hilang karena runtime stage pakai base image tanpa setuptools. Builder stage install via `requirements.txt` tapi COPY hanya copy app code, bukan site-packages.
- **Root cause:** Containerfile Stage 1 (builder) `pip install -r requirements.txt` → install setuptools. Stage 2 (runtime) COPY hanya `/app/src`, bukan environment Python dari builder.
- **Fix:** Pada Containerfile kedua service, tambahkan `RUN pip install 'setuptools<81'` di runtime stage (setelah COPY app), ATAU copy seluruh virtual environment dari builder stage.
- **Files:**
  - `backend/analytics-service/Containerfile` — tambah `RUN pip install 'setuptools<81'` di runtime stage
  - `backend/kyc-service/Containerfile` — sama
- **Lalu rebuild image:** `podman build --no-cache -f Containerfile -t payu-analytics-service:latest .` (untuk kedua service)

### 0.3 — Rebuild api-portal-service image dengan uber-jar

- **Status:** Sudah dibangun dan deploy, tapi perlu pastikan build command terdokumentasi
- **Build command:** `cd backend/api-portal-service && mvn package -DskipTests -Dquarkus.package.jar.type=uber-jar`
- **Lalu:** `podman build --no-cache -f Containerfile -t payu-api-portal-service:latest .`
- **Alternatif permanen:** Tambah property `quarkus.package.jar.type=uber-jar` di `pom.xml` properties

---

## 🔴 P0 — Frontend Critical (Auth & Data Pipeline)

### 0.4 — Fix useAuth login: gunakan real user data dari BFF response

- **File:** `frontend/web-app/src/hooks/useAuth.ts` (line 23-33)
- **Problem:** `onSuccess` callback membuat `mockUser` kosong, bukan pakai data user asli dari response BFF login. `accountId` di-set empty string `''`.
- **Impact:** Semua hook yang gated `enabled: !!accountId` (balance, transactions, wallet, dll) tidak pernah fire. Dashboard kosong tanpa data.
- **Fix:** Ganti `mockUser` dengan data dari `response.data.user` (yang sudah di-decode dari JWT oleh BFF). Set `accountId` dari `user.id` (sub claim).

### 0.5 — Fix isAuthenticated hilang setelah page refresh

- **File:** `frontend/web-app/src/stores/authStore.ts` (line 82-86)
- **Problem:** `isAuthenticated` tidak di-persist di localStorage (intentional), tapi setelah refresh nilainya `false`. Ini menyebabkan WebSocket dan beberapa guard tidak berfungsi.
- **Fix:** Derive `isAuthenticated` dari `!!user && !!accountId` sebagai computed value, bukan standalone boolean. Atau persist `isAuthenticated` di partialize.

### 0.6 — Fix login redirect ke `/dashboard` bukan `/`

- **File:** `frontend/web-app/src/app/[locale]/login/page.tsx` (line 49)
- **Problem:** `router.push('/')` harusnya redirect ke `/dashboard` setelah login berhasil.
- **Fix:** `router.push('/dashboard')` atau lebih baik `router.push(l('/dashboard'))` (locale-aware).

---

## 🟡 P1 — Frontend High Priority

### 1.1 — Fix BFF proxy fallback masking errors

- **File:** `frontend/web-app/src/app/api/v1/[...path]/route.ts` (line 76-87)
- **Problem:** Semua GET error dari gateway di-mask dengan response `200 OK` + `{ data: null, items: [], total: 0 }`. User tidak tahu backend down.
- **Fix:** Return status 503 atau tandai response dengan `{ error: true, _fallback: true }` agar page bisa tampilkan error state.

### 1.2 — Fix WebSocket URL hardcoded localhost

- **File:** `frontend/web-app/src/hooks/useAnalytics.ts` (line 20)
- **Problem:** `NEXT_PUBLIC_WS_URL` fallback ke `ws://localhost:8080`. Ini baked at build time.
- **Fix:** Set `NEXT_PUBLIC_WS_URL` di environment atau ganti fallback ke relative WebSocket URL.

### 1.3 — Fix Bills page recentPayments query disabled

- **File:** `frontend/web-app/src/app/[locale]/bills/page.tsx` (line 38)
- **Problem:** `enabled: false` — query tidak pernah jalan.
- **Fix:** Ganti ke `enabled: !!accountId` atau `enabled: true`.

### 1.4 — Fix Bills page API endpoint mismatch

- **File:** `frontend/web-app/src/app/[locale]/bills/page.tsx` (line 35-45)
- **Problem:** Pakai `/payments` tapi backend billing-service expect `/billing/payments`.
- **Fix:** Sesuaikan endpoint dengan gateway routing.

---

## 🟡 P1 — Navigation & Routing

### 1.5 — Fix hardcoded non-locale links

- **Files:**
  - `frontend/web-app/src/app/[locale]/dashboard/page.tsx` (line 139) — `href="/investments"`
  - `frontend/web-app/src/app/[locale]/login/page.tsx` (line 180) — `href="/onboarding"`
  - `frontend/web-app/src/app/[locale]/backoffice/page.tsx` (line 14-19) — semua `/backoffice/*` links
  - `frontend/web-app/src/components/ErrorBoundary.tsx` (line 39) — `href="/"`
- **Fix:** Gunakan locale helper `l()` untuk semua internal links.

### 1.6 — Fix logout redirect non-locale-aware

- **Files:** `frontend/web-app/src/hooks/useAuth.ts` (line 51), `frontend/web-app/src/lib/api.ts` (line 78)
- **Problem:** `window.location.href = '/login'` tanpa locale prefix.
- **Fix:** Ambil current locale dan redirect ke `/${locale}/login`.

---

## 🟢 P2 — Frontend Data Integration (Hardcoded → Real API)

Dashboard components masih pakai hardcoded/mock data. Perlu integrasi dengan real backend API.

### 2.1 — BalanceCard: percentage hardcoded `45.2`

- **File:** `frontend/web-app/src/app/[locale]/dashboard/page.tsx` (line 87)

### 2.2 — FinancialHealthScore: hardcoded `score={78}`

- **File:** `frontend/web-app/src/app/[locale]/dashboard/page.tsx` (line 92)

### 2.3 — SpendingInsights: static default categories

- **File:** `frontend/web-app/src/components/dashboard/SpendingInsights.tsx` (line 40-80)

### 2.4 — StatsCharts: hardcoded monthly spending

- **File:** `frontend/web-app/src/components/dashboard/StatsCharts.tsx` (line 50-58)

### 2.5 — TransferActivity: fake transfer list

- **File:** `frontend/web-app/src/components/dashboard/TransferActivity.tsx` (line 34-39)

### 2.6 — InvestmentPerformance & BudgetTracking: no API calls

- **Files:** `frontend/web-app/src/components/dashboard/InvestmentPerformance.tsx`, `BudgetTracking.tsx`

### 2.7 — BalanceCard: "Net Worth" computed as `balance * 1.5` (fake math)

- **File:** `frontend/web-app/src/components/dashboard/BalanceCard.tsx` (line 76)

### 2.8 — Analytics page: hardcoded fallback data + trajectoryData

- **File:** `frontend/web-app/src/app/[locale]/analytics/page.tsx` (line 31-62)

### 2.9 — Pockets page: hardcoded savingGoals & sharedPockets

- **File:** `frontend/web-app/src/app/[locale]/pockets/page.tsx` (line 59-100)

### 2.10 — Transfer page: hardcoded recentContacts

- **File:** `frontend/web-app/src/app/[locale]/transfer/page.tsx` (line 84-89)

### 2.11 — Rewards page: 17x `as any` casts + hardcoded fallback

- **File:** `frontend/web-app/src/app/[locale]/rewards/page.tsx` (line 28-59)

### 2.12 — Lending page: hardcoded payLaterStats.transactions

- **File:** `frontend/web-app/src/app/[locale]/lending/page.tsx` (line 52-60)

### 2.13 — Cards page: hardcoded card details

- **File:** `frontend/web-app/src/app/[locale]/cards/page.tsx` (line 19-23)

### 2.14 — Security page: hardcoded sessions

- **File:** `frontend/web-app/src/app/[locale]/security/page.tsx` (line 17-21)

### 2.15 — Settings page: hardcoded user profile instead of auth store

- **File:** `frontend/web-app/src/app/[locale]/settings/page.tsx` (line 60-68)

### 2.16 — Backoffice page: hardcoded stats

- **File:** `frontend/web-app/src/app/[locale]/backoffice/page.tsx` (line 8-12)

### 2.17 — QRIS page: no camera/scanner API integration

- **File:** `frontend/web-app/src/app/[locale]/qris/page.tsx`

---

## 🟢 P2 — Code Quality & Type Safety

### 2.18 — Remove `as any` casts di rewards page

- **File:** `frontend/web-app/src/app/[locale]/rewards/page.tsx` — 17 occurrences

### 2.19 — Fix `as any` cast di cards page

- **File:** `frontend/web-app/src/app/[locale]/cards/page.tsx` (line 19)

### 2.20 — Fix unsafe double cast di notifications page

- **File:** `frontend/web-app/src/app/[locale]/notifications/page.tsx` (line 38-40) — `as unknown as Array<...>`

### 2.21 — Fix WebSocket reconnect stale callbacks

- **File:** `frontend/web-app/src/hooks/useWebSocket.ts` (line 64-75)

---

## 🟢 P2 — i18n / Localization

### 2.22 — Tambah `useTranslations()` ke semua page

- **Pages yang belum pakai i18n:**
  - Analytics, Pockets, Bills, Cards, Investments, QRIS, Lending, Security, Settings, Support, Rewards, Backoffice, Transfer
- **Problem:** Semua text hardcoded dalam Bahasa Indonesia, tidak support English locale.

---

## 🔵 P3 — Misc / Nice-to-Have

### 3.1 — Self-host noise texture SVG

- **Files:** `src/app/[locale]/page.tsx`, `login/page.tsx`, `onboarding/page.tsx`
- **Problem:** External dependency `https://grainy-gradients.vercel.app/noise.svg`
- **Fix:** Download dan taruh di `public/noise.svg`

### 3.2 — Verify logo.svg exists di public folder

- **File:** `frontend/web-app/src/app/[locale]/login/page.tsx` (line 64)

### 3.3 — Tambah `quarkus.package.jar.type=uber-jar` permanen di pom.xml

- **File:** `backend/api-portal-service/pom.xml`
- \*\*Supaya tidak perlu pass `-Dquarkus.package.jar.type=uber-jar` di setiap build

---

## Referensi Cepat

### Credentials

| User       | Password    | Role       |
| ---------- | ----------- | ---------- |
| admin      | P@ssw0rd123 | admin      |
| customer1  | P@ssw0rd123 | customer   |
| customer2  | P@ssw0rd123 | customer   |
| backoffice | P@ssw0rd123 | backoffice |

### Key URLs

| Service  | URL                    |
| -------- | ---------------------- |
| Web App  | http://localhost:3001  |
| Gateway  | http://localhost:8080  |
| Keycloak | http://localhost:8099  |
| Grafana  | http://localhost:3000  |
| Kafka UI | http://localhost:8088  |
| Jaeger   | http://localhost:16686 |

### Architecture

```
Browser → Next.js BFF (3001) → Gateway (8080) → Backend Services → Keycloak (8099→8080)
                ↕ httpOnly cookies          ↕ Bearer JWT
```
