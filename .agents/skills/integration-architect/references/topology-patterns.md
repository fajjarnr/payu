# Kafka Streams Advanced Topology Patterns

Patterns for implementing stateful stream processing in PayU using Kafka Streams (Java/Quarkus).

## 1. Stream-Table Join (Enrichment)

Use `GlobalKTable` for reference data (like User Profiles) that needs to be joined with high-velocity streams (Transactions).

```java
public class EnrichmentTopology {
    public void build(StreamsBuilder builder) {
        // 1. Transaction Stream (High Volume)
        KStream<String, Transaction> transactions = builder.stream(
            "payu.transaction.created",
            Consumed.with(Serdes.String(), transactionSerde)
        );

        // 2. User Profile Table (Global Reference - Replicated to all nodes)
        GlobalKTable<String, UserProfile> userProfiles = builder.globalTable(
            "payu.user.profile",
            Consumed.with(Serdes.String(), profileSerde)
        );

        // 3. Join Logic (Enrich Transaction with Fraud Score from Profile)
        KStream<String, EnrichedTransaction> enriched = transactions.join(
            userProfiles,
            (txnKey, txn) -> txn.getUserId(), // Foreign Key Extractor
            (txn, profile) -> new EnrichedTransaction(txn, profile.getFraudScore())
        );

        // 4. Sink
        enriched.to("payu.transaction.enriched", Produced.with(Serdes.String(), enrichedSerde));
    }
}
```

## 2. Windowed Aggregation (Real-time Analytics)

Calculate running totals (e.g., "Total Transfer Amount per User in last 1 hour").

```java
public class AnalyticsTopology {
    public void build(StreamsBuilder builder) {
        KStream<String, Transaction> transactions = builder.stream("payu.transaction.created");

        transactions
            .groupByKey() // Re-partition by User ID if needed
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .aggregate(
                () -> 0.0, // Initializer
                (key, txn, agg) -> agg + txn.getAmount(), // Aggregator
                Materialized.<String, Double, WindowStore<Bytes, byte[]>>as("hourly-volumes")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Double())
            )
            .toStream()
            .map((windowedKey, value) -> KeyValue.pair(
                windowedKey.key() + "@" + windowedKey.window().start(), 
                value
            ))
            .to("payu.analytics.hourly-volume");
    }
}
```

## 3. Topology Unit Testing (Fast Verification)

Use `TopologyTestDriver` to test logic without spinning up Kafka brokers.

```java
public class TopologyTest {
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    @BeforeEach
    public void setup() {
        var builder = new StreamsBuilder();
        new EnrichmentTopology().build(builder);
        
        var props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        
        testDriver = new TopologyTestDriver(builder.build(), props);
        
        inputTopic = testDriver.createInputTopic(
            "payu.transaction.created", 
            new StringSerializer(), new TransactionSerializer()
        );
        outputTopic = testDriver.createOutputTopic(
            "payu.transaction.enriched", 
            new StringDeserializer(), new EnrichedSerializer()
        );
    }

    @Test
    public void shouldEnrichTransaction() {
        // Pipe Input
        inputTopic.pipeInput("txn-1", new Transaction("user-1", 1000.0));
        
        // Verify Output
        var result = outputTopic.readRecord();
        assertEquals("user-1", result.value().getUserId());
        assertNotNull(result.value().getFraudScore());
    }


    @AfterEach
    public void tearDown() {
        testDriver.close();
    }
}
```

## 4. Branching (Split Streams)

Route events to different paths based on content logic (e.g., High Value vs Standard Orders).

```java
public class BranchingTopology {
    public void build(StreamsBuilder builder) {
        KStream<String, Order> orders = builder.stream("payu.orders");

        Map<String, KStream<String, Order>> branches = orders
            .split(Named.as("branch-"))
            .branch((key, order) -> order.getAmount() > 1_000_000, Branched.as("high-value"))
            .branch((key, order) -> order.getAmount() > 0, Branched.as("standard"))
            .defaultBranch(Branched.as("invalid"));

        branches.get("branch-high-value").to("payu.orders.high-priority");
        branches.get("branch-standard").to("payu.orders.standard");
        branches.get("branch-invalid").to("payu.orders.dlq");
    }
}
```

## 5. Stateful Deduplication (Low-Level Processor API)

Use a custom `Transformer` with a `KeyValueStore` to filter duplicate events within a time window (e.g., 10 minutes).

```java
public class DeduplicationTopology {
    public void build(StreamsBuilder builder) {
        // Define State Store
        var storeBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore("dedup-store"),
            Serdes.String(),
            Serdes.Long() // Store Timestamp
        );
        builder.addStateStore(storeBuilder);

        KStream<String, Event> events = builder.stream("payu.events");

        events.transform(() -> new DeduplicationTransformer(), "dedup-store")
              .to("payu.events.unique");
    }

    public static class DeduplicationTransformer implements Transformer<String, Event, KeyValue<String, Event>> {
        private KeyValueStore<String, Long> store;
        private static final long DEDUP_WINDOW_MS = 10 * 60 * 1000L; // 10 mins

        @Override
        public void init(ProcessorContext context) {
            this.store = (KeyValueStore<String, Long>) context.getStateStore("dedup-store");
        }

        @Override
        public KeyValue<String, Event> transform(String key, Event value) {
            Long lastSeen = store.get(key);
            long now = System.currentTimeMillis();

            if (lastSeen != null && (now - lastSeen) < DEDUP_WINDOW_MS) {
                return null; // Duplicate -> Drop
            }

            store.put(key, now);
            return KeyValue.pair(key, value); // Unique -> Forward
        }

        @Override
        public void close() {}
    }
}
```

## 6. Co-Partitioning Strategy

Fix `TopologyException: Invalid topology: ... not co-partitioned` errors when joining streams.

**Problem:** Joining `Stream A` (6 partitions) with `Stream B` (3 partitions).
**Solution:** Repartition `Stream B` to match `Stream A`.

```java
public class CoPartitioningTopology {
    public void build(StreamsBuilder builder) {
        KStream<String, Order> orders = builder.stream("payu.orders"); // 6 partitions
        KStream<String, Payment> payments = builder.stream("payu.payments"); // 3 partitions

        // Repartition payments to match orders
        KStream<String, Payment> repartitionedPayments = payments.repartition(
            Repartitioned.<String, Payment>as("payments-repartitioned")
                .withKeySerde(Serdes.String())
                .withValueSerde(paymentSerde)
                .withNumberOfPartitions(6) // Must match 'orders' partition count
        );

        // Now safe to join
        orders.join(
            repartitionedPayments,
            (order, payment) -> new JoinedResult(order, payment),
            JoinWindows.ofSizeWithNoGrace(Duration.ofMinutes(5))
        ).to("payu.orders.paid");
    }
}
```
