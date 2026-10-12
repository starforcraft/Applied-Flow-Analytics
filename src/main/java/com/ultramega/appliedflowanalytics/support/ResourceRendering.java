package com.ultramega.appliedflowanalytics.support;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public record ResourceRendering(AEKey resource) {
    public Component getDisplayName(final AEKey key) {
        return key.getDisplayName();
    }

    public String formatAmount(final long amount, final boolean compact) {
        if (!compact || amount >= 0) {
            return this.resource.formatAmount(amount, compact ? AmountFormat.SLOT : AmountFormat.FULL);
        }
        return "-" + this.resource.formatAmount(-amount, AmountFormat.SLOT);
    }

    public String formatAmount(final long amount) {
        return this.formatAmount(amount, true);
    }

    public void render(final AEKey key, final GuiGraphics graphics, final int x, final int y) {
        AEKeyRendering.drawInGui(Minecraft.getInstance(), graphics, x, y, key);
    }

    public void render(final AEKey key, final PoseStack pose, final MultiBufferSource buffers, final int light, final Level level) {
        AEKeyRendering.drawOnBlockFace(pose, buffers, key, 1.0f, light, level);
    }
}
