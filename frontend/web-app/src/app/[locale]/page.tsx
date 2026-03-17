'use client';

import Image from 'next/image';
import { Link } from '@/lib/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslations, useLocale } from 'next-intl';
import { Shield, Zap, Menu, X, PieChart, Globe, ArrowUpRight } from 'lucide-react';
import { useState, useEffect, useRef } from 'react';
import DOMPurify from 'isomorphic-dompurify';
import gsap from 'gsap';
import { Observer } from 'gsap/Observer';

import BentoGrid from '@/components/landing/BentoGrid';
import LogoTicker from '@/components/landing/LogoTicker';

if (typeof window !== 'undefined') {
  gsap.registerPlugin(Observer);
}

export default function LandingPage() {
  const t = useTranslations('landing');
  const locale = useLocale();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [currentSlide, setCurrentSlide] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const isAnimating = useRef(false);

  const totalSlides = 4;
  const slideIds = ['hero', 'app', 'about', 'support'];

  // Helper for localized links
  const l = (path: string) => locale === 'id' ? path : `/${locale}${path}`;

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const ctx = gsap.context(() => {
      Observer.create({
        target: window,
        type: 'wheel,touch,pointer',
        wheelSpeed: 1,
        onUp: () => !isAnimating.current && currentSlide > 0 && goToSlide(currentSlide - 1),
        onDown: () => !isAnimating.current && currentSlide < totalSlides - 1 && goToSlide(currentSlide + 1),
        tolerance: 10,
        preventDefault: true
      });
    });

    return () => ctx.revert();
  }, [currentSlide]);

  const goToSlide = (index: number) => {
    if (index < 0 || index >= totalSlides) return;
    isAnimating.current = true;
    setCurrentSlide(index);

    gsap.to(containerRef.current, {
      xPercent: -index * 100,
      duration: 1.2,
      ease: 'power4.inOut',
      onComplete: () => {
        isAnimating.current = false;
        setScrolled(index > 0);
      }
    });
  };

  const handleNavClick = (e: React.MouseEvent, targetId: string) => {
    e.preventDefault();
    const index = slideIds.indexOf(targetId.replace('#', ''));
    if (index !== -1) {
      goToSlide(index);
    }
    setMobileMenuOpen(false);
  };

  return (
    <div className="h-screen w-screen overflow-hidden bg-[#243c33] text-white font-inter">
      {/* Navigation - Symmetrical Organic Pattern */}
      <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-700 ${
        scrolled ? 'bg-[#1a2e26]/90 backdrop-blur-2xl border-b border-white/5 py-4 shadow-2xl' : 'bg-transparent py-10'
      }`}>
        <div className="max-w-7xl mx-auto px-6 md:px-12 flex items-center justify-between">
          {/* Left Nav */}
          <div className="hidden md:flex items-center gap-10 font-bold uppercase tracking-[0.2em] text-[10px] text-white/60">
            <a href="#app" onClick={(e) => handleNavClick(e, '#app')} className="hover:text-white transition-colors cursor-pointer">{t('nav.features')}</a>
            <a href="#about" onClick={(e) => handleNavClick(e, '#about')} className="hover:text-white transition-colors cursor-pointer">{t('nav.about')}</a>
          </div>

          {/* Centered Logo */}
          <Link href={l('/')} className="flex flex-col items-center gap-1 group cursor-pointer absolute left-1/2 -translate-x-1/2" aria-label="PayU Home">
            <span className="text-3xl font-black tracking-[-0.05em] text-white uppercase italic">PayU</span>
            <div className="w-8 h-0.5 bg-emerald-500 rounded-full group-hover:w-12 transition-all" />
          </Link>

          {/* Right Nav */}
          <div className="hidden md:flex items-center gap-10 font-bold uppercase tracking-[0.2em] text-[10px] text-white/60">
            <a href="#support" onClick={(e) => handleNavClick(e, '#support')} className="hover:text-white transition-colors cursor-pointer">{t('nav.support')}</a>
            <Link href={l('/login')} className="px-6 py-2 bg-white/5 hover:bg-white/10 rounded-full transition-all border border-white/10 text-white cursor-pointer">{t('nav.login')}</Link>
          </div>

          {/* Mobile Menu Button */}
          <button 
            className="md:hidden w-10 h-10 flex items-center justify-center bg-white/5 rounded-full transition-colors cursor-pointer" 
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </nav>

      {/* Mobile Nav Overlay */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div 
            initial={{ opacity: 0, x: '100%' }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: '100%' }}
            className="fixed inset-0 z-[60] bg-[#1a2e26] p-12 flex flex-col items-center justify-center gap-8 font-black uppercase tracking-widest"
          >
            <button className="absolute top-10 right-10 text-white" onClick={() => setMobileMenuOpen(false)}><X size={40} /></button>
            <a href="#app" className="text-4xl" onClick={(e) => handleNavClick(e, '#app')}>{t('nav.features')}</a>
            <a href="#about" className="text-4xl" onClick={(e) => handleNavClick(e, '#about')}>{t('nav.about')}</a>
            <a href="#support" className="text-4xl" onClick={(e) => handleNavClick(e, '#support')}>{t('nav.support')}</a>
          </motion.div>
        )}
      </AnimatePresence>

      <div ref={containerRef} className="h-full w-full flex flex-row transition-none will-change-transform">
        {/* Slide 1: Hero - The Core Identity */}
        {/* Slide 1: Hero - The Core Identity */}
        <section className="h-screen min-w-full relative flex flex-col items-center justify-center text-center overflow-hidden bg-[#050a08]">
          {/* Bankong-inspired Atmosphere: Deep Green Mist */}
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_30%,rgba(16,185,129,0.15)_0%,rgba(6,78,59,0.4)_40%,rgba(2,6,4,1)_100%)] z-0" />
          <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 mix-blend-overlay z-0" />
          
          {/* Organic Foliage System (Foreground & Background) */}
          <div className="absolute inset-0 pointer-events-none z-0 overflow-hidden">
             {/* Left Palm Leaf - Blurred Foreground */}
             <div className="absolute -bottom-[10vh] -left-[20vw] md:-bottom-10 md:-left-20 w-[60vh] h-[60vh] md:w-[600px] md:h-[600px] text-[#064e3b] opacity-40 md:opacity-80 blur-[2px] animate-[pulse_4s_ease-in-out_infinite]">
                <svg viewBox="0 0 500 500" fill="currentColor" className="w-full h-full drop-shadow-2xl">
                   <path d="M250,500 C200,400 0,300 0,100 C20,150 50,200 80,250 C60,200 40,150 40,100 C80,180 120,260 150,320 C140,260 130,200 130,150 C180,250 220,350 250,450 C280,350 320,250 370,150 C370,200 360,260 350,320 C380,260 420,180 460,100 C460,150 440,200 420,250 C450,200 480,150 500,100 C500,300 300,400 250,500 Z" />
                </svg>
             </div>
             
             {/* Right Fern - Sharp Foreground */}
             <div className="absolute -bottom-[15vh] -right-[15vw] md:-bottom-20 md:-right-10 w-[50vh] h-[70vh] md:w-[500px] md:h-[700px] text-[#042f2e] opacity-50 md:opacity-90">
                 <svg viewBox="0 0 500 700" fill="currentColor" className="w-full h-full drop-shadow-2xl rotate-12">
                     <path d="M250,700 Q150,500 50,200 Q100,250 150,300 Q120,200 100,100 Q180,200 220,280 Q210,180 200,80 Q250,200 270,300 Q300,180 320,50 Q350,180 380,280 Q430,200 480,100 Q450,250 400,350 Q350,500 250,700 Z" />
                     {/* Leaf Veins */}
                     <path d="M250,700 Q200,450 100,150" stroke="#064e3b" strokeWidth="5" fill="none" opacity="0.3" />
                     <path d="M250,700 Q300,450 400,150" stroke="#064e3b" strokeWidth="5" fill="none" opacity="0.3" />
                 </svg>
             </div>

             {/* Top Hanging Vines - Hide on small mobile to clear header */}
             <div className="absolute -top-10 -right-10 md:-top-20 md:right-20 w-[30vh] h-[40vh] md:w-[300px] md:h-[400px] text-[#065f46] opacity-30 md:opacity-40 rotate-180 blur-sm hidden sm:block">
                 <svg viewBox="0 0 300 400" fill="currentColor" className="w-full h-full">
                     <path d="M150,0 Q180,100 250,200 Q200,150 150,0 M150,0 Q120,120 50,250" />
                     <circle cx="250" cy="200" r="5" />
                     <circle cx="50" cy="250" r="8" />
                 </svg>
             </div>
          </div>

          <div className="max-w-6xl mx-auto px-6 relative z-10 flex flex-col items-center space-y-12">
             <motion.div 
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="px-6 py-2 rounded-full bg-white/10 border border-white/20 text-emerald-100/80 text-sm font-medium backdrop-blur-md mb-6"
             >
               {t('badge')}
             </motion.div>
             
             <h1 
               className="text-5xl md:text-7xl font-bold leading-tight text-white tracking-tight text-shadow-lg"
                dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(t.raw('heroTitle')) }}
             />
             
             {/* The Floating Card System */}
            <div className="relative w-full h-[40vh] min-h-[300px] flex items-center justify-center perspective-1000">
               
               {/* Background Shadow */}
               <div className="absolute top-1/2 left-1/2 -translate-x-1/2 translate-y-[35%] w-[60%] h-8 bg-black/60 rounded-full blur-[40px] opacity-60 pointer-events-none" />
               
               {/* Pedestal/Base */}
               <div className="absolute top-1/2 left-1/2 -translate-x-1/2 translate-y-[30%] w-[40%] h-8 md:h-12 bg-gradient-to-b from-white/5 to-transparent rounded-t-[3rem] border-t border-white/10 pointer-events-none" />
               
               <motion.div 
                 animate={{ y: [0, -20, 0], rotateZ: [-1, 1, -1] }}
                 transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
                 className="relative z-10 w-[85vw] max-w-[340px] md:w-[500px] md:max-w-none aspect-[1.586] rounded-[1.5rem] p-[5%] md:p-10 overflow-hidden shadow-2xl bg-white flex flex-col justify-between"
               >
                  {/* Abstract Green Organic Pattern (Top Right) */}
                  <div className="absolute -top-[20%] -right-[20%] w-[80%] h-[100%] pointer-events-none opacity-90">
                     <svg viewBox="0 0 200 200" fill="none" className="w-full h-full text-[#1a4437]">
                        <path d="M100 0 C 120 40 160 30 200 60 V 0 Z" fill="#34d399" fillOpacity="0.2" />
                        <path d="M80 0 C 130 60 140 100 200 120 V 0 Z" fill="#10b981" fillOpacity="0.4" />
                        <path d="M120 0 C 150 50 160 80 200 100 V 0 Z" fill="#065f46" />
                        <circle cx="150" cy="50" r="2" fill="white" fillOpacity="0.5" />
                        <circle cx="170" cy="30" r="3" fill="white" fillOpacity="0.3" />
                     </svg>
                  </div>

                  <div className="relative z-10 w-full flex justify-between items-start">
                     <span className="text-[6vw] md:text-3xl font-bold tracking-tight text-[#1a2e26] leading-none">PayU</span>
                     {/* Chip Icon */}
                     <div className="w-[12%] aspect-[1.3] bg-gradient-to-tr from-yellow-200 to-yellow-500 rounded-md md:rounded-lg flex items-center justify-center border border-yellow-600/20 shadow-inner relative overflow-hidden">
                         <div className="w-full h-[1px] bg-yellow-600/30 mb-[20%] absolute" />
                         <div className="h-full w-[1px] bg-yellow-600/30 absolute" />
                     </div>
                  </div>
                  
                  <div className="relative z-10 space-y-[4%] w-full">
                     <p className="text-[4.5vw] md:text-2xl font-semibold tracking-widest font-mono text-[#1a2e26] w-full text-center">3243 4535 1345 6432</p>
                     
                     <div className="flex justify-between items-end w-full">
                         <div className="flex-1">
                             <p className="text-[1.8vw] md:text-[10px] font-semibold tracking-wider text-[#1a2e26]/60 mb-[2%]">VALID THRU</p>
                             <p className="text-[3vw] md:text-lg font-bold text-[#1a2e26] leading-none">12/27</p>
                             <p className="text-[3.5vw] md:text-xl font-bold mt-[2%] text-[#1a2e26] truncate">Fajar Nur Rohman</p>
                         </div>
                         {/* Mastercard-style circles */}
                         <div className="flex -space-x-[25%] opacity-90 h-[5vw] md:h-10 items-end">
                             <div className="h-full aspect-square rounded-full bg-red-600 mix-blend-multiply" />
                             <div className="h-full aspect-square rounded-full bg-yellow-500 mix-blend-multiply" />
                         </div>
                     </div>
                  </div>
               </motion.div>
            </div>

            <div className="flex flex-col items-center gap-6">
               <Link href={l('/onboarding')} className="px-10 py-4 bg-white text-[#050a08] hover:bg-emerald-50 rounded-full font-bold text-base transition-colors shadow-lg">
                  {t('getStarted')}
               </Link>
               <p className="text-emerald-100/60 text-sm font-medium">{t('hero.freeAdmin')}</p>
            </div>
          </div>
        </section>

        {/* Slide 2: Asymmetrical Tech Layout - Filling Space */}
        <section className="h-screen min-w-full relative flex items-center bg-[#1a2e26] overflow-hidden">
          <div className="absolute inset-0 opacity-10 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] mix-blend-overlay" />
          
          {/* Wayang Silhouette - Left Side */}
          <div className="absolute -left-[5%] top-1/2 -translate-y-1/2 w-[70vh] h-[100vh] opacity-[0.06] select-none pointer-events-none grayscale flex items-center justify-center -rotate-12 mix-blend-color-dodge">
             <svg viewBox="0 0 100 150" fill="currentColor" className="w-full h-full text-emerald-700">
                 <path d="M50 0 C 50 0 35 30 35 55 C 35 70 20 90 5 100 C 0 105 5 120 20 130 C 30 135 40 145 50 150 C 60 145 70 135 80 130 C 95 120 100 105 95 100 C 80 90 65 70 65 55 C 65 30 50 0 50 0 Z" />
             </svg>
          </div>

           <div className="max-w-7xl mx-auto px-6 w-full grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center relative z-10 py-16 md:py-0">
              {/* Left Content (5 Cols) */}
              <div className="lg:col-span-5 space-y-6 md:space-y-8 text-left">
                 <div className="inline-block px-3 py-1 md:px-4 border border-emerald-500/30 rounded-full bg-emerald-900/20 backdrop-blur-sm">
                      <span className="text-emerald-400 text-[10px] md:text-xs font-semibold uppercase tracking-wider">{t('slide2.badge')}</span>
                 </div>
                  <h2 className="text-3xl md:text-6xl font-bold text-white leading-tight">
                     {t('slide2.title')} <br/> <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-emerald-600">{t('slide2.titleHighlight')}</span>
                  </h2>
                  <p className="text-emerald-100/70 text-sm md:text-lg font-normal leading-relaxed max-w-md border-l-2 border-emerald-500/30 pl-4 md:pl-6">
                     {t('slide2.subtitle')}
                  </p>
              </div>
              
              {/* Right Content (7 Cols) - Vertical Stack on Mobile, Staggered on Desktop */}
              <div className="lg:col-span-7 flex flex-col md:grid md:grid-cols-2 gap-4 md:gap-6 mt-8 md:mt-0">
                 <div className="space-y-4 md:space-y-6 md:translate-y-12">
                      <div className="p-6 md:p-8 bg-[#0f1d18]/80 rounded-2xl md:rounded-[2rem] border border-white/5 hover:border-emerald-500/30 transition-all duration-300 hover:-translate-y-1 group">
                         <Zap className="w-6 h-6 md:w-8 md:h-8 text-emerald-400 mb-3 md:mb-4" />
                          <h4 className="text-lg md:text-xl font-bold text-white mb-2">{t('slide2.analytics.title')}</h4>
                          <p className="text-xs md:text-sm text-white/50 font-normal leading-relaxed">{t('slide2.analytics.desc')}</p>
                      </div>
                      <div className="p-6 md:p-8 bg-emerald-900/20 rounded-2xl md:rounded-[2rem] border border-white/5 hover:border-emerald-500/30 transition-all duration-300 hover:-translate-y-1 group backdrop-blur-md">
                         <Globe className="w-6 h-6 md:w-8 md:h-8 text-emerald-400 mb-3 md:mb-4" />
                          <h4 className="text-lg md:text-xl font-bold text-white mb-2">{t('slide2.connectivity.title')}</h4>
                          <p className="text-xs md:text-sm text-white/50 font-normal leading-relaxed">{t('slide2.connectivity.desc')}</p>
                      </div>
                 </div>
                 <div className="md:pt-0">
                      <div className="p-6 md:p-8 bg-gradient-to-b from-white/5 to-[#0f1d18]/50 rounded-2xl md:rounded-[2rem] border border-white/10 hover:border-emerald-500/30 transition-all duration-300 hover:-translate-y-1 group h-full flex flex-col justify-center">
                         <Shield className="w-8 h-8 md:w-10 md:h-10 text-emerald-400 mb-4 md:mb-6" />
                          <h4 className="text-xl md:text-2xl font-bold text-white mb-2">{t('slide2.security.title')}</h4>
                          <p className="text-xs md:text-sm text-white/50 font-normal leading-relaxed">{t('slide2.security.desc')}</p>
                      </div>
                 </div>
              </div>
           </div>
        </section>

        {/* Slide 3: Simple Values - Global Impact */}
        <section className="h-screen min-w-full relative flex items-center bg-[#1e332b] overflow-hidden">
           {/* Architectural Grid Lines */}
           <div className="absolute inset-0 z-0 bg-[linear-gradient(to_right,rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:100px_100px]" />
           
           <div className="max-w-7xl mx-auto px-6 w-full grid grid-cols-1 lg:grid-cols-2 gap-0 items-center relative z-10 h-full">
              {/* Text Side with Frosted Backing */}
              <div className="h-full flex flex-col justify-center lg:pr-24 relative">
                 <div className="absolute inset-0 bg-gradient-to-r from-[#1e332b] to-transparent z-0 lg:hidden" />
                 <div className="relative z-10 space-y-12">
                     <div className="space-y-6">
                         <span className="inline-block text-emerald-400 text-xs font-semibold tracking-wider uppercase bg-black/20 px-4 py-2 rounded-lg">{t('slide3.badge')}</span>
                         <h2 className="text-5xl md:text-7xl font-bold text-white leading-tight">
                            {t('slide3.title')} <br/> <span className="text-emerald-500">{t('slide3.titleHighlight')}</span>
                         </h2>
                     </div>
                     <div className="flex gap-16 border-t border-white/10 pt-8">
                        <div>
                           <p className="text-4xl font-bold text-white">50T+</p>
                            <p className="text-sm text-emerald-400 font-medium mt-1">{t('slide3.statsAnnual')}</p>
                        </div>
                        <div>
                           <p className="text-4xl font-bold text-white">2.4M+</p>
                            <p className="text-sm text-emerald-400 font-medium mt-1">{t('slide3.statsTrusted')}</p>
                        </div>
                     </div>
                 </div>
              </div>
              
              {/* Visual Side - Full Height Bleed */}
              <div className="hidden lg:flex h-full items-center justify-center relative bg-[#152620] border-l border-white/5">
                 <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(16,185,129,0.05)_0%,transparent_60%)]" />
                 
                 {/* Massive PayU Circle */}
                 <div className="aspect-square w-[120%] rounded-full border-[1px] border-white/5 flex items-center justify-center relative group">
                    <div className="absolute inset-0 rounded-full border border-dashed border-emerald-500/20 animate-[spin_60s_linear_infinite]" />
                    <div className="text-[20vw] opacity-[0.03] select-none font-black italic whitespace-nowrap">PAYU</div>
                    
                    {/* Floating Wayang Detail */}
                    <div className="absolute top-[20%] right-[20%] w-64 h-96 opacity-30 rotate-12 mix-blend-overlay">
                        <svg viewBox="0 0 100 150" fill="currentColor" className="w-full h-full text-emerald-600">
                             <path d="M50 0 C 50 0 35 30 35 55 C 35 70 20 90 5 100 C 0 105 5 120 20 130 C 30 135 40 145 50 150 C 60 145 70 135 80 130 C 95 120 100 105 95 100 C 80 90 65 70 65 55 C 65 30 50 0 50 0 Z" />
                        </svg>
                    </div>

                    <div className="absolute bottom-[25%] left-[25%] p-8 bg-neutral-900/90 rounded-[2rem] border border-white/10 shadow-2xl backdrop-blur-xl">
                       <PieChart className="w-12 h-12 text-emerald-400 mb-4" />
                       <p className="text-xs text-white/50 font-bold uppercase tracking-widest mb-1">Growth YTD</p>
                       <p className="text-4xl font-black text-white italic tracking-tighter">+14.2%</p>
                    </div>
                 </div>
              </div>
           </div>
        </section>

        {/* Slide 4: Simple CTA - Join the Future */}
        <section className="h-screen min-w-full relative flex flex-col justify-center bg-[#0a1410] overflow-hidden">
           <div className="max-w-4xl mx-auto px-6 text-center space-y-16">
              <div className="space-y-6">
                  <h2 className="text-4xl md:text-6xl font-bold text-white leading-tight">{t('slide4.title')} <br/> <span className="text-emerald-500">{t('slide4.titleHighlight')}</span></h2>
                  <p className="text-white/60 text-lg font-normal">{t('slide4.subtitle')}</p>
              </div>

              <div className="flex flex-col items-center gap-10">
                 <Link href={l('/onboarding')} className="px-20 py-8 bg-emerald-500 hover:bg-emerald-400 text-white rounded-full font-black text-sm uppercase tracking-[0.5em] shadow-[0_30px_60px_rgba(16,185,129,0.3)] transition-all scale-110">
                     {t('slide4.button')}
                  </Link>
                 
                 <footer className="w-full flex justify-between items-center text-[8px] font-black uppercase tracking-[0.4em] text-white/10 pt-20 border-t border-white/5">
                     <p>{t('footer.rights')}</p>
                      <div className="flex gap-8">
                         <Link href={l('/terms')} className="hover:text-emerald-500 transition-colors">{t('slide4.terms')}</Link>
                         <Link href={l('/privacy')} className="hover:text-emerald-500 transition-colors">{t('slide4.privacy')}</Link>
                     </div>
                 </footer>
              </div>
           </div>
           
           {/* Background branding subtle */}
           <div className="absolute bottom-[-10%] right-[-10%] text-[25vw] font-black text-white/[0.02] italic tracking-tightest pointer-events-none uppercase">Future</div>
        </section>
      </div>

      {/* Slide Indicators - Horizontal at Bottom */}
      <div className="fixed bottom-10 left-1/2 -translate-x-1/2 z-50 flex flex-row gap-4">
        {slideIds.map((_, i) => (
          <button 
            key={i} 
            onClick={() => goToSlide(i)}
            className={`w-1.5 h-1.5 rounded-full transition-all duration-500 ${currentSlide === i ? 'bg-emerald-500 w-8' : 'bg-white/20'}`}
          />
        ))}
      </div>
    </div>
  );
}
