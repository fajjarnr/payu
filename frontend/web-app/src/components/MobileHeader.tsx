'use client';

import { ArrowLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';

interface MobileHeaderProps {
 title: string;
 showBack?: boolean;
}

export default function MobileHeader({ title, showBack = true }: MobileHeaderProps) {
 const router = useRouter();

 return (
  <header className="sticky top-0 z-40 bg-card/80 backdrop-blur-md border-b border-border px-4 h-16 flex items-center justify-between">
   <div className="flex items-center gap-3">
    {showBack && (
     <button
      onClick={() => router.back()}
      className="p-2 -ml-2 rounded-full hover:bg-muted text-foreground transition-colors"
     >
      <ArrowLeft className="h-5 w-5" />
     </button>
    )}
    <h1 className="text-lg font-black text-foreground">{title}</h1>
   </div>
  </header>
 );
}
