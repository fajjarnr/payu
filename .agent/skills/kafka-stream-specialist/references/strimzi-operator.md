# Strimzi Operator (AMQ Streams) Reference

PayU uses Red Hat AMQ Streams (based on Strimzi) for managing Kafka clusters on OpenShift.
Use these Custom Resource Definitions (CRDs) for declarative management of topics and users.

## 1. Declarative Topic Management (`KafkaTopic`)

Commit these files to the GitOps repository to manage topics.

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: payu.transaction.created
  namespace: kafka
  labels:
    strimzi.io/cluster: payu-cluster
spec:
  topicName: payu.transaction.created
  partitions: 6
  replicas: 3
  config:
    retention.ms: 2592000000  # 30 days
    min.insync.replicas: 2
    segment.bytes: 1073741824
    cleanup.policy: delete
```

## 2. Declarative User Management (`KafkaUser`)

Manage authentication and ACLs.

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaUser
metadata:
  name: transaction-service-user
  namespace: kafka
  labels:
    strimzi.io/cluster: payu-cluster
spec:
  authentication:
    type: tls
  authorization:
    type: simple
    acls:
      # Producer ACLs
      - resource:
          type: topic
          name: payu.transaction.created
          patternType: literal
        operations: [Write, Describe]
      # Consumer ACLs
      - resource:
          type: group
          name: transaction-service-group
          patternType: literal
        operations: [Read]
```

## 3. Kafka Cluster Configuration (`Kafka`)

Reference configuration for the cluster itself (KRaft mode).

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: payu-cluster
  namespace: kafka
  annotations:
    strimzi.io/kraft: enabled
    strimzi.io/node-pools: enabled
spec:
  kafka:
    version: 3.7.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
        authentication:
          type: tls
    config:
      default.replication.factor: 3
      min.insync.replicas: 2
      auto.create.topics.enable: false # Enforce using KafkaTopic
    resources:
      requests:
        memory: 4Gi
        cpu: "2"
      limits:
        memory: 8Gi
        cpu: "4"
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
```

## 4. Production Best Practices

| Category | Guideline |
| :--- | :--- |
| **Storage** | Use SSD-backed storage classes (`gp3`, `io2`) with `iopsPerGB: 50+`. |
| **Affinity** | Use `podAntiAffinity` to spread brokers across Availability Zones. |
| **PDB** | Set `PodDisruptionBudget` to `maxUnavailable: 1` for zero-downtime upgrades. |
| **Network** | Restrict access using `NetworkPolicy` to allow only specific microservices. |
