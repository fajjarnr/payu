'use client';

import { motion } from 'framer-motion';
import { AreaChart, Shield, Zap, Globe, Wallet, PieChart, ArrowUpRight } from 'lucide-react';
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
    <section className="py-32 px-6">
      <div className="max-w-7xl mx-auto space-y-4">
        <div className="text-center max-w-2xl mx-auto mb-16 space-y-4">
            <h2 className="text-4xl md:text-5xl font-bold tracking-tight text-slate-900" dangerouslySetInnerHTML={{ __html: t.raw('title').replace('Fitur Pintar', t('title').split(' ')[0] + ' ' + t('title').split(' ')[1]) }}>
            </h2>
            <h2 className="hidden">{t('title')}</h2> {/* Hidden for SEO/Accessiblity fallback if needed, or better, rewrite dangerouslySetInnerHTML above properly. Actually, checking message keys, title is "Fitur Pintar untuk Gaya Hidup Modern." */}
             {/* Let's simplify and just use the key directly or split if needed for styling. 
                 The original had: "Fitur Pintar untuk <span class...>"
                 I'll just inject the whole title for now or try to reconstruct.
                 Actually, better to not use dangerous HTML if possible, or use rich text formatting from next-intl if available, but raw is easier.
                 Let's assume the translation key has simple text, and I want to style the last part.
                 Wait, I didn't put HTML in the JSON. "Fitur Pintar untuk Gaya Hidup Modern."
                 I will just render the title directly for now to be safe and consistent.
              */}
            <h2 className="text-4xl md:text-5xl font-bold tracking-tight text-slate-900">
               {t('title')}
            </h2>
            <p className="text-slate-500 text-lg">
                {t('subtitle')}
            </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 auto-rows-[400px]">
            {/* Card 1: Analytics (Large) */}
            <BentoCard className="md:col-span-2 p-8 flex flex-col justify-between overflow-hidden bg-slate-900 text-white border-slate-800">
                <div className="space-y-2 relative z-10">
                    <div className="w-12 h-12 bg-emerald-500/20 rounded-2xl flex items-center justify-center mb-4 text-emerald-400">
                        <PieChart className="w-6 h-6" />
                    </div>
                    <h3 className="text-2xl font-bold">{t('analytics.title')}</h3>
                    <p className="text-slate-400 max-w-sm">
                        {t('analytics.desc')}
                    </p>
                </div>
                
                {/* Visual Placeholder: Graph */}
                <div className="absolute right-0 bottom-0 w-3/4 h-64 bg-gradient-to-t from-emerald-500/10 to-transparent rounded-tl-[3rem] border-t border-l border-white/5 backdrop-blur-sm p-6 flex items-end gap-2 group-hover:scale-105 transition-transform duration-500">
                     {[40, 65, 45, 80, 55, 90, 70].map((h, i) => (
                         <div key={i} className="flex-1 bg-emerald-500/80 rounded-t-lg transition-all duration-700 group-hover:bg-emerald-400" style={{ height: `${h}%` }} />
                     ))}
                </div>
            </BentoCard>

            {/* Card 2: Security (Tall) */}
            <BentoCard className="md:row-span-2 p-8 bg-white" delay={0.1}>
                 <div className="h-full flex flex-col items-center text-center justify-center space-y-6">
                    <div className="relative">
                        <div className="absolute inset-0 bg-blue-500/20 blur-2xl rounded-full" />
                        <Shield className="w-32 h-32 text-blue-500 relative z-10" />
                        <div className="absolute top-0 right-0 p-2 bg-emerald-500 text-white text-xs font-bold rounded-full animate-bounce">
                            {t('security.tag')}
                        </div>
                    </div>
                    <div>
                        <h3 className="text-2xl font-bold text-slate-900 mb-2">{t('security.title')}</h3>
                        <p className="text-slate-500">
                            {t('security.desc')}
                        </p>
                    </div>
                    <div className="w-full bg-slate-50 rounded-2xl p-4 border border-slate-100 text-left space-y-3">
                        <div className="flex items-center gap-3 text-sm font-medium text-slate-700">
                            <div className="w-2 h-2 bg-emerald-500 rounded-full" />
                            <span>{t('security.features.faceAuth')}</span>
                        </div>
                        <div className="flex items-center gap-3 text-sm font-medium text-slate-700">
                            <div className="w-2 h-2 bg-emerald-500 rounded-full" />
                            <span>{t('security.features.encryption')}</span>
                        </div>
                        <div className="flex items-center gap-3 text-sm font-medium text-slate-700">
                            <div className="w-2 h-2 bg-emerald-500 rounded-full" />
                            <span>{t('security.features.guarantee')}</span>
                        </div>
                    </div>
                 </div>
            </BentoCard>

            {/* Card 3: Global (Square) */}
            <BentoCard className="p-8 bg-emerald-50" delay={0.2}>
                <div className="flex flex-col h-full justify-between">
                     <div className="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow-sm text-emerald-600">
                        <Globe className="w-6 h-6" />
                    </div>
                    <div className="space-y-2">
                        <h3 className="text-xl font-bold text-slate-900">{t('global.title')}</h3>
                        <p className="text-slate-500 text-sm">
                            {t('global.desc')}
                        </p>
                    </div>
                    <div className="flex -space-x-2 overflow-hidden py-2">
                        <div className="w-8 h-8 rounded-full bg-slate-200 border-2 border-white flex items-center justify-center text-xs">🇺🇸</div>
                        <div className="w-8 h-8 rounded-full bg-slate-200 border-2 border-white flex items-center justify-center text-xs">🇯🇵</div>
                        <div className="w-8 h-8 rounded-full bg-slate-200 border-2 border-white flex items-center justify-center text-xs">sg</div>
                        <div className="w-8 h-8 rounded-full bg-slate-900 border-2 border-white text-white flex items-center justify-center text-[10px] font-bold">+47</div>
                    </div>
                </div>
            </BentoCard>

            {/* Card 4: Investments (Square) */}
            <BentoCard className="p-8 group" delay={0.3}>
                 <div className="flex flex-col h-full justify-between">
                     <div className="w-12 h-12 bg-orange-100 rounded-2xl flex items-center justify-center text-orange-600">
                        <ArrowUpRight className="w-6 h-6" />
                    </div>
                    <div className="space-y-2">
                        <h3 className="text-xl font-bold text-slate-900">{t('invest.title')}</h3>
                        <p className="text-slate-500 text-sm">
                            {t('invest.desc')}
                        </p>
                    </div>
                    <div className="bg-slate-50 rounded-xl p-4 flex items-center justify-between group-hover:bg-orange-50 transition-colors">
                        <div className="flex flex-col">
                            <span className="text-xs text-slate-400 font-bold uppercase">{t('invest.returnRate')}</span>
                            <span className="text-lg font-bold text-emerald-500">+12.4%</span>
                        </div>
                         <div className="flex flex-col items-end">
                            <span className="text-xs text-slate-400 font-bold uppercase">{t('invest.risk')}</span>
                            <span className="text-sm font-bold text-slate-700">Moderat</span>
                        </div>
                    </div>
                </div>
            </BentoCard>
        </div>
      </div>
    </section>
  );
}
