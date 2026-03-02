# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Status          | Count | Breakdown                                              |
| :-------------- | :---: | :----------------------------------------------------- |
| **Active Epics** |   0   | All completed ✅                                       |
| **Open Stories** |   0   | All completed ✅                                       |
| **Tech Debt**   |   0   | All completed ✅                                       |
| **Spikes**      |   4   | ARCH-001 – ARCH-004                                    |
| **Deferred**    |   5   | P2-FE-003, OCP-007, OCP-010, DR-001, Card Token/3DS   |
| **Bugs**        | 0/232 | 229 fixed, 4 Won't Do (BUG-BE-061, 076, 080, 091)     |

> **Completed Epics**: 24/24 fully done. All stories & tech debt cleared.
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.

### 🐛 Bug Scorecard

| Kategori                  | Open  | Won't Do | Done |  Total   |
| :------------------------ | :---: | :------: | :--: | :------: |
| Backend Logic             |   0   |    3     | 144  | **147**  |
| Frontend Logic            |   0   |    0     |  46  |  **46**  |
| Frontend-Backend Mismatch |   0   |    0     |  29  |  **29**  |
| Auth / Session            |   0   |    0     |  10  |  **10**  |
| **TOTAL**                 | **0** |  **4**   | 229  | **~232** |

### Won't Do (4 items)

| Key        | Summary                                   | Resolution                                                |
| :--------- | :---------------------------------------- | :-------------------------------------------------------- |
| BUG-BE-061 | Promotion `getTransactionAmount()` → ZERO | Won't Do — gamification removed (SIMP-002)                |
| BUG-BE-076 | API Portal sandbox in-memory              | Won't Do — partner belum ada, sandbox belum relevan       |
| BUG-BE-080 | Lending pre-approval endpoints missing    | Won't Do — feature belum aktif di frontend                |
| BUG-BE-091 | Fixed-window rate limit burstable         | Won't Do — low-traffic fase awal. Superseded oleh IMP-005 |

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                  | Impact                            | Status   |
| :------- | :---- | :-------------------------------------------------------- | :-------------------------------- | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                    | Scope `kyc-service`               | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?     | Output format `statement-service` | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client?        | Multi-tenancy `support-service`   | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client? | Multi-tenant mode `cms-service`   | 📋 To Do |

---

## 🔮 Deferred (Icebox)

| Key       | Type  | Summary                               | Notes                                            |
| :-------- | :---- | :------------------------------------ | :----------------------------------------------- |
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN)   | ❄️ Deferred                                      |
| OCP-007   | Story | Service Mesh mTLS enforcement         | ❄️ Planned                                       |
| OCP-010   | Story | API versioning headers                | ❄️ Planned                                       |
| DR-001    | Story | Disaster Recovery live test execution | ❄️ Scripts ready                                 |
| DEFER-001 | Story | Card Tokenization & 3DS               | ❄️ Requires PCI-DSS scope + card network kontrak |

---

## 📊 Metrics

### Completed Summary

| Metric            | Value                                        |
| :---------------- | :------------------------------------------- |
| Completed Epics   | 24 fully done (see PROGRESS.md)              |
| Completed Stories | 86/86 (IMP + GAP)                            |
| Completed SP      | 265/265                                      |
| Completion Rate   | 100% stories, 100% SP                        |
| Bugs Fixed        | 229/232 (~99%)                               |
| Tech Debt         | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)|

---

_Last Updated: March 2, 2026 | 0 Active Epics · 0 Open Stories · 0 Open SP · 0 Tech Debt · 4 Spikes · 5 Deferred_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
