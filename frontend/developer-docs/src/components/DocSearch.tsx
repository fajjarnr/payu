'use client';

import { useState, useCallback, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Search, X, FileText, ArrowRight } from 'lucide-react';

interface SearchResult {
  title: string;
  description: string;
  path: string;
  category: string;
}

const SEARCH_INDEX: SearchResult[] = [
  // Getting Started
  { title: 'Quick Start', description: 'Panduan memulai integrasi PayU API', path: '/getting-started', category: 'Getting Started' },
  { title: 'Authentication', description: 'OAuth2, JWT token, dan autentikasi API', path: '/getting-started/auth', category: 'Getting Started' },
  { title: 'Webhooks', description: 'Konfigurasi webhook untuk event notification', path: '/getting-started/webhooks', category: 'Getting Started' },
  // Guides
  { title: 'Partner Payments', description: 'Integrasi pembayaran mitra dan split payment', path: '/guides/partner-payments', category: 'Guides' },
  { title: 'QRIS Payments', description: 'Pembayaran via QR Code Indonesia Standard', path: '/guides/qris-payments', category: 'Guides' },
  { title: 'BI-FAST Transfers', description: 'Transfer real-time antar bank via BI-FAST', path: '/guides/bifast-transfers', category: 'Guides' },
  { title: 'Investment API', description: 'Reksa dana, deposito, SBN — beli dan jual investasi', path: '/guides/investments', category: 'Guides' },
  { title: 'Lending API', description: 'Pengajuan pinjaman, cicilan, dan repayment', path: '/guides/lending', category: 'Guides' },
  // SDK
  { title: 'Java SDK', description: 'Library Java untuk integrasi PayU API', path: '/sdk/java', category: 'SDK' },
  { title: 'TypeScript SDK', description: 'Library TypeScript/Node.js untuk PayU API', path: '/sdk/typescript', category: 'SDK' },
  { title: 'Python SDK', description: 'Library Python untuk PayU API', path: '/sdk/python', category: 'SDK' },
];

export default function DocSearch() {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const router = useRouter();

  const search = useCallback((q: string) => {
    if (!q.trim()) {
      setResults([]);
      return;
    }
    const lower = q.toLowerCase();
    const filtered = SEARCH_INDEX.filter(
      (item) =>
        item.title.toLowerCase().includes(lower) ||
        item.description.toLowerCase().includes(lower) ||
        item.category.toLowerCase().includes(lower)
    );
    setResults(filtered);
    setSelectedIndex(0);
  }, []);

  const open = useCallback(() => {
    setIsOpen(true);
    setTimeout(() => inputRef.current?.focus(), 50);
  }, []);

  const close = useCallback(() => {
    setIsOpen(false);
    setQuery('');
    setResults([]);
  }, []);

  const navigate = useCallback(
    (path: string) => {
      close();
      router.push(path);
    },
    [close, router]
  );

  // Keyboard shortcut: Cmd/Ctrl + K
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        if (isOpen) close();
        else open();
      }
      if (e.key === 'Escape' && isOpen) {
        close();
      }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [isOpen, open, close]);

  // Arrow key navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((i) => Math.min(i + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter' && results[selectedIndex]) {
      navigate(results[selectedIndex].path);
    }
  };

  return (
    <>
      {/* Search trigger button */}
      <button
        onClick={open}
        className="flex items-center gap-2 px-3 py-1.5 rounded-xl border border-border bg-muted/50 text-muted-foreground text-sm hover:bg-accent/50 transition-colors"
      >
        <Search className="w-4 h-4" />
        <span>Cari docs...</span>
        <kbd className="hidden sm:inline-flex items-center gap-0.5 px-1.5 py-0.5 bg-background rounded text-xs border border-border font-mono">
          ⌘K
        </kbd>
      </button>

      {/* Modal overlay */}
      {isOpen && (
        <div className="fixed inset-0 z-[100]">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={close} />
          <div className="fixed top-[15%] left-1/2 -translate-x-1/2 w-full max-w-xl">
            <div className="bg-card border border-border rounded-2xl shadow-2xl overflow-hidden">
              {/* Search input */}
              <div className="flex items-center gap-3 px-4 border-b border-border">
                <Search className="w-5 h-5 text-muted-foreground shrink-0" />
                <input
                  ref={inputRef}
                  type="text"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    search(e.target.value);
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="Cari dokumentasi..."
                  className="flex-1 py-4 bg-transparent outline-none text-base placeholder:text-muted-foreground"
                />
                {query && (
                  <button onClick={() => { setQuery(''); setResults([]); }} className="text-muted-foreground hover:text-foreground">
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>

              {/* Results */}
              <div className="max-h-80 overflow-y-auto">
                {results.length > 0 ? (
                  <div className="py-2">
                    {results.map((result, index) => (
                      <button
                        key={result.path}
                        onClick={() => navigate(result.path)}
                        className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors ${
                          index === selectedIndex ? 'bg-accent text-bank-green' : 'hover:bg-accent/50'
                        }`}
                      >
                        <FileText className="w-5 h-5 shrink-0 text-muted-foreground" />
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-sm truncate">{result.title}</div>
                          <div className="text-xs text-muted-foreground truncate">{result.description}</div>
                        </div>
                        <span className="text-xs text-muted-foreground shrink-0">{result.category}</span>
                        <ArrowRight className="w-4 h-4 shrink-0 text-muted-foreground" />
                      </button>
                    ))}
                  </div>
                ) : query ? (
                  <div className="px-4 py-8 text-center text-muted-foreground text-sm">
                    Tidak ada hasil untuk &quot;{query}&quot;
                  </div>
                ) : (
                  <div className="px-4 py-8 text-center text-muted-foreground text-sm">
                    Ketik untuk mencari dokumentasi...
                  </div>
                )}
              </div>

              {/* Footer */}
              <div className="flex items-center gap-4 px-4 py-2 border-t border-border text-xs text-muted-foreground">
                <span className="flex items-center gap-1">
                  <kbd className="px-1 py-0.5 bg-muted rounded border border-border font-mono">↑↓</kbd> navigasi
                </span>
                <span className="flex items-center gap-1">
                  <kbd className="px-1 py-0.5 bg-muted rounded border border-border font-mono">↵</kbd> buka
                </span>
                <span className="flex items-center gap-1">
                  <kbd className="px-1 py-0.5 bg-muted rounded border border-border font-mono">esc</kbd> tutup
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
