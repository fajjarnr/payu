'use client';

import { Link } from '@/lib/navigation';
import { motion, AnimatePresence, MotionConfig } from 'framer-motion';
import { useTranslations } from 'next-intl';
import { Shield, Zap, Menu, X, PieChart, Globe } from 'lucide-react';
import { Fragment, useState, useEffect, useRef } from 'react';

const SLIDE_IDS = ['hero', 'app', 'about', 'support'] as const;

export default function LandingPage() {
  const t = useTranslations('landing');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [currentSlide, setCurrentSlide] = useState(0);
  const rawHeroTitle = t.raw('heroTitle') as string;

  const containerRef = useRef<HTMLDivElement>(null);

  const goToSlide = (index: number) => {
    const targetId = SLIDE_IDS[index];
    const el = document.getElementById(targetId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleScroll = () => {
      setScrolled(container.scrollTop > 50);
    };

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const index = SLIDE_IDS.indexOf(entry.target.id as (typeof SLIDE_IDS)[number]);
            if (index !== -1) {
              setCurrentSlide(index);
            }
          }
        });
      },
      {
        root: container,
        threshold: 0.5,
      }
    );

    container.addEventListener('scroll', handleScroll);
    SLIDE_IDS.forEach((id) => {
      const el = document.getElementById(id);
      if (el) observer.observe(el);
    });

    return () => {
      container.removeEventListener('scroll', handleScroll);
      observer.disconnect();
    };
  }, []);

  const handleNavClick = (e: React.MouseEvent, targetId: string) => {
    e.preventDefault();
    const el = document.getElementById(targetId.replace('#', ''));
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
    setMobileMenuOpen(false);
  };

  return (
    <MotionConfig reducedMotion="user">
      <div className="h-screen w-screen overflow-hidden bg-void font-inter text-snow [font-feature-settings:'ss01','ss03']">
        {/* Navigation */}
        <nav
          className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
            scrolled
              ? 'border-b border-white/[0.07] bg-void/90 py-4 backdrop-blur-xl'
              : 'bg-transparent py-8'
          }`}
        >
          <div className="mx-auto flex max-w-[1080px] items-center justify-between px-6">
            {/* Logo */}
            <Link href={'/'} className="flex items-center cursor-pointer" aria-label="PayU Home">
              <span className="text-xl font-normal tracking-tight text-snow">PayU</span>
            </Link>

            {/* Center Links */}
            <div className="hidden md:flex items-center gap-8 text-sm font-normal text-fog">
              <a href="#app" onClick={(e) => handleNavClick(e, '#app')} className={`transition-colors hover:text-snow cursor-pointer ${currentSlide === 1 ? 'text-snow' : ''}`}>
                {t('nav.features')}
              </a>
              <a href="#about" onClick={(e) => handleNavClick(e, '#about')} className={`transition-colors hover:text-snow cursor-pointer ${currentSlide === 2 ? 'text-snow' : ''}`}>
                {t('nav.about')}
              </a>
              <a href="#support" onClick={(e) => handleNavClick(e, '#support')} className={`transition-colors hover:text-snow cursor-pointer ${currentSlide === 3 ? 'text-snow' : ''}`}>
                {t('nav.support')}
              </a>
            </div>

            {/* Right */}
            <div className="flex items-center gap-3">
              <Link
                href={'/login'}
                className="hidden md:inline-flex min-h-[44px] items-center rounded-pill border border-white/[0.08] bg-white/[0.05] px-5 py-2 text-sm font-normal text-snow transition-colors hover:bg-white/10 cursor-pointer"
              >
                {t('nav.login')}
              </Link>
              <button
                className="flex size-11 items-center justify-center rounded-pill border border-white/[0.08] bg-white/[0.05] text-snow transition-colors cursor-pointer md:hidden"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                aria-expanded={mobileMenuOpen}
                aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
              >
                {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
              </button>
            </div>
          </div>
        </nav>

        {/* Mobile Nav Overlay */}
        <AnimatePresence>
          {mobileMenuOpen && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.3, ease: 'easeOut' }}
              className="fixed inset-0 z-[60] flex flex-col items-center justify-center gap-10 bg-void"
            >
              <button
                className="absolute top-8 right-6 flex size-11 items-center justify-center text-snow cursor-pointer"
                onClick={() => setMobileMenuOpen(false)}
                aria-label="Close menu"
              >
                <X size={24} />
              </button>
              <a href="#app" className="text-2xl font-normal text-chalk transition-colors hover:text-snow" onClick={(e) => handleNavClick(e, '#app')}>
                {t('nav.features')}
              </a>
              <a href="#about" className="text-2xl font-normal text-chalk transition-colors hover:text-snow" onClick={(e) => handleNavClick(e, '#about')}>
                {t('nav.about')}
              </a>
              <a href="#support" className="text-2xl font-normal text-chalk transition-colors hover:text-snow" onClick={(e) => handleNavClick(e, '#support')}>
                {t('nav.support')}
              </a>
              <Link href={'/login'} className="mt-4 inline-flex min-h-[44px] items-center rounded-pill bg-bone px-6 py-2.5 text-sm font-normal text-ink transition-colors hover:bg-snow cursor-pointer">
                {t('nav.login')}
              </Link>
            </motion.div>
          )}
        </AnimatePresence>

        <div ref={containerRef} className="h-full w-full overflow-y-auto snap-y snap-mandatory scroll-smooth">
          {/* Slide 1: Hero */}
          <section id="hero" className="relative flex h-screen w-full snap-start flex-col items-center justify-center overflow-hidden bg-void">
            <div className="relative z-10 mx-auto flex w-full max-w-[1080px] flex-col items-center px-6 pt-24 pb-12 text-center">
              <motion.div
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, ease: 'easeOut' }}
                className="mb-8 inline-flex items-center gap-2 rounded-full border border-white/[0.08] bg-white/[0.03] px-4 py-1.5 text-xs font-normal text-fog"
              >
                {t('badge')}
              </motion.div>

              <motion.h1
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, ease: 'easeOut' }}
                className="text-display font-normal text-snow lg:text-display-lg"
              >
                {rawHeroTitle.split(/<br\s*\/?>/i).map((line, index) => (
                  <Fragment key={`${index}-${line}`}>
                    {index > 0 && <br />}
                    {line}
                  </Fragment>
                ))}
              </motion.h1>

              {/* Card Mockup — Default restyle */}
              <motion.div
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, ease: 'easeOut' }}
                className="relative mt-12 aspect-[1.586] w-full max-w-[400px] rounded-card border-[0.5px] border-white/[0.08] bg-graphite p-7 shadow-panel"
              >
                <div className="flex items-start justify-between">
                  <span className="text-lg font-normal tracking-tight text-snow">PayU</span>
                  <div className="h-7 w-9 rounded-xs border border-white/[0.08] bg-charcoal" aria-hidden="true" />
                </div>
                <p className="mt-7 text-left text-base tracking-[0.22em] text-chalk">3243 4535 1345 6432</p>
                <div className="mt-7 flex items-end justify-between">
                  <div className="text-left">
                    <p className="text-[9px] font-medium tracking-wider text-fog uppercase">Valid Thru</p>
                    <p className="mt-0.5 text-sm font-medium text-snow">12/27</p>
                  </div>
                  <div className="flex -space-x-2.5" aria-hidden="true">
                    <div className="size-7 rounded-full border border-white/[0.06] bg-charcoal" />
                    <div className="size-7 rounded-full border border-white/[0.1] bg-white/[0.06]" />
                  </div>
                </div>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, ease: 'easeOut', delay: 0.1 }}
                className="mt-10 flex flex-col items-center gap-4"
              >
                <Link
                  href={'/onboarding'}
                  className="inline-flex min-h-[44px] items-center rounded-pill bg-bone px-6 py-2.5 text-sm font-normal text-ink shadow-[0_1px_4px_rgba(0,0,0,0.1),0_0_1px_rgba(0,0,0,0.1)] transition-colors hover:bg-snow cursor-pointer"
                >
                  {t('getStarted')}
                </Link>
                <p className="text-sm font-normal text-steel">{t('hero.freeAdmin')}</p>
              </motion.div>
            </div>
          </section>

          {/* Slide 2: Features */}
          <section id="app" className="relative flex h-screen w-full snap-start items-center overflow-hidden bg-void">
            <div className="mx-auto grid w-full max-w-[1080px] grid-cols-1 items-center gap-12 px-6 py-24 lg:grid-cols-12 lg:gap-16">
              <div className="space-y-6 lg:col-span-5">
                <span className="inline-block rounded-full border border-white/[0.08] bg-white/[0.03] px-4 py-1.5 text-xs font-normal text-fog">
                  {t('slide2.badge')}
                </span>
                <h2 className="text-heading-lg font-normal text-snow">
                  {t('slide2.title')} <br />
                  <span className="text-arc-blue">{t('slide2.titleHighlight')}</span>
                </h2>
                <p className="max-w-md text-subheading font-normal text-fog">{t('slide2.subtitle')}</p>
              </div>

              <div className="grid gap-4 md:grid-cols-2 lg:col-span-7">
                <div className="space-y-4">
                  <div className="rounded-card border-[0.5px] border-white/[0.07] bg-graphite p-7 transition-colors duration-300 hover:border-white/[0.14]">
                    <Zap size={16} className="mb-4 text-signal-blue" aria-hidden="true" />
                    <h3 className="mb-1.5 text-base font-normal text-snow">{t('slide2.analytics.title')}</h3>
                    <p className="text-sm leading-relaxed text-fog">{t('slide2.analytics.desc')}</p>
                  </div>
                  <div className="rounded-card border-[0.5px] border-white/[0.07] bg-graphite p-7 transition-colors duration-300 hover:border-white/[0.14]">
                    <Globe size={16} className="mb-4 text-signal-blue" aria-hidden="true" />
                    <h3 className="mb-1.5 text-base font-normal text-snow">{t('slide2.connectivity.title')}</h3>
                    <p className="text-sm leading-relaxed text-fog">{t('slide2.connectivity.desc')}</p>
                  </div>
                </div>
                <div className="md:h-full">
                  <div className="flex h-full flex-col justify-center rounded-card border-[0.5px] border-white/[0.07] bg-graphite p-7 transition-colors duration-300 hover:border-white/[0.14]">
                    <Shield size={16} className="mb-4 text-signal-blue" aria-hidden="true" />
                    <h3 className="mb-1.5 text-base font-normal text-snow">{t('slide2.security.title')}</h3>
                    <p className="text-sm leading-relaxed text-fog">{t('slide2.security.desc')}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Slide 3: Stats */}
          <section id="about" className="relative flex h-screen w-full snap-start items-center overflow-hidden bg-void">
            <div className="absolute inset-0 z-0 bg-[linear-gradient(to_right,rgba(255,255,255,0.02)_0.5px,transparent_0.5px),linear-gradient(to_bottom,rgba(255,255,255,0.02)_0.5px,transparent_0.5px)] bg-[size:100px_100px]" />

            <div className="relative z-10 mx-auto grid w-full max-w-[1080px] grid-cols-1 items-center gap-16 px-6 py-24 lg:grid-cols-2">
              <div className="space-y-10">
                <div className="space-y-6">
                  <span className="inline-block rounded-full border border-white/[0.08] bg-white/[0.03] px-4 py-1.5 text-xs font-normal text-fog">
                    {t('slide3.badge')}
                  </span>
                  <h2 className="text-heading-lg font-normal text-snow">
                    {t('slide3.title')} <br />
                    <span className="text-arc-blue">{t('slide3.titleHighlight')}</span>
                  </h2>
                </div>
                <div className="flex gap-16 border-t border-white/[0.07] pt-8">
                  <div>
                    <p className="text-heading-lg font-normal text-snow">50T+</p>
                    <p className="mt-1 text-sm font-normal text-steel">{t('slide3.statsAnnual')}</p>
                  </div>
                  <div>
                    <p className="text-heading-lg font-normal text-snow">2.4M+</p>
                    <p className="mt-1 text-sm font-normal text-steel">{t('slide3.statsTrusted')}</p>
                  </div>
                </div>
              </div>

              {/* Product panel */}
              <div className="hidden lg:block">
                <div className="overflow-hidden rounded-card border-[0.5px] border-white/[0.07] bg-graphite shadow-panel">
                  <div className="flex items-center justify-between border-b border-white/[0.05] px-5 py-3">
                    <div className="flex gap-1.5" aria-hidden="true">
                      <div className="size-2 rounded-full bg-white/[0.08]" />
                      <div className="size-2 rounded-full bg-white/[0.08]" />
                      <div className="size-2 rounded-full bg-white/[0.08]" />
                    </div>
                    <span className="inline-flex items-center gap-1.5 rounded-xs border border-mint/60 px-1.5 py-0.5 text-[9px] font-medium text-mint">
                      <span className="size-1 rounded-full bg-mint" aria-hidden="true" />
                      LIVE
                    </span>
                  </div>
                  <div className="divide-y divide-white/[0.04] px-5">
                    <div className="flex items-center justify-between py-3">
                      <span className="text-xs text-chalk">QRIS Settlement</span>
                      <span className="text-xs tabular-nums text-snow">Rp 1.240.000.000</span>
                    </div>
                    <div className="flex items-center justify-between py-3">
                      <span className="text-xs text-chalk">Transfer Clearing</span>
                      <span className="text-xs tabular-nums text-snow">Rp 892.500.000</span>
                    </div>
                    <div className="flex items-center justify-between py-3">
                      <span className="text-xs text-chalk">Virtual Accounts</span>
                      <span className="text-xs tabular-nums text-snow">Rp 407.250.000</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 border-t border-white/[0.05] px-5 py-4">
                    <PieChart size={16} className="text-signal-blue" aria-hidden="true" />
                    <div>
                      <p className="text-[9px] font-medium tracking-wider text-fog uppercase">Growth YTD</p>
                      <p className="text-sm font-medium tabular-nums text-snow">+14.2%</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Slide 4: CTA */}
          <section id="support" className="relative flex h-screen w-full snap-start items-center overflow-hidden bg-void">
            <div className="relative z-10 mx-auto flex w-full max-w-[1080px] flex-col items-center px-6 py-24 text-center">
              <h2 className="text-display font-normal text-snow lg:text-display-lg">
                {t('slide4.title')} <br />
                <span className="text-arc-blue">{t('slide4.titleHighlight')}</span>
              </h2>
              <p className="mt-6 max-w-md text-subheading font-normal text-fog">{t('slide4.subtitle')}</p>
              <Link
                href={'/onboarding'}
                className="mt-10 inline-flex min-h-[44px] items-center rounded-pill bg-bone px-6 py-2.5 text-sm font-normal text-ink shadow-[0_1px_4px_rgba(0,0,0,0.1),0_0_1px_rgba(0,0,0,0.1)] transition-colors hover:bg-snow cursor-pointer"
              >
                {t('slide4.button')}
              </Link>
            </div>

            <footer className="absolute bottom-0 left-0 right-0 border-t border-white/[0.07]">
              <div className="mx-auto flex max-w-[1080px] items-center justify-between px-6 py-6">
                <span className="text-xs font-normal text-steel">PayU</span>
                <div className="flex gap-6 text-xs font-normal text-steel">
                  <Link href={'/terms'} className="transition-colors hover:text-chalk cursor-pointer">
                    {t('slide4.terms')}
                  </Link>
                  <Link href={'/privacy'} className="transition-colors hover:text-chalk cursor-pointer">
                    {t('slide4.privacy')}
                  </Link>
                </div>
              </div>
            </footer>
          </section>
        </div>

        {/* Slide Indicators */}
        <div className="fixed bottom-10 left-1/2 z-50 flex -translate-x-1/2 gap-4">
          {SLIDE_IDS.map((id, i) => (
            <button
              key={id}
              onClick={() => goToSlide(i)}
              aria-label={`Go to ${id}`}
              className="cursor-pointer p-3"
            >
              <span
                className={`block h-1.5 rounded-full transition-all duration-300 ${
                  currentSlide === i ? 'w-8 bg-bone' : 'w-1.5 bg-white/20 hover:bg-white/40'
                }`}
              />
            </button>
          ))}
        </div>
      </div>
    </MotionConfig>
  );
}
