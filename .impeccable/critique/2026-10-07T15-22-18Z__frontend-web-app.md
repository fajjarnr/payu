---
target: frontend/web-app
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 3
target_identity: "file:/home/ubuntu/payu/frontend/web-app"
timestamp: 2026-10-07T15-22-18Z
slug: frontend-web-app
closed: true
---
Method: dual-agent (A: AssessADesignReview · B: AssessBDetectorEvidence)

## Design Health Score — 23/40 (Acceptable)

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Transfer review→authorize→receipt exemplary; PromoPopup ambush at 3s, QRIS simulator toast as fake status |
| 2 | Match System / Real World | 3 | BI-FAST/SKN/RTGS, Kantong, Rp/id-ID strong; English leaks at scan/pay/card moments |
| 3 | User Control and Freedom | 3 | Back-from-review, Transfer Lagi, Esc modals; QRIS camera toggle simulated, not real |
| 4 | Consistency and Standards | 2 | Two primaries (#00D09C vs #0A6B48), Inter spec vs Outfit 800, antd+Tailwind+inline-style dialects |
| 5 | Error Prevention | 2 | Confirm gate good; free-text recipient falls back to raw accountId as name; all rails "Gratis" |
| 6 | Recognition Rather Than Recall | 2 | Favorites exist but form front-loads recall; review can show bare account-ID as recipient |
| 7 | Flexibility and Efficiency | 2 | No saved-route default, amount presets, or repeat-last-transfer one-tap |
| 8 | Aesthetic and Minimalist Design | 1 | ~10 competing dashboard regions + blur-orbs per card; signal drowns |
| 9 | Error Recovery | 3 | Inline on-blur validation, preserved form state, Transfer Lagi; real failure copy untested |
| 10 | Help and Documentation | 2 | Dead "Hubungi Kami", inactive settings items, jargon reassurance with no scam guidance |
| **Total** | | **23/40** | **Acceptable — significant improvements needed before users are happy** |

## Design Specificity Verdict

**LLM assessment**: Two products stitched together. A genuinely Indonesian money-movement core (BI-FAST/SKN/RTGS rail picker, QRIS scanner + daily-limit card, Rp/id-ID formatting, mostly-Bahasa transfer review/success) wrapped in a generic emerald-fintech dashboard (health score, VIP badge, segmented offers, investment CTA). Core flows feel PayU; shell feels any-neobank — BalanceCard trio, pastel QuickActions grid, and gradient card art with Mastercard-style circles are palette-swappable with Revolut/Jenius/SeaBank. English microcopy breaks locality at exactly the high-attention moments ("Scanning for QRIS Codes...", "E-Wallet Access", "Owner/Exp", "Beneficiaries"). Outfit 800 headings voice a second brand over the specced Inter. Missed character: QRIS — Indonesia's signature payment ritual — is a decorative frame with simulator toast and empty history; all four rails render as equal cards with no BI-FAST guidance; patungan/split-bill exists as a route but has no entry point while "Skor Kesehatan Finansial" and VIP tiers take dashboard priority.

**Deterministic scan**: `detect --json` exit 2, 7 findings across 4 files. 6 are false positives (2× broken-image in `__tests__` next/image mocks; 4× QR-scanner corner brackets on qris/page.tsx:117-120 misread as accent borders). 1 genuine finding the review missed: notifications/page.tsx:138 unread indicator combines `border-l-4` with `rounded-2xl` — thick accent on a fully-rounded card. Browser/manual evidence added what source review couldn't: axe color-contrast serious on landing (14 nodes — primary-on-tinted badges, white/15 overlays, opacity-70 muted text) and login (2 nodes); keyboard focus is browser-default 1px with no custom `:focus-visible` ring; 2 sub-44px touch targets in the login header ("Daftar Sekarang" link at 20px height); no mobile-390 overflow; reduced-motion guards present.

**Visual overlays**: none — no detect.js ships with the skill and the CLI has no visible Chromium, so injection was never attempted; evidence comes from native browser automation (landing/login rendered) plus source-level review. Auth guard redirected /id/qris and /id/notifications to login, so those two routes are source-only evidence.

## Overall Impression

The transfer review screen proves this team can do careful-banker craft; the dashboard proves it doesn't yet trust that instinct — it shouts where the review screen reads back. Biggest opportunity: make home do one job ("saldo + ada yang aneh?") with the same gravity as the confirm gate, and the whole app upgrades a band.

## What's Working

1. **Transfer review-then-authorize gate with localized receipt** — recipient + Rp amount + rail + fee + schedule + source pocket + memo before "Otorisasi Transfer Sekarang". Hardest Operate pattern, genuinely good; serves trust + financial clarity directly.
2. **Full Bahasa Indonesia money grammar** — rails, Kantong, Tagihan, Sekarang/Terjadwal/Berulang, id-ID dates, Rp tabular-nums held end to end through success. Rare, differentiating, worth protecting.
3. **Accessibility scaffolding is structural** — SkipLinks, radiogroup rail/schedule pickers, 44px day cells, aria-pressed edit mode, reduced-motion kills, id/en parity. Markup treats a11y as layout; weak point is visual (tiny uppercase muted labels), not structure.

## Priority Issues

- **[P0] Recipient identity can render as a bare account-number string at confirm time** — Why: `resolveReviewContact` falls back to the typed accountId as the recipient "name" (initial = first digit); a mistyped digit yields a confident review screen with no beneficiary-name verification — the review gate's purpose defeated, wrong-transfer/social-engineering shape. Fix: block authorize on unverified recipient — beneficiary-name lookup with hard stop + "cek kembali" state; never render an ID as a name. Evidence: transfer/page.tsx review screen. Suggested: $impeccable harden
- **[P1] Dashboard buries the job in decoration; promo modal ambushes at 3s** — Why: ~10 regions before any action; attention tax every session; low-bandwidth/older users may never reach TransferActivity, the fraud-checking surface. Fix: single-focus home (saldo + anomaly + 3 next actions), kill autoplay/popup or gate behind first-task completion. Evidence: dashboard/page.tsx + PromoPopup delay={3000}. Suggested: $impeccable distill
- **[P1] QRIS pay flow is theater** — Why: pulsing empty frame + English idle copy + "Merchant PayU Simulator" toast + "Belum ada riwayat" teaches "PayU doesn't work here" at the most time-pressured interaction. Fix: re-plan the flow — merchant-name/amount-confirm idiom, real scan states, merchant-receive path ("Tampilkan QR toko"), settlement timing. Evidence: qris/page.tsx setTimeout simulator. Suggested: $impeccable shape
- **[P1] Sidebar IA: 14 destinations against a 5-tab mental model** — Why: Transfer vs scheduled-transfers vs split-bill vs bills vs pockets vs exchange spreads money tasks across six homes; MobileNav swaps Profile for Settings. Fix: regroup to ≤5 task-based clusters, reconcile mobile/desktop. Evidence: DashboardLayout menus. Suggested: $impeccable layout
- **[P2] Rendered brand contradicts the token (forest #0A6B48 vs mint #00D09C), silently** — Why: marketing/onboarding promise mint, app delivers dark emerald; future specs against the documented token risk unreadable combos. Fix: decide — keep accessible emerald and rewrite DESIGN.md tokens, or restore mint for display + dark for text. Evidence: globals.css `--primary: 160 84% 26%`. Suggested: $impeccable document

## Persona Red Flags

**Sam (low-vision / older)**: text-[10–12px] uppercase tracking-[0.2–0.3em] muted labels at 40–70% opacity on nearly every card ("PROTOKOL KEAMANAN", "TERPAKAI HARI INI"); 31-cell 44px recurring-day grid with no list/date alternative; PAN in tracked-out mono over gradient + blur orbs; axe already flags 14 contrast nodes on landing.
**Maya (low-bandwidth / mobile-only)**: full ~10-region dashboard + 6s carousel autoplay on mobile, no condensed Operate view; bottom nav omits Bills/History/Help (drawer or dead-end "Lihat Semua Fitur" only); QRIS assumes capable hardware, no manual/offline fallback.
**Chris (skeptical-security)**: one-tap PAN reveal with no visible step-up auth; "Terlindungi Protokol Keamanan" asserts without verifiable control; "Enkripsi RESP-V3" jargon beside a demonstrably fake simulator toast; PromoPopup + SegmentedOffers read as dark patterns DESIGN.md principle 3 forbids, with no provenance.
**Bu Sari (UMKM warung owner, project-specific)**: QRIS page is pay-only — receive path is a half-built teaser behind "Tampilkan Kode Saya" with "Authorized Only" watermark; no settlement timing, merchant fee line, or nightly rekap; daily-limit card shows "Rp 10.000.000 / 0%" — zero information; patungan unreachable from QRIS or QuickActions.

## Minor Observations

- Settings ships dead controls (inactive billingPlan/privacySecurity/advanced; English "Beneficiaries" tab) — settings is where trust dies.
- "Bantuan?" card offers only a handler-less "Hubungi Kami".
- "Transfer Lagi" keeps stale rail/schedule state — could silently re-arm a scheduled payment.
- Review shows hardcoded-looking "Kantong Utama Cair / Rp 86.353.000" — doubt about which pocket is debited.
- Cards "Ubah Limit" modal edits dailyLimit but never submits monthlyLimit.
- All rails "Gratis" + "Tidak terbatas" RTGS reads as placeholder parity; SKN-vs-BI-FAST timing is the only real differentiator.
- Detector-genuine: notifications unread `border-l-4` on `rounded-2xl` card — restyle indicator to fit the radius.

## Questions to Consider

- If the review screen is this good, why does the dashboard — seen 10× more often — show zero of the same careful-banker voice?
- What is the one rails question PayU answers better than any Indonesian bank, and why is it four equal cards instead of one recommendation + three alternatives?
- If Bu Sari's QRIS settlement failed tonight, which screen tells her what happened, when it resolves, and who to call?
