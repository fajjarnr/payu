# LitmusChaos OpenShift 4.20 + CRI-O Compatibility Report

> **Status**: Documented — Known upstream limitation
> **Date**: 2026-05-02
> **Affected**: Litmus 3.28.0 (go-runner helper binary)
> **Cluster**: OpenShift 4.20.6, CRI-O 1.33.10, Kernel 5.14.0-503

---

## Executive Summary

LitmusChaos **helper-based experiments deadlock indefinitely** on OpenShift 4.20 + CRI-O. Only `pod-delete` (non-helper) works reliably. This is a **platform-specific bug** in the Litmus `go-runner:3.28.0` helper binary, not a configuration issue.

---

## Compatibility Matrix

| Experiment | Helper Pods? | Status | Notes |
|-----------|-------------|--------|-------|
| `pod-delete` | No | **Working** | Uses K8s API directly |
| `container-kill` | Yes | **Deadlock** | Helper futex_wait deadlock |
| `disk-fill` | Yes | **Deadlock** | Helper futex_wait deadlock |
| `pod-cpu-hog` | Yes | **Deadlock** | Helper futex_wait deadlock |
| `pod-memory-hog` | Yes | **Deadlock** | Helper futex_wait deadlock |
| `pod-network-latency` | Yes | **Deadlock** | Helper futex_wait deadlock |
| `pod-network-loss` | Yes | **Deadlock** | Helper futex_wait deadlock |
| `node-cpu-hog` | No* | Unknown | Runs as DaemonSet, may work |
| `node-memory-hog` | No* | Unknown | Runs as DaemonSet, may work |

> *Node-level experiments do not spawn helper pods but require privileged node access.

---

## Root Cause Analysis

### Symptom

Helper pod logs stop at:

```
[PreReq]: Getting the ENV variables
```

Then nothing. The pod runs forever (until ChaosEngine timeout).

### Debugging Evidence

1. **Process stack** (`/proc/<pid>/stack`):
   ```
   futex_wait_queue_me
   futex_wait
   do_futex
   __x64_sys_futex
   ```

2. **Thread dump** (`/proc/<pid>/task/*/stack`):
   - 10 Go runtime threads
   - All blocked on `futex_wait`
   - One thread stuck in `TCP SYN_SENT` to `172.30.0.1:443`

3. **Network verification**:
   - Test pods with identical image + security context reach K8s API fine
   - `curl -k https://172.30.0.1:443` from test pod → HTTP 403 (expected)
   - NetworkPolicy `allow-all-egress` applied and verified

4. **CRI-O socket**:
   - Accessible at `/run/crio/crio.sock` (also symlinked at `/var/run/crio/crio.sock`)
   - Helper pod mounts socket correctly but never uses it (deadlocks before)

5. **Security context**:
   - SCC `litmus-chaos` applied: `privileged`, `hostPID`, `hostPath`, `NET_ADMIN`
   - `seccompProfiles`: `["runtime/default", "unconfined"]`
   - Service account token mounted correctly

6. **Reproduction**:
   - Running `./helpers` manually in an isolated test pod with identical env vars reproduces the deadlock
   - Not reproducible on Docker Desktop or kind clusters

### Conclusion

The `./helpers` binary (compiled from Litmus go-runner) enters a **futex deadlock during initialization** specifically on OpenShift 4.20 + CRI-O. This is likely triggered by:

- A CRI-O-specific container runtime check
- A seccomp/SELinux interaction unique to OpenShift
- A Go runtime futex behavior change on RHEL 9.4 kernel (5.14.0-503)

**This is an upstream Litmus bug**, not a PayU configuration issue.

---

## Required RBAC / SCC Setup (Applied)

```bash
# SCC for Litmus chaos experiments
oc adm policy add-scc-to-user litmus-chaos -z litmus -n payu-dev

# Allow litmus to create events (helper pods log events)
oc patch clusterrole litmus --type='json' \
  -p='[{"op": "add", "path": "/rules/3/verbs/-", "value": "create"}]'

# Allow unconfined seccomp for helper pods
oc patch scc litmus-chaos --type='merge' \
  -p '{"seccompProfiles":["runtime/default","unconfined"]}'

# Add litmus SA to SCC
oc patch scc litmus-chaos --type='json' \
  -p='[{"op": "add", "path": "/users/-", "value": "system:serviceaccount:payu-dev:litmus"}]'
```

---

## Recommended Workarounds

### 1. Use `pod-delete` Only (Immediate)

`pod-delete` is the only experiment confirmed working. Use it for basic resiliency testing.

### 2. Node-Level Experiments (Alternative)

`node-cpu-hog` and `node-memory-hog` run as DaemonSets and do not spawn helper pods. They may work but require:
- `privileged: true`
- `hostPID: true`
- Node-level resource impact (affects all pods on node)

### 3. Chaos Mesh (Alternative Tool)

Consider [Chaos Mesh](https://chaos-mesh.org/) as an alternative for CPU/network/disk chaos. It uses a different architecture (daemon-based) that may not have the same deadlock issue.

> ⚠️ **Note**: Chaos Mesh was previously evaluated and deferred due to operational complexity. Re-evaluate if Litmus remains blocked.

### 4. Custom Helper Image (Advanced)

Build a patched `go-runner` image with:
- Updated Go runtime (1.22+)
- Removed or fixed CRI-O initialization check
- Added debug logging before futex-prone code paths

### 5. Wait for Upstream Fix

Track [Litmus GitHub Issues](https://github.com/litmuschaos/litmus/issues) for OpenShift/CRI-O compatibility fixes.

---

## Files

- **ChaosEngine manifests**: `infrastructure/platform/chaos/litmus-chaos-engines.yaml`
- **SCC definition**: `infrastructure/platform/chaos/litmus-scc.yaml` (if exists)
- **This report**: `docs/guides/LITMUS_CHAOS_OPENSHIFT_COMPATIBILITY.md`

---

## References

- [Litmus OpenShift Docs](https://docs.litmuschaos.io/docs/platform/openshift)
- [Litmus Experiment Hub](https://hub.litmuschaos.io/)
- [OpenShift SCC Documentation](https://docs.openshift.com/container-platform/4.20/authentication/managing-security-context-constraints.html)
- [CRI-O Architecture](https://github.com/cri-o/cri-o/blob/main/docs/crio.8.md)
