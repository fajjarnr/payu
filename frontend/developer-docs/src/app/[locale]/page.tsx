import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { Code2, BookOpen, Zap } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function HomePage({ params: { locale } }: { params: { locale: string } }) {
  const t = useTranslations('hero');
  const tNav = useTranslations('nav');
  const tFeatures = useTranslations('features');

  const features = [
    {
      icon: Zap,
      title: tFeatures('easyIntegration.title'),
      description: tFeatures('easyIntegration.description'),
    },
    {
      icon: BookOpen,
      title: tFeatures('comprehensive.title'),
      description: tFeatures('comprehensive.description'),
    },
    {
      icon: Code2,
      title: tFeatures('sandbox.title'),
      description: tFeatures('sandbox.description'),
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <nav className="border-b border-border bg-card">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <Link href="/" className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-full bg-bank-green" />
              <span className="font-black text-xl tracking-tighter">PayU</span>
            </Link>
            <div className="flex space-x-8">
              <Link href="/getting-started" className="text-muted-foreground hover:text-foreground transition-colors">
                {tNav('gettingStarted')}
              </Link>
              <Link href="/guides/partner-payments" className="text-muted-foreground hover:text-foreground transition-colors">
                {tNav('guides')}
              </Link>
              <Link href="/sdk/java" className="text-muted-foreground hover:text-foreground transition-colors">
                {tNav('sdk')}
              </Link>
              <a 
                href="https://api-portal.payu.id" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                {tNav('api')}
              </a>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
        <div className="text-center mb-16">
          <h1 className="font-black text-5xl md:text-6xl tracking-tighter mb-6">
            {t('title')}
          </h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto mb-8">
            {t('subtitle')}
          </p>
          <div className="flex justify-center gap-4">
            <Link 
              href="/getting-started" 
              className="px-6 py-3 rounded-[2.5rem] bg-bank-green text-white font-medium shadow-2xl shadow-bank-green/20 hover:opacity-90 transition-opacity"
            >
              {t('getStarted')}
            </Link>
            <Link 
              href="/guides/partner-payments" 
              className="px-6 py-3 rounded-[2.5rem] border border-border bg-card text-foreground font-medium hover:bg-accent transition-colors"
            >
              {t('viewDocs')}
            </Link>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            return (
              <div 
                key={index}
                className="p-6 rounded-3xl border border-border bg-card hover:shadow-xl transition-shadow"
              >
                <div className="w-12 h-12 rounded-2xl bg-accent flex items-center justify-center mb-4">
                  <Icon className="w-6 h-6 text-bank-green" />
                </div>
                <h3 className="font-black text-lg mb-2 tracking-tight">
                  {feature.title}
                </h3>
                <p className="text-muted-foreground">
                  {feature.description}
                </p>
              </div>
            );
          })}
        </div>
      </main>
    </div>
  );
}
