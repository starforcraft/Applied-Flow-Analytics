package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.localization.ButtonToolTips;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public final class DetectorModeSideButtonWidget extends SettingToggleButton<RedstoneMode> {
    private final FlowLevelEmitterContainerMenu menu;

    public DetectorModeSideButtonWidget(final FlowLevelEmitterContainerMenu menu) {
        super(Settings.REDSTONE_EMITTER, toRedstoneMode(menu.getConfigManager().getSetting(FlowSettings.MODE)),
            (button, backwards) -> menu.setSetting(FlowSettings.MODE, button.getNextValue(backwards) == RedstoneMode.HIGH_SIGNAL
                ? DetectorMode.ABOVE_OR_EQUAL : DetectorMode.BELOW));
        this.menu = menu;
    }

    private static RedstoneMode toRedstoneMode(final DetectorMode mode) {
        return mode == DetectorMode.ABOVE_OR_EQUAL ? RedstoneMode.HIGH_SIGNAL : RedstoneMode.LOW_SIGNAL;
    }

    @Override
    public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.set(toRedstoneMode(this.menu.getConfigManager().getSetting(FlowSettings.MODE)));
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(ButtonToolTips.RedstoneMode.text(), createFlowAnalyticsTranslation("gui",
            "flow.detector_mode." + this.menu.getConfigManager().getSetting(FlowSettings.MODE).name().toLowerCase(Locale.ROOT)));
    }
}
