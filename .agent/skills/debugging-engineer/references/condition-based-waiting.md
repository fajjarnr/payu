# Condition-Based Waiting (Stability Pattern)

## Overview
Flaky tests often guess at timing with arbitrary delays (`Thread.sleep(1000)`). This creates race conditions.
**Core principle:** Wait for the actual condition you care about, not a guess about how long it takes.

## Java Implementation (Awaitility)

PayU uses **Awaitility** for robust condition waiting in JUnit 5.

### ❌ Anti-Pattern: Arbitrary Sleep
```java
service.processAsync();
Thread.sleep(1000); // Guessing it takes 1s
assertThat(repo.count()).isEqualTo(1);
```

### ✅ Best Practice: Awaitility
```java
service.processAsync();

await().atMost(Duration.ofSeconds(5))
       .pollInterval(Duration.ofMillis(100))
       .untilAsserted(() -> {
           assertThat(repo.count()).isEqualTo(1);
       });
```

## When to Use
- **Kafka Consumers**: Wait until message appears in topic.
- **Async Sagas**: Wait until Saga status becomes `COMPLETED`.
- **Database Updates**: Wait until record is committed.

## Quick Patterns

| Scenario | Pattern |
|----------|---------|
| **Wait for Status** | `await().until(() -> service.getStatus() == State.READY)` |
| **Wait for Records** | `await().until(() -> repository.count() >= 5)` |
| **Wait for Log** | `await().untilAsserted(() -> assertThat(logs).contains("Processing finished"))` |

## Common Mistakes
1.  **Polling too fast**: Don't use `pollInterval(1ms)`. Default is usually fine.
2.  **No Timeout**: Always set `.atMost()`.
3.  **State Caching**: Ensure the method called inside `until()` actually fetches fresh data (not checking a stale variable).
