'use client';

import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, ArrowLeft } from 'lucide-react';

interface Props {
 children: ReactNode;
 fallback?: ReactNode;
}

interface State {
 hasError: boolean;
 error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
 public state: State = {
  hasError: false,
  error: null,
 };

 public static getDerivedStateFromError(error: Error): State {
  return { hasError: true, error };
 }

 public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
  console.error('Error caught by boundary:', error, errorInfo);
 }

 private handleRefresh = () => {
  window.location.reload();
 };

 private handleGoBack = () => {
  window.history.back();
 };

 private handleGoHome = () => {
  window.location.href = '/';
 };

 public render() {
  if (this.state.hasError) {
   if (this.props.fallback) {
    return this.props.fallback;
   }

   return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
     <div className="max-w-md w-full bg-card rounded-[3rem] p-10 border border-border shadow-sm text-center relative overflow-hidden">
      <div className="absolute top-0 right-0 w-48 h-48 bg-red-500/5 rounded-full blur-3xl" />

      <div className="relative z-10">
       <div className="h-20 w-20 bg-red-50 dark:bg-red-900/10 rounded-[1.5rem] flex items-center justify-center mx-auto mb-8 border border-red-100 dark:border-red-900/20">
        <AlertTriangle className="h-10 w-10 text-red-500" />
       </div>

       <h2 className="text-2xl font-black text-foreground  mb-4">
        Terjadi Kesalahan
       </h2>
       
       <p className="text-sm text-gray-500 font-medium mb-8 leading-relaxed">
        Maaf, terjadi kesalahan yang tidak terduga. Kami telah mencatat masalah ini dan sedang mengatasinya.
       </p>

       {process.env.NODE_ENV === 'development' && this.state.error && (
        <div className="bg-red-50 dark:bg-red-900/10 rounded-2xl p-4 mb-8 text-left border border-red-100 dark:border-red-900/20">
         <p className="text-[10px] font-black text-gray-400 tracking-widest mb-2">Detail Teknis</p>
         <p className="text-xs text-red-600 dark:text-red-400 font-mono break-words">
          {this.state.error.message}
         </p>
        </div>
       )}

       <div className="space-y-3">
        <button
         onClick={this.handleRefresh}
         className="w-full bg-foreground text-background py-5 rounded-[1.25rem] font-black text-xs tracking-[0.2em] hover:bg-bank-green hover:text-white transition-all active:scale-95 shadow-xl flex items-center justify-center gap-2"
        >
         <RefreshCw className="h-4 w-4" />
         Muat Ulang Halaman
        </button>
        
        <div className="flex gap-3">
         <button
          onClick={this.handleGoBack}
          className="flex-1 bg-gray-50 dark:bg-gray-900 py-4 rounded-[1.25rem] font-black text-[10px] tracking-widest border border-border hover:bg-gray-100 dark:hover:bg-gray-800 transition-all active:scale-95 flex items-center justify-center gap-2"
         >
          <ArrowLeft className="h-4 w-4" />
          Kembali
         </button>
         
         <button
          onClick={this.handleGoHome}
          className="flex-1 bg-gray-50 dark:bg-gray-900 py-4 rounded-[1.25rem] font-black text-[10px] tracking-widest border border-border hover:bg-gray-100 dark:hover:bg-gray-800 transition-all active:scale-95 flex items-center justify-center gap-2"
         >
          <Home className="h-4 w-4" />
          Beranda
         </button>
        </div>
       </div>

       <p className="text-[10px] text-gray-400 font-black tracking-widest mt-8">
        Masalah berlanjut? Hubungi tim dukungan kami.
       </p>
      </div>
     </div>
    </div>
   );
  }

  return this.props.children;
 }
}
