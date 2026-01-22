'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { LifeBuoy, MessageCircle, Mail, Phone, ExternalLink, HelpCircle, FileText, CheckCircle2 } from 'lucide-react';
import clsx from 'clsx';

export default function SupportPage() {
    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-12 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Support Terminal</h2>
                        <p className="text-sm text-gray-500 font-medium">How can we assist you today? Our specialized teams are available 24/7.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {[
                        { label: 'Live Assistance', desc: 'Real-time chat with support agents.', icon: MessageCircle, action: 'Connect Now', color: 'bg-bank-green' },
                        { label: 'Signal Protocol', desc: 'Secure asynchronous email support.', icon: Mail, action: 'Send Signal', color: 'bg-blue-600' },
                        { label: 'Direct Priority', desc: 'Immediate global telephony support.', icon: Phone, action: 'Request Call', color: 'bg-foreground' },
                    ].map((channel, i) => (
                        <div key={i} className="bg-card rounded-[2.5rem] p-8 border border-border shadow-sm flex flex-col items-center text-center group hover:shadow-xl hover:shadow-bank-green/5 transition-all">
                            <div className={clsx("h-16 w-16 mb-6 rounded-3xl flex items-center justify-center text-white shadow-xl transition-transform group-hover:scale-110", channel.color)}>
                                <channel.icon className="h-8 w-8" />
                            </div>
                            <h3 className="text-lg font-black text-foreground uppercase tracking-tight mb-2">{channel.label}</h3>
                            <p className="text-[10px] text-gray-500 font-bold uppercase tracking-widest leading-relaxed mb-8">{channel.desc}</p>
                            <button className="w-full py-4 bg-gray-50 dark:bg-gray-900 border border-border rounded-2xl font-black text-[10px] uppercase tracking-widest hover:bg-bank-green hover:text-white hover:border-bank-green transition-all active:scale-95">
                                {channel.action}
                            </button>
                        </div>
                    ))}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* Knowledge Base */}
                    <div className="lg:col-span-12">
                        <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm">
                            <h3 className="text-xl font-black text-foreground uppercase tracking-tight mb-8">Intelligence Repository</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                {[
                                    { title: 'Identity Synchronization', desc: 'Updating your eKYC and biometric mapping.', icon: HelpCircle },
                                    { title: 'Global Transaction Limits', desc: 'Understanding your account credit tier limits.', icon: FileText },
                                    { title: 'Hardware Token Issues', desc: 'Troubleshooting MFA and secure key errors.', icon: HelpCircle },
                                    { title: 'Fraud Prevention Protocol', desc: 'Action steps for suspicious account activity.', icon: FileText },
                                ].map((faq, i) => (
                                    <div key={i} className="flex gap-6 p-6 bg-gray-50 dark:bg-gray-900/50 rounded-[2rem] border border-transparent hover:border-border transition-all cursor-pointer group">
                                        <div className="h-12 w-12 bg-white dark:bg-gray-800 rounded-2xl flex items-center justify-center shadow-sm shrink-0 transition-transform group-hover:rotate-12">
                                            <faq.icon className="h-5 w-5 text-bank-green" />
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2 mb-1">
                                                <h4 className="font-black text-foreground text-sm uppercase tracking-tight">{faq.title}</h4>
                                                <ExternalLink className="h-3 w-3 text-gray-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                                            </div>
                                            <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">{faq.desc}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="lg:col-span-12">
                        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[3rem] p-12 text-white relative overflow-hidden group">
                            <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
                                <div className="space-y-4 max-w-2xl">
                                    <h3 className="text-3xl font-black italic tracking-tighter">System Integrity Status.</h3>
                                    <div className="flex flex-wrap gap-4">
                                        {['Gateway', 'Transactions', 'Wallet', 'Auth'].map((svc, i) => (
                                            <div key={i} className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/10">
                                                <CheckCircle2 className="h-4 w-4 text-bank-green" />
                                                <span className="text-[10px] font-black uppercase tracking-widest">{svc} Operational</span>
                                            </div>
                                        ))}
                                    </div>
                                    <p className="text-sm text-gray-400 font-medium pt-4">All infrastructure nodes are currently reporting 100% uptime with zero latency issues detected.</p>
                                </div>
                                <button className="whitespace-nowrap px-10 py-5 bg-bank-green text-white rounded-3xl font-black uppercase text-xs tracking-widest hover:bg-bank-emerald transition-all active:scale-95 shadow-2xl shadow-bank-green/20">
                                    Report Node Issue
                                </button>
                            </div>
                            <LifeBuoy className="absolute top-[-40px] left-[-40px] h-64 w-64 text-white/5 -rotate-12 group-hover:rotate-12 transition-transform duration-[2000ms]" />
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
