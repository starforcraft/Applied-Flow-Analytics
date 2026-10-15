package com.ultramega.appliedflowanalytics.service;

import com.ultramega.appliedflowanalytics.AppliedFlowAnalyticsMod;
import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.network.MenuState;
import com.ultramega.appliedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.appliedflowanalytics.resource.ResourceChangeKey;

import appeng.api.stacks.AEKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;

public final class SnapshotService {
    private SnapshotService() {
    }

    public static void execute(final ServerPlayer player,
                               final FlowTerminalContainerMenu menu,
                               final long requestId,
                               final int granularity,
                               final boolean allStored) {
        AppliedFlowAnalyticsMod.queueServerWork(player.level().getServer(), () -> {
            if (player.containerMenu != menu || !menu.stillValid(player)) {
                return;
            }

            final Map<AEKey, Map<Short, Long>> result = new HashMap<>();
            final var part = menu.getHost();
            final var grid = part.getMainNode().getGrid();
            final var component = part.getHistoryNode().getHistoryComponent().orElse(null);
            if (part.getMainNode().isActive() && grid != null && component != null) {
                final var history = component.getData().orElse(null);
                if (history != null) {
                    final Map<ResourceChangeKey, Long> snapshot = history.getLastSnapshotAggregated(granularity);
                    snapshot.forEach((key, value) -> result.computeIfAbsent(key.resourceKey(), ignored -> new HashMap<>())
                        .put(key.sign(), Math.abs(value)));
                }
                if (allStored) {
                    final var stored = grid.getStorageService().getCachedInventory();
                    final var resources = new HashSet<AEKey>();
                    for (final var entry : stored) {
                        if (entry.getLongValue() > 0) {
                            resources.add(entry.getKey());
                        }
                    }
                    result.keySet().retainAll(resources);
                    resources.forEach(resource -> result.putIfAbsent(resource, Map.of((short) +1, 0L, (short) -1, 0L)));
                }
            }
            menu.sendMenuStateUpdate(player, requestId, new MenuState.Snapshot(granularity, allStored, result));
        });
    }

    public static void executeDetailed(final ServerPlayer player,
                                       final FlowTerminalContainerMenu menu,
                                       final long requestId,
                                       final AEKey resource,
                                       final int granularity) {
        AppliedFlowAnalyticsMod.queueServerWork(player.level().getServer(), () -> {
            if (player.containerMenu != menu || !menu.stillValid(player)) {
                return;
            }

            final Map<ResourceChangeGranularityKey, long[]> result = new HashMap<>();
            final var part = menu.getHost();
            final var grid = part.getMainNode().getGrid();
            final var component = part.getHistoryNode().getHistoryComponent().orElse(null);
            if (part.getMainNode().isActive() && grid != null && component != null) {
                component.getData().ifPresent(history -> result.putAll(history.getGenerationDetails(resource, granularity)));
                final long stored = grid.getStorageService().getCachedInventory().get(resource);
                result.put(new ResourceChangeGranularityKey(resource, (short) 0, granularity), new long[]{stored});
            }
            menu.sendMenuStateUpdate(player, requestId, new MenuState.DetailedSnapshot(result));
        });
    }
}
