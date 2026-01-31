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
    <div className="min-h-screen bg-white text-slate-900 selection:bg-emerald-500/30 overflow-x-hidden font-inter">
      {/* Navigation - Clean Minimalist Pattern */}
      <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        scrolled ? 'bg-white/80 backdrop-blur-lg border-b border-slate-100 py-4 shadow-sm' : 'bg-transparent py-8'
      }`}>
        <div className="max-w-7xl mx-auto px-6 md:px-12 flex items-center justify-between">
          <Link href={l('/')} className="flex items-center gap-3 group cursor-pointer" aria-label="PayU Home">
            <div className="w-10 h-10 bg-[#10b981] rounded-xl flex items-center justify-center shadow-lg shadow-emerald-500/20 group-hover:scale-105 transition-transform">
              <div className="relative">
                <span className="text-white font-bold text-xl leading-none">U</span>
                <div className="absolute -top-1 -right-1 flex gap-0.5">
                   <div className="w-1.5 h-1.5 bg-white/40 rounded-full" />
                   <div className="w-1.5 h-1.5 bg-white/20 rounded-full" />
                </div>
              </div>
            </div>
            <span className="text-2xl font-bold tracking-tight text-slate-900">
              PayU
            </span>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden md:flex items-center gap-12 font-medium">
            <Link href="#app" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">App</Link>
            <Link href="#about" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">About</Link>
            <Link href="#support" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">Support</Link>
            <Link href="#contact" className="text-sm text-slate-500 hover:text-slate-900 transition-colors cursor-pointer">Contact</Link>
            <Link href={l('/onboarding')} className="px-8 py-3.5 border border-slate-200 rounded-full text-sm font-semibold hover:bg-slate-50 transition-all active:scale-95 cursor-pointer ml-4">
              Get started
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
            <Link href="#app" className="text-lg text-slate-600" onClick={() => setMobileMenuOpen(false)}>App</Link>
            <Link href="#about" className="text-lg text-slate-600" onClick={() => setMobileMenuOpen(false)}>About</Link>
            <Link href="#support" className="text-lg text-slate-600" onClick={() => setMobileMenuOpen(false)}>Support</Link>
            <Link href={l('/onboarding')} className="text-lg bg-emerald-500 text-white p-4 rounded-2xl text-center shadow-lg shadow-emerald-500/20" onClick={() => setMobileMenuOpen(false)}>Get started</Link>
          </motion.div>
        )}
      </nav>

      <main>
        {/* Hero Section - Standard Side-by-Side Pattern */}
        <section className="relative pt-32 pb-20 md:pt-48 md:pb-40 overflow-hidden">
          <div className="max-w-7xl mx-auto px-6 md:px-12">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-20 items-center">
              {/* Left Column - Content */}
              <div className="space-y-8 max-w-2xl text-left">
                <motion.span 
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="inline-block text-emerald-500 font-bold uppercase tracking-[0.2em] text-sm"
                >
                  MOBILE BANK
                </motion.span>
                <motion.h1 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.1 }}
                  className="text-4xl sm:text-6xl md:text-7xl font-bold leading-[1.1] text-slate-900 tracking-tight"
                >
                  PayU: Platform Digital Banking yang Mudah, Cepat, dan Aman.
                </motion.h1>
                <motion.p 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.2 }}
                  className="text-lg text-slate-500 leading-relaxed max-w-lg"
                >
                  Platform perbankan digital mandiri yang cepat, mudah, dan aman. Infrastruktur pembayaran untuk berbagai proyek Anda.
                </motion.p>
                
                <motion.div 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3 }}
                  className="pt-4"
                >
                  <Link href="https://play.google.com" className="inline-block hover:scale-105 transition-transform duration-300">
                    <img src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg" alt="Google Play Store" className="h-16 w-auto" />
                  </Link>
                </motion.div>
              </div>

              {/* Right Column - Premium Mockup */}
              <motion.div 
                initial={{ opacity: 0, scale: 0.8, rotate: 5 }}
                animate={{ opacity: 1, scale: 1, rotate: 0 }}
                transition={{ delay: 0.4, duration: 1, ease: [0.16, 1, 0.3, 1] }}
                className="relative"
              >
                {/* Background Decorative Circle */}
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[120%] bg-slate-100 rounded-full -z-10 opacity-60" />
                
                <div className="relative z-10 select-none">
                  <img 
                    src="/hero-mockup.svg" 
                    alt="PayU App Interface Mockup" 
                    className="w-full max-w-[600px] mx-auto drop-shadow-[0_40px_80px_rgba(0,0,0,0.12)] hover:scale-[1.02] transition-transform duration-500"
                  />
                </div>
                
                {/* Floating Elements for Spark */}
                <div className="absolute top-1/4 -right-8 w-16 h-16 bg-white rounded-3xl shadow-xl flex items-center justify-center animate-bounce duration-[3000ms]">
                  <Zap className="text-emerald-500 w-8 h-8" />
                </div>
                <div className="absolute bottom-1/4 -left-8 w-20 h-20 bg-white rounded-[2rem] shadow-xl flex items-center justify-center animate-pulse">
                  <Shield className="text-blue-500 w-10 h-10" />
                </div>
              </motion.div>
            </div>
          </div>
        </section>

        {/* Features Split Section */}
        <section id="app" className="py-24 bg-slate-50">
          <div className="max-w-7xl mx-auto px-6 md:px-12 grid md:grid-cols-3 gap-12">
            <FeatureCard 
              icon={<Zap className="w-6 h-6" />}
              title="Transfer Instan"
              desc="Kirim uang ke bank mana pun dalam hitungan detik tanpa biaya tersembunyi."
            />
            <FeatureCard 
              icon={<Shield className="w-6 h-6" />}
              title="Keamanan Tinggi"
              desc="Data dan aset Anda dilindungi dengan enkripsi tingkat lanjut dan autentikasi biometrik."
            />
            <FeatureCard 
              icon={<TrendingUp className="w-6 h-6" />}
              title="Investasi Cerdas"
              desc="Tumbuhkan kekayaan Anda dengan produk investasi yang dikurasi khusus untuk Anda."
            />
          </div>
        </section>

        {/* Highlight Section */}
        <section className="py-24 border-y border-slate-100 bg-white">
          <div className="max-w-7xl mx-auto px-6 md:px-12 text-center space-y-12">
            <h2 className="text-3xl md:text-5xl font-bold tracking-tight">Standard Baru Perbankan.</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
              <StatItem value="2M+" label="Active Users" />
              <StatItem value="Rp50T+" label="Volume Transaksi" />
              <StatItem value="99.9%" label="Uptime" />
              <StatItem value="24/7" label="Support" />
            </div>
          </div>
        </section>

        {/* Call to Action */}
        <section className="py-32">
          <div className="max-w-5xl mx-auto px-6 text-center space-y-10">
            <h2 className="text-4xl md:text-6xl font-bold tracking-tight text-slate-900">Siap untuk memulai?</h2>
            <p className="text-lg text-slate-500 max-w-2xl mx-auto">
              Daftar sekarang dan nikmati pengalaman perbankan digital termudah di Indonesia. Hanya butuh 5 menit untuk verifikasi.
            </p>
            <div className="flex flex-col sm:flex-row justify-center gap-4">
               <Link href={l('/onboarding')} className="px-10 py-5 bg-emerald-500 text-white rounded-2xl font-bold text-lg shadow-xl shadow-emerald-500/20 hover:scale-105 hover:bg-emerald-600 transition-all active:scale-95">
                 Daftar Sekarang
               </Link>
               <Link href="#contact" className="px-10 py-5 border border-slate-200 rounded-2xl font-bold text-lg hover:bg-slate-50 transition-all">
                 Hubungi Kami
               </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="py-16 bg-white border-t border-slate-100">
        <div className="max-w-7xl mx-auto px-6 md:px-12">
          <div className="flex flex-col md:flex-row justify-between items-center gap-8">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-emerald-500 rounded-lg flex items-center justify-center text-white font-bold">U</div>
              <span className="text-xl font-bold">PayU</span>
            </div>
            <div className="flex gap-8 text-sm text-slate-500 font-medium">
              <Link href="#" className="hover:text-slate-900">App</Link>
              <Link href="#" className="hover:text-slate-900">About</Link>
              <Link href="#" className="hover:text-slate-900">Support</Link>
              <Link href="#" className="hover:text-slate-900">Contact</Link>
            </div>
            <p className="text-sm text-slate-400">© 2026 PayU. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({ icon, title, desc }: { icon: React.ReactNode; title: string; desc: string }) {
  return (
    <div className="p-8 rounded-3xl bg-white border border-slate-100 hover:shadow-xl hover:shadow-slate-200/50 transition-all duration-300 group">
      <div className="w-12 h-12 bg-emerald-500/10 rounded-2xl flex items-center justify-center mb-6 text-emerald-600 group-hover:bg-emerald-500 group-hover:text-white transition-colors">
        {icon}
      </div>
      <h3 className="text-xl font-bold mb-3 text-slate-900">{title}</h3>
      <p className="text-slate-500 leading-relaxed text-sm">{desc}</p>
    </div>
  );
}

function StatItem({ value, label }: { value: string; label: string }) {
  return (
    <div className="space-y-1">
      <div className="text-3xl md:text-5xl font-bold text-slate-900 tracking-tight">{value}</div>
      <div className="text-xs font-bold text-slate-400 uppercase tracking-widest">{label}</div>
    </div>
  );
}
