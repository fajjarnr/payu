# Contributing to PayU

Selamat berkontribusi di PayU Digital Banking Platform! Untuk menjaga kualitas dan konsistensi codebase kami, mohon ikuti panduan berikut.

## 🛠️ Development Guidelines (SOP)

1. **Shared Starters**: Selalu gunakan `security-starter`, `resilience-starter`, dan `cache-starter` untuk fitur-fitur cross-cutting. Jangan implementasi manual di level service.
2. **DTO First**: Definisikan DTO/Request/Response di package `interfaces.dto` sebelum implementasi logic.
3. **Port-Adapter Interface**: Gunakan Hexagonal Architecture untuk core services. Semua external communication harus lewat Port interface di domain layer.
4. **Error Handling**: Gunakan `GlobalExceptionHandler` dan custom `BusinessException` dengan error codes yang unik (e.g., `ACC_001`).
5. **Idempotency**: Semua endpoint payment/transfer WAJIB support `X-Idempotency-Key` header.
6. **Annotation Processor**: Prioritaskan Lombok. Jika gagal kompilasi setelah 2 upaya, beralih ke implementasi manual.
7. **Doc Sync**: Setiap update signifikan WAJIB memperbarui `CHANGELOG.md`.

## 🌿 Git Workflow

- **Branch Naming**: 
  - `feature/PAYU-[Jiras-ID]-description`
  - `fix/PAYU-[Jira-ID]-description`
  - `refactor/description`
- **Pull Requests**:
  - Berikan ringkasan perubahan yang jelas.
  - Lampirkan bukti testing (screenshot atau output log).
  - Minimal 1 approval dari tim terkait.
- **Commit Messages**: Gunakan [Conventional Commits](https://www.conventionalcommits.org/).

## 🧪 Testing Standards

- **Unit Tests**: Minimum 80% coverage untuk logic baru.
- **Integration Tests**: Wajib untuk aliran data antar-service.
- **E2E Tests**: Pastikan tidak ada regresi pada aliran transaksi utama.

---
_Last Updated: March 22, 2026_
