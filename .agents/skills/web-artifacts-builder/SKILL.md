---
name: web-artifacts-builder
version: 2.0.0
maturity: stable
updated: 2026-05-04
author: payu-platform-team
requires: [frontend-architect]
tags: [tools, bundling, scaffolding, vite, react, tailwind, documentation]
related: [frontend-architect, technical-writer, product-designer]
description: **Master Skill**: Frontend Artifact Specialist. Suite of tools for creating elaborate, multi-component PayU HTML artifacts using React, Tailwind CSS, shadcn/ui, and Vite for documentation, PRDs, interactive demos, and standalone applications.
---

# Web Artifacts Builder (PayU Edition)

You are the **Specialized Frontend Artifact Builder** for the PayU platform. You create standalone, high-fidelity React-based interactive artifacts used for documentation, PRDs, and advanced system demos.

---

## 🚀 Quick Start Workflow

### From Idea to Single-File Artifact

```bash
# 1. Initialize new artifact project
bash .agents/skills/web-artifacts-builder/scripts/init-artifact.sh payment-flow-demo

# 2. Navigate and develop
cd payment-flow-demo
npm install
npm run dev  # Vite dev server on localhost:5173

# 3. Build & bundle
npm run build
bash ../scripts/bundle-artifact.sh

# 4. Deliver
# Output: dist/bundle.html (~2MB, all-inclusive)
```

---

## 📁 Artifact Project Structure

```
payment-flow-demo/
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── public/
│   └── payu-logo.svg
├── src/
│   ├── main.tsx              # Entry point
│   ├── App.tsx               # Main application
│   ├── index.css             # Tailwind imports + custom styles
│   ├── components/
│   │   ├── ui/               # shadcn/ui components
│   │   │   ├── button.tsx
│   │   │   ├── card.tsx
│   │   │   └── input.tsx
│   │   ├── layout/
│   │   │   ├── Header.tsx
│   │   │   └── Sidebar.tsx
│   │   └── features/
│   │       ├── TransactionFlow.tsx
│   │       └── LedgerTable.tsx
│   ├── hooks/
│   │   └── useSimulatedStream.ts
│   ├── lib/
│   │   └── utils.ts
│   └── types/
│       └── index.ts
└── scripts/
    └── bundle.ts             # Single-file bundler
```

---

## 🛠️ Configuration Files

### vite.config.ts

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { viteSingleFile } from 'vite-plugin-singlefile'

export default defineConfig({
  plugins: [
    react(),
    viteSingleFile()  // Bundles everything into single HTML
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    target: 'esnext',
    cssCodeSplit: false,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
      },
    },
  },
})
```

### tailwind.config.ts

```typescript
import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // PayU Brand Colors
        emerald: {
          50: '#ecfdf5',
          100: '#d1fae5',
          200: '#a7f3d0',
          300: '#6ee7b7',
          400: '#34d399',
          500: '#10b981',  // Primary
          600: '#059669',
          700: '#047857',
          800: '#065f46',
          900: '#064e3b',
          950: '#022c22',
        },
        // Surface Colors (Dark Mode)
        surface: {
          50: '#f8fafc',
          100: '#f1f5f9',
          800: '#1e293b',
          900: '#0f172a',
          950: '#020617',
        },
      },
      fontFamily: {
        display: ['Outfit', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}

export default config
```

### package.json

```json
{
  "name": "payu-artifact",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0"
  },
  "dependencies": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "framer-motion": "^11.0.0",
    "lucide-react": "^0.400.0",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.0",
    "class-variance-authority": "^0.7.0",
    "recharts": "^2.12.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.0",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.4.0",
    "vite": "^5.2.0",
    "vite-plugin-singlefile": "^2.0.0"
  }
}
```

---

## 🎨 PayU Premium Design System

### The Emerald Token Set

```tsx
// src/lib/design-tokens.ts
export const tokens = {
  colors: {
    primary: {
      DEFAULT: '#10b981',  // emerald-500
      hover: '#059669',    // emerald-600
      active: '#047857',   // emerald-700
    },
    surface: {
      dark: '#0f172a',     // slate-900
      darker: '#020617',   // slate-950
      card: 'rgba(255, 255, 255, 0.05)',
    },
    text: {
      primary: '#f8fafc',
      secondary: '#94a3b8',
      muted: '#64748b',
    },
    status: {
      success: '#22c55e',
      warning: '#f59e0b',
      error: '#ef4444',
      info: '#3b82f6',
    },
  },
  shadows: {
    glow: '0 0 20px rgba(16, 185, 129, 0.3)',
    card: '0 4px 6px -1px rgba(0, 0, 0, 0.3)',
  },
}
```

### Glassmorphism Components

```tsx
// src/components/ui/glass-card.tsx
import { cn } from '@/lib/utils'

interface GlassCardProps {
  children: React.ReactNode
  className?: string
  glow?: boolean
}

