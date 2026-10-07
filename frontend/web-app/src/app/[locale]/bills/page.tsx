"use client";

import {
  Smartphone,
  Zap,
  Droplets,
  Wifi,
  CreditCard,
  Heart,
  Tv,
  Gamepad2,
  Plus,
  ChevronRight,
  LifeBuoy,
  CheckCircle2,
} from "@/components/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { CreatePaymentRequest, PaymentResponse } from "@/types";
import api from "@/lib/api";
import { useState } from "react";
import { useRouter } from "next/navigation";
import DashboardLayout from "@/components/DashboardLayout";
import { SkipLink } from "@/lib/a11y";
import { Button, Input } from "antd";
import { useUIStore } from "@/stores";
import { useAuthStore } from "@/stores/authStore";
import { cn, idempotencyKeyFor } from "@/lib/utils";
import { notify as toast } from "@/lib/notify";
import { formatCurrency, parseCurrencyExact } from "@/lib/currency";

export default function BillsPage() {
  const router = useRouter();
  const { addToast } = useUIStore();
  const { accountId: authAccountId } = useAuthStore();
  const [selectedBiller, setSelectedBiller] = useState<{
    name: string;
    icon: React.ComponentType<{ className?: string }>;
    color: string;
    code: string;
  } | null>(null);
  const [customerId, setCustomerId] = useState("");
  const [amount, setAmount] = useState("");
  const [showSuccess, setShowSuccess] = useState(false);
  const [successData, setSuccessData] = useState<{
    billerName: string;
    billerIcon: React.ComponentType<{ className?: string }>;
    billerColor: string;
    customerId: string;
    amount: string;
    timestamp: Date;
  } | null>(null);

  const billers = [
    {
      name: "Pulsa",
      icon: Smartphone,
      color: "bg-primary-light text-secondary",
      code: "PULSA",
    },
    {
      name: "Listrik (PLN)",
      icon: Zap,
      color: "bg-warning text-secondary",
      code: "PLN",
    },
    {
      name: "Air (PDAM)",
      icon: Droplets,
      color: "bg-primary-light text-primary-dark",
      code: "PDAM",
    },
    {
      name: "Internet/TV",
      icon: Wifi,
      color: "bg-secondary text-white",
      code: "INTERNET",
    },
    {
      name: "Saldo Kartu",
      icon: CreditCard,
      color: "bg-accent text-white",
      code: "CARDS",
    },
    { name: "BPJS", icon: Heart, color: "bg-success text-white", code: "BPJS" },
    { name: "TV Kabel", icon: Tv, color: "bg-accent text-white", code: "TV" },
    {
      name: "Game Voucher",
      icon: Gamepad2,
      color: "bg-accent text-white",
      code: "VOUCHER",
    },
  ];

  const accountId = authAccountId;

  const { data: recentPayments, isLoading } = useQuery({
    queryKey: ["recent-payments", accountId],
    queryFn: async () => {
      const response = await api.get("/payments?size=5");
      return response.data;
    },
    enabled: !!accountId,
  });

  const paymentMutation = useMutation({
    mutationFn: (data: CreatePaymentRequest) => {
      // FE-IDM-003: deterministic idempotency key so a safe retry of the same
      // bill payment reuses the same key (no duplicate mutation on timeout/retry).
      return api.post("/payments", data, {
        headers: {
          "X-Idempotency-Key": idempotencyKeyFor(
            "billing:pay",
            `${data.billerCode}:${data.customerId}:${data.amount}`,
          ),
        },
      });
    },
    onSuccess: () => {
      addToast(
        `Pembayaran ${selectedBiller?.name} sebesar ${formatCurrency(amount)} telah diproses.`,
        "success",
      );
      setSuccessData({
        billerName: selectedBiller?.name ?? "",
        billerIcon: selectedBiller?.icon ?? Zap,
        billerColor: selectedBiller?.color ?? "bg-primary-light text-secondary",
        customerId,
        amount,
        timestamp: new Date(),
      });
      setShowSuccess(true);
      setSelectedBiller(null);
      setCustomerId("");
      setAmount("");
    },
    onError: (error) => {
      console.error("Pembayaran gagal:", error);
      addToast(
        "Terjadi kesalahan saat memproses pembayaran. Silakan coba lagi.",
        "error",
      );
    },
  });

  const handlePay = () => {
    if (
      !selectedBiller ||
      !customerId ||
      !amount ||
      parseCurrencyExact(amount) === "0"
    ) {
      toast.warning("Silakan isi semua bidang yang diperlukan");
      return;
    }

    const data: CreatePaymentRequest = {
      accountId: authAccountId ?? "",
      billerCode: selectedBiller.code,
      customerId,
      amount: parseCurrencyExact(amount),
    };

    paymentMutation.mutate(data);
  };

  const recentBills = recentPayments?.content || [];

  if (showSuccess && successData) {
    const SuccessIcon = successData.billerIcon;
    return (
      <DashboardLayout>
        <SkipLink href="#main-content" />
        <main id="main-content" className="overflow-x-hidden">
          <div className="flex flex-col items-center justify-center min-h-[60vh] px-4 py-12 text-center">
            <div className="animate-fade-in flex flex-col items-center">
              <div className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center mb-8">
                <CheckCircle2 className="w-12 h-12 text-primary" />
              </div>

              <h2 className="text-3xl font-bold text-foreground tracking-tight mb-2">
                Pembayaran Berhasil!
              </h2>
              <p className="text-sm text-muted-foreground font-medium mb-10">
                Transaksi Anda telah diproses dengan aman.
              </p>

              <div className="bg-card rounded-2xl p-6 sm:p-8 border border-border shadow-sm w-full max-w-md mb-10">
                <div className="flex items-center gap-4 mb-6 pb-6 border-b border-border">
                  <div
                    className={`w-14 h-14 rounded-xl ${successData.billerColor} flex items-center justify-center shadow-lg`}
                  >
                    <SuccessIcon className="h-7 w-7" />
                  </div>
                  <div className="text-left">
                    <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase mb-1">
                      Penyedia Layanan
                    </p>
                    <h3 className="text-lg font-bold text-foreground">
                      {successData.billerName}
                    </h3>
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                      ID Pelanggan
                    </span>
                    <span className="font-bold text-foreground text-sm">
                      {successData.customerId}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                      Jumlah
                    </span>
                    <span className="text-2xl font-bold text-foreground">
                      {formatCurrency(successData.amount)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                      Waktu
                    </span>
                    <span className="font-bold text-foreground text-sm">
                      {successData.timestamp.toLocaleString("id-ID", {
                        day: "numeric",
                        month: "long",
                        year: "numeric",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row gap-4 w-full max-w-md">
                <Button
                  type="primary"
                  onClick={() => {
                    setShowSuccess(false);
                    setSuccessData(null);
                  }}
                  className="flex-1 h-14 rounded-xl shadow-lg shadow-primary/20 font-bold"
                >
                  Bayar Lagi
                </Button>
                <Button
                  type="default"
                  onClick={() => router.push("/dashboard")}
                  className="flex-1 h-14 rounded-xl font-bold"
                >
                  Kembali ke Dashboard
                </Button>
              </div>
            </div>
          </div>
        </main>
      </DashboardLayout>
    );
  }

  if (selectedBiller) {
    return (
      <DashboardLayout>
        <SkipLink href="#main-content" />
        <main id="main-content" className="overflow-x-hidden">
          <div className="space-y-6 lg:space-y-8">
            <div className="flex items-center gap-4">
              <Button
                type="default"
                onClick={() => setSelectedBiller(null)}
                className="h-12 w-12 rounded-xl"
                aria-label="Kembali"
              >
                <ChevronRight className="h-6 w-6 rotate-180" />
              </Button>
            </div>

            <div className="bg-card rounded-2xl p-6 sm:p-10 border border-border relative overflow-hidden group shadow-sm">
              <div
                className={cn(
                  "absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 opacity-10 rounded-full blur-3xl -z-0",
                  selectedBiller.color.split(" ")[0],
                )}
              />

              <div className="relative z-10 flex items-center gap-4 sm:gap-6 mb-6 pb-6 border-b border-border">
                <div
                  className={`w-16 h-16 sm:w-20 sm:h-20 rounded-xl ${selectedBiller.color} flex items-center justify-center shadow-xl transition-transform group-hover:scale-110`}
                >
                  <selectedBiller.icon className="h-8 w-8 sm:h-10 sm:w-10" />
                </div>
                <div>
                  <p className="text-xs text-text-disabled font-bold tracking-widest mb-1">
                    Penyedia Layanan
                  </p>
                  <h3 className="text-xl sm:text-2xl font-bold text-foreground ">
                    {selectedBiller.name}
                  </h3>
                  <p className="text-xs font-bold text-bank-green tracking-widest">
                    Mitra Pembayaran Resmi
                  </p>
                </div>
              </div>

              <div className="space-y-6 lg:space-y-8 relative z-10">
                <div className="group">
                  <label
                    htmlFor="bills-customer-id"
                    className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase ml-1 block mb-4 group-focus-within:text-primary transition-colors"
                  >
                    ID Pelanggan / Nomor Rekening
                  </label>
                  <Input
                    id="bills-customer-id"
                    type="text"
                    value={customerId}
                    onChange={(e) => setCustomerId(e.target.value)}
                    placeholder="Masukkan ID unik Anda"
                    className="h-16 text-lg sm:text-xl"
                  />
                </div>

                <div className="group">
                  <label
                    htmlFor="bills-amount"
                    className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase ml-1 block mb-4 group-focus-within:text-primary transition-colors"
                  >
                    Jumlah Pembayaran (IDR)
                  </label>
                  <div className="relative">
                    <div className="absolute left-6 top-1/2 -translate-y-1/2 text-xl sm:text-2xl font-bold text-muted-foreground/30 pointer-events-none">
                      Rp
                    </div>
                    <Input
                      id="bills-amount"
                      type="number"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="0"
                      className="h-20 pl-16 text-3xl sm:text-4xl"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-6">
              <Button
                type="primary"
                onClick={handlePay}
                disabled={paymentMutation.isPending}
                className="w-full h-16 rounded-2xl shadow-xl shadow-primary/20"
              >
                {paymentMutation.isPending
                  ? "Sedang Memproses..."
                  : "Konfirmasi & Bayar Sekarang"}
              </Button>
              <p className="text-center text-xs text-muted-foreground font-bold tracking-widest uppercase opacity-60">
                Transaksi aman terenkripsi oleh Infrastruktur Protokol PayU
              </p>
            </div>
          </div>
        </main>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content" className="overflow-x-hidden">
        <div className="space-y-6 lg:space-y-8">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
            <div>
              <h2 className="text-3xl font-bold text-foreground tracking-tight">
                Tagihan & Top-up
              </h2>
              <p className="text-sm text-muted-foreground font-medium mt-1">
                Bayar tagihan utilitas dan top up dompet digital Anda secara
                instan.
              </p>
            </div>
            <div className="bg-primary/10 px-6 py-3 rounded-full border border-primary/20 hidden md:block shadow-sm">
              <p className="text-xs font-bold text-primary tracking-widest uppercase animate-pulse">
                Penyelesaian Real-time 24/7
              </p>
            </div>
          </div>

          <div className="bg-card rounded-2xl p-8 sm:p-12 border border-border relative overflow-hidden shadow-sm">
            <div className="absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 bg-primary/5 rounded-full blur-3xl" />
            <h3 className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase mb-6 text-center opacity-60">
              Kategori Layanan
            </h3>
            <div className="grid grid-cols-3 sm:grid-cols-4 gap-4 sm:gap-12 relative z-10">
              {billers.map((item) => (
                <Button
                  type="text"
                  key={item.name}
                  onClick={() => setSelectedBiller(item)}
                  className="flex flex-col items-center gap-4 transition-all group active:scale-95"
                >
                  <div
                    className={`w-16 h-16 sm:w-20 sm:h-20 rounded-2xl ${item.color} flex items-center justify-center shadow-lg transition-transform group-hover:scale-110`}
                  >
                    <item.icon className="h-7 w-7 sm:h-9 sm:w-9" />
                  </div>
                  <span className="text-xs font-bold text-foreground tracking-widest uppercase">
                    {item.name}
                  </span>
                </Button>
              ))}
              <Button
                type="text"
                className="flex flex-col items-center gap-4 transition-all group active:scale-95"
              >
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-muted/50 flex items-center justify-center text-muted-foreground group-hover:text-primary transition-colors">
                  <Plus className="h-7 w-7 sm:h-9 sm:w-9" />
                </div>
                <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                  Lainnya
                </span>
              </Button>
            </div>
          </div>

          <div className="space-y-8">
            <h3 className="text-xl font-bold text-foreground tracking-tight">
              Aktivitas Terakhir
            </h3>
            {!isLoading && recentBills.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {recentBills.map((bill: PaymentResponse) => (
                  <div
                    key={bill.id}
                    className="bg-card p-6 sm:p-8 rounded-2xl flex items-center justify-between border border-border hover:shadow-xl transition-all group shadow-sm"
                  >
                    <div className="flex items-center gap-6">
                      <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary transition-transform group-hover:scale-110">
                        <Zap className="h-6 w-6" />
                      </div>
                      <div>
                        <div className="font-bold text-foreground text-sm uppercase tracking-wider">
                          {bill.billerCode}
                        </div>
                        <div className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase mt-1">
                          Ref: {bill.referenceNumber.slice(0, 10)}...
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-foreground text-base tabular-nums">
                        {formatCurrency(bill.amount)}
                      </div>
                      <div className="text-xs font-bold text-primary tracking-[0.2em] uppercase mt-1">
                        {bill.status}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-card rounded-2xl p-12 text-center border-2 border-dashed border-border flex flex-col items-center justify-center">
                <LifeBuoy className="h-16 w-16 text-muted/20 mb-6" />
                <p className="text-muted-foreground font-bold tracking-[0.2em] text-xs uppercase max-w-xs leading-relaxed opacity-60">
                  Pembayaran tagihan terakhir Anda akan muncul di sini.
                </p>
              </div>
            )}
          </div>
        </div>
      </main>
    </DashboardLayout>
  );
}
