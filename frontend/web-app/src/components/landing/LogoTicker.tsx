'use client';

import { motion } from 'framer-motion';

const logos = [
  { name: 'TechCrunch', color: '#10B981' },
  { name: 'Forbes', color: '#10B981' },
  { name: 'Bloomberg', color: '#10B981' },
  { name: 'CNBC Indonesia', color: '#10B981' },
  { name: 'DailySocial', color: '#10B981' },
  { name: 'Kompas', color: '#10B981' },
];

import { useTranslations } from 'next-intl';

// ... (existing imports logos)

export default function LogoTicker() {
  const t = useTranslations('landing.ticker');

  return (
    <section className="py-10 bg-[#1e332b] border-y border-white/5 overflow-hidden">
      <div className="max-w-7xl mx-auto px-6 md:px-12 flex items-center gap-8">
        <span className="text-[10px] font-black text-white/20 uppercase tracking-[0.3em] whitespace-nowrap hidden md:block">
            {t('label')}
        </span>
        
        <div className="flex-1 overflow-hidden relative">
            <motion.div 
                className="flex gap-16 items-center whitespace-nowrap"
                animate={{ x: [0, -1000] }}
                transition={{
                    repeat: Infinity,
                    duration: 40,
                    ease: "linear"
                }}
            >
                {[...logos, ...logos, ...logos].map((logo, idx) => (
                    <div key={idx} className="text-xl md:text-2xl font-black text-white/30 hover:text-emerald-400 transition-colors cursor-default select-none uppercase italic">
                        {logo.name}
                    </div>
                ))}
            </motion.div>
            
            {/* Gradient Masks */}
            <div className="absolute left-0 top-0 bottom-0 w-20 bg-gradient-to-r from-[#1e332b] to-transparent z-10" />
            <div className="absolute right-0 top-0 bottom-0 w-20 bg-gradient-to-l from-[#1e332b] to-transparent z-10" />
        </div>
      </div>
    </section>
  );
}
