---
description: Workflow untuk koordinasi multi-agent dan dispatch tugas paralel pada platform PayU.
---

// turbo-all

# Multi-Agent Coordination Workflow (AI-Native)

Workflow ini dirancang untuk memaksimalkan penggunaan **Claude Code / Antigravity Assistant** dalam menangani tugas kompleks secara paralel, cepat, dan terkoordinasi.

## 🚀 Orchestration Patterns

Gunakan pola ini untuk mempercepat siklus SDLC (Discovery -> Analysis -> Implementation -> Verification).

### 1. Sequential Chain (Berantai)

Gunakan jika output satu agen menjadi input bagi agen berikutnya.

> "Gunakan `@principal-architect` untuk memetakan struktur kode, lalu berikan hasilnya ke `@core-banking-engineer` untuk mereview API."

### 2. Swarm Mode (Parallel Dispatch - Adaptive)
Gunakan untuk tugas independen yang masif (Scatter-Gather). Platform PayU mendukung eksekusi agen paralel secara dinamis sesuai kompleksitas tugas.
> "Dispatch `@auditor` (Audit), `@migrator` (Schema), `@tester` (Test Case), `@styler` (UI), dan `@logic-builder` (Domain) secara bersamaan di berbagai titik berbeda."
- **Topology**: Mesh (Peer-to-Peer).
- **Syarat**: Tugas tidak boleh saling bergantung secara langsung pada *write-access* terhadap file yang sama.
- **Speed**: Dapat mereduksi waktu eksekusi tugas masif secara signifikan.

### 3. Hierarchical Mode (Queen-Worker)

Gunakan untuk tugas besar yang butuh _central planner_.

> "Gunakan `@principal-architect` sebagai 'Queen' untuk memecah strategi migrasi, lalu delegasikan ke `@core-banking-engineer` dan `@platform-engineer` sebagai 'Workers'."

- **Topology**: Hierarchical.
- **Flow**: Architect Planning -> Worker Execution -> Architect Review.

### 4. Pattern: Comprehensive Analysis

Alur: `principal-architect` → `domain-agents` → `synthesis`

- Memetakan codebase secara utuh sebelum melakukan perubahan besar.
- Sangat efektif untuk _onboarding_ pada service baru atau refaktor arsitektur.

## 🧠 Shared Context Protocol (Swarm Memory)

Agar "Swarm" efektif, semua agen harus berbagi _memory state_:

1.  **Context Store**: Gunakan file `docs/context/active_task.md` sebagai _Shared Memory_.
2.  **Handoff Rules**:
    - Agent A (Output): Tulis hasil analisa ke `active_task.md`.
    - Agent B (Input): Baca `active_task.md` sebelum eksekusi.
3.  **Conflict Resolution**: Jika dua agen memodifikasi file yang sama, gunakan _Merge Strategy_ (manual review oleh User/Architect).

---

## 📋 Common Orchestration Scenarios

| Skenario           | Urutan Agen (Orchestration)                              |
| :----------------- | :------------------------------------------------------- |
| **Feature Review** | `affected-domain-agents` → `@quality-engineer`           |
| **Security Audit** | `@cybersecurity-architect` → `@debugging-methodology` → `synthesis` |
| **Refactor DB**    | `@data-architect` → `@core-banking-engineer` → `@quality-engineer` |
| **Bug Fixing**     | `@debugging-methodology` → `domain-agent` → `@quality-engineer`    |

---

## 📝 Synthesis Protocol (Reporting)

// turbo
Setelah semua agen selesai bekerja, lakukan sintesis laporan dengan format:

```markdown
## 🤖 Orchestration Synthesis

### Summary

[Apa yang telah dicapai secara keseluruhan]

### Agent Contributions

| Agent               | Findings / Actions                                |
| :------------------ | :------------------------------------------------ |
| @quality-engineer        | Menemukan null pointer di AccountService line 45  |
| @core-banking-engineer   | Mengimplementasikan null-check dan fallback logic |
| @quality-engineer        | Menambahkan unit test untuk skenario null         |

### Consitencies & Action Items

- [ ] Verifikasi integrasi di Staging
- [ ] Update dokumentasi di docs/adr/
```

---

## 💡 Best Practices untuk Kecepatan (Speed)

1. **Discovery First**: Selalu mulai dengan `@principal-architect` atau tool `grep_search` untuk memastikan konteks benar sebelum agen domain mulai bekerja.
2. **Context Passing**: Pastikan temuan dari satu langkah dikirimkan secara eksplisit ke langkah berikutnya.
3. **Synthesis Single-Report**: Mintalah satu laporan terpadu (Synthesis) daripada laporan terpisah-pisah untuk efisiensi review.
4. **Resume Capability**: Jika agen terhenti, gunakan instruksi "Resume agent [id]" untuk melanjutkan pekerjaan tanpa kehilangan konteks.

---

_Last Updated: January 2026_
