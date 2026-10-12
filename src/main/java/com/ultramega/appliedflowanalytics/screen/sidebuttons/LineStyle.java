package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum LineStyle {
    BLOCKY,
    EXACT;

    public static LineStyle next(final LineStyle current) {
        return LineStyle.values()[(current.ordinal() + 1) % LineStyle.values().length];
    }

    public ResourceLocation getSprite() {
        return createFlowAnalyticsIdentifier("widget/side_button/line_style/" + this.toString().toLowerCase(Locale.ROOT));
    }
}
