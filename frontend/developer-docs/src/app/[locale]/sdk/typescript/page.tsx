import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { FileCode, Package, Zap } from 'lucide-react';

export default function TypeScriptSDKPage() {
  const t = useTranslations('sidebar');

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <Link href="/" className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-full bg-bank-green" />
              <span className="font-black text-xl tracking-tighter">PayU</span>
            </Link>
            <div className="flex space-x-8">
              <Link href="/getting-started" className="text-muted-foreground hover:text-foreground transition-colors">
                {t('gettingStarted')}
              </Link>
              <Link href="/guides/partner-payments" className="text-muted-foreground hover:text-foreground transition-colors">
                {t('guides')}
              </Link>
              <Link href="/sdk/java" className="text-muted-foreground hover:text-foreground transition-colors">
                {t('sdkExamples')}
              </Link>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="flex gap-12">
          <aside className="w-64 shrink-0">
            <nav className="sticky top-24 space-y-1">
              <Link href="/getting-started" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                {t('quickStart')}
              </Link>
              <div className="pt-4">
                <p className="px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  {t('sdkExamples')}
                </p>
                <Link href="/sdk/java" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('java')}
                </Link>
                <Link href="/sdk/python" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
                  {t('python')}
                </Link>
                <Link href="/sdk/typescript" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
                  {t('typescript')}
                </Link>
              </div>
            </nav>
          </aside>

          <main className="flex-1 min-w-0">
            <div className="mb-12">
              <div className="flex items-center gap-2 text-bank-green text-sm font-medium mb-4">
                <span>SDK</span>
                <span>/</span>
                <span>TypeScript</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                PayU TypeScript SDK
              </h1>
              <p className="text-xl text-muted-foreground">
                SDK resmi PayU untuk TypeScript/JavaScript dan React/Next.js applications.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Installation
              </h2>
              <div className="grid md:grid-cols-2 gap-6 mb-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <h3 className="font-bold text-sm mb-4 text-muted-foreground">npm</h3>
                  <div className="code-block">
                    <pre>
                      <code>{`npm install @payu/sdk`}</code>
                    </pre>
                  </div>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <h3 className="font-bold text-sm mb-4 text-muted-foreground">yarn</h3>
                  <div className="code-block">
                    <pre>
                      <code>{`yarn add @payu/sdk`}</code>
                    </pre>
                  </div>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Quick Start
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`import { PayUClient, PaymentMethod, Environment } from '@payu/sdk';

// Initialize client
const client = new PayUClient({
  clientId: 'your_client_id',
  clientSecret: 'your_client_secret',
  environment: Environment.SANDBOX
});

// Create payment
const response = await client.payments.create({
  amount: 150000,
  currency: 'IDR',
  merchantReference: 'ORD-12345',
  customerId: 'CUST-001',
  description: 'Pembayaran TokoBapak',
  paymentMethod: PaymentMethod.QRIS,
  callbackUrl: 'https://your-app.com/webhook'
});

console.log('Payment ID:', response.paymentId);
console.log('Status:', response.status);`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Authentication
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`import { PayUClient } from '@payu/sdk';

const client = new PayUClient({
  clientId: 'your_client_id',
  clientSecret: 'your_client_secret',
  environment: Environment.SANDBOX
});

// Get access token
const token = await client.auth.getToken();
console.log('Access Token:', token.accessToken);

// Token is automatically managed by the client
// No need to manually refresh tokens`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Next.js Integration
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`// app/api/webhook/payment/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { PayUClient } from '@payu/sdk';

const client = new PayUClient({
  clientId: process.env.PAYU_CLIENT_ID!,
  clientSecret: process.env.PAYU_CLIENT_SECRET!,
  environment: process.env.PAYU_ENVIRONMENT === 'production' 
    ? Environment.PRODUCTION 
    : Environment.SANDBOX
});

export async function POST(request: NextRequest) {
  const payload = await request.text();
  const signature = request.headers.get('X-PayU-Signature');
  
  try {
    const event = client.webhooks.verifyAndParse(
      payload,
      signature!
    );
    
    if (event.eventType === 'payment.completed') {
      // Handle completed payment
      const paymentId = event.paymentId;
      // Update your database...
    }
    
    return NextResponse.json({ status: 'ok' });
  } catch (error) {
    return NextResponse.json(
      { error: 'Invalid signature' },
      { status: 401 }
    );
  }
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                React Hook
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`import { usePayU } from '@payu/sdk/react';

function PaymentButton() {
  const { createPayment, isLoading } = usePayU();
  
  const handlePayment = async () => {
    try {
      const response = await createPayment({
        amount: 150000,
        currency: 'IDR',
        merchantReference: 'ORD-12345',
        paymentMethod: PaymentMethod.QRIS,
        callbackUrl: 'https://your-app.com/webhook'
      });
      
      // Display QR code...
    } catch (error) {
      // Handle error...
    }
  };
  
  return (
    <button 
      onClick={handlePayment}
      disabled={isLoading}
      className="px-4 py-2 rounded-xl bg-bank-green text-white"
    >
      {isLoading ? 'Processing...' : 'Pay Now'}
    </button>
  );
}`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Features
              </h2>
              <div className="grid md:grid-cols-2 gap-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Package className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Type-Safe</h3>
                  <p className="text-sm text-muted-foreground">
                    Full TypeScript support dengan type definitions
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">React Hooks</h3>
                  <p className="text-sm text-muted-foreground">
                    Custom hooks untuk React applications
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <FileCode className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Tree Shakeable</h3>
                  <p className="text-sm text-muted-foreground">
                    Optimized bundle size dengan tree shaking
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Framework Agnostic</h3>
                  <p className="text-sm text-muted-foreground">
                    Bisa digunakan dengan React, Next.js, Vue, Node.js
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Configuration
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`// .env.local
PAYU_CLIENT_ID=your_client_id
PAYU_CLIENT_SECRET=your_client_secret
PAYU_ENVIRONMENT=sandbox
PAYU_WEBHOOK_SECRET=your_webhook_secret

// lib/payu.ts
import { PayUClient, Environment } from '@payu/sdk';

export const payuClient = new PayUClient({
  clientId: process.env.PAYU_CLIENT_ID!,
  clientSecret: process.env.PAYU_CLIENT_SECRET!,
  environment: process.env.PAYU_ENVIRONMENT === 'production' 
    ? Environment.PRODUCTION 
    : Environment.SANDBOX
});`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Documentation Links
              </h2>
              <div className="space-y-3">
                <a 
                  href="https://payu-sdk.netlify.app/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-3 p-4 rounded-2xl border border-border bg-card hover:shadow-lg transition-shadow"
                >
                  <FileCode className="w-5 h-5 text-bank-green" />
                  <span>API Documentation</span>
                </a>
                <a 
                  href="https://github.com/payu/payu-typescript-sdk"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-3 p-4 rounded-2xl border border-border bg-card hover:shadow-lg transition-shadow"
                >
                  <Package className="w-5 h-5 text-bank-green" />
                  <span>GitHub Repository</span>
                </a>
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