export function GlassCard({ children, className, glow }: GlassCardProps) {
  return (
    <div
      className={cn(
        // Base glassmorphism
        'bg-white/5 backdrop-blur-xl',
        'border border-white/10 rounded-2xl',
        'shadow-xl shadow-black/20',
        // Glow effect
        glow && 'ring-1 ring-emerald-500/20 shadow-emerald-500/10',
        className
      )}
    >
      {children}
    </div>
  )
}

// Usage
<GlassCard glow className="p-6">
  <h2 className="text-xl font-display font-semibold text-white">
    Transaction Summary
  </h2>
</GlassCard>
```

### Gradient Backgrounds

```tsx
// src/components/layout/GradientBackground.tsx
export function GradientBackground({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-surface-950 relative overflow-hidden">
      {/* Gradient orbs */}
      <div className="absolute top-0 left-1/4 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl" />
      <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-emerald-600/10 rounded-full blur-3xl" />
      
      {/* Grid pattern overlay */}
      <div 
        className="absolute inset-0 opacity-5"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.4'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`
        }}
      />
      
      {/* Content */}
      <div className="relative z-10">
        {children}
      </div>
    </div>
  )
}
```

---

## 🏗️ Complex Component Examples

### Interactive Ledger Table

```tsx
// src/components/features/LedgerTable.tsx
import { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Search, ArrowUpRight, ArrowDownLeft } from 'lucide-react'

interface LedgerEntry {
  id: string
  type: 'CREDIT' | 'DEBIT'
  amount: number
  description: string
  timestamp: string
  status: 'COMPLETED' | 'PENDING' | 'FAILED'
}

export function LedgerTable({ entries }: { entries: LedgerEntry[] }) {
  const [filter, setFilter] = useState('')
  const [sortBy, setSortBy] = useState<'timestamp' | 'amount'>('timestamp')
  
  const filteredEntries = useMemo(() => {
    return entries
      .filter(e => 
        e.description.toLowerCase().includes(filter.toLowerCase()) ||
        e.id.includes(filter)
      )
      .sort((a, b) => 
        sortBy === 'timestamp' 
          ? new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
          : b.amount - a.amount
      )
  }, [entries, filter, sortBy])

  return (
    <div className="space-y-4">
      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input
          type="text"
          placeholder="Search transactions..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="w-full pl-10 pr-4 py-2 bg-white/5 border border-white/10 rounded-lg
                     text-white placeholder:text-slate-500 focus:outline-none 
                     focus:ring-2 focus:ring-emerald-500/50"
        />
      </div>
      
      {/* Table */}
      <div className="overflow-hidden rounded-xl border border-white/10">
        <table className="w-full">
          <thead className="bg-white/5">
            <tr className="text-left text-sm text-slate-400">
              <th className="p-4">Type</th>
              <th className="p-4">Amount</th>
              <th className="p-4">Description</th>
              <th className="p-4">Status</th>
            </tr>
          </thead>
          <tbody>
            <AnimatePresence>
              {filteredEntries.map((entry, index) => (
                <motion.tr
                  key={entry.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-t border-white/5 hover:bg-white/5"
                >
                  <td className="p-4">
                    {entry.type === 'CREDIT' ? (
                      <ArrowDownLeft className="w-5 h-5 text-emerald-500" />
                    ) : (
                      <ArrowUpRight className="w-5 h-5 text-red-400" />
                    )}
                  </td>
                  <td className={`p-4 font-mono ${
                    entry.type === 'CREDIT' ? 'text-emerald-400' : 'text-red-400'
                  }`}>
                    {entry.type === 'CREDIT' ? '+' : '-'}
                    Rp {entry.amount.toLocaleString()}
                  </td>
                  <td className="p-4 text-white">{entry.description}</td>
                  <td className="p-4">
                    <StatusBadge status={entry.status} />
                  </td>
                </motion.tr>
              ))}
            </AnimatePresence>
          </tbody>
        </table>
      </div>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const styles = {
    COMPLETED: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
    PENDING: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    FAILED: 'bg-red-500/20 text-red-400 border-red-500/30',
  }
  
  return (
    <span className={`px-2 py-1 text-xs rounded-full border ${styles[status as keyof typeof styles]}`}>
      {status}
    </span>
  )
}
```

### Real-time Transaction Monitor

```tsx
// src/components/features/TransactionMonitor.tsx
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Activity } from 'lucide-react'

interface StreamEvent {
  id: string
  type: string
  data: Record<string, unknown>
  timestamp: Date
}

// Simulates Kafka consumer for demos
export function useSimulatedStream(interval = 2000) {
  const [events, setEvents] = useState<StreamEvent[]>([])
  
  useEffect(() => {
    const timer = setInterval(() => {
      const mockEvent: StreamEvent = {
        id: `evt-${Date.now()}`,
        type: ['TransferInitiated', 'PaymentReceived', 'BalanceUpdated'][
          Math.floor(Math.random() * 3)
        ],
        data: {
          amount: Math.floor(Math.random() * 1000000) + 10000,
          walletId: `wallet-${Math.random().toString(36).slice(2, 8)}`,
        },
        timestamp: new Date(),
      }
      
      setEvents(prev => [mockEvent, ...prev.slice(0, 19)])
    }, interval)
    
    return () => clearInterval(timer)
  }, [interval])
  
  return events
}

export function TransactionMonitor() {
  const events = useSimulatedStream(1500)
  
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Activity className="w-5 h-5 text-emerald-500 animate-pulse" />
        <h3 className="text-lg font-semibold text-white">Live Transaction Feed</h3>
      </div>
      
      <div className="h-80 overflow-hidden relative">
        <div className="absolute inset-0 overflow-y-auto space-y-2 pr-2 custom-scrollbar">
          {events.map((event, i) => (
            <motion.div
              key={event.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="p-3 bg-white/5 rounded-lg border border-white/10"
            >
              <div className="flex justify-between items-start">
                <span className="text-xs text-emerald-400 font-mono">
                  {event.type}
                </span>
                <span className="text-xs text-slate-500">
                  {event.timestamp.toLocaleTimeString()}
                </span>
              </div>
              <pre className="mt-2 text-xs text-slate-400 overflow-x-auto">
                {JSON.stringify(event.data, null, 2)}
              </pre>
            </motion.div>
          ))}
        </div>
        
        {/* Fade overlay */}
        <div className="absolute bottom-0 left-0 right-0 h-16 bg-gradient-to-t from-surface-950 to-transparent pointer-events-none" />
      </div>
    </div>
  )
}
```

---

## 📊 Data Visualization

### Financial Chart Component

```tsx
// src/components/features/BalanceChart.tsx
import { 
  AreaChart, Area, XAxis, YAxis, CartesianGrid, 
  Tooltip, ResponsiveContainer 
} from 'recharts'

interface DataPoint {
  date: string
  balance: number
}

export function BalanceChart({ data }: { data: DataPoint[] }) {
  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data}>
          <defs>
            <linearGradient id="balanceGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid 
            strokeDasharray="3 3" 
            stroke="rgba(255,255,255,0.1)" 
          />
          <XAxis 
            dataKey="date" 
            stroke="#64748b"
            fontSize={12}
          />
          <YAxis 
            stroke="#64748b"
            fontSize={12}
            tickFormatter={(value) => `Rp ${(value / 1000000).toFixed(0)}M`}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#1e293b',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: '8px',
            }}
            labelStyle={{ color: '#f8fafc' }}
          />
          <Area
            type="monotone"
            dataKey="balance"
            stroke="#10b981"
            strokeWidth={2}
            fill="url(#balanceGradient)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
