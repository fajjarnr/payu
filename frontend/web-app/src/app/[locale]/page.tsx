'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import { useTranslations, useLocale } from 'next-intl';
import { Shield, Zap, TrendingUp, ChevronRight, Menu, X, Globe, Lock, CreditCard } from 'lucide-react';
import { useState, useEffect } from 'react';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';

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
    <div className="min-h-screen bg-background text-foreground selection:bg-emerald-500/30 overflow-x-hidden">
      {/* Navigation - Floating Navbar Pattern */}
      <nav className={`fixed top-4 left-4 right-4 z-50 transition-all duration-500 rounded-[2rem] border border-border ${
        scrolled ? 'glass bg-background/80 shadow-glass py-4 px-8' : 'bg-transparent py-6 px-10'
      } max-w-7xl mx-auto`}>
        <div className="flex items-center justify-between">
          <Link href={l('/')} className="flex items-center gap-4 group cursor-pointer" aria-label="PayU Home">
            <div className="w-12 h-12 bg-emerald-500 rounded-2xl flex items-center justify-center shadow-lg shadow-emerald-500/20 group-hover:rotate-6 transition-transform">
              <span className="text-white font-black text-2xl italic leading-none">U</span>
            </div>
            <span className="text-3xl font-black bg-clip-text text-transparent bg-gradient-to-r from-white to-white/70 uppercase tracking-tighter">
              PayU
            </span>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden md:flex items-center gap-10 font-inter">
            <Link href="#features" className="text-sm font-black uppercase tracking-[0.15em] text-white/50 hover:text-emerald-500 transition-colors cursor-pointer">Fitur</Link>
            <Link href="#security" className="text-sm font-black uppercase tracking-[0.15em] text-white/50 hover:text-emerald-500 transition-colors cursor-pointer">Keamanan</Link>
            <div className="h-5 w-px bg-white/10 mx-2" />
            <Link href={l('/login')} className="text-sm font-black uppercase tracking-[0.15em] text-white/90 hover:text-emerald-500 transition-colors px-4 py-2 cursor-pointer">
              {t('login')}
            </Link>
            <Link href={l('/onboarding')} className="px-8 py-4 bg-emerald-500 text-white rounded-[1.25rem] font-black uppercase tracking-[0.12em] text-xs shadow-2xl shadow-emerald-500/30 hover:scale-105 transition-all active:scale-95 cursor-pointer">
              {t('getStarted')}
            </Link>
          </div>

          {/* Mobile Menu Button - 48x48px touch target */}
          <button 
            className="md:hidden w-12 h-12 flex items-center justify-center hover:bg-white/10 rounded-2xl transition-colors cursor-pointer" 
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
          >
            {mobileMenuOpen ? <X size={28} className="text-white" /> : <Menu size={28} className="text-white" />}
          </button>
        </div>

        {/* Mobile Nav Overlay */}
        {mobileMenuOpen && (
          <motion.div 
            initial={{ opacity: 0, scale: 0.95, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            className="md:hidden absolute top-[calc(100%+16px)] left-0 right-0 bg-background border border-border p-8 rounded-[2.5rem] shadow-2xl flex flex-col gap-6 z-50 glass"
          >
            <Link href={l('/login')} className="text-xl font-black uppercase tracking-[0.15em] py-4 text-white border-b border-white/5 cursor-pointer" onClick={() => setMobileMenuOpen(false)}>{t('login')}</Link>
            <Link href={l('/onboarding')} className="text-lg font-black uppercase tracking-[0.15em] text-white bg-emerald-500 p-6 rounded-3xl text-center shadow-xl shadow-emerald-500/30 cursor-pointer" onClick={() => setMobileMenuOpen(false)}>{t('getStarted')}</Link>
          </motion.div>
        )}
      </nav>

      <main>
        {/* Hero Section */}
        <section className="relative pt-40 pb-20 md:pt-56 md:pb-32 font-outfit">
          {/* Enhanced Background Elements */}
          <div className="absolute inset-0 -z-10 overflow-hidden">
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-[800px] bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-emerald-500/10 via-transparent to-transparent opacity-50" />
            <div className="absolute top-[-10%] right-[-10%] w-[500px] h-[500px] bg-emerald-500/5 rounded-full blur-[120px]" />
            <div className="absolute bottom-[20%] left-[-10%] w-[400px] h-[400px] bg-emerald-400/5 rounded-full blur-[100px]" />
          </div>

          <div className="max-w-7xl mx-auto px-6 sm:px-8">
            <PageTransition>
              <div className="flex flex-col items-center text-center">
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="inline-flex items-center gap-2 mb-10 px-4 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[10px] font-black tracking-[0.2em] uppercase"
                >
                  <Globe className="w-3.5 h-3.5" />
                  Pioneer of Future Banking
                </motion.div>
                
                <h1 className="text-5xl sm:text-7xl md:text-8xl font-black mb-10 leading-[0.95] tracking-tighter max-w-5xl text-white uppercase">
                  {t('heroTitle')}
                </h1>
                
                <p className="text-lg md:text-xl text-white/60 mb-14 max-w-2xl mx-auto leading-relaxed font-medium font-inter">
                  {t('heroSubtitle')}
                </p>
                
                <div className="flex flex-col sm:flex-row items-center justify-center gap-5 w-full sm:w-auto">
                  <Link href={l('/onboarding')} className="w-full sm:w-auto px-10 py-5 bg-emerald-500 text-white rounded-2xl font-black text-lg shadow-2xl shadow-emerald-500/40 hover:scale-105 hover:shadow-emerald-500/50 transition-all active:scale-95 flex items-center justify-center gap-3 cursor-pointer">
                    {t('getStarted')} <ChevronRight className="w-5 h-5" />
                  </Link>
                  <Link href={l('/dashboard')} className="w-full sm:w-auto px-10 py-5 glass rounded-2xl font-bold text-lg hover:bg-white/5 transition-all text-center border border-white/10 text-white cursor-pointer">
                    Demo Interactive
                  </Link>
                </div>

                {/* Main Preview with Premium Glass Frame */}
                <motion.div 
                  initial={{ opacity: 0, y: 100 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3, duration: 1.2, ease: [0.16, 1, 0.3, 1] }}
                  className="mt-24 md:mt-32 relative w-full group"
                >
                  <div className="relative mx-auto max-w-5xl rounded-[2.5rem] overflow-hidden p-1 bg-gradient-to-b from-white/10 to-transparent shadow-[0_50px_100px_-20px_rgba(0,0,0,0.8)]">
                    <div className="rounded-[2.25rem] overflow-hidden bg-gray-900 border border-white/5">
                      <img 
                        src="/dashboard-ui.png" 
                        alt="PayU Digital Banking Dashboard Overview" 
                        className="w-full h-auto opacity-90 group-hover:opacity-100 transition-opacity duration-700"
                      />
                    </div>
                    {/* Inner Glow Surround */}
                    <div className="absolute inset-0 rounded-[2.5rem] pointer-events-none border border-white/10" />
                  </div>
                </motion.div>
              </div>
            </PageTransition>
          </div>
        </section>

        {/* Features Grid - More balanced padding */}
        <section id="features" className="py-24 md:py-40 bg-background font-outfit">
          <div className="max-w-7xl mx-auto px-6 sm:px-8">
            <div className="mb-24 text-center space-y-6">
              <h2 className="text-4xl md:text-6xl font-black tracking-tight leading-tight text-white uppercase">
                Modern Standard.<br /><span className="text-emerald-500 italic underline decoration-emerald-500/30 underline-offset-8">Tanpa Kompromi.</span>
              </h2>
              <p className="text-lg md:text-xl text-white/50 max-w-2xl mx-auto font-medium font-inter normal-case">
                Arsitektur enterprise tingkat dunia untuk keamanan dan kecepatan eksekusi transaksi instan.
              </p>
            </div>

            <StaggerContainer className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8 md:gap-12">
              <StaggerItem>
                <FeatureCard 
                  icon={<Zap className="w-7 h-7" />}
                  title={t('features.instantTransfer.title')}
                  desc={t('features.instantTransfer.desc')}
                  badge="BI-FAST 24/7"
                />
              </StaggerItem>
              <StaggerItem>
                <FeatureCard 
                  icon={<Shield className="w-7 h-7" />}
                  title={t('features.secure.title')}
                  desc={t('features.secure.desc')}
                  badge="AES-256 BANK GRADE"
                />
              </StaggerItem>
              <StaggerItem>
                <FeatureCard 
                  icon={<TrendingUp className="w-7 h-7" />}
                  title={t('features.smartInvesting.title')}
                  desc={t('features.smartInvesting.desc')}
                  badge="AI ANALYTICS"
                />
              </StaggerItem>
            </StaggerContainer>
          </div>
        </section>

        {/* Stats Section */}
        <section className="py-24 border-y border-border bg-background/50 font-outfit">
          <div className="max-w-7xl mx-auto px-6 sm:px-8 grid grid-cols-2 md:grid-cols-4 gap-12 text-center">
            <StatItem value="2M+" label="Active Users" />
            <StatItem value="Rp50T+" label="AUM" />
            <StatItem value="99.99%" label="Uptime" />
            <StatItem value="24/7" label="Support" />
          </div>
        </section>

        {/* Big CTA Section - Emerald Card Pattern */}
        <section className="py-32 md:py-48 overflow-hidden bg-background font-outfit">
          <div className="max-w-7xl mx-auto px-6 sm:px-8">
            <div className="bg-emerald-500 rounded-[3rem] p-12 md:p-32 text-center text-white relative overflow-hidden shadow-[0_50px_100px_-20px_rgba(16,185,129,0.4)]">
              <div className="relative z-10 space-y-12">
                <div className="space-y-6">
                  <h2 className="text-5xl md:text-8xl font-black mb-8 leading-[0.9] tracking-tighter italic uppercase">
                    Join The Elite.
                  </h2>
                  <p className="text-lg md:text-2xl opacity-90 max-w-2xl mx-auto font-medium leading-relaxed font-inter text-emerald-50 normal-case">
                    Buka akun perbankan digital eksklusif Anda hanya dalam 5 menit. Efisiensi tanpa batas menanti.
                  </p>
                </div>
                <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
                  <Link href={l('/onboarding')} className="w-full sm:w-auto px-12 py-6 bg-white text-emerald-600 rounded-2xl font-black text-2xl shadow-xl hover:scale-105 hover:bg-emerald-50 transition-all active:scale-95 cursor-pointer">
                    Daftar Gratis
                  </Link>
                  <Link href={l('/support')} className="flex items-center gap-2 font-bold text-lg hover:underline underline-offset-8 cursor-pointer group">
                    Hubungi Priority Concierge <ChevronRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                  </Link>
                </div>
              </div>
              
              {/* Abstract Glass Patterns */}
              <div className="absolute top-0 left-0 w-full h-full opacity-30 pointer-events-none">
                <div className="absolute top-0 left-0 w-[500px] h-[500px] bg-white rounded-full blur-[150px] -translate-x-1/2 -translate-y-1/2" />
                <div className="absolute bottom-0 right-0 w-[600px] h-[600px] bg-emerald-300 rounded-full blur-[180px] translate-x-1/2 translate-y-1/2 opacity-40" />
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="py-24 bg-background border-t border-border font-inter">
        <div className="max-w-7xl mx-auto px-6 sm:px-8">
          <div className="grid md:grid-cols-4 gap-16 mb-24">
            <div className="md:col-span-2 space-y-10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-emerald-500 rounded-xl flex items-center justify-center">
                  <span className="text-white font-black text-xl italic leading-none">U</span>
                </div>
                <span className="text-2xl font-black tracking-tight text-white font-outfit uppercase">PayU</span>
              </div>
              <p className="text-white/50 text-lg max-w-sm leading-relaxed font-medium font-inter">
                Platform perbankan digital standalone dengan arsitektur microservices enterprise yang aman dan andal.
              </p>
            </div>
            <div className="space-y-8">
              <h4 className="font-black uppercase tracking-widest text-xs text-emerald-500">Navigasi</h4>
              <ul className="space-y-4">
                <li><Link href={l('/features')} className="text-white/40 hover:text-white transition-colors font-medium cursor-pointer">Fitur Kami</Link></li>
                <li><Link href={l('/pricing')} className="text-white/40 hover:text-white transition-colors font-medium cursor-pointer">Batas Akun</Link></li>
                <li><Link href={l('/help')} className="text-white/40 hover:text-white transition-colors font-medium cursor-pointer">Pusat Bantuan</Link></li>
              </ul>
            </div>
            <div className="space-y-8">
              <h4 className="font-black uppercase tracking-widest text-xs text-emerald-500">Keamanan</h4>
              <ul className="space-y-4">
                <li><Link href={l('/legal/privacy')} className="text-white/40 hover:text-white transition-colors font-medium cursor-pointer">Kebijakan Privasi</Link></li>
                <li><Link href={l('/legal/terms')} className="text-white/40 hover:text-white transition-colors font-medium cursor-pointer">Syarat Penggunaan</Link></li>
                <li><Link href={l('/about')} className="text-white/40 hover:text-white transition-colors font-medium cursor-pointer">Tentang Kami</Link></li>
              </ul>
            </div>
          </div>
          <div className="pt-10 border-t border-white/5 flex flex-col md:flex-row justify-between items-center gap-8">
            <p className="text-sm font-bold text-white/30">{t('footer.legal')}</p>
            <div className="flex items-center gap-4 px-5 py-2.5 bg-white/5 rounded-full text-[10px] font-black text-white/40 tracking-[0.1em] uppercase border border-white/5 italic">
              <Lock className="w-3.5 h-3.5 text-emerald-500" /> Secure Protocol v4.20.0-PRO
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({ icon, title, desc, badge }: { icon: React.ReactNode; title: string; desc: string; badge: string }) {
  return (
    <div className="p-10 md:p-14 rounded-[3rem] bg-white/[0.02] border border-white/5 hover:border-emerald-500/30 transition-all duration-500 hover:bg-white/[0.04] group shadow-2xl flex flex-col items-start text-left relative overflow-hidden">
      <div className="w-14 h-14 bg-emerald-500/10 rounded-2xl flex items-center justify-center mb-10 group-hover:bg-emerald-500 group-hover:text-white transition-all duration-500 shadow-inner">
        {icon}
      </div>
      <div className="inline-block mb-6 px-3 py-1 bg-white/5 rounded-full text-[9px] font-black tracking-widest uppercase text-emerald-500 border border-emerald-500/10">
        {badge}
      </div>
      <h3 className="text-2xl md:text-3xl font-black mb-5 tracking-tight leading-tight text-white font-outfit">{title}</h3>
      <p className="text-white/40 leading-relaxed font-medium font-inter text-lg">{desc}</p>
      
      {/* Absolute Decorative Glow */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 blur-3xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-700" />
    </div>
  );
}

function StatItem({ value, label }: { value: string; label: string }) {
  return (
    <div className="space-y-3">
      <div className="text-4xl md:text-6xl font-black tracking-tighter text-white font-outfit">{value}</div>
      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-500/60 font-inter">{label}</div>
    </div>
  );
}
