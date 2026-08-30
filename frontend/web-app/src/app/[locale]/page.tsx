'use client';

import { Link } from '@/lib/navigation';
import { useTranslations } from 'next-intl';
import {
  BadgeCheck,
  BarChart3,
  CheckCircle2,
  Fingerprint,
  Globe,
  Lock,
  Menu,
  Plus,
  QrCode,
  ReceiptText,
  Shield,
  Wallet,
  X,
  Zap,
} from 'lucide-react';
import { Fragment, useState } from 'react';

export default function LandingPage() {
  const t = useTranslations('landing');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const rawHeroTitle = t.raw('heroTitle') as string;

  const handleNavClick = (e: React.MouseEvent, targetId: string) => {
    e.preventDefault();
    document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth' });
    setMobileMenuOpen(false);
  };

  return (
    <div className="min-h-screen bg-background font-inter text-foreground">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-border bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-[1080px] items-center justify-between px-4 sm:px-6">
          <Link href={'/'} className="flex items-center gap-1.5 cursor-pointer" aria-label="PayU Home">
            <span className="font-heading text-xl font-bold tracking-tight">PayU</span>
            <span className="size-2 rounded-full bg-primary" aria-hidden="true" />
          </Link>

          <nav className="hidden md:flex items-center gap-8 text-sm text-muted-foreground" aria-label="Main">
            <a href="#features" onClick={(e) => handleNavClick(e, 'features')} className="transition-colors hover:text-foreground cursor-pointer">
              {t('nav.features')}
            </a>
            <a href="#how" onClick={(e) => handleNavClick(e, 'how')} className="transition-colors hover:text-foreground cursor-pointer">
              {t('how.title')}
            </a>
            <a href="#about" onClick={(e) => handleNavClick(e, 'about')} className="transition-colors hover:text-foreground cursor-pointer">
              {t('nav.about')}
            </a>
          </nav>

          <div className="flex items-center gap-2.5">
            <Link
              href={'/login'}
              className="hidden sm:inline-flex min-h-[40px] items-center rounded-full border border-border bg-card px-5 text-sm font-medium transition-colors hover:bg-accent/10 cursor-pointer"
            >
              {t('nav.login')}
            </Link>
            <Link
              href={'/onboarding'}
              className="hidden sm:inline-flex min-h-[40px] items-center rounded-full bg-primary px-5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 cursor-pointer"
            >
              {t('getStarted')}
            </Link>
            <button
              className="flex size-10 items-center justify-center rounded-full border border-border bg-card cursor-pointer md:hidden shrink-0"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
            >
              {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>

        {mobileMenuOpen && (
          <div className="border-t border-border bg-background md:hidden">
            <nav className="mx-auto flex max-w-[1080px] flex-col gap-1 px-4 sm:px-6 py-4" aria-label="Mobile">
              <a href="#features" onClick={(e) => handleNavClick(e, 'features')} className="rounded-lg px-3 py-3 text-base font-medium hover:bg-muted cursor-pointer">
                {t('nav.features')}
              </a>
              <a href="#how" onClick={(e) => handleNavClick(e, 'how')} className="rounded-lg px-3 py-3 text-base font-medium hover:bg-muted cursor-pointer">
                {t('how.title')}
              </a>
              <a href="#about" onClick={(e) => handleNavClick(e, 'about')} className="rounded-lg px-3 py-3 text-base font-medium hover:bg-muted cursor-pointer">
                {t('nav.about')}
              </a>
              <Link href={'/login'} className="rounded-lg px-3 py-3 text-base font-medium hover:bg-muted cursor-pointer">
                {t('nav.login')}
              </Link>
              <Link href={'/onboarding'} className="mt-1 inline-flex min-h-[44px] items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground cursor-pointer">
                {t('getStarted')}
              </Link>
            </nav>
          </div>
        )}
      </header>

      <main>
        {/* Hero */}
        <section className="relative overflow-hidden">
          <div className="pointer-events-none absolute -top-40 right-[-12%] size-[520px] rounded-full bg-primary/10 blur-3xl" aria-hidden="true" />
          <div className="pointer-events-none absolute bottom-[-30%] left-[-10%] size-[400px] rounded-full bg-primary/5 blur-3xl" aria-hidden="true" />

          <div className="relative mx-auto grid max-w-[1080px] grid-cols-1 items-center gap-10 sm:gap-14 px-4 sm:px-6 pb-16 sm:pb-20 pt-10 sm:pt-16 lg:pt-24 lg:grid-cols-2">
            <div className="min-w-0">
              <span className="inline-block animate-fade-in rounded-full bg-primary/10 px-3 sm:px-4 py-1.5 text-[11px] sm:text-xs font-semibold text-primary motion-reduce:animate-none">
                {t('badge')}
              </span>
              <h1 className="animate-fade-in mt-4 sm:mt-6 font-heading text-[28px] leading-[1.1] sm:text-4xl lg:text-5xl xl:text-6xl font-extrabold tracking-tight motion-reduce:animate-none" style={{ animationDelay: '80ms' }}>
                {rawHeroTitle.split(/<br\s*\/?>/i).map((line, index) => (
                  <Fragment key={`${index}-${line}`}>
                    {index > 0 && <br />}
                    {line}
                  </Fragment>
                ))}
              </h1>
              <p className="animate-fade-in mt-6 max-w-md text-lg leading-relaxed text-muted-foreground motion-reduce:animate-none" style={{ animationDelay: '160ms' }}>
                {t('slide4.subtitle')}
              </p>
              <div className="animate-fade-in mt-8 flex flex-wrap items-center gap-3 motion-reduce:animate-none" style={{ animationDelay: '240ms' }}>
                <Link
                  href={'/onboarding'}
                  className="inline-flex min-h-[48px] items-center rounded-full bg-primary px-7 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-all hover:bg-primary/90 cursor-pointer"
                >
                  {t('getStarted')}
                </Link>
                <Link
                  href={'/login'}
                  className="inline-flex min-h-[48px] items-center rounded-full border border-border bg-card px-7 text-sm font-semibold transition-colors hover:bg-accent/10 cursor-pointer"
                >
                  {t('nav.login')}
                </Link>
              </div>
              <p className="animate-fade-in mt-4 text-xs text-muted-foreground motion-reduce:animate-none" style={{ animationDelay: '300ms' }}>
                {t('hero.freeAdmin')}
              </p>

              <dl className="mt-10 flex gap-10 border-t border-border pt-6">
                <div>
                  <dt className="sr-only">{t('slide3.statsAnnual')}</dt>
                  <dd className="font-heading text-2xl font-bold tabular-nums tracking-tight">50T+</dd>
                  <p className="mt-0.5 text-xs text-muted-foreground">{t('slide3.statsAnnual')}</p>
                </div>
                <div>
                  <dt className="sr-only">{t('slide3.statsTrusted')}</dt>
                  <dd className="font-heading text-2xl font-bold tabular-nums tracking-tight">2.4M+</dd>
                  <p className="mt-0.5 text-xs text-muted-foreground">{t('slide3.statsTrusted')}</p>
                </div>
                <div className="hidden sm:block">
                  <dt className="sr-only">{t('secure.item3')}</dt>
                  <dd className="flex items-center gap-1.5 font-heading text-2xl font-bold tracking-tight">
                    ISO
                    <BadgeCheck size={20} className="text-primary" aria-hidden="true" />
                  </dd>
                  <p className="mt-0.5 text-xs text-muted-foreground">{t('secure.item3')}</p>
                </div>
              </dl>
            </div>

            {/* Phone Mockup — decorative */}
            <div className="relative mx-auto hidden w-[290px] [perspective:1400px] sm:block lg:w-[310px]" aria-hidden="true">
              <div className="animate-[float-3d_8s_ease-in-out_infinite] motion-reduce:animate-none">
              <div className="absolute -inset-10 rounded-full bg-primary/10 blur-3xl" />
              <div className="relative rounded-[3rem] border border-border bg-card p-2.5 shadow-2xl shadow-primary/10">
                <div className="space-y-5 rounded-[2.5rem] bg-background px-5 pb-7 pt-4">
                  <div className="mx-auto h-1.5 w-16 rounded-full bg-border" />
                  <div className="flex items-center justify-between pt-1">
                    <div>
                      <p className="text-[10px] text-muted-foreground">PayU</p>
                      <p className="text-sm font-semibold tracking-tight">Demo Preview</p>
                    </div>
                    <div className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">P</div>
                  </div>

                  <div className="rounded-2xl bg-primary p-4 text-primary-foreground shadow-lg shadow-primary/30">
                    <p className="text-[10px] opacity-70">Total Saldo</p>
                    <p className="mt-0.5 font-heading text-xl font-bold tracking-tight tabular-nums">Rp24.562.800</p>
                    <p className="mt-3 text-[10px] font-medium opacity-70">•••• 4682</p>
                  </div>

                  <div className="grid grid-cols-4 gap-2">
                    {[
                      { icon: QrCode, label: 'QRIS' },
                      { icon: Zap, label: 'Kirim' },
                      { icon: Plus, label: 'Top Up' },
                      { icon: Wallet, label: 'Pocket' },
                    ].map(({ icon: Icon, label }) => (
                      <div key={label} className="flex flex-col items-center gap-1.5">
                        <div className="flex size-11 w-full items-center justify-center rounded-xl bg-primary/10 text-primary">
                          <Icon size={18} />
                        </div>
                        <span className="text-[9px] font-medium text-muted-foreground">{label}</span>
                      </div>
                    ))}
                  </div>

                  <div className="space-y-3">
                    {[
                      { name: 'QRIS Merchant', amount: '-Rp45.000', positive: false },
                      { name: 'Top Up Pocket', amount: '+Rp500.000', positive: true },
                      { name: 'Transfer Budi', amount: '-Rp120.000', positive: false },
                    ].map((tx) => (
                      <div key={tx.name} className="flex items-center justify-between">
                        <div className="flex items-center gap-2.5">
                          <div className={`flex size-8 items-center justify-center rounded-full ${tx.positive ? 'bg-primary/15 text-primary' : 'bg-muted text-muted-foreground'}`}>
                            {tx.positive ? <Plus size={14} /> : <QrCode size={14} />}
                          </div>
                          <span className="text-xs font-medium">{tx.name}</span>
                        </div>
                        <span className={`text-xs font-semibold tabular-nums ${tx.positive ? 'text-primary' : ''}`}>{tx.amount}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="absolute -right-12 top-16 flex items-center gap-2.5 rounded-2xl border border-border bg-card px-4 py-3 shadow-xl">
                <CheckCircle2 size={18} className="text-primary" />
                <div>
                  <p className="text-[11px] font-semibold">Payment Successful</p>
                  <p className="text-[10px] text-muted-foreground">QRIS • Rp45.000</p>
                </div>
              </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features */}
        <section id="features" className="scroll-mt-24 border-t border-border py-20 sm:py-24">
          <div className="mx-auto max-w-[1080px] px-6">
            <div className="max-w-xl">
              <span className="inline-block rounded-full bg-primary/10 px-4 py-1.5 text-xs font-semibold text-primary">
                {t('slide2.badge')}
              </span>
              <h2 className="mt-5 font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">
                {t('slide2.title')} <span className="text-primary">{t('slide2.titleHighlight')}</span>
              </h2>
              <p className="mt-4 text-muted-foreground">{t('slide2.subtitle')}</p>
            </div>

            <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {[
                { icon: BarChart3, title: t('slide2.analytics.title'), desc: t('slide2.analytics.desc') },
                { icon: QrCode, title: t('features.qris.title'), desc: t('features.qris.desc') },
                { icon: Wallet, title: t('features.pockets.title'), desc: t('features.pockets.desc') },
                { icon: ReceiptText, title: t('features.bills.title'), desc: t('features.bills.desc') },
                { icon: Globe, title: t('slide2.connectivity.title'), desc: t('slide2.connectivity.desc') },
                { icon: Shield, title: t('slide2.security.title'), desc: t('slide2.security.desc') },
              ].map(({ icon: Icon, title, desc }) => (
                <div key={title} className="group rounded-2xl border border-border bg-card p-7 transition-all duration-300 hover:-translate-y-1 hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5">
                  <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                    <Icon size={20} />
                  </div>
                  <h3 className="mt-5 font-heading text-lg font-bold tracking-tight">{title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* How it works */}
        <section id="how" className="scroll-mt-24 py-20 sm:py-24">
          <div className="mx-auto max-w-[1080px] px-6">
            <div className="rounded-3xl bg-muted/50 px-8 py-14 sm:px-14">
              <h2 className="font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">{t('how.title')}</h2>
              <ol className="mt-12 grid gap-10 sm:grid-cols-3 sm:gap-6">
                {[
                  { n: 1, title: t('how.step1.title'), desc: t('how.step1.desc') },
                  { n: 2, title: t('how.step2.title'), desc: t('how.step2.desc') },
                  { n: 3, title: t('how.step3.title'), desc: t('how.step3.desc') },
                ].map(({ n, title, desc }) => (
                  <li key={n} className="relative border-t-2 border-primary/20 pt-6 sm:border-t-0 sm:border-l-2 sm:border-l-primary/20 sm:pl-8 sm:pt-0">
                    <span className="font-heading text-5xl font-extrabold tracking-tighter text-primary/25" aria-hidden="true">
                      0{n}
                    </span>
                    <h3 className="mt-3 font-heading text-lg font-bold tracking-tight">{title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{desc}</p>
                  </li>
                ))}
              </ol>
            </div>
          </div>
        </section>

        {/* Security / About */}
        <section id="about" className="scroll-mt-24 pb-20 sm:pb-24">
          <div className="mx-auto max-w-[1080px] px-6">
            <div className="relative overflow-hidden rounded-3xl bg-primary px-8 py-14 text-primary-foreground sm:px-14 sm:py-16">
              <div className="pointer-events-none absolute -right-24 -top-24 size-72 rounded-full bg-white/10 blur-2xl" aria-hidden="true" />
              <div className="pointer-events-none absolute -bottom-28 -left-20 size-80 rounded-full bg-black/10 blur-2xl" aria-hidden="true" />

              <div className="relative grid gap-12 lg:grid-cols-2 lg:items-center">
                <div>
                  <span className="inline-block rounded-full bg-white/15 px-4 py-1.5 text-xs font-semibold">
                    {t('slide3.badge')}
                  </span>
                  <h2 className="mt-5 font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">
                    {t('slide3.title')} <span className="opacity-70">{t('slide3.titleHighlight')}</span>
                  </h2>
                  <p className="mt-4 max-w-md leading-relaxed opacity-80">{t('slide2.security.desc')}</p>
                </div>

                <ul className="space-y-4">
                  {[
                    { icon: Lock, label: t('secure.item1') },
                    { icon: Fingerprint, label: t('secure.item2') },
                    { icon: BadgeCheck, label: t('secure.item3') },
                  ].map(({ icon: Icon, label }) => (
                    <li key={label} className="flex items-center gap-4 rounded-2xl bg-white/10 px-5 py-4 backdrop-blur-sm">
                      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/15">
                        <Icon size={18} aria-hidden="true" />
                      </span>
                      <span className="text-sm font-semibold">{label}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="relative mt-14 flex gap-14 border-t border-white/15 pt-8">
                <div>
                  <p className="font-heading text-3xl font-extrabold tabular-nums tracking-tighter sm:text-4xl">50T+</p>
                  <p className="mt-1 text-sm opacity-70">{t('slide3.statsAnnual')}</p>
                </div>
                <div>
                  <p className="font-heading text-3xl font-extrabold tabular-nums tracking-tighter sm:text-4xl">2.4M+</p>
                  <p className="mt-1 text-sm opacity-70">{t('slide3.statsTrusted')}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* CTA */}
        <section id="support" className="scroll-mt-24 border-t border-border py-24 sm:py-32">
          <div className="mx-auto flex max-w-[1080px] flex-col items-center px-6 text-center">
            <h2 className="max-w-2xl font-heading text-4xl font-extrabold tracking-tight sm:text-5xl">
              {t('slide4.title')} <span className="text-primary">{t('slide4.titleHighlight')}</span>
            </h2>
            <p className="mt-5 max-w-md text-muted-foreground">{t('slide4.subtitle')}</p>
            <Link
              href={'/onboarding'}
              className="mt-9 inline-flex min-h-[48px] items-center rounded-full bg-primary px-8 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-colors hover:bg-primary/90 cursor-pointer"
            >
              {t('slide4.button')}
            </Link>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-border bg-muted/40">
        <div className="mx-auto max-w-[1080px] px-6 py-12">
          <div className="flex flex-col justify-between gap-8 sm:flex-row">
            <div className="max-w-xs">
              <div className="flex items-center gap-1.5">
                <span className="font-heading text-lg font-bold tracking-tight">PayU</span>
                <span className="size-1.5 rounded-full bg-primary" aria-hidden="true" />
              </div>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{t('footer.tagline')}</p>
            </div>
            <div className="flex flex-col gap-2 text-sm text-muted-foreground">
              <span className="mb-1 text-xs font-semibold uppercase tracking-wider text-foreground/60">Legal</span>
              <Link href={'/terms'} className="transition-colors hover:text-foreground cursor-pointer">
                {t('slide4.terms')}
              </Link>
              <Link href={'/privacy'} className="transition-colors hover:text-foreground cursor-pointer">
                {t('slide4.privacy')}
              </Link>
            </div>
          </div>
          <div className="mt-10 border-t border-border pt-6 text-xs text-muted-foreground">
            © 2026 PayU. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