```

---

## 🔧 Bundle Script

```typescript
// scripts/bundle.ts
import { build } from 'vite'
import { readFileSync, writeFileSync } from 'fs'
import { join } from 'path'

async function bundle() {
  // Build with Vite
  await build({
    configFile: 'vite.config.ts',
  })
  
  // Read the built HTML
  const distPath = join(process.cwd(), 'dist', 'index.html')
  let html = readFileSync(distPath, 'utf-8')
  
  // Inline all Base64 images
  html = html.replace(
    /src="(public\/[^"]+)"/g, 
    (_, path) => {
      const imgBuffer = readFileSync(path)
      const base64 = imgBuffer.toString('base64')
      const ext = path.split('.').pop()
      return `src="data:image/${ext};base64,${base64}"`
    }
  )
  
  // Add timestamp comment
  html = html.replace(
    '</head>',
    `<!-- Built: ${new Date().toISOString()} -->\n</head>`
  )
  
  writeFileSync(join(process.cwd(), 'dist', 'bundle.html'), html)
  console.log('✅ bundle.html created successfully!')
}

bundle()
```

---

## 🛡️ Artifact Quality Checklist

### Design
- [ ] Uses PayU Emerald color palette
- [ ] Dark mode properly implemented
- [ ] Glassmorphism cards used for depth
- [ ] Typography uses Outfit/Inter fonts

### Responsiveness
- [ ] Looks premium on Desktop (1920px)
- [ ] Adapts properly to Tablet (768px)
- [ ] Functional on Mobile (375px)

### Performance
- [ ] Bundle size < 3MB
- [ ] Initial load < 2 seconds
- [ ] No console errors
- [ ] Animations smooth at 60fps

### Self-Contained
- [ ] All images converted to Base64
- [ ] No external API dependencies
- [ ] Works offline after load
- [ ] Single HTML file deliverable

### Interactivity
- [ ] All buttons functional
- [ ] Filters work correctly
- [ ] Animations enhance UX
- [ ] Error states handled

---

## 📚 References

- [Vite Documentation](https://vitejs.dev/)
- [vite-plugin-singlefile](https://github.com/nicholasreynolds/vite-plugin-singlefile)
- [shadcn/ui](https://ui.shadcn.com/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Framer Motion](https://www.framer.com/motion/)
- [Recharts](https://recharts.org/)
- [Lucide Icons](https://lucide.dev/)
- [Glassmorphism CSS Generator](https://hype4.academy/tools/glassmorphism-generator)

---
*Last Updated: 2026-05-04*
