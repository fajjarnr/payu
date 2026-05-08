# ZeroSSL — Alternative ACME CA

ZeroSSL is an alternative to Let's Encrypt. Apply manually when needed.

## Prerequisites

1. Register at https://app.zerossl.com/
2. Get your EAC credentials from **Developer → ACME EAC**
3. Fill `zerossl-creds.yaml` with your HMAC key

## Apply

```bash
# Fill in zerossl_hmac_key in zerossl-creds.yaml first
oc apply -f infrastructure/platform/security/cert-manager/zerossl/
```

## Verify

```bash
oc get clusterissuer zerossl-production-issuer
oc get certificate -A | grep zerossl
```
