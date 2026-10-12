package com.ultramega.appliedflowanalytics.screen;

import com.ultramega.appliedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.appliedflowanalytics.part.FlowMonitorPart;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.GranularitySideButtonWidget;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyleSideButtonWidget;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.MonitorDisplaySideButtonWidget;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.MonitorItemVisibility;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.SettingToggleButton;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FlowMonitorScreen extends AEBaseScreen<FlowMonitorContainerMenu> {
    private final SettingToggleButton<RedstoneMode> redstoneModeButton;

    public FlowMonitorScreen(final FlowMonitorContainerMenu menu,
                             final Inventory playerInventory,
                             final Component title, final ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.setTextContent(TEXT_ID_DIALOG_TITLE, title);

        this.redstoneModeButton = this.addToLeftToolbar(new SettingToggleButton<>(Settings.REDSTONE_CONTROLLED,
            menu.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED), value -> value != RedstoneMode.SIGNAL_PULSE,
            (button, backwards) -> menu.setSetting(Settings.REDSTONE_CONTROLLED, button.getNextValue(backwards))
        ));
        this.addToLeftToolbar(new MonitorDisplaySideButtonWidget<>(FlowMonitorPart.ITEM_VISIBILITY_TAG,
            this.menu, FlowSettings.ITEM_VISIBILITY,
            MonitorItemVisibility.values(),
            MonitorItemVisibility::getSprite
        ));
        this.addToLeftToolbar(new MonitorDisplaySideButtonWidget<>(FlowMonitorPart.FLOW_TEXT_TAG,
            this.menu, FlowSettings.FLOW_TEXT,
            FlowDirection.values(),
            FlowDirection::getSprite
        ));
        this.addToLeftToolbar(new GranularitySideButtonWidget(
            this.menu
        ));
        this.addToLeftToolbar(new LineStyleSideButtonWidget(
            this.menu
        ));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.redstoneModeButton.set(this.menu.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED));
    }
}
