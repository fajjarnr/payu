'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { QrCode, Camera, History, Image as ImageIcon, ShieldCheck, Info } from 'lucide-react';
import { toast } from 'sonner';

export default function QRISPage() {
   
  const [isScanning, setIsScanning] = useState(false);
  const [showMyQr, setShowMyQr] = useState(false);
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const handleToggleCamera = () => {
    setIsScanning(!isScanning);
    if (!isScanning) {
      toast.info('Kamera aktif — arahkan ke kode QRIS');
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setIsScanning(true);
      setTimeout(() => {
        setIsScanning(false);
        toast.success(`QR Code dari "${file.name}" terdeteksi: Merchant PayU Simulator`);
      }, 1000);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-12">
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileChange}
          accept="image/*"
          className="hidden"
        />

        <div className="flex justify-between items-end">
          <div>
            <h2 className="text-3xl font-bold text-foreground ">Pembayaran QRIS</h2>
            <p className="text-sm text-gray-500 font-medium">Pindai kode QRIS merchant atau P2P untuk membayar secara instan.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-8 xl:gap-8">
          {/* Main Scanner Column (8 units) */}
          <div className="md:col-span-12 lg:col-span-8 space-y-8 xl:space-y-12">
            <div className="bg-card rounded-2xl border border-border shadow-2xl relative overflow-hidden group min-h-[480px] xl:min-h-[550px] flex flex-col items-center justify-center p-8 sm:p-14 xl:p-8">
              {/* Premium Background Effects */}
              <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-emerald-500/5 rounded-full blur-[100px] -z-0" />
              <div className="absolute bottom-0 left-0 w-64 h-64 bg-emerald-500/5 rounded-full blur-[80px] -z-0" />
              
              <div className="relative z-10 w-full max-w-md text-center space-y-12">
                <div className="relative aspect-square max-w-[350px] xl:max-w-[400px] mx-auto">
                    {/* Scanner Frame */}
                    <div className={`absolute inset-0 rounded-2xl border-2 border-dashed transition-all duration-700 ${isScanning ? 'bg-emerald-500/10 border-emerald-500' : 'bg-muted/20 border-border group-hover:border-emerald-500/40'}`} />
                    <div className="absolute inset-8 xl:inset-10 border-2 border-emerald-500/20 rounded-xl animate-pulse" />
                    
                    {/* Floating Scanner Icon */}
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                        <div className={`w-22 h-22 xl:w-24 xl:h-24 bg-background rounded-2xl flex items-center justify-center mb-5 shadow-2xl border border-border transition-transform duration-500 ${isScanning ? 'scale-110 ring-4 ring-emerald-500/30' : 'group-hover:scale-110'}`}>
                            <Camera className={`h-9 w-9 xl:h-10 xl:w-10 ${isScanning ? 'text-emerald-400 animate-pulse' : 'text-emerald-500'}`} />
                        </div>
                        <p className="text-xs font-bold text-muted-foreground tracking-[0.3em] uppercase opacity-40">
                          {isScanning ? 'Kamera Aktif — Mengarahkan ke QR...' : 'Scanning for QRIS Codes...'}
                        </p>
                    </div>

                    {/* Corner Borders */}
                    <div className="absolute top-0 left-0 w-10 h-10 border-t-4 border-l-4 border-emerald-500 rounded-tl-2xl" />
                    <div className="absolute top-0 right-0 w-10 h-10 border-t-4 border-r-4 border-emerald-500 rounded-tr-2xl" />
                    <div className="absolute bottom-0 left-0 w-10 h-10 border-b-4 border-l-4 border-emerald-500 rounded-bl-2xl" />
                    <div className="absolute bottom-0 right-0 w-10 h-10 border-b-4 border-r-4 border-emerald-500 rounded-br-2xl" />
                </div>

                <div className="flex flex-col sm:flex-row gap-5 max-w-sm mx-auto">
                  <button 
                    onClick={handleToggleCamera}
                    data-testid="qris-camera-button"
                    className="flex-1 bg-gradient-to-r from-emerald-600 to-emerald-500 text-white py-5 rounded-xl font-bold text-xs tracking-[0.2em] shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/40 transition-all active:scale-95 flex items-center justify-center gap-2 uppercase">
                    <Camera className="h-4 w-4" /> {isScanning ? 'Tutup Kamera' : 'Buka Kamera'}
                  </button>
                  <button 
                    onClick={handleUploadClick}
                    data-testid="qris-upload-button"
                    className="flex-1 bg-muted/40 text-foreground py-5 rounded-xl font-bold text-xs tracking-[0.2em] border border-border hover:bg-muted/60 transition-all active:scale-95 flex items-center justify-center gap-2 uppercase">
                    <ImageIcon className="h-4 w-4 text-emerald-500" /> Unggah Foto
                  </button>
                </div>
              </div>
            </div>

            {/* Recent Payments Section */}
            <div className="bg-card rounded-2xl border border-border shadow-sm p-8 sm:p-8 xl:p-8">
              <div className="flex justify-between items-center mb-8">
                <div className="flex items-center gap-3">
                    <History className="h-5 w-5 text-emerald-500" />
                    <h3 className="text-lg xl:text-xl font-bold text-foreground">Aktivitas Terakhir</h3>
                </div>
                <button 
                  onClick={() => toast.info('Menampilkan semua transaksi QRIS')}
                  className="text-xs font-bold text-emerald-600 tracking-[0.2em] hover:text-emerald-500 transition-colors uppercase border-b border-emerald-500/20">
                  Lihat Semua
                </button>
              </div>

              <div className="space-y-4">
                <div className="py-20 text-center bg-muted/10 rounded-2xl border border-dashed border-border/50">
                   <div className="w-16 h-16 bg-muted/30 rounded-full flex items-center justify-center mx-auto mb-4 opacity-30">
                      <History className="h-8 w-8 text-foreground" />
                   </div>
                   <p className="text-xs font-bold text-muted-foreground/40 tracking-[0.1em] uppercase">Belum ada riwayat transaksi QRIS</p>
                </div>
              </div>
            </div>
          </div>

          {/* Right Sidebar Column (4 units) */}
          <div className="md:col-span-12 lg:col-span-4 space-y-8 xl:space-y-12">
            {/* Security Status Card */}
            <div className="bg-card rounded-2xl p-8 xl:p-8 border border-border shadow-sm">
              <h3 className="text-xs font-bold text-muted-foreground tracking-[0.2em] mb-8 uppercase opacity-60">Protokol Keamanan</h3>
              <div className="space-y-8">
                <div className="flex gap-4">
                  <div className="h-10 w-10 bg-emerald-500/10 rounded-xl flex items-center justify-center shrink-0 border border-emerald-500/10">
                    <ShieldCheck className="h-5 w-5 text-emerald-500" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-foreground">Enkripsi RESP-V3</p>
                    <p className="text-xs text-muted-foreground font-medium tracking-tight mt-1 leading-relaxed opacity-70">Token dinamik di-hash per transaksi untuk keamanan maksimal.</p>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="h-10 w-10 bg-emerald-500/10 rounded-xl flex items-center justify-center shrink-0 border border-emerald-500/10">
                    <Info className="h-5 w-5 text-emerald-500" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-foreground">Lisensi ASPI/BI</p>
                    <p className="text-xs text-muted-foreground font-medium tracking-tight mt-1 leading-relaxed opacity-70">Sistem pembayaran tunduk pada regulasi QRIS Nasional.</p>
                  </div>
                </div>
              </div>
            </div>

            {/* My QR Card */}
            <div className="bg-gray-900 rounded-2xl p-8 xl:p-8 text-white relative overflow-hidden shadow-2xl group border border-white/5">
              <div className="relative z-10">
                <div className="flex justify-between items-start mb-8">
                    <div>
                        <h4 className="font-bold text-xl tracking-tight">QRIS Personal</h4>
                        <p className="text-xs text-emerald-400 font-bold tracking-widest uppercase">E-Wallet Access</p>
                    </div>
                    <QrCode className="h-7 w-7 text-emerald-500/40" />
                </div>
                
                <div className="bg-white/5 backdrop-blur-sm rounded-2xl p-8 mb-8 flex justify-center border border-white/5 shadow-inner group-hover:bg-white/10 transition-colors">
                   <QrCode className={`h-32 w-32 transition-all ${showMyQr ? 'text-emerald-400 scale-105' : 'text-white/20'}`} />
                   {!showMyQr && (
                     <div className="absolute inset-0 flex items-center justify-center">
                         <p className="text-xs font-bold text-white/30 tracking-[0.2em] uppercase origin-center -rotate-12">Authorized Only</p>
                     </div>
                   )}
                </div>
                
                <button 
                    onClick={() => setShowMyQr(!showMyQr)}
                    data-testid="qris-show-personal-button"
                    className="w-full py-4 bg-emerald-600/20 hover:bg-emerald-600/40 text-emerald-400 rounded-xl font-bold text-xs tracking-[0.2em] transition-all border border-emerald-600/30 uppercase">
                    {showMyQr ? 'Sembunyikan Kode' : 'Tampilkan Kode Saya'}
                </button>
              </div>
              <div className="absolute top-[-30px] left-[-30px] w-40 h-40 bg-emerald-500/10 rounded-full blur-[80px]" />
            </div>

            {/* Daily Limit Card */}
            <div className="bg-muted/30 rounded-2xl p-8 xl:p-8 border border-border flex flex-col justify-between min-h-[180px] xl:min-h-[200px]">
               <div className="space-y-1">
                  <p className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase opacity-60">Limit Harian QRIS</p>
                  <p className="text-2xl xl:text-3xl font-bold text-foreground tabular-nums">Rp 10.000.000</p>
               </div>
               <div className="space-y-4">
                 <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
                    <div className="h-full bg-emerald-500/40 rounded-full" style={{ width: '0%' }} />
                 </div>
                 <p className="text-xs font-bold text-emerald-600 tracking-widest uppercase">0% Terpakai</p>
               </div>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
