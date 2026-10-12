package com.ultramega.appliedflowanalytics.data;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparseSnapshotHistoryTest {
    @Test
    void keepsInflowAndOutflowSeparateAcrossIdleTicks() {
        final var history = new SparseSnapshotHistory<String>(209);
        history.record(Map.of("in", 64L, "out", -64L));
        history.advanceEmpty(19);
        assertEquals(64L, history.sumRecent(20, "in"::equals));
        assertEquals(-64L, history.sumRecent(20, "out"::equals));
        history.advanceEmpty(1);
        assertEquals(0L, history.sumRecent(20, key -> true));
        assertEquals(21, history.size());
    }

    @Test
    void expiresActivityAndDoesNotCreateNonzeroSnapshotsForIdleGaps() {
        final var history = new SparseSnapshotHistory<String>(4);
        history.record(Map.of("in", 12L));
        history.advanceEmpty(4);
        assertEquals(4, history.size());
        assertTrue(history.asList().stream().allMatch(Map::isEmpty));
        assertFalse(history.advanceEmpty(1));
    }

    @Test
    void aggregatesNewestTimeBucketsWithoutCompressingIdleIntervals() {
        final var history = new SparseSnapshotHistory<String>(100);
        history.record(Map.of("item", 3L));
        history.advanceEmpty(2);
        history.record(Map.of("item", 7L));
        history.advanceEmpty(2);
        final long[] buckets = new long[3];
        history.forEachRecent(2, 3, (index, deltas) -> buckets[index] += deltas.getOrDefault("item", 0L));
        assertArrayEquals(new long[]{3, 7, 0}, buckets);
    }

    @Test
    void diagnosticViewRetainsItsOriginalContentsAfterRecordingAndTrimming() {
        final var history = new SparseSnapshotHistory<String>(4);
        history.record(Map.of("item", 3L));
        final var before = history.asList();
        history.record(Map.of("item", 7L));
        history.trimToLast(1);
        assertEquals(1, before.size());
        assertEquals(Map.of("item", 3L), before.getFirst());
        assertEquals(Map.of("item", 7L), history.asList().getFirst());
    }
}
