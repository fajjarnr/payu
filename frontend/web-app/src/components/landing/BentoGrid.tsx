'use client';

import { motion } from 'framer-motion';
import { AreaChart, Shield, Zap, Globe, Wallet, PieChart, ArrowUpRight } from 'lucide-react'; // eslint-disable-line @typescript-eslint/no-unused-vars
import { cn } from '@/lib/utils'; // Assuming this exists, based on skeleton.tsx

const BentoCard = ({ className, children, delay = 0 }: { className?: string; children: React.ReactNode; delay?: number }) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    whileInView={{ opacity: 1, y: 0 }}
    viewport={{ once: true }}
    transition={{ duration: 0.5, delay }}
    whileHover={{ y: -5 }}
    className={cn(
      "bg-white rounded-[2rem] border border-slate-100 shadow-xl shadow-slate-200/50 overflow-hidden relative group hover:shadow-2xl hover:shadow-emerald-500/10 transition-all duration-500",
      className
    )}
  >
    {children}
  </motion.div>
);

import { useTranslations } from 'next-intl';

export default function BentoGrid() {
  const t = useTranslations('landing.bento');

  return (
    <section className="py-20 px-6 relative overflow-hidden bg-[#1e332b]">
      {/* Background Decorative Element */}
      <div className="absolute top-1/4 -right-20 w-[600px] h-[600px] bg-emerald-500/10 rounded-full blur-[120px] -z-10" />

      <div className="max-w-7xl mx-auto space-y-4">
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-4">
            <h2 className="text-4xl md:text-5xl font-black tracking-tight text-white uppercase italic">
               {t('title').split(' ').slice(0, 2).join(' ')} <span className="text-emerald-400">{t('title').split(' ').slice(2).join(' ')}</span>
            </h2>
            <p className="text-white/40 text-sm md:text-base max-w-2xl mx-auto font-medium">
                {t('subtitle')}
            </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 auto-rows-[340px]">
            {/* Card 1: Analytics (Large) */}
            <BentoCard className="md:col-span-2 p-10 flex flex-col justify-between overflow-hidden bg-[#14241e] text-white border-white/5 shadow-2xl">
                <div className="space-y-4 relative z-10">
                    <div className="w-14 h-14 bg-emerald-500/10 rounded-2xl flex items-center justify-center mb-6 text-emerald-400 border border-white/10">
                        <PieChart className="w-7 h-7" />
                    </div>
                    <h3 className="text-3xl font-black tracking-tight uppercase italic">{t('analytics.title')}</h3>
                    <p className="text-white/40 text-lg max-w-md leading-relaxed font-medium">
                        {t('analytics.desc')}
                    </p>
                </div>
                
                {/* Visual Placeholder: Enhanced Chart */}
                <div className="absolute right-0 bottom-0 w-3/4 h-72 bg-gradient-to-t from-emerald-500/20 via-emerald-500/5 to-transparent rounded-tl-[4rem] border-t border-l border-white/10 backdrop-blur-xl p-10 flex items-end gap-3 group-hover:translate-y-2 transition-transform duration-700">
                     {[45, 75, 55, 95, 65, 100, 80, 85].map((h, i) => (
                         <div key={i} className="flex-1 bg-emerald-500/40 rounded-t-xl transition-all duration-1000 group-hover:bg-emerald-400 group-hover:scale-y-105" style={{ height: `${h}%`, transitionDelay: `${i * 50}ms` }} />
                     ))}
                </div>
            </BentoCard>

            {/* Card 2: Security (Tall) */}
            <BentoCard className="md:row-span-2 p-10 bg-[#162922] border-white/5" delay={0.1}>
                 <div className="h-full flex flex-col items-center text-center justify-center space-y-10">
                    <div className="relative">
                        <div className="absolute inset-0 bg-emerald-500/10 blur-3xl rounded-full scale-150 animate-pulse" />
                        <div className="relative z-10 w-40 h-40 bg-white/5 rounded-full flex items-center justify-center shadow-2xl border border-white/10 backdrop-blur-md">
                            <Shield className="w-20 h-20 text-emerald-400" />
                        </div>
                        <div className="absolute -top-4 -right-4 px-4 py-2 bg-emerald-500 text-white text-[10px] font-black uppercase tracking-widest rounded-full shadow-lg">
                            {t('security.tag')}
                        </div>
                    </div>
                    <div className="space-y-4">
                        <h3 className="text-2xl font-black text-white tracking-tight uppercase italic">{t('security.title')}</h3>
                        <p className="text-white/40 text-sm leading-relaxed px-4 font-medium">
                            {t('security.desc')}
                        </p>
                    </div>
                    <div className="w-full space-y-3">
                        {[
                          t('security.features.faceAuth'),
                          t('security.features.encryption'),
                          t('security.features.guarantee')
                        ].map((feature, i) => (
                          <div key={i} className="flex items-center gap-4 p-4 bg-white/5 rounded-2xl border border-white/5 text-left group/feature hover:bg-white/10 transition-all duration-300">
                              <div className="w-2 h-2 bg-emerald-500 rounded-full" />
                              <span className="text-[10px] font-black text-white/60 uppercase tracking-widest">{feature}</span>
                          </div>
                        ))}
                    </div>
                 </div>
            </BentoCard>

            {/* Card 3: Global (Square) */}
            <BentoCard className="p-10 bg-[#1a2e26] border-white/5" delay={0.2}>
                <div className="flex flex-col h-full justify-between">
                     <div className="w-14 h-14 bg-white/5 rounded-2xl flex items-center justify-center shadow-xl text-emerald-400 border border-white/10">
                        <Globe className="w-7 h-7" />
                    </div>
                    <div className="space-y-3">
                        <h3 className="text-2xl font-black text-white tracking-tight uppercase italic">{t('global.title')}</h3>
                        <p className="text-white/40 text-sm leading-relaxed font-medium">
                            {t('global.desc')}
                        </p>
                    </div>
                    <div className="flex -space-x-3 overflow-hidden py-2">
                        {['🇺🇸', '🇯🇵', '🇸🇬', '🇪🇺', '🇦🇺'].map((flag, idx) => (
                            <div key={idx} className="w-10 h-10 rounded-full bg-[#14241e] border-2 border-[#1a2e26] flex items-center justify-center text-lg shadow-sm">
                              {flag}
                            </div>
                        ))}
                        <div className="w-10 h-10 rounded-full bg-emerald-500 border-2 border-[#1a2e26] text-white flex items-center justify-center text-[10px] font-black">+42</div>
                    </div>
                </div>
            </BentoCard>

            {/* Card 4: Investments (Square) */}
            <BentoCard className="p-10 group bg-[#1c3029] border-white/5" delay={0.3}>
                 <div className="flex flex-col h-full justify-between">
                     <div className="w-14 h-14 bg-white/5 rounded-2xl flex items-center justify-center text-emerald-400 shadow-sm border border-white/10">
                        <ArrowUpRight className="w-7 h-7" />
                    </div>
                    <div className="space-y-3">
                        <h3 className="text-2xl font-black text-white tracking-tight uppercase italic">{t('invest.title')}</h3>
                        <p className="text-white/40 text-sm leading-relaxed font-medium">
                            {t('invest.desc')}
                        </p>
                    </div>
                    <div className="bg-white/5 rounded-2xl p-6 flex items-center justify-between border border-white/5">
                        <div className="space-y-1">
                            <span className="text-[10px] text-white/30 font-bold uppercase tracking-widest">{t('invest.returnRate')}</span>
                            <span className="text-xl font-black text-emerald-400 block">+14.2%</span>
                        </div>
                    </div>
                </div>
            </BentoCard>
        </div>
      </div>
    </section>
  );
}
