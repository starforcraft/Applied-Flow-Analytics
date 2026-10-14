package com.ultramega.appliedflowanalytics.part;

import com.ultramega.appliedflowanalytics.Platform;
import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.network.FlowHistoryNode;
import com.ultramega.appliedflowanalytics.registry.ModDataComponents;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.config.Settings;
import appeng.api.ids.AEComponents;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManagerBuilder;
import appeng.helpers.IConfigInvHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;

import java.math.BigDecimal;
import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowLevelEmitterPart extends AbstractLevelEmitterPart implements IConfigInvHost {
    public static final ResourceLocation MODEL_BASE_OFF = createFlowAnalyticsIdentifier("part/flow_level_emitter_off");
    public static final ResourceLocation MODEL_BASE_ON = createFlowAnalyticsIdentifier("part/flow_level_emitter_on");
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE_OFF, StorageLevelEmitterPart.MODEL_STATUS_OFF);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE_OFF, StorageLevelEmitterPart.MODEL_STATUS_ON);
    private static final IPartModel MODELS_ACTIVE_OFF = new PartModel(MODEL_BASE_OFF, StorageLevelEmitterPart.MODEL_STATUS_HAS_CHANNEL);
    private static final IPartModel MODELS_ACTIVE_ON = new PartModel(MODEL_BASE_ON, StorageLevelEmitterPart.MODEL_STATUS_HAS_CHANNEL);

    private final FlowHistoryNode historyNode;
    private final ConfigInventory filter = ConfigInventory.configTypes(1).changeListener(this::settingsChanged).build();
    private boolean loadingData;
    private boolean output;
    private double amount;

    public FlowLevelEmitterPart(final IPartItem<?> partItem) {
        super(partItem);
        this.historyNode = new FlowHistoryNode(this, this::historyTick);
    }

    @Override
    protected void registerSettings(final IConfigManagerBuilder builder) {
        super.registerSettings(builder);
        builder.registerSetting(FlowSettings.FLOW_DIRECTION, FlowDirection.INFLOW);
    }

    @Override
    public void addToWorld() {
        this.getMainNode().setIdlePowerUsage(Platform.getServerConfig().getFlowLevelEmitterEnergyUsage());
        super.addToWorld();
    }

    @Override
    protected int getUpgradeSlots() {
        return 0;
    }

    private void historyTick() {
        final AEKey resource = this.filter.getKey(0);
        final var history = this.historyNode.getHistoryComponent().orElse(null);
        if (resource == null || history == null || !this.getMainNode().isActive()) {
            this.output = false;
        } else {
            final var flow = history.getRollingFlow(resource, Granularity.SECOND.getTickAmount());
            final BigDecimal threshold = BigDecimal.valueOf(this.amount).multiply(BigDecimal.valueOf(resource.getAmountPerUnit()));
            this.output = flow.recordedTicks() > 0 && this.getFlowDirection().matches(flow.inflow(), flow.outflow(), threshold,
                this.getConfigManager().getSetting(Settings.REDSTONE_EMITTER));
        }
        this.updateState();
    }

    private void settingsChanged() {
        if (!this.loadingData && this.getHost() != null && !this.isClientSide()) {
            this.getHost().markForSave();
            this.historyTick();
        }
    }

    @Override
    protected void configureWatchers() {
        this.settingsChanged();
    }

    @Override
    protected boolean hasDirectOutput() {
        return true;
    }

    @Override
    protected boolean getDirectOutput() {
        return this.output;
    }

    public FlowHistoryNode getHistoryNode() {
        return this.historyNode;
    }

    @Override
    public ConfigInventory getConfig() {
        return this.filter;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(final double amount) {
        if (Double.isFinite(amount)) {
            this.amount = amount;
            this.settingsChanged();
        }
    }

    public FlowDirection getFlowDirection() {
        return this.getConfigManager().getSetting(FlowSettings.FLOW_DIRECTION);
    }

    public void setFlowDirection(final FlowDirection value) {
        this.getConfigManager().putSetting(FlowSettings.FLOW_DIRECTION, Objects.requireNonNull(value));
    }

    public void writeConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.filter.writeToChildTag(tag, "filter", provider);
        tag.putDouble("amount", this.amount);
    }

    public void readConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.loadingData = true;
        try {
            this.filter.readFromChildTag(tag, "filter", provider);
            this.amount = Double.isFinite(tag.getDouble("amount")) ? tag.getDouble("amount") : 0;
        } finally {
            this.loadingData = false;
        }
    }

    @Override
    public void writeToNBT(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.writeToNBT(tag, provider);
        this.historyNode.saveHistoryId(tag);
        this.writeConfiguration(tag, provider);
    }

    @Override
    public void readFromNBT(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.loadingData = true;
        try {
            super.readFromNBT(tag, provider);
            this.historyNode.loadHistoryId(tag);
            this.readConfiguration(tag, provider);
        } finally {
            this.loadingData = false;
        }
    }

    @Override
    public boolean onUseWithoutItem(final Player player, final Vec3 pos) {
        if (!this.isClientSide()) {
            MenuOpener.open(FlowLevelEmitterContainerMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive()) {
            return this.isLevelEmitterOn() ? MODELS_ACTIVE_ON : MODELS_ACTIVE_OFF;
        }
        return this.isPowered() ? MODELS_ON : MODELS_OFF;
    }

    @Override
    public void exportSettings(final SettingsFrom mode, final DataComponentMap.Builder builder) {
        super.exportSettings(mode, builder);
        builder.set(ModDataComponents.FLOW_THRESHOLD.get(), this.amount);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            builder.set(AEComponents.EXPORTED_SETTINGS, this.getConfigManager().exportSettings());
            builder.set(AEComponents.EXPORTED_CONFIG_INV, this.filter.toList());
            final var historyId = this.historyNode.getHistoryId();
            if (historyId != null) {
                builder.set(ModDataComponents.HISTORY_ID.get(), historyId);
            }
        }
    }

    @Override
    public void importSettings(final SettingsFrom mode, final DataComponentMap input, @Nullable final Player player) {
        super.importSettings(mode, input, player);
        final Double threshold = input.get(ModDataComponents.FLOW_THRESHOLD.get());
        if (threshold != null) {
            this.setAmount(threshold);
        }
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
