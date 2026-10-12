package com.ultramega.appliedflowanalytics.screen;

import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.DetectorModeSideButtonWidget;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.MonitorDisplaySideButtonWidget;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FlowLevelEmitterScreen extends AEBaseScreen<FlowLevelEmitterContainerMenu> {
    private static final int RATE_PRECISION = 1_000_000;
    private final NumberEntryWidget amount;

    public FlowLevelEmitterScreen(final FlowLevelEmitterContainerMenu menu,
                                  final Inventory inventory,
                                  final Component title,
                                  final ScreenStyle style) {
        super(menu, inventory, title, style);

        this.setTextContent(TEXT_ID_DIALOG_TITLE, title);
        this.amount = this.widgets.addNumberEntryWidget("level", this.getNumberEntryType());
        this.amount.setTextFieldStyle(style.getWidget("levelInput"));
        this.amount.setMinValue(Long.MIN_VALUE);
        this.amount.setLongValue(Math.round(this.menu.getAmount() * RATE_PRECISION));
        this.amount.setOnChange(this::applyAmount);
        this.amount.setOnConfirm(this::onClose);

        this.addToLeftToolbar(new DetectorModeSideButtonWidget(this.menu));
        this.addToLeftToolbar(new MonitorDisplaySideButtonWidget<>(
            "detector_direction", this.menu,
            FlowSettings.FLOW_DIRECTION,
            FlowDirection.values(),
            FlowDirection::getSprite
        ));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        final NumberEntryType type = this.getNumberEntryType();
        if (!type.equals(this.amount.getType())) {
            this.amount.setType(type);
        }
    }

    private NumberEntryType getNumberEntryType() {
        final var resource = this.menu.getHost().getFilter().getKey(0);
        final String unit = resource == null ? null : resource.getUnitSymbol();
        return new NumberEntryType(RATE_PRECISION, unit == null ? "/s" : unit + "/s");
    }

    private void applyAmount() {
        this.amount.getLongValue().ifPresent(value -> this.menu.setAmount(value / (double) RATE_PRECISION));
    }
}
