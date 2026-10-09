package id.payu.gateway.adapter.persistence;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GW-CACHE-001: verifies the shard planning logic that keeps Hot Rod payloads bounded
 * (an unbounded per-day list exceeded the ~10 MB message limit and broke the connection).
 */
class HotRodApiAnalyticsRepositoryTest {

    private static final int MAX = 200;

    @Test
    void partitionSplitsEventsIntoBoundedBatches() {
        List<String> events = new ArrayList<>();
        for (int i = 0; i < 450; i++) {
            events.add("{\"i\":" + i + "}");
        }

        List<List<String>> batches = HotRodApiAnalyticsRepository.partition(events, MAX);

        assertEquals(3, batches.size());
        assertEquals(MAX, batches.get(0).size());
        assertEquals(MAX, batches.get(1).size());
        assertEquals(50, batches.get(2).size());
    }

    @Test
    void partitionKeepsAllEventsInOrder() {
        List<String> events = List.of("a", "b", "c", "d", "e");

        List<List<String>> batches = HotRodApiAnalyticsRepository.partition(events, 2);

        assertEquals(events, batches.stream().flatMap(List::stream).toList());
    }

    @Test
    void shardIndicesCoversEveryRecordedShard() {
        assertEquals(List.of(0), HotRodApiAnalyticsRepository.shardIndices(List.of()));
        assertEquals(List.of(0, 1, 2),
            HotRodApiAnalyticsRepository.shardIndices(List.of("0:10", "1:5", "2:199")));
        assertEquals(List.of(0, 1),
            HotRodApiAnalyticsRepository.shardIndices(List.of("0:200", "1:100")));
    }
}
