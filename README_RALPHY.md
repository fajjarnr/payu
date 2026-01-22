# Ralphy Usage Guide - PayU Platform

Dokumentasi ini menjelaskan cara menggunakan **Ralphy** (Autonomous AI Coding Agent) di proyek PayU.

## Overview

Ralphy terintegrasi dengan engine **OpenCode** dan model **GLM-4** untuk membantu pengembangan fitur secara otonom berdasarkan aturan arsitektur yang telah ditentukan di `.ralphy/config.yaml`.

## Persyaratan
- Node.js (digunakan via nvm: `/home/jay/.nvm/versions/node/v24.13.0/bin/ralphy`)
- OpenCode agent terinstal di sistem.

## Cara Menjalankan

### 1. Menjalankan Tugas Spesifik
Gunakan perintah berikut untuk memberikan instruksi langsung:
```bash
# Contoh: Menambah unit test
ralphy --opencode --model zai-coding-plan/glm-4.7 "Tambahkan unit test untuk balance reservation di wallet-service"
```

### 2. Menjalankan Secara Otonom (PRD Mode)
Jika dijalankan tanpa argumen tugas, Ralphy akan otomatis mengecek checklist di `PRD.md` dan mengerjakannya satu per satu:
```bash
ralphy --opencode --model zai-coding-plan/glm-4.7
```

### 3. Menggunakan tmux (Rekomendasi)
Agar Ralphy tetap berjalan di background meskipun Anda menutup terminal:
```bash
# Membuat session baru
tmux new -s payu "ralphy --opencode --model zai-coding-plan/glm-4.7"

# Detach (Keluar dari tampilan tapi tetap jalan)
Tekan Ctrl + B lalu D

# Attach (Masuk kembali ke log Ralphy)
tmux attach -t payu
```

## Utilitas Utama: Ralph Script
Ralphy dikonfigurasi untuk menggunakan script `./scripts/ralph.sh` sebagai jembatan untuk build dan testing.

| Command | Deskripsi |
|---------|-----------|
| `./scripts/ralph.sh status` | Cek status seluruh microservices |
| `./scripts/ralph.sh build` | Build semua service Java (Spring Boot & Quarkus) |
| `./scripts/ralph.sh test` | Jalankan semua test unit & integrasi |
| `./scripts/ralph.sh doctor` | Cek kelengkapan tools pengembangan |

## Tips Performa

Untuk mempercepat proses development dan E2E testing (pemanasan cache Maven), gunakan perintah paralel berikut di level host:
```bash
mvn clean package -DskipTests -T 1C
```
Rule ini sudah dimasukkan ke dalam konfigurasi Ralphy agar dia melakukan pre-build secara otomatis sebelum menjalankan tes besar.

## Rules & Konfigurasi
Semua aturan pengembangan (seperti Hexagonal Architecture, Java 21, dan Spring Boot 3.4) didefinisikan di `.ralphy/config.yaml`. Jangan mengubah file ini kecuali ingin mengubah standar penulisan kode proyek.
