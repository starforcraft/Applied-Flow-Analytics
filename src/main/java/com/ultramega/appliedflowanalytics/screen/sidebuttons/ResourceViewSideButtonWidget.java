package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;

public final class ResourceViewSideButtonWidget extends FlowSideButtonWidget<ResourceView> {
    public ResourceViewSideButtonWidget(final FlowTerminalContainerMenu menu, final Runnable onChange) {
        super("resource_view", menu::getResourceView, value -> {
            menu.setResourceView(value);
            onChange.run();
        }, ResourceView::next, ResourceView::next, ResourceView::getSprite);
    }
}
