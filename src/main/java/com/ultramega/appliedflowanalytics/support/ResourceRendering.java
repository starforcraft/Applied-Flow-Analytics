package com.ultramega.appliedflowanalytics.support;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.client.api.AEKeyRendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public record ResourceRendering(AEKey resource) {
    public Component getDisplayName(final AEKey key) {
        return key.getDisplayName();
    }

    public String formatAmount(final long amount, final boolean compact) {
        if (!compact || amount >= 0) {
            return this.resource.formatAmount(amount, compact ? AmountFormat.SLOT : AmountFormat.FULL);
        }
        return "-" + this.resource.formatAmount(amount == Long.MIN_VALUE ? Long.MAX_VALUE : -amount, AmountFormat.SLOT);
    }

    public String formatAmount(final long amount) {
        return this.formatAmount(amount, true);
    }

    public void render(final AEKey key, final GuiGraphicsExtractor graphics, final int x, final int y) {
        AEKeyRendering.drawInGui(Minecraft.getInstance(), graphics, x, y, key);
    }
}
