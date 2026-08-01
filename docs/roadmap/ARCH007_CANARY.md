# ARCH-007 — 24h `payu-dev` Canary Evidence Pack

**Canary window**: 2026-08-01 00:32Z → 2026-08-02 00:32Z (final operator-managed mTLS stack;
window di-resume setelah kredensial cluster dipulihkan. Fase pertama 20:47Z→23:21Z
(2j34m, errs 0, 23/23) tercatat di bawah sebagai pra-gap.)
**Stack**: Data Grid Infinispan CR (`WellFormed=True`, mTLS), cache `payu` text/plain,
Hot Rod 16.2.1 native clients, `payu-cache:11222`, dev overlay tanpa `SPRING_MAIN_SOURCES`.

## Evidence gates (AGENTS / TODOS ARCH-007 done criteria)

| Gate | Criterion | Evidence source |
|---|---|---|
| No cache errors | 0 `RedisConnectionException` / `ISPN005061` / `unclosed iterator` / `HotRodClientException` / `CacheNotFound` / `unstarted` / `Invalid cred` / `SSLHandshake` | `/tmp/arch007-canary.log` (checkpoint tiap 15m, setsid) |
| Workload readiness | 23/23 backend Ready | field `backend_ready` pada file yang sama |
| p95 latency | Hot Rod round-trip steady 1–2ms, tanpa regresi >10% | `/tmp/arch007-canary-latency.log` (sampler tiap 5m, `deepHealth.datagrid.latency`) |
| Scheduler lock overlap | 0 lock overlap / ShedLock error | scan log tiap checkpoint |
| Duplicate replay | 0 idempotency replay error di path payment/transfer | scan log tiap checkpoint |

## Checkpoint log (live)

```text
2026-07-31 20:50:44Z errs=0 backend_ready=23/23
2026-07-31 21:05:47Z errs=0 backend_ready=23/23
2026-07-31 21:20:50Z errs=0 backend_ready=23/23
2026-07-31 21:35:52Z errs=0 backend_ready=23/23
2026-07-31 21:50:55Z errs=0 backend_ready=23/23
2026-07-31 22:05:58Z errs=0 backend_ready=23/23
2026-07-31 22:21:01Z errs=0 backend_ready=3/23   (mid-rollout deploy; errs 0)
2026-07-31 22:36:01Z errs=0 backend_ready=23/23
2026-07-31 22:51:04Z errs=0 backend_ready=23/23
2026-07-31 23:06:07Z errs=0 backend_ready=22/23   (mid-rollout notification; errs 0)
2026-07-31 23:21:10Z errs=0 backend_ready=23/23
```

> **Gap 2026-07-31 23:35Z**: kredensial cluster (`jay`, token 24h) expire; monitor
> dihentikan. Checkpoint valid terakhir 23:21Z (≈2j34m dari 24j). Lanjut canary
> butuh kubeconfig/login baru; checkpoint berikutnya di-resume dari titik ini.

> **Resume 2026-08-01 00:32Z**: akses cluster pulih (kubeconfig baru); monitor 24h
> di-restart (15m errs + 5m latency). Verifikasi awal: 23/23 deploy Ready, Data Grid
> `WellFormed=True`, account health UP, datagrid latency 2ms.

## Latency sampler (live)

```text
steady-state samples: 1–2ms (cold-start saat rollout: ~3.3–3.7s, transien)
2026-07-31 23:04:45Z datagrid_latency=2ms
```

## E2E regression saat canary

16 suite ALL PASS (auth-login 6/6, wallet-balance 8/8, transaction-history, cards-crud 14/14,
api-portal 4/4, partner-integration 5/5, billing-billers 6/6, lending-investment 8/8,
transaction-disbursements 9/9, account-service 5/5, promotion-catalog 7/7, fx-rates 13/13,
cms-statement 7/7, verify-nik-cache 200/200, notification-health 3/3, health-check-all).

## Promosi checklist (setelah gate lolos)

- [ ] 24 jam tanpa error cache + p95 steady (file checkpoint di atas)
- [ ] Promosi SIT (CR sudah `WellFormed=True`; secret Vault ada) → UAT → preprod → prod
- [ ] Image tag = git tag; CHANGELOG entry
- [ ] Tutup ARCH-007; hapus gate dari TODOS
