package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum MonitorItemVisibility {
    SHOW,
    HIDE;

    public ResourceLocation getSprite() {
        return createFlowAnalyticsIdentifier("widget/side_button/item_visibility/" + this.toString().toLowerCase(Locale.ROOT));
    }
}
