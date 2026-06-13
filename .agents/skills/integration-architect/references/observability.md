# Kafka Observability & Monitoring (AMQ Streams)

Production-grade monitoring for Red Hat AMQ Streams using Prometheus and Grafana.

## 1. Metric Scaping Strategy (Strimzi)

AMQ Streams supports JMX Exporter out-of-the-box. Ensure your `Kafka` Custom Resource has metrics enabled.

### Kafka CR Configuration
```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: payu-cluster
  namespace: kafka
spec:
  kafka:
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
```

### PodMonitor (Prometheus Operator)
To scrape metrics automatically:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: kafka-metrics
  namespace: kafka
  labels:
    app: strimzi
spec:
  selector:
    matchLabels:
      strimzi.io/kind: Kafka
  podMetricsEndpoints:
    - port: tcp-prometheus
      interval: 30s
```

## 2. Critical Alerting Rules

Apply these `PrometheusRule` to your cluster to catch issues before outages.

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: kafka-alerts
  namespace: monitoring
spec:
  groups:
    - name: kafka.rules
      interval: 30s
      rules:
        # 🚨 CRITICAL: Data Loss Risk
        - alert: KafkaUnderReplicatedPartitions
          expr: sum(kafka_server_replica_manager_under_replicated_partitions) > 0
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Kafka Under-Replicated Partitions"
            description: "{{ $value }} partitions are under-replicated. Check broker health."

        # 🚨 CRITICAL: Service Down
        - alert: KafkaOfflinePartitions
          expr: kafka_controller_offline_partitions_count > 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: "Kafka Offline Partitions"
            description: "{{ $value }} partitions are offline. Immediate action required!"

        # ⚠️ WARNING: Consumer Lag (Slow Processing)
        - alert: KafkaConsumerLagHigh
          expr: sum by (consumergroup) (kafka_consumergroup_lag) > 10000
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "High Consumer Lag: {{ $labels.consumergroup }}"
            description: "Lag is {{ $value }} messages. Scale consumers or check logic."

        # ⚠️ WARNING: Broker Load
        - alert: KafkaBrokerHighCPU
          expr: os_process_cpu_load{job="kafka"} > 0.8
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Broker High CPU"
            description: "CPU usage > 80% on {{ $labels.instance }}."
```

## 3. Key Dashboards

When building Grafana dashboards, focus on these 4 golden signals:

| Signal | Metric | PromQL |
| :--- | :--- | :--- |
| **Throughput** | Bytes In/Out | `sum(rate(kafka_server_brokertopicmetrics_bytesin_total[1m]))` |
| **Latency** | Produce Latency | `kafka_network_requestmetrics_requestqueuetime_mean + kafka_network_requestmetrics_localtime_mean` |
| **Health** | Active Controller | `kafka_controller_active_controller_count` (Must be 1) |
| **Lag** | Consumer Lag | `kafka_consumergroup_lag` |
