# PayU Docker to Podman Migration Status

## Summary

Migrasi dari Docker ke Podman telah selesai dilakukan untuk platform PayU Digital Banking.

## Status Komponen

| Komponen | Status | Lokasi |
|----------|--------|--------|
| Container Images | ✅ Complete | `backend/**/Containerfile`, `frontend/**/Containerfile` |
| Build Scripts | ✅ Complete | `scripts/build-all-podman.sh`, `scripts/build-service-podman.sh` |
| Compose Files | ✅ Complete | `podman-compose.yml`, `podman-compose.test.yml` |
| Quadlet Files | ✅ Complete | `infrastructure/quadlet/*.container` |
| Dokumentasi | ✅ Complete | `docs/operations/PODMAN_MIGRATION_GUIDE.md` |
| Aliases | ✅ Complete | `scripts/podman-aliases.sh` |
| Konfigurasi | ✅ Complete | `.containers.conf.toml.example` |

## Struktur File Baru

```
payu/
├── scripts/
│   ├── build-all-podman.sh          # Build semua service dengan Podman
│   ├── build-service-podman.sh      # Build single service
│   ├── scan-images-podman.sh        # Security scanning dengan Trivy
│   └── podman-aliases.sh            # Shell aliases untuk Podman
├── infrastructure/
│   └── quadlet/
│       ├── README.md                # Dokumentasi quadlet
│       ├── account-service.container
│       ├── auth-service.container
│       ├── gateway-service.container
│       ├── transaction-service.container
│       ├── wallet-service.container
│       ├── postgres.container
│       ├── redis.container
│       └── kafka.container
├── docs/operations/
│   └── PODMAN_MIGRATION_GUIDE.md    # Panduan migrasi lengkap
├── podman-compose.yml               # Compose file untuk Podman
├── podman-compose.test.yml          # Compose file untuk testing
└── .containers.conf.toml.example    # Konfigurasi Podman
```

## Quick Commands

```bash
# Build semua services
./scripts/build-all-podman.sh

# Build single service
./scripts/build-service-podman.sh account-service

# Jalankan dengan podman-compose
podman-compose -f podman-compose.yml up -d

# Gunakan aliases
source scripts/podman-aliases.sh
dcp up -d  # podman-compose up -d
dps        # podman ps
```

## Perubahan Utama

1. **Containerfile vs Dockerfile** - Semua service sekarang menggunakan `Containerfile` (standar Podman), tapi tetap kompatibel dengan `Dockerfile`

2. **Build Scripts** - Script build sekarang mendukung kedua format dan secara otomatis mencari `Containerfile` terlebih dahulu

3. **Quadlet Integration** - File `.container` untuk systemd integration memungkinkan management container sebagai system services

4. **Security** - Konfigurasi dioptimalkan untuk rootless Podman dengan SELinux support

## Next Steps

1. Testing end-to-end dengan Podman
2. Update CI/CD pipelines untuk menggunakan Podman/Buildah
3. Training tim developer untuk menggunakan Podman
4. Decommission Docker setelah validasi lengkap

## Referensi

- [Podman Migration Guide](../operations/PODMAN_MIGRATION_GUIDE.md)
