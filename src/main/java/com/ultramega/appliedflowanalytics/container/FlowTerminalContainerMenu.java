package com.ultramega.appliedflowanalytics.container;

import com.ultramega.appliedflowanalytics.network.MenuState;
import com.ultramega.appliedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.appliedflowanalytics.part.FlowTerminalPart;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.menu.implementations.MenuTypeBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public class FlowTerminalContainerMenu extends AbstractFlowPartMenu<FlowTerminalPart> {
    public static final MenuType<FlowTerminalContainerMenu> TYPE = MenuTypeBuilder
        .create(FlowTerminalContainerMenu::new, FlowTerminalPart.class)
        .build(createFlowAnalyticsIdentifier("flow_terminal"));

    public final Level world;
    public final Player entity;

    public FlowTerminalContainerMenu(final MenuType<FlowTerminalContainerMenu> type, final int id,
                                     final Inventory inventory, final FlowTerminalPart host) {
        super(type, id, inventory, host);
        this.entity = inventory.player;
        this.world = inventory.player.level();
    }

    public void sendMenuStateUpdate(final Player player, final long requestId, final MenuState state) {
        final MenuStateUpdateMessage message = new MenuStateUpdateMessage(this.containerId, requestId, state);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        } else if (player.level().isClientSide) {
            PacketDistributor.sendToServer(message);
        }
    }

    public SortDir getSortingDirection() {
        return this.getConfigManager().getSetting(Settings.SORT_DIRECTION);
    }

    public void setSortingDirection(final SortDir value) {
        this.setSetting(Settings.SORT_DIRECTION, value);
    }

    public SortOrder getSortingType() {
        return this.getConfigManager().getSetting(Settings.SORT_BY);
    }

    public void setSortingType(final SortOrder value) {
        this.setSetting(Settings.SORT_BY, value);
    }

    public Granularity getGranularity() {
        return this.getConfigManager().getSetting(FlowSettings.GRANULARITY);
    }

    public void setGranularity(final Granularity value) {
        this.setSetting(FlowSettings.GRANULARITY, value);
    }

    public LineStyle getLineStyle() {
        return this.getConfigManager().getSetting(FlowSettings.LINE_STYLE);
    }

    public void setLineStyle(final LineStyle value) {
        this.setSetting(FlowSettings.LINE_STYLE, value);
    }

    public ResourceView getResourceView() {
        return this.getConfigManager().getSetting(FlowSettings.RESOURCE_VIEW);
    }

    public void setResourceView(final ResourceView value) {
        this.setSetting(FlowSettings.RESOURCE_VIEW, value);
    }
}
