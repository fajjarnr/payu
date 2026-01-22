'use client';

import { ArrowLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

interface MobileHeaderProps {
  title: string;
  showBack?: boolean;
}

export default function MobileHeader({ title, showBack = true }: MobileHeaderProps) {
  const router = useRouter();

  return (
    <header className="sticky top-0 z-40 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-100 dark:border-gray-800 px-4 h-16 flex items-center justify-between">
      <div className="flex items-center gap-4">
        {showBack && (
          <button 
            onClick={() => router.back()}
            className="p-2 -ml-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-700 dark:text-gray-200"
          >
            <ArrowLeft className="h-6 w-6" />
          </button>
        )}
        <h1 className="text-lg font-bold text-gray-900 dark:text-white">{title}</h1>
      </div>
    </header>
  );
}
