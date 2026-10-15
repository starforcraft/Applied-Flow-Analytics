package com.ultramega.appliedflowanalytics.network;

import appeng.api.networking.IGridNodeService;
import appeng.parts.AEBasePart;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FlowHistoryNode implements IGridNodeService {
    private static final String HISTORY_ID = "FlowHistoryId";

    private final AEBasePart owner;
    private final Runnable onTick;
    @Nullable
    private UUID historyId;

    public FlowHistoryNode(final AEBasePart owner, final Runnable onTick) {
        this.owner = owner;
        this.onTick = onTick;
        owner.getMainNode().addService(FlowHistoryNode.class, this);
    }

    @Nullable
    public ServerLevel getServerLevel() {
        return this.owner.getHost() != null && this.owner.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    public boolean isActive() {
        return this.owner.getMainNode().isActive();
    }

    public void tick() {
        this.onTick.run();
    }

    @Nullable
    public UUID getHistoryId() {
        return this.historyId;
    }

    public void setHistoryId(final UUID id) {
        if (!id.equals(this.historyId)) {
            this.historyId = id;
            this.owner.getHost().markForSave();
        }
    }

    public Optional<FlowHistoryNetworkComponent> getHistoryComponent() {
        final var grid = this.owner.getMainNode().getGrid();
        return grid == null ? Optional.empty() : Optional.of(grid.getService(FlowHistoryNetworkComponent.class));
    }

    public void saveHistoryId(final ValueOutput tag) {
        if (this.historyId != null) {
            tag.store(HISTORY_ID, UUIDUtil.CODEC, this.historyId);
        }
    }

    public void loadHistoryId(final ValueInput tag) {
        this.historyId = tag.read(HISTORY_ID, UUIDUtil.CODEC).orElse(null);
    }
}
