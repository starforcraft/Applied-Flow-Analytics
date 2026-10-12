package com.ultramega.appliedflowanalytics.network;

import com.ultramega.appliedflowanalytics.data.FlowSnapshotData;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public final class FlowHistoryNetworkComponent implements IGridService, IGridServiceProvider {
    public static final int MONITOR_SAMPLES = 200;

    private static final Set<FlowHistoryNetworkComponent> ACTIVE = new HashSet<>();
    private static final Map<UUID, FlowHistoryNetworkComponent> OWNERS = new HashMap<>();

    private final IGrid network;
    private final Set<FlowHistoryNode> nodes = new HashSet<>();
    private final Map<Query, FlowSnapshotData.Samples> queries = new HashMap<>();
    private final Map<RollingQuery, FlowSnapshotData.FlowTotals> rollingQueries = new HashMap<>();

    @Nullable
    private FlowSnapshotData data;
    @Nullable
    private UUID historyId;
    private boolean resolve = true;
    private boolean recording;

    public FlowHistoryNetworkComponent(final IGrid network, final IStorageService storage) {
        this.network = network;
        ((FlowStorageAccess) storage.getInventory()).flowAnalytics$setFlowHistory(this);
    }

    @Override
    public void addNode(final IGridNode gridNode, @Nullable final CompoundTag savedData) {
        final FlowHistoryNode node = gridNode.getService(FlowHistoryNode.class);
        if (node != null) {
            this.nodes.add(node);
            this.resolve = true;
            ACTIVE.add(this);
        }
    }

    @Override
    public void removeNode(final IGridNode gridNode) {
        final FlowHistoryNode node = gridNode.getService(FlowHistoryNode.class);
        if (node != null) {
            this.nodes.remove(node);
            this.resolve = true;
            if (this.nodes.isEmpty()) {
                ACTIVE.remove(this);
                this.recording = false;
                if (this.historyId != null) {
                    OWNERS.remove(this.historyId, this);
                }
            }
        }
    }

    public static void tickAll() {
        final var components = new ArrayList<>(ACTIVE);
        components.forEach(FlowHistoryNetworkComponent::resolveHistory);
        components.forEach(FlowHistoryNetworkComponent::tick);
    }

    public static void clear() {
        ACTIVE.forEach(component -> component.recording = false);
        ACTIVE.clear();
        OWNERS.clear();
    }

    public Optional<FlowSnapshotData> getData() {
        this.resolveHistory();
        return Optional.ofNullable(this.data);
    }

    public long getRevision() {
        return this.getData().map(FlowSnapshotData::getRevision).orElse(-1L);
    }

    public FlowSnapshotData.Samples getSamples(final AEKey resource,
                                               final int granularity) {
        this.resolveHistory();
        if (this.data == null) {
            return new FlowSnapshotData.Samples(new long[0], new long[0]);
        }
        return this.queries.computeIfAbsent(new Query(resource, granularity), key ->
            this.data.getSamples(resource::equals, granularity, MONITOR_SAMPLES));
    }

    public FlowSnapshotData.FlowTotals getRollingFlow(final AEKey resource,
                                                      final int windowTicks) {
        this.resolveHistory();
        if (this.data == null) {
            return new FlowSnapshotData.FlowTotals(0, 0, 0);
        }
        return this.rollingQueries.computeIfAbsent(new RollingQuery(resource, windowTicks), key ->
            this.data.getRollingFlow(resource::equals, windowTicks));
    }

    private void resolveHistory() {
        if (!this.resolve || this.nodes.isEmpty()) {
            return;
        }
        final ServerLevel level = this.nodes.stream().map(FlowHistoryNode::getServerLevel)
            .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (level == null) {
            return;
        }

        UUID selectedId = this.historyId;
        FlowSnapshotData selected = this.data;
        // If multiple recorders or reunited branches describe overlapping pasts: choose one, never add them
        for (final FlowHistoryNode node : this.nodes) {
            final UUID id = node.getHistoryId();
            if (id != null && !id.equals(selectedId)) {
                final FlowSnapshotData candidate = FlowSnapshotData.get(level, id);
                if (selected == null || candidate.getRecordedIntervals() > selected.getRecordedIntervals()
                    || (candidate.getRecordedIntervals() == selected.getRecordedIntervals()
                    && selectedId != null && id.compareTo(selectedId) < 0)) {
                    selected = candidate;
                    selectedId = id;
                }
            }
        }

        final FlowHistoryNetworkComponent owner = selectedId == null ? null : OWNERS.get(selectedId);
        if (selectedId == null || (owner != null && owner != this)) {
            selectedId = UUID.randomUUID();
            final FlowSnapshotData fork = FlowSnapshotData.get(level, selectedId);
            if (selected != null) {
                selected.copyTo(fork);
            }
            selected = fork;
        }

        if (this.historyId != null) {
            OWNERS.remove(this.historyId, this);
        }
        final UUID resolvedId = selectedId;
        this.historyId = resolvedId;
        this.data = selected;
        OWNERS.put(resolvedId, this);
        this.nodes.forEach(node -> node.setHistoryId(resolvedId));
        this.resolve = false;
        this.queries.clear();
        this.rollingQueries.clear();
        this.recording = this.nodes.stream().anyMatch(FlowHistoryNode::isActive);
    }

    public void recordChange(final AEKey resource, final long amount) {
        if (amount == 0 || this.nodes.isEmpty() || this.nodes.stream().noneMatch(FlowHistoryNode::isActive)) {
            return;
        }
        this.resolveHistory();
        if (this.data != null) {
            this.data.recordChange(resource, amount);
        }
    }

    private void tick() {
        this.recording = this.nodes.stream().anyMatch(FlowHistoryNode::isActive);
        if (this.recording && this.data != null) {
            this.queries.keySet().removeIf(query -> query.granularity() == Granularity.TICK.getTickAmount());
            if (this.data.tick()) {
                this.queries.clear();
            }
            // All detectors querying the same resource share one rolling sum per recorded tick
            this.rollingQueries.clear();
        }
        this.nodes.forEach(FlowHistoryNode::tick);
    }

    private record Query(AEKey resource, int granularity) {
    }

    private record RollingQuery(AEKey resource, int windowTicks) {
    }
}
