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
import appeng.api.util.IConfigManager;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

import java.util.Objects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class FlowTerminalPart extends AbstractFlowDisplayPart {

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
    public void writeConfiguration(final ValueOutput tag) {
        this.configManager.writeToNBT(tag);
    }

    @Override
    public void readConfiguration(final ValueInput tag) {
        this.loadingData = true;
        try {
            this.configManager.readFromNBT(tag);
        } finally {
            this.loadingData = false;
        }
    }

    @Override
    public void writeToNBT(final ValueOutput tag) {
        super.writeToNBT(tag);
        this.writeConfiguration(tag);
    }

    @Override
    public void readFromNBT(final ValueInput tag) {
        super.readFromNBT(tag);
        this.readConfiguration(tag);
    }

    @Override
    public boolean onUseWithoutItem(final Player player, final Vec3 pos) {
        if (InteractionUtil.canWrenchRotate(player.getInventory().getSelectedItem())) {
            return super.onUseWithoutItem(player, pos);
        }
        if (!this.isClientSide()) {
            MenuOpener.open(FlowTerminalContainerMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }
}
