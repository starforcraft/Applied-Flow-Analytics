package com.ultramega.appliedflowanalytics.data;

import java.util.UUID;

public interface FlowHistoryStorage {
    FlowSnapshotData appliedflowanalytics$getHistory(UUID networkId);
}
