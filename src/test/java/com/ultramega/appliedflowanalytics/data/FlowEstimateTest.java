package com.ultramega.appliedflowanalytics.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FlowEstimateTest {
    @Test
    void scalesNetFlowUsingTheActualObservationWindow() {
        final var estimate = FlowEstimate.from(new long[]{20, 0}, new long[]{0, 5}, 40);
        assertEquals(7.5, estimate.forTicks(20));
        assertEquals(450.0, estimate.forTicks(1200));
    }

    @Test
    void unavailableHistoryDoesNotInventAnEstimate() {
        assertFalse(FlowEstimate.from(new long[0], new long[0], 0).available());
    }
}
