package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import com.ultramega.appliedflowanalytics.container.AbstractFlowPartMenu;
import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import static com.ultramega.appliedflowanalytics.part.FlowMonitorPart.GRANULARITY_TAG;

public final class GranularitySideButtonWidget extends FlowSideButtonWidget<Granularity> {
    public GranularitySideButtonWidget(final AbstractFlowPartMenu<?> menu) {
        super(GRANULARITY_TAG, () -> menu.getConfigManager().getSetting(FlowSettings.GRANULARITY),
            value -> menu.setSetting(FlowSettings.GRANULARITY, value), Granularity::next, Granularity::prev, Granularity::getSprite);
    }

    public GranularitySideButtonWidget(final FlowTerminalContainerMenu menu, final Runnable onChange) {
        super(GRANULARITY_TAG, menu::getGranularity, value -> {
            menu.setGranularity(value);
            onChange.run();
        }, Granularity::next, Granularity::prev, Granularity::getSprite);
    }
}
