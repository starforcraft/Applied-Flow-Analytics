package com.ultramega.appliedflowanalytics.part;

import com.ultramega.appliedflowanalytics.Platform;
import com.ultramega.appliedflowanalytics.network.FlowHistoryNode;
import com.ultramega.appliedflowanalytics.registry.ModDataComponents;

import appeng.api.parts.IPartItem;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.util.SettingsFrom;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractFlowDisplayPart extends AbstractDisplayPart {
    protected final FlowHistoryNode historyNode;

    protected AbstractFlowDisplayPart(final IPartItem<?> partItem, final boolean requireChannel) {
        super(partItem, requireChannel);
        this.historyNode = new FlowHistoryNode(this, this::historyTick);
    }

    @Override
    public void addToWorld() {
        this.getMainNode().setIdlePowerUsage(this instanceof FlowTerminalPart
            ? Platform.getServerConfig().getFlowTerminalEnergyUsage()
            : Platform.getServerConfig().getFlowMonitorEnergyUsage());
        super.addToWorld();
    }

    protected void historyTick() {
    }

    public FlowHistoryNode getHistoryNode() {
        return this.historyNode;
    }

    protected void writeConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
    }

    protected void readConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
    }

    @Override
    public void writeToNBT(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.writeToNBT(tag, provider);
        this.historyNode.saveHistoryId(tag);
    }

    @Override
    public void readFromNBT(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.readFromNBT(tag, provider);
        this.historyNode.loadHistoryId(tag);
    }

    @Override
    public void exportSettings(final SettingsFrom mode, final DataComponentMap.Builder builder) {
        super.exportSettings(mode, builder);
        final CompoundTag tag = new CompoundTag();
        this.writeConfiguration(tag, this.getLevel().registryAccess());
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            this.historyNode.saveHistoryId(tag);
        }
        builder.set(ModDataComponents.FLOW_SETTINGS.get(), tag);
    }

    @Override
    public void importSettings(final SettingsFrom mode, final DataComponentMap input, @Nullable final Player player) {
        super.importSettings(mode, input, player);
        final CompoundTag tag = input.get(ModDataComponents.FLOW_SETTINGS.get());
        if (tag != null) {
            this.readConfiguration(tag, this.getLevel().registryAccess());
            if (mode == SettingsFrom.DISMANTLE_ITEM) {
                this.historyNode.loadHistoryId(tag);
            }
            this.getHost().markForSave();
            this.getHost().markForUpdate();
        }
    }

}
