# Changelog

## 2026-10-07 — Restore files lost to backdrop-filter removal

Commit: `a76b949b1` (`fix(web-app): resolve P0/P1/P2 audit issues`)

During emergency fixes, a `backdrop-filter` removal pass deleted JSX blocks
alongside the CSS declarations, blanking out whole components/pages. The
following 8 files were restored from git HEAD:

- `src/components/MobileHeader.tsx`
- `src/app/[locale]/page.tsx`
- `src/app/[locale]/backoffice/layout.tsx`
- `src/components/cms/BannerCarousel.tsx`
- `src/app/[locale]/cards/page.tsx`
- `src/app/[locale]/backoffice/fraud/[id]/page.tsx`
- `src/app/[locale]/merchant/register/page.tsx`
- `src/app/[locale]/qris/page.tsx`

Reason: the backdrop-filter removal had deleted JSX blocks, not just the
`backdrop-filter` CSS rules.
