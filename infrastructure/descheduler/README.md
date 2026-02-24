# Kube Descheduler Setup for PayU

## Overview

Kube Descheduler digunakan untuk:
1. **Rebalance pods** yang tidak merata di cluster
2. **Evict pods** dari node yang overloaded
3. **Respect pod topology spread constraints**

## Current Configuration

### KubeDescheduler CR
```yaml
apiVersion: operator.openshift.io/v1
kind: KubeDescheduler
spec:
  deschedulingIntervalSeconds: 300  # Run every 5 minutes
  mode: Automatic                    # Actually evict pods
  profiles:
    - TopologyAndDuplicates          # Spread pods evenly across nodes
    - LifecycleAndUtilization        # Balance based on utilization
    - EvictPodsWithPVC               # Allow evicting pods with PVC
  profileCustomizations:
    namespaces:
      included:
        - payu-dev                   # Only target payu-dev namespace
```

### Topology Spread Constraints (per Deployment)
```yaml
spec:
  template:
    spec:
      topologySpreadConstraints:
        - maxSkew: 1                 # Max difference between nodes
          topologyKey: kubernetes.io/hostname
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: <service-name>
```

## Pod Distribution Results

### Before (Without Descheduler)
```
Node 1: 14 pods
Node 2: 11 pods
Node 3: 8 pods
```

### After (With Topology Spread)
```
Node 1: 18 pods
Node 2: 16 pods
Node 3: 13 pods
Node 4: 10 pods
```

## Profiles Explained

| Profile | Function | Use Case |
|---------|----------|----------|
| `TopologyAndDuplicates` | Evicts pods to achieve even distribution across topology domains | Primary profile for even distribution |
| `LifecycleAndUtilization` | Evicts long-running pods and pods on high-utilization nodes | Balance resource usage |
| `EvictPodsWithPVC` | Allows descheduler to evict pods with PVC | Required for stateful pods |

## Commands

### Check Descheduler Status
```bash
oc get kubedescheduler -n openshift-kube-descheduler-operator
oc get pods -n openshift-kube-descheduler-operator
```

### View Pod Distribution
```bash
oc get pods -n payu-dev -o wide | awk '{print $7}' | sort | uniq -c
```

### View Descheduler Logs
```bash
oc logs -n openshift-kube-descheduler-operator -l app=descheduler
```

### Manual Trigger (for testing)
```bash
# Delete descheduler pod to force immediate run
oc delete pod -n openshift-kube-descheduler-operator -l app=descheduler
```

## Notes

1. **Mode**: `Automatic` means pods are actually evicted. Use `Predictive` for dry-run.
2. **Interval**: 300 seconds (5 minutes) between descheduling cycles
3. **PDB Respected**: PodDisruptionBudgets are honored - pods won't be evicted if it violates PDB
4. **Graceful Eviction**: Pods are evicted with 30s grace period by default

## Troubleshooting

### Pods not being evicted
- Check PDB: `oc get pdb -n payu-dev`
- Check descheduler logs
- Verify profile includes the namespace

### Uneven distribution persists
- Check if nodes have taints preventing pod scheduling
- Verify topology spread constraints are applied
- Check node resources (CPU/Memory)
