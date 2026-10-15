package com.ultramega.appliedflowanalytics.part;

import com.ultramega.appliedflowanalytics.Platform;
import com.ultramega.appliedflowanalytics.network.FlowHistoryNode;
import com.ultramega.appliedflowanalytics.registry.ModDataComponents;

import appeng.api.ids.AEComponents;
import appeng.api.parts.IPartItem;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.IConfigInvHost;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.util.SettingsFrom;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class AbstractFlowDisplayPart extends AbstractDisplayPart implements IConfigurableObject {
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

    protected void writeConfiguration(final ValueOutput tag) {
    }

    protected void readConfiguration(final ValueInput tag) {
    }

    @Override
    public void writeToNBT(final ValueOutput tag) {
        super.writeToNBT(tag);
        this.historyNode.saveHistoryId(tag);
    }

    @Override
    public void readFromNBT(final ValueInput tag) {
        super.readFromNBT(tag);
        this.historyNode.loadHistoryId(tag);
    }

    @Override
    public void exportSettings(final SettingsFrom mode, final DataComponentMap.Builder builder) {
        super.exportSettings(mode, builder);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            builder.set(AEComponents.EXPORTED_SETTINGS, this.getConfigManager().exportSettings());
            if (this instanceof IConfigInvHost configInvHost) {
                builder.set(AEComponents.EXPORTED_CONFIG_INV, configInvHost.getConfig().toList());
            }
            final var historyId = this.historyNode.getHistoryId();
            if (historyId != null) {
                builder.set(ModDataComponents.HISTORY_ID.get(), historyId);
            }
        }
    }

    @Override
    public void importSettings(final SettingsFrom mode, final DataComponentMap input, @Nullable final Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            final var historyId = input.get(ModDataComponents.HISTORY_ID.get());
            if (historyId != null) {
                this.historyNode.setHistoryId(historyId);
            }
        }
        this.getHost().markForSave();
        this.getHost().markForUpdate();
    }
}
