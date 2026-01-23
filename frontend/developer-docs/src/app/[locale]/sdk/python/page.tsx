import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { FileCode, Package, Zap } from 'lucide-react';

export default function PythonSDKPage() {
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
                <Link href="/sdk/python" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
                  {t('python')}
                </Link>
                <Link href="/sdk/typescript" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
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
                <span>Python</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                PayU Python SDK
              </h1>
              <p className="text-xl text-muted-foreground">
                SDK resmi PayU untuk Python applications dan FastAPI/Django.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Installation
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`pip install payu-sdk`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Quick Start
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`from payu_sdk import PayUClient, PaymentMethod, Environment

# Initialize client
client = PayUClient(
    client_id="your_client_id",
    client_secret="your_client_secret",
    environment=Environment.SANDBOX
)

# Create payment
response = client.payments.create({
    "amount": 150000,
    "currency": "IDR",
    "merchant_reference": "ORD-12345",
    "customer_id": "CUST-001",
    "description": "Pembayaran TokoBapak",
    "payment_method": PaymentMethod.QRIS,
    "callback_url": "https://your-app.com/webhook"
})

print(f"Payment ID: {response.payment_id}")
print(f"Status: {response.status}")`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Authentication
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`from payu_sdk import PayUClient

# Get access token
token = client.auth.get_token()
print(f"Access Token: {token.access_token}")

# Token is automatically managed by the client
# No need to manually refresh tokens`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                FastAPI Integration
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`from fastapi import FastAPI, Request, HTTPException
from payu_sdk import PayUClient, WebhookEvent

app = FastAPI()
payu_client = PayUClient(
    client_id="your_client_id",
    client_secret="your_client_secret",
    environment=Environment.SANDBOX
)

@app.post("/webhook/payment")
async def handle_payment_webhook(request: Request):
    payload = await request.body()
    signature = request.headers.get("X-PayU-Signature")
    
    try:
        event = payu_client.webhooks.verify_and_parse(
            payload.decode(), 
            signature
        )
        
        if event.event_type == "payment.completed":
            # Handle completed payment
            payment_id = event.payment_id
            # Update your database...
            
        return {"status": "ok"}
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid signature")`}</code>
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
                  <h3 className="font-bold mb-2">Async Support</h3>
                  <p className="text-sm text-muted-foreground">
                    Full async/await support dengan asyncio
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Type Hints</h3>
                  <p className="text-sm text-muted-foreground">
                    Full type hints untuk IDE support yang lebih baik
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <FileCode className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Pydantic Models</h3>
                  <p className="text-sm text-muted-foreground">
                    Data validation dengan Pydantic
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Framework Agnostic</h3>
                  <p className="text-sm text-muted-foreground">
                    Bisa digunakan dengan FastAPI, Django, Flask, dll
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
                  <code>{`# .env
PAYU_CLIENT_ID=your_client_id
PAYU_CLIENT_SECRET=your_client_secret
PAYU_ENVIRONMENT=sandbox
PAYU_WEBHOOK_SECRET=your_webhook_secret

# Python code
import os
from payu_sdk import PayUClient, Environment

client = PayUClient(
    client_id=os.getenv("PAYU_CLIENT_ID"),
    client_secret=os.getenv("PAYU_CLIENT_SECRET"),
    environment=Environment(os.getenv("PAYU_ENVIRONMENT"))
)`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Documentation Links
              </h2>
              <div className="space-y-3">
                <a 
                  href="https://payu-python-sdk.readthedocs.io/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-3 p-4 rounded-2xl border border-border bg-card hover:shadow-lg transition-shadow"
                >
                  <FileCode className="w-5 h-5 text-bank-green" />
                  <span>Read the Docs</span>
                </a>
                <a 
                  href="https://github.com/payu/payu-python-sdk"
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
