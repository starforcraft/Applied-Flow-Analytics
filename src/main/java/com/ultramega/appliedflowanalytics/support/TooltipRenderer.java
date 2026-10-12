package com.ultramega.appliedflowanalytics.support;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TooltipRenderer {
    private TooltipRenderer() {
    }

    public static void renderTooltip(final GuiGraphics graphics, final List<Component> lines, final int x, final int y) {
        graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, x, y);
    }
}
