import { Link } from '@/lib/navigation';
import { Users, AlertTriangle, Headphones, FileText, ClipboardCheck, ArrowUpRight, ShieldCheck, Zap } from 'lucide-react';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';

export default function BackofficeDashboard() {
  const stats = [
    { label: 'Total Customers', value: '\u2014', change: '\u2014', icon: Users, color: 'text-blue-500', bg: 'bg-blue-500/10' },
    { label: 'Active Sessions', value: '\u2014', change: '\u2014', icon: Zap, color: 'text-amber-500', bg: 'bg-amber-500/10' },
    { label: 'Security Alerts', value: '\u2014', change: '\u2014', icon: ShieldCheck, color: 'text-emerald-500', bg: 'bg-emerald-500/10' },
  ];

  const quickLinks = [
    { name: 'KYC Reviews', description: 'Review pending customer verifications', href: '/backoffice/kyc', icon: Users, color: 'bg-indigo-500' },
    { name: 'Fraud Monitoring', description: 'Investigate suspicious activities', href: '/backoffice/fraud', icon: AlertTriangle, color: 'bg-rose-500' },
    { name: 'Customer Ops', description: 'Manage support cases and inquiries', href: '/backoffice/customers', icon: Headphones, color: 'bg-blue-500' },
    { name: 'CMS Content', description: 'Manage banners and dynamic content', href: '/backoffice/cms', icon: FileText, color: 'bg-emerald-500' },
    { name: 'Audit Logs', description: 'Review system changes and audits', href: '/backoffice/compliance', icon: ClipboardCheck, color: 'bg-slate-500' },
  ];

  return (
    <PageTransition>
      <div className="space-y-12 pb-12">
        <StaggerContainer>
          <StaggerItem>
            <div className="mb-8">
              <h2 className="text-3xl font-bold text-foreground tracking-tight">Command Center</h2>
              <p className="text-sm text-muted-foreground font-medium mt-1">Sistem orkestrasi internal PayU Digital Banking.</p>
            </div>
          </StaggerItem>

          <StaggerItem>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
              {stats.map((stat, i) => (
                <div key={i} className="bg-card p-5 sm:p-6 lg:p-8 rounded-2xl border border-border shadow-sm group hover:shadow-card transition-all">
                  <div className="flex justify-between items-start mb-6">
                    <div className={`h-12 w-12 rounded-xl ${stat.bg} ${stat.color} flex items-center justify-center transition-transform group-hover:scale-110`}>
                      <stat.icon className="h-6 w-6" />
                    </div>
                    <span className="text-xs font-bold text-emerald-500 bg-emerald-500/10 px-2 py-1 rounded-lg">
                      {stat.change}
                    </span>
                  </div>
                  <div>
                    <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{stat.label}</p>
                    <h3 className="text-3xl font-bold text-foreground tabular-nums">{stat.value}</h3>
                  </div>
                </div>
              ))}
            </div>
          </StaggerItem>

          <StaggerItem>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
              {quickLinks.map((link, i) => (
                <Link key={i} href={link.href} className="group">
                  <div className="bg-card p-5 sm:p-6 lg:p-8 rounded-2xl border border-border shadow-sm hover:shadow-xl hover:border-primary/20 transition-all h-full flex flex-col">
                    <div className="flex justify-between items-start mb-8">
                      <div className={`h-14 w-14 rounded-xl ${link.color} text-white flex items-center justify-center shadow-lg transition-transform group-hover:scale-110`}>
                        <link.icon className="h-7 w-7" />
                      </div>
                      <div className="h-10 w-10 bg-muted/50 rounded-lg flex items-center justify-center text-muted-foreground group-hover:text-primary transition-colors">
                        <ArrowUpRight className="h-5 w-5" />
                      </div>
                    </div>
                    <div className="flex-1">
                      <h3 className="text-xl font-bold text-foreground mb-2 group-hover:text-primary transition-colors">{link.name}</h3>
                      <p className="text-sm text-muted-foreground leading-relaxed">{link.description}</p>
                    </div>
                    <div className="mt-8 pt-6 border-t border-border">
                      <span className="text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase group-hover:text-primary transition-colors">Open Management &rarr;</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </StaggerItem>
        </StaggerContainer>
      </div>
    </PageTransition>
  );
}
