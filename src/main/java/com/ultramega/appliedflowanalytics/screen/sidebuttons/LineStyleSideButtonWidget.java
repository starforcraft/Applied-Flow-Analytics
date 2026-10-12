package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import com.ultramega.appliedflowanalytics.container.AbstractFlowPartMenu;
import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import static com.ultramega.appliedflowanalytics.part.FlowMonitorPart.LINE_STYLE_TAG;

public final class LineStyleSideButtonWidget extends FlowSideButtonWidget<LineStyle> {
    public LineStyleSideButtonWidget(final AbstractFlowPartMenu<?> menu) {
        super(LINE_STYLE_TAG, () -> menu.getConfigManager().getSetting(FlowSettings.LINE_STYLE),
            value -> menu.setSetting(FlowSettings.LINE_STYLE, value), LineStyle::next, null, LineStyle::getSprite);
    }

    public LineStyleSideButtonWidget(final FlowTerminalContainerMenu menu) {
        super(LINE_STYLE_TAG, menu::getLineStyle, menu::setLineStyle, LineStyle::next, null, LineStyle::getSprite);
    }
}
