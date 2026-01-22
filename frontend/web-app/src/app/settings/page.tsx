'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { User, Mail, Smartphone, MapPin, Globe, Bell, Moon, Sun, Trash2, Shield, CreditCard } from 'lucide-react';
import clsx from 'clsx';

export default function SettingsPage() {
    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-10 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Account Ecosystem</h2>
                        <p className="text-sm text-gray-500 font-medium">Manage your personal profile, regional preferences, and notification triggers.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* Sidebar Profiles */}
                    <div className="lg:col-span-4 space-y-8">
                        <div className="bg-card rounded-[2.5rem] p-8 border border-border shadow-sm flex flex-col items-center text-center relative overflow-hidden">
                            <div className="absolute top-0 right-0 w-32 h-32 bg-bank-green/5 rounded-full blur-3xl" />

                            <div className="relative w-28 h-28 bg-gradient-to-br from-bank-green to-bank-emerald rounded-[2rem] flex items-center justify-center text-white font-black text-4xl shadow-2xl shadow-bank-green/20 mb-6">
                                F
                            </div>
                            <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Fajar Nur Romadhon</h3>
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Premium Tier Member</p>

                            <div className="w-full h-[1px] bg-gray-100 dark:bg-gray-800 my-8" />

                            <div className="w-full space-y-4">
                                <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-widest">
                                    <span className="text-gray-400">Account ID</span>
                                    <span className="text-foreground">PAYU-09228373</span>
                                </div>
                                <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-widest">
                                    <span className="text-gray-400">Status</span>
                                    <span className="text-bank-green">Verified eKYC</span>
                                </div>
                            </div>
                        </div>

                        <div className="bg-card rounded-[2.5rem] p-4 border border-border shadow-sm">
                            <div className="space-y-1">
                                {[
                                    { label: 'General Profile', icon: User, active: true },
                                    { label: 'Billing & Plans', icon: CreditCard, active: false },
                                    { label: 'Privacy & Security', icon: Shield, active: false },
                                    { label: 'Advanced Settings', icon: Globe, active: false },
                                ].map((item, i) => (
                                    <button
                                        key={i}
                                        className={clsx(
                                            "w-full flex items-center gap-4 p-4 rounded-2xl transition-all font-black text-[10px] uppercase tracking-widest",
                                            item.active ? "bg-bank-green text-white shadow-lg shadow-bank-green/20" : "text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-900 hover:text-foreground"
                                        )}
                                    >
                                        <item.icon className="h-5 w-5" />
                                        {item.label}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Main Settings Form */}
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm space-y-12">
                            {/* Personal Details */}
                            <section className="space-y-8">
                                <h3 className="text-lg font-black text-foreground uppercase tracking-widest flex items-center gap-3">
                                    <div className="h-8 w-8 bg-gray-50 dark:bg-gray-900 rounded-xl flex items-center justify-center">
                                        <User className="h-4 w-4 text-gray-400" />
                                    </div>
                                    Personal Credentials
                                </h3>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                                    <div className="space-y-3">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Full Legal Name</label>
                                        <input type="text" defaultValue="Fajar Nur Romadhon" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all" />
                                    </div>
                                    <div className="space-y-3">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Contact Email</label>
                                        <input type="email" defaultValue="fajar@payu.id" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all" />
                                    </div>
                                    <div className="space-y-3">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Phone Protocol</label>
                                        <input type="text" defaultValue="+62 812-3456-7890" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all" />
                                    </div>
                                    <div className="space-y-3">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Current Residence</label>
                                        <input type="text" defaultValue="Jakarta, Indonesia" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-4 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all" />
                                    </div>
                                </div>
                            </section>

                            <div className="h-[1px] w-full bg-gray-100 dark:bg-gray-800" />

                            {/* Preferences */}
                            <section className="space-y-8">
                                <h3 className="text-lg font-black text-foreground uppercase tracking-widest flex items-center gap-3">
                                    <div className="h-8 w-8 bg-gray-50 dark:bg-gray-900 rounded-xl flex items-center justify-center">
                                        <Bell className="h-4 w-4 text-gray-400" />
                                    </div>
                                    System Preferences
                                </h3>

                                <div className="space-y-6">
                                    {[
                                        { label: 'Push Notifications', desc: 'Real-time transaction & status alerts', icon: Bell, active: true },
                                        { label: 'Dark Mode Graphics', desc: 'High contrast visual interface', icon: Moon, active: false },
                                        { label: 'Marketing Insights', desc: 'Promotions, news, and rewards updates', icon: Globe, active: true },
                                    ].map((pref, i) => (
                                        <div key={i} className="flex items-center justify-between group">
                                            <div>
                                                <p className="font-black text-foreground uppercase tracking-tight text-sm">{pref.label}</p>
                                                <p className="text-[10px] text-gray-500 font-bold uppercase tracking-widest mt-1">{pref.desc}</p>
                                            </div>
                                            <div className={clsx(
                                                "w-12 h-6 rounded-full relative p-1 transition-all",
                                                pref.active ? "bg-bank-green" : "bg-gray-200 dark:bg-gray-700"
                                            )}>
                                                <div className={clsx(
                                                    "w-4 h-4 bg-white rounded-full transition-all",
                                                    pref.active ? "translate-x-6" : "translate-x-0"
                                                )} />
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </section>

                            <div className="h-[1px] w-full bg-gray-100 dark:bg-gray-800" />

                            <div className="flex gap-4 pt-4">
                                <button className="flex-1 bg-bank-green text-white py-5 rounded-3xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-xl shadow-bank-green/20">
                                    Synchronize Profile
                                </button>
                                <button className="px-5 bg-red-50 text-red-500 rounded-3xl font-black hover:bg-red-100 transition-all active:scale-95 flex items-center justify-center gap-2">
                                    <Trash2 className="h-5 w-5" />
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
