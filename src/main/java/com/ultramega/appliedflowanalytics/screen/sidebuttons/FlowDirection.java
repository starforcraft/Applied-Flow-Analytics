package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import appeng.api.config.RedstoneMode;

import java.math.BigDecimal;
import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum FlowDirection {
    NET(0xff66ddff),
    INFLOW(0xff63cf78),
    OUTFLOW(0xffef6b73);

    private final int color;

    FlowDirection(final int color) {
        this.color = color;
    }

    public ResourceLocation getSprite() {
        return createFlowAnalyticsIdentifier("widget/side_button/flow_text/" + this.toString().toLowerCase(Locale.ROOT));
    }

    public int getColor() {
        return this.color;
    }

    public long getValue(final long inflow, final long outflow) {
        return switch (this) {
            case NET -> inflow - outflow;
            case INFLOW -> inflow;
            case OUTFLOW -> -outflow;
        };
    }

    public long getComparisonValue(final long inflow, final long outflow) {
        return switch (this) {
            case NET -> inflow - outflow;
            case INFLOW -> inflow;
            case OUTFLOW -> outflow;
        };
    }

    public boolean matches(final long inflow, final long outflow, final BigDecimal threshold, final RedstoneMode mode) {
        final int comparison = BigDecimal.valueOf(this.getComparisonValue(inflow, outflow)).compareTo(threshold);
        return switch (mode) {
            case HIGH_SIGNAL -> comparison >= 0;
            case LOW_SIGNAL -> comparison < 0;
            default -> throw new IllegalArgumentException("Unsupported emitter mode: " + mode);
        };
    }
}
