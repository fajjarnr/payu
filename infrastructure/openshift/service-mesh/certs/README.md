# Certificate Management for Service Mesh

This directory contains TLS certificates for the PayU Digital Banking Platform Service Mesh.

## Required Certificates

### 1. Ingress Gateway Certificate

**File**: `tls.crt` and `tls.key`

**Purpose**: TLS termination at the ingress gateway

**CN (Common Name)**: `api.payu.id`

**SANs (Subject Alternative Names)**:
- `api.payu.local` (local development)
- `api.payu.dev` (development)
- `api.payu.sit` (SIT)
- `api.payu.uat` (UAT)
- `api.payu.preprod` (pre-production)
- `api.payu.id` (production)

**Example with OpenSSL**:

```bash
# Generate private key
openssl genrsa -out tls.key 2048

# Generate CSR
openssl req -new -key tls.key -out ingress.csr -subj "/C=ID/ST=Jakarta/L=Jakarta/O=PayU/CN=api.payu.id"

# Generate self-signed certificate (for development)
openssl x509 -req -days 365 -in ingress.csr -signkey tls.key -out tls.crt \
  -extfile <(cat <<EOF
subjectAltName=DNS:api.payu.local,DNS:api.payu.dev,DNS:api.payu.sit,DNS:api.payu.uat,DNS:api.payu.preprod,DNS:api.payu.id
EOF
)

# For production, use a CA like Let's Encrypt or corporate PKI
```

### 2. CA Certificate for mTLS

**File**: `ca.crt`

**Purpose**: Certificate authority for verifying mTLS connections

**Note**: This is automatically managed by Istio Citadel

**To extract the CA certificate**:

```bash
# Get the CA certificate from Istio
oc get secret -n istio-system istio-ca-secret -o jsonpath='{.data.ca-cert\.crt}' | base64 -d > ca.crt
```

## Certificate Rotation

### Automatic Rotation

Istio Citadel automatically rotates certificates every 90 days. No manual intervention required.

### Manual Rotation

If you need to rotate certificates manually:

```bash
# 1. Backup current certificates
oc get secret payu-ingress-cert -n istio-system -o yaml > ingress-cert-backup.yaml

# 2. Update the secret with new certificates
oc create secret tls payu-ingress-cert \
  --cert=tls.crt \
  --key=tls.key \
  --namespace=istio-system \
  --dry-run=client -o yaml | oc apply -f -

# 3. Restart ingress gateway to load new certificates
oc rollout restart deployment/istio-ingressgateway -n istio-system

# 4. Verify new certificate
openssl s_client -connect api.payu.id:443 -servername api.payu.id
```

## Using Let's Encrypt for Production

For production, use Let's Encrypt for free, trusted certificates:

```bash
# Install cert-manager
oc apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer for Let's Encrypt
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@payu.id
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: openshift-default
EOF

# Create Certificate resource
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: payu-ingress-cert
  namespace: istio-system
spec:
  secretName: payu-ingress-cert
  dnsNames:
    - api.payu.id
    - api.payu.preprod
    - api.payu.uat
    - api.payu.sit
    - api.payu.dev
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
EOF
```

## Certificate Validation

### Validate Certificate Chain

```bash
# Check certificate expiration
openssl x509 -in tls.crt -noout -dates

# Check certificate details
openssl x509 -in tls.crt -noout -text

# Verify certificate against CA
openssl verify -CAfile ca.crt tls.crt
```

### Validate mTLS

```bash
# Test mTLS connection
oc exec -it account-service-xxx -n payu-prod -c istio-proxy -- \
  openssl s_client -connect auth-service:8082 -alpn istio

# Should show:
# - TLS handshake successful
# - Certificate presented
# - Verify return code: 0 (ok)
```

## Security Notes

1. **Never commit private keys** to version control
2. **Use strong passphrases** for private keys
3. **Rotate certificates regularly** (every 90 days)
4. **Monitor certificate expiration** and set up alerts
5. **Use separate certificates** for each environment
6. **Store secrets securely** using sealed secrets or external secret managers

## Troubleshooting

### Issue: Certificate Expired

```bash
# Check expiration date
openssl x509 -in tls.crt -noout -enddate

# Rotate certificate (see Manual Rotation above)
```

### Issue: mTLS Connection Failures

```bash
# Check if CA certificate is correct
oc get configmap -n istio-system istio-ca-root-cert -o yaml

# Restart pods to reload certificates
oc rollout restart deployment/account-service -n payu-prod
```

### Issue: Inbound Connections Fail

```bash
# Check ingress gateway logs
oc logs -f istio-ingressgateway-xxx -n istio-system

# Check if TLS secret exists
oc get secret payu-ingress-cert -n istio-system
```

## References

- [Istio Certificate Management](https://istio.io/latest/docs/tasks/security/cert-management/)
- [OpenShift Certificate Management](https://docs.openshift.com/container-platform/4.20/security/certificates/service-serving-cert.html)
- [cert-manager Documentation](https://cert-manager.io/docs/)
