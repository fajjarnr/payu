'use client';

import Image from 'next/image';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { useTranslations, useLocale } from 'next-intl';
import { Shield, Zap, Menu, X } from 'lucide-react';
import { useState, useEffect } from 'react';
import BentoGrid from '@/components/landing/BentoGrid';
import LogoTicker from '@/components/landing/LogoTicker';

export default function LandingPage() {
  const t = useTranslations('landing');
  const locale = useLocale();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  // Helper for localized links
  const l = (path: string) => locale === 'id' ? path : `/${locale}${path}`;

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-white text-slate-900 selection:bg-emerald-500/30 overflow-x-hidden font-inter">
      {/* Navigation - Clean Minimalist Pattern */}
      <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        scrolled ? 'bg-white/80 backdrop-blur-lg border-b border-slate-100 py-4 shadow-sm' : 'bg-transparent py-8'
      }`}>
        <div className="max-w-7xl mx-auto px-6 md:px-12 flex items-center justify-between">
          <Link href={l('/')} className="flex items-center gap-3 group cursor-pointer" aria-label="PayU Home">
            <Image src="/logo.svg" alt="PayU Logo" width={40} height={40} className="w-10 h-10 group-hover:scale-105 transition-transform" />
            <span className="text-2xl font-bold tracking-tight text-slate-900">
              PayU
            </span>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden md:flex items-center gap-8 font-medium">
            <Link href="#app" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">{t('nav.features')}</Link>
            <Link href="#about" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">{t('nav.about')}</Link>
            <Link href="#support" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">{t('nav.support')}</Link>
            <Link href={l('/login')} className="text-sm font-semibold text-emerald-600 hover:bg-emerald-50 px-4 py-2 rounded-lg transition-all cursor-pointer">{t('nav.login')}</Link>
            <Link href={l('/onboarding')} className="px-6 py-3 bg-slate-900 text-white hover:bg-slate-800 rounded-full text-sm font-semibold transition-all active:scale-95 cursor-pointer shadow-lg shadow-slate-900/20">
              {t('nav.openAccount')}
            </Link>
          </div>

          {/* Mobile Menu Button */}
          <button 
            className="md:hidden w-10 h-10 flex items-center justify-center hover:bg-slate-100 rounded-full transition-colors cursor-pointer" 
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        {/* Mobile Nav Overlay */}
        {mobileMenuOpen && (
          <motion.div 
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="md:hidden absolute top-full left-0 right-0 bg-white border-b border-slate-100 p-8 shadow-xl flex flex-col gap-6 font-medium"
          >
            <Link href="#app" className="text-lg text-slate-600" onClick={() => setMobileMenuOpen(false)}>{t('nav.features')}</Link>
            <Link href={l('/login')} className="text-lg text-slate-600" onClick={() => setMobileMenuOpen(false)}>{t('nav.login')}</Link>
            <Link href={l('/onboarding')} className="text-lg bg-emerald-500 text-white p-4 rounded-2xl text-center shadow-lg shadow-emerald-500/20" onClick={() => setMobileMenuOpen(false)}>{t('nav.openAccount')}</Link>
          </motion.div>
        )}
      </nav>

      <main>
        {/* Hero Section - Standard Side-by-Side Pattern */}
        <section className="relative pt-32 pb-20 md:pt-48 md:pb-32 overflow-hidden">
          {/* Animated Background Mesh */}
          <div className="absolute top-0 right-0 -z-10 opacity-30 pointer-events-none overflow-hidden h-[800px] w-full max-w-[100vw]">
             <div className="absolute top-[-20%] right-[-10%] w-[800px] h-[800px] bg-emerald-300/30 rounded-full blur-[120px] animate-pulse" style={{ animationDuration: '4s' }} />
             <div className="absolute top-[20%] right-[20%] w-[600px] h-[600px] bg-blue-300/30 rounded-full blur-[100px]" />
          </div>

          <div className="max-w-7xl mx-auto px-6 md:px-12">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-20 items-center">
              {/* Left Column - Content */}
              <div className="space-y-8 max-w-2xl text-left relative z-10">
                <motion.div 
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-100 text-emerald-600 text-xs font-bold uppercase tracking-widest"
                >
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                  {t('badge')}
                </motion.div>
                
                <motion.h1 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.1 }}
                  className="text-5xl sm:text-7xl font-bold leading-[1.1] tracking-tight text-slate-900"
                  dangerouslySetInnerHTML={{ __html: t.raw('heroTitle') }}
                />
                
                <motion.p 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.2 }}
                  className="text-lg md:text-xl text-slate-500 leading-relaxed max-w-lg"
                >
                  {t('heroSubtitle')}
                </motion.p>
                
                <motion.div 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3 }}
                  className="pt-4 flex flex-wrap gap-4"
                >
                   <Link href={l('/onboarding')} className="px-8 py-4 bg-slate-900 hover:bg-slate-800 text-white rounded-2xl font-bold text-lg shadow-xl shadow-slate-900/20 active:scale-95 transition-all">
                      {t('getStarted')}
                   </Link>
                   <Link href="#video" className="px-8 py-4 bg-white text-slate-900 border border-slate-200 hover:bg-slate-50 rounded-2xl font-bold text-lg active:scale-95 transition-all flex items-center gap-2">
                      <Zap className="w-5 h-5 text-emerald-500" /> {t('demo')}
                   </Link>
                </motion.div>
                
                <div className="pt-8 flex items-center gap-4 text-sm text-slate-500 font-medium">
                    <div className="flex -space-x-2">
                        {[1,2,3,4].map(i => (
                            <div key={i} className={`w-8 h-8 rounded-full border-2 border-white bg-gray-${i*100} flex items-center justify-center text-xs overflow-hidden`}>
                                <img src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${i}`} alt="User" />
                            </div>
                        ))}
                    </div>
                    <div>{t('trustedBy', { count: '2M+' })}</div>
                </div>
              </div>

              {/* Right Column - Premium Mockup */}
              <motion.div 
                initial={{ opacity: 0, scale: 0.8, rotate: 5 }}
                animate={{ opacity: 1, scale: 1, rotate: 0 }}
                transition={{ delay: 0.4, duration: 1, ease: [0.16, 1, 0.3, 1] }}
                className="relative z-10 lg:translate-x-12"
              >
                <div className="relative z-10 select-none">
                  <img 
                    src="/hero-mockup.svg" 
                    alt="PayU App Interface Mockup" 
                    className="w-full max-w-[700px] mx-auto drop-shadow-[0_40px_80px_rgba(0,0,0,0.15)] hover:scale-[1.02] transition-transform duration-700 will-change-transform"
                  />
                </div>
              </motion.div>
            </div>
          </div>
        </section>

        {/* Logo Ticker */}
        <LogoTicker />

        {/* Bento Grid Features */}
        <section id="app" className="bg-slate-50/50">
           <BentoGrid />
        </section>

        {/* Highlight Section */}
        <section className="py-32 border-y border-slate-100 bg-white relative overflow-hidden">
          <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 touch-none pointer-events-none" />
          <div className="max-w-7xl mx-auto px-6 md:px-12 text-center space-y-16 relative z-10">
            <div className="space-y-4">
                <h2 className="text-4xl md:text-6xl font-bold tracking-tight">{t('features.title')}</h2>
                <p className="text-xl text-slate-500">{t('features.subtitle')}</p>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
              <StatItem value="2M+" label={t('stats.activeUsers')} />
              <StatItem value="Rp50T+" label={t('stats.transactionVolume')} />
              <StatItem value="99.99%" label={t('stats.uptime')} />
              <StatItem value="<50ms" label={t('stats.latency')} />
            </div>
          </div>
        </section>

        {/* Call to Action */}
        <section className="py-32 relative overflow-hidden">
          <div className="absolute inset-0 bg-slate-900 -z-20" />
           {/* Abstract Glows */}
          <div className="absolute top-0 left-1/4 w-[500px] h-[500px] bg-emerald-500/20 rounded-full blur-[120px] -z-10" />
          <div className="absolute bottom-0 right-1/4 w-[500px] h-[500px] bg-blue-500/20 rounded-full blur-[120px] -z-10" />

          <div className="max-w-4xl mx-auto px-6 text-center space-y-12 text-white">
            <h2 className="text-5xl md:text-7xl font-bold tracking-tight">{t('cta.title')}</h2>
            <p className="text-xl text-slate-400 max-w-2xl mx-auto leading-relaxed">
              {t('cta.subtitle')}
            </p>
            <div className="flex flex-col sm:flex-row justify-center gap-6 pt-8">
               <Link href={l('/onboarding')} className="px-12 py-6 bg-emerald-500 text-white rounded-2xl font-bold text-xl shadow-xl shadow-emerald-500/20 hover:scale-105 hover:bg-emerald-400 transition-all active:scale-95">
                 {t('cta.button')}
               </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="py-16 bg-white border-t border-slate-100">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="flex flex-col md:flex-row justify-between items-center gap-8">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg flex items-center justify-center overflow-hidden">
                <Image src="/logo.svg" alt="PayU Logo" width={32} height={32} />
              </div>
              <span className="text-xl font-bold text-slate-900">PayU</span>
            </div>
            <div className="flex gap-8 text-sm text-slate-500 font-medium">
              <Link href="#" className="hover:text-slate-900">{t('footer.features')}</Link>
              <Link href="#" className="hover:text-slate-900">{t('footer.career')}</Link>
              <Link href="#" className="hover:text-slate-900">{t('footer.blog')}</Link>
              <Link href="#" className="hover:text-slate-900">{t('footer.privacy')}</Link>
            </div>
            <p className="text-sm text-slate-400">{t('footer.rights')}</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

function StatItem({ value, label }: { value: string; label: string }) {
  return (
    <div className="space-y-2">
      <div className="text-4xl md:text-6xl font-bold text-slate-900 tracking-tighter">{value}</div>
      <div className="text-sm font-bold text-emerald-600 uppercase tracking-widest">{label}</div>
    </div>
  );
}
