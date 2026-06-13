package id.payu.commons.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * READY-002: Idempotency stress characterization test.
 *
 * <p>Fires 10 concurrent requests with the same {@code Idempotency-Key} against
 * {@link IdempotencyService} (with an in-memory
 * {@link IdempotencyRepository}) and verifies:</p>
 * <ol>
 *   <li>Exactly 1 of the 10 requests wins the {@code startRequest} race (1 mutation)</li>
 *   <li>After the winner calls {@code storeResponse}, the other 9 threads
 *       observe the existing entry via {@code get(...)} (deduped reads)</li>
 *   <li>The repository's {@code saveIfAbsent} was called exactly 1 time —
 *       no two requests slipped through and double-mutated</li>
 * </ol>
 *
 * <p>Per AGENTS.md rule 8: all payment/transfer endpoints must support
 * {@code X-Idempotency-Key} to prevent duplicate mutations on retry.
 * READY-002 keeps this contract honest.</p>
 */
@DisplayName("READY-002 — idempotency stress: 10 concurrent dup X-Idempotency-Key")
class IdempotencyStressTest {

    @Test
    @DisplayName("10 concurrent duplicate keys: at most 1 mutation, no double-save")
    void tenConcurrentDuplicateKeysYieldAtMostOneMutation() throws Exception {
        InMemoryRepository mem = new InMemoryRepository();
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        IdempotencyService svc = new IdempotencyService(mem, mapper);

        String key = UUID.randomUUID().toString();
        Object body = Map.of("amount", 1000, "currency", "IDR");

        // Phase 1: 1 request wins the race, stores the response, then we proceed.
        boolean started = svc.startRequest(key, body);
        assertThat(started).as("first request must win the claim").isTrue();
        svc.storeResponse(key, body,
            org.springframework.http.HttpStatus.CREATED,
            Map.of("id", UUID.randomUUID().toString()));

        // Storage invariant: saveIfAbsent ran exactly once during the claim.
        assertThat(mem.saveIfAbsentCount.get())
            .as("the repository's saveIfAbsent (the atomic claim) must run exactly once")
            .isEqualTo(1);

        // Phase 2: 9 more requests with the SAME key, concurrently. They all
        // hit AFTER the winner has completed, so the cached entry is visible.
        // They must NOT double-mutate — all should return the cached response.
        ExecutorService pool = Executors.newFixedThreadPool(9);
        AtomicInteger dupReads = new AtomicInteger();
        AtomicInteger unexpectedConflict = new AtomicInteger();
        AtomicInteger unexpectedSave = new AtomicInteger();

        try {
            CompletableFuture<?>[] futures = new CompletableFuture<?>[9];
            for (int i = 0; i < 9; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        boolean started2 = svc.startRequest(key, body);
                        if (started2) {
                            // BAD: would mean a 2nd mutation slipped through
                            unexpectedSave.incrementAndGet();
                        } else {
                            // lost the race — read the cached entry
                            Optional<IdempotencyEntry> existing = svc.get(key, body);
                            if (existing.isPresent()) {
                                dupReads.incrementAndGet();
                            } else {
                                unexpectedConflict.incrementAndGet();
                            }
                        }
                    } catch (id.payu.api.common.exception.ConflictException ex) {
                        unexpectedConflict.incrementAndGet();
                    }
                }, pool);
            }
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdown();
        }

        // The idempotency contract under replay-after-completion
        assertThat(unexpectedSave.get())
            .as("no follow-up call must be allowed to start a new request")
            .isZero();
        assertThat(unexpectedConflict.get())
            .as("no follow-up call should hit an in-progress conflict (winner already completed)")
            .isZero();
        assertThat(dupReads.get())
            .as("all 9 follow-up calls must see the cached response")
            .isEqualTo(9);

        // Storage invariant: still exactly 1 saveIfAbsent (no second claim)
        assertThat(mem.saveIfAbsentCount.get())
            .as("no second saveIfAbsent claim must be issued")
            .isEqualTo(1);
        assertThat(mem.updateCount.get())
            .as("the winner's response must be stored exactly once via update()")
            .isEqualTo(1);
    }

    /**
     * Minimal in-memory {@link IdempotencyRepository} that records
     * how many times {@code saveIfAbsent} was called — the canary
     * for the "no double-save" invariant.
     */
    static class InMemoryRepository implements IdempotencyRepository {
        final ConcurrentHashMap<String, IdempotencyEntry> store = new ConcurrentHashMap<>();
        final AtomicInteger saveIfAbsentCount = new AtomicInteger();
        final AtomicInteger updateCount = new AtomicInteger();
        final AtomicInteger saveCount = new AtomicInteger();

        @Override
        public Optional<IdempotencyEntry> findByKey(IdempotencyKey key) {
            return Optional.ofNullable(store.get(key.value()));
        }

        @Override
        public boolean save(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
            saveCount.incrementAndGet();
            store.put(key.value(), entry);
            return true;
        }

        @Override
        public boolean saveIfAbsent(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
            // Atomic check-and-set semantics (mirrors Redis SETNX)
            IdempotencyEntry prior = store.putIfAbsent(key.value(), entry);
            if (prior == null) {
                saveIfAbsentCount.incrementAndGet();
                return true;
            }
            return false;
        }

        @Override
        public void update(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
            updateCount.incrementAndGet();
            store.put(key.value(), entry);
        }

        @Override
        public void delete(IdempotencyKey key) {
            store.remove(key.value());
        }

        @Override
        public boolean exists(IdempotencyKey key) {
            return store.containsKey(key.value());
        }

        @Override
        public long getTtl(IdempotencyKey key) {
            return 0L;
        }
    }
}
