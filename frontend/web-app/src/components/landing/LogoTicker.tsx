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

export default function LogoTicker() {
  return (
    <section className="py-10 bg-white border-y border-slate-100 overflow-hidden">
      <div className="max-w-7xl mx-auto px-6 md:px-12 flex items-center gap-8">
        <span className="text-sm font-bold text-slate-400 uppercase tracking-widest whitespace-nowrap hidden md:block">
            Trusted by Leaders:
        </span>
        
        <div className="flex-1 overflow-hidden relative mask-linear-fade">
            <motion.div 
                className="flex gap-16 items-center whitespace-nowrap"
                animate={{ x: [0, -1000] }}
                transition={{
                    repeat: Infinity,
                    duration: 30,
                    ease: "linear"
                }}
            >
                {[...logos, ...logos, ...logos].map((logo, idx) => (
                    <div key={idx} className="text-xl md:text-2xl font-bold text-slate-300 hover:text-emerald-500 transition-colors cursor-default select-none">
                        {logo.name}
                    </div>
                ))}
            </motion.div>
            
            {/* Gradient Masks */}
            <div className="absolute left-0 top-0 bottom-0 w-20 bg-gradient-to-r from-white to-transparent" />
            <div className="absolute right-0 top-0 bottom-0 w-20 bg-gradient-to-l from-white to-transparent" />
        </div>
      </div>
    </section>
  );
}
