# PayU Architecture & Domain Glossary

This living glossary documents domain concepts, technical terms, bounded context entities, and architectural protocols used across the PayU platform.

---

## 🏦 Caching & Data Grid (ARCH-007 / INFRA-025)

### Hot Rod Protocol
**Type**: Technical Protocol  
**Domain**: Data Grid / Caching Infrastructure  
**Definition**: A high-performance, binary, topology-aware client/server protocol native to Red Hat Data Grid / Infinispan. Hot Rod enables direct TCP connections to primary node key owners in the cluster without extra proxy hops.

### ProtoStream
**Type**: Serialization Technology  
**Domain**: Data Grid / Serialization  
**Definition**: A compact binary serialization library for Java objects based on Google Protocol Buffers (Protobuf). ProtoStream provides schema-driven serialization, high speed, small payload footprints, and backward/forward compatibility across microservices.

### Near Caching (Invalidation Mode)
**Type**: Architectural Pattern  
**Domain**: Caching Infrastructure  
**Definition**: A two-tier caching technique where a Hot Rod client maintains an in-memory L1 cache inside the client pod (e.g., up to 10,000 entries). When any cluster node mutates or evicts a key, the Data Grid server pushes invalidation events over Hot Rod TCP connections to purge stale entries from client L1 caches instantly.

### Stale-While-Revalidate (SWR)
**Type**: Caching Pattern  
**Domain**: Resilience & Latency  
**Definition**: A caching strategy that serves slightly stale data immediately to the client while triggering an asynchronous background refresh from the database or underlying service. SWR eliminates cache stampedes and latency spikes on hot keys.

### Lifespan vs MaxIdle
**Type**: Data Grid Metadata  
**Domain**: Cache Eviction  
**Definition**:
- **Lifespan (Hard TTL)**: The absolute maximum time an entry is allowed to exist in cache from creation time.
- **MaxIdle**: The maximum allowed idle time between consecutive reads before an entry expires.

---

## 🔐 SNAP-BI & Core Banking Terms

### SNAP-BI (Standar Nasional API Pembayaran Indonesia)
**Type**: Regulatory Standard  
**Domain**: Payment Gateway  
**Definition**: The standard Open API spec for payment operations in Indonesia mandated by Bank Indonesia. Includes mandatory headers (`X-SIGNATURE`, `X-TIMESTAMP`, `X-PARTNER-ID`, `X-EXTERNAL-ID`) and standardized RFC 9457 error payloads.

### Double-Entry Ledger
**Type**: Core Accounting Pattern  
**Domain**: Financial Operations  
**Definition**: Immutable financial ledger system where every monetary transaction consists of equal debit and credit entries (`debit == credit`). Updates and deletions are strictly forbidden; corrections require explicit reversal entries.

---
*Created and maintained via `grill-with-docs` sessions.*
