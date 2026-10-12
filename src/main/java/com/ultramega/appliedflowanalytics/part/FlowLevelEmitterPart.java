package com.ultramega.appliedflowanalytics.part;

import com.ultramega.appliedflowanalytics.Platform;
import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.network.FlowHistoryNode;
import com.ultramega.appliedflowanalytics.registry.ModDataComponents;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.DetectorMode;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManagerBuilder;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;

import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowLevelEmitterPart extends AbstractLevelEmitterPart {
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
        builder.registerSetting(FlowSettings.MODE, DetectorMode.BELOW);
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
            final long threshold = this.normalizeAmount(resource);
            this.output = flow.recordedTicks() > 0 && this.getFlowDirection().matches(flow.inflow(), flow.outflow(), threshold, this.getMode());
        }
        this.updateState();
    }

    private long normalizeAmount(final AEKey resource) {
        final double scaled = this.amount * resource.getAmountPerUnit();
        return (long) Math.clamp(scaled, (double) Long.MIN_VALUE, (double) Long.MAX_VALUE);
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

    public ConfigInventory getFilter() {
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

    public DetectorMode getMode() {
        return this.getConfigManager().getSetting(FlowSettings.MODE);
    }

    public void setMode(final DetectorMode value) {
        this.getConfigManager().putSetting(FlowSettings.MODE, Objects.requireNonNull(value));
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
        this.getConfigManager().writeToNBT(tag, provider);
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
