package com.ultramega.appliedflowanalytics.part;

import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.IConfigManager;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.util.InteractionUtil;

import java.util.Objects;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowTerminalPart extends AbstractFlowDisplayPart {
    public static final ResourceLocation MODEL_OFF = createFlowAnalyticsIdentifier("part/flow_terminal_off");
    public static final ResourceLocation MODEL_ON = createFlowAnalyticsIdentifier("part/flow_terminal_on");
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final IConfigManager configManager = FlowSettings.createConfigManager(this::settingsChanged);
    private boolean loadingData;

    public FlowTerminalPart(final IPartItem<?> partItem) {
        super(partItem, true);
    }

    private void settingsChanged() {
        if (!this.loadingData && this.getHost() != null && !this.isClientSide()) {
            this.getHost().markForSave();
        }
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    public SortDir getSortingDirection() {
        return this.configManager.getSetting(Settings.SORT_DIRECTION);
    }

    public void setSortingDirection(final SortDir value) {
        this.configManager.putSetting(Settings.SORT_DIRECTION, Objects.requireNonNull(value));
    }

    public SortOrder getSortingType() {
        return this.configManager.getSetting(Settings.SORT_BY);
    }

    public void setSortingType(final SortOrder value) {
        this.configManager.putSetting(Settings.SORT_BY, Objects.requireNonNull(value));
    }

    public Granularity getGranularity() {
        return this.configManager.getSetting(FlowSettings.GRANULARITY);
    }

    public void setGranularity(final Granularity value) {
        this.configManager.putSetting(FlowSettings.GRANULARITY, Objects.requireNonNull(value));
    }

    public LineStyle getLineStyle() {
        return this.configManager.getSetting(FlowSettings.LINE_STYLE);
    }

    public void setLineStyle(final LineStyle value) {
        this.configManager.putSetting(FlowSettings.LINE_STYLE, Objects.requireNonNull(value));
    }

    public ResourceView getResourceView() {
        return this.configManager.getSetting(FlowSettings.RESOURCE_VIEW);
    }

    public void setResourceView(final ResourceView value) {
        this.configManager.putSetting(FlowSettings.RESOURCE_VIEW, Objects.requireNonNull(value));
    }

    @Override
    public void writeConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.configManager.writeToNBT(tag, provider);
    }

    @Override
    public void readConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.loadingData = true;
        try {
            this.configManager.readFromNBT(tag, provider);
        } finally {
            this.loadingData = false;
        }
    }

    @Override
    public void writeToNBT(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.writeToNBT(tag, provider);
        this.writeConfiguration(tag, provider);
    }

    @Override
    public void readFromNBT(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.readFromNBT(tag, provider);
        this.readConfiguration(tag, provider);
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public boolean onUseWithoutItem(final Player player, final Vec3 pos) {
        if (InteractionUtil.canWrenchRotate(player.getInventory().getSelected())) {
            return super.onUseWithoutItem(player, pos);
        }
        if (!this.isClientSide()) {
            MenuOpener.open(FlowTerminalContainerMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }
}
