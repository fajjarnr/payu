import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/navigation';
import { FileCode, Package, Zap } from 'lucide-react';

export default function JavaSDKPage() {
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
              <Link href="/sdk/java" className="text-bank-green font-medium">
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
                <Link href="/sdk/java" className="block px-4 py-2 rounded-xl bg-accent text-bank-green font-medium">
                  {t('java')}
                </Link>
                <Link href="/sdk/python" className="block px-4 py-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-accent/50 transition-colors">
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
                <span>Java</span>
              </div>
              <h1 className="font-black text-4xl md:text-5xl tracking-tighter mb-4">
                PayU Java SDK
              </h1>
              <p className="text-xl text-muted-foreground">
                SDK resmi PayU untuk Java dan Spring Boot applications.
              </p>
            </div>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Installation
              </h2>
              <div className="grid md:grid-cols-2 gap-6 mb-6">
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <h3 className="font-bold text-sm mb-4 text-muted-foreground">Maven</h3>
                  <div className="code-block">
                    <pre>
                      <code>{`<dependency>
  <groupId>id.payu</groupId>
  <artifactId>payu-sdk</artifactId>
  <version>1.0.0</version>
</dependency>`}</code>
                    </pre>
                  </div>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <h3 className="font-bold text-sm mb-4 text-muted-foreground">Gradle</h3>
                  <div className="code-block">
                    <pre>
                      <code>{`implementation 'id.payu:payu-sdk:1.0.0'`}</code>
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
                  <code>{`import id.payu.sdk.PayUClient;
import id.payu.sdk.model.PaymentRequest;
import id.payu.sdk.model.PaymentResponse;

// Initialize client
PayUClient client = PayUClient.builder()
    .clientId("your_client_id")
    .clientSecret("your_client_secret")
    .environment(Environment.SANDBOX)
    .build();

// Create payment
PaymentRequest request = PaymentRequest.builder()
    .amount(150000L)
    .currency("IDR")
    .merchantReference("ORD-12345")
    .customerId("CUST-001")
    .description("Pembayaran TokoBapak")
    .paymentMethod(PaymentMethod.QRIS)
    .callbackUrl("https://your-app.com/webhook")
    .build();

PaymentResponse response = client.payments().create(request);
System.out.println("Payment ID: " + response.getPaymentId());
System.out.println("Status: " + response.getStatus());`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Authentication
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`import id.payu.sdk.auth.OAuth2Token;
import id.payu.sdk.model.TokenRequest;

// Get access token
TokenRequest tokenRequest = TokenRequest.builder()
    .clientId("your_client_id")
    .clientSecret("your_client_secret")
    .grantType("client_credentials")
    .build();

OAuth2Token token = client.auth().getToken(tokenRequest);
System.out.println("Access Token: " + token.getAccessToken());

// Token is automatically refreshed by the client
// No need to manually manage token lifecycle`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Webhook Handler
              </h2>
              <div className="code-block">
                <pre>
                  <code>{`import id.payu.sdk.webhook.WebhookHandler;
import id.payu.sdk.webhook.WebhookEvent;

@Controller
@RequestMapping("/webhook")
public class PayUWebhookController {

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader("X-PayU-Signature") String signature) {
        
        try {
            WebhookEvent event = client.webhooks()
                .verifyAndParse(payload, signature);
            
            if ("payment.completed".equals(event.getEventType())) {
                // Handle completed payment
                String paymentId = event.getPaymentId();
                // Update your database...
            }
            
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(401).build();
        }
    }
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
                  <h3 className="font-bold mb-2">Auto Token Management</h3>
                  <p className="text-sm text-muted-foreground">
                    Otomatis refresh access token tanpa manual handling
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Reactive Support</h3>
                  <p className="text-sm text-muted-foreground">
                    Mendukung Reactor dan RxJava untuk async operations
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <FileCode className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Type-Safe Models</h3>
                  <p className="text-sm text-muted-foreground">
                    Request/response models dengan type safety
                  </p>
                </div>
                <div className="p-6 rounded-3xl border border-border bg-card">
                  <Zap className="w-8 h-8 text-bank-green mb-3" />
                  <h3 className="font-bold mb-2">Spring Boot Integration</h3>
                  <p className="text-sm text-muted-foreground">
                    Auto-configuration untuk Spring Boot 3.x
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
                  <code>{`# application.yml
payu:
  client:
    id: \${PAYU_CLIENT_ID}
    secret: \${PAYU_CLIENT_SECRET}
    environment: sandbox
    webhook:
      secret: \${PAYU_WEBHOOK_SECRET}
    timeout:
      connect: 5000
      read: 30000`}</code>
                </pre>
              </div>
            </section>

            <section className="mb-16">
              <h2 className="font-black text-2xl tracking-tight mb-6">
                Documentation Links
              </h2>
              <div className="space-y-3">
                <a 
                  href="https://javadoc.io/doc/id.payu/payu-sdk/latest"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-3 p-4 rounded-2xl border border-border bg-card hover:shadow-lg transition-shadow"
                >
                  <FileCode className="w-5 h-5 text-bank-green" />
                  <span>JavaDoc Documentation</span>
                </a>
                <a 
                  href="https://github.com/payu/payu-java-sdk"
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
