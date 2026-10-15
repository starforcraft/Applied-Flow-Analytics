package com.ultramega.appliedflowanalytics.screen.components;

import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.support.ResourceRendering;
import com.ultramega.appliedflowanalytics.support.TooltipRenderer;

import appeng.api.stacks.AEKey;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public class FlowItemButton {
    private static final WidgetSprites DEFAULT_SPRITES = new WidgetSprites(
        createFlowAnalyticsIdentifier("widget/button"),
        createFlowAnalyticsIdentifier("widget/button"),
        createFlowAnalyticsIdentifier("widget/button_highlighted")
    );

    public final Runnable onClick;
    @Nullable
    private final Identifier overlay;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final String label;
    private final Font font;
    private final List<Component> tooltipLines;
    @Nullable
    private ResourceContent resourceContent;

    public FlowItemButton(final int x,
                          final int y,
                          final int width,
                          final int height,
                          final AEKey resourceKey,
                          final Map<Short, Long> change,
                          final Granularity granularity,
                          final Runnable onClick,
                          final List<Component> lines) {
        this(x, y, width, height, "", onClick, lines);
        final long net = change.getOrDefault((short) +1, 0L) - change.getOrDefault((short) -1, 0L);
        this.resourceContent = new ResourceContent(resourceKey, net, granularity);
    }

    public FlowItemButton(final int x,
                          final int y,
                          final int width,
                          final int height,
                          final String label,
                          final Runnable onClick,
                          final List<Component> lines) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.font = Minecraft.getInstance().font;
        this.overlay = null;
        this.tooltipLines = lines;
    }

    public void render(final GuiGraphicsExtractor graphics, final boolean hovered) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DEFAULT_SPRITES.get(true, hovered), this.x, this.y, this.width, this.height);
        if (this.resourceContent == null) {
            graphics.text(this.font, this.label, this.x + 5, this.y + 6, 0xFFFFFF);
        }
        if (this.overlay != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.overlay, this.x, this.y, this.width, this.height);
        }
        if (this.resourceContent != null) {
            this.renderResourceContent(graphics, this.resourceContent, hovered);
        }
    }

    private void renderResourceContent(final GuiGraphicsExtractor graphics, final ResourceContent content, final boolean hovered) {
        final ResourceRendering resourceRendering = new ResourceRendering(content.resourceKey());
        final int yOffset = hovered ? 1 : 0;
        final int left = this.x + 5;
        final int top = this.y + 3 + yOffset;
        resourceRendering.render(content.resourceKey(), graphics, left, top);

        final long net = content.net();
        final String amount = (net >= 0 ? "+" : "") + resourceRendering.formatAmount(net, true);
        graphics.text(this.font, amount + content.granularity().perStr(), left + 20, top + 5, net > 0 ? 0xff00ff00 : net < 0 ? 0xffff0000 : 0xffffffff);

        // TODO: decide if we want to show inflow/outflow too
        /*graphics.text(font, "+" + ItemResourceRendering.INSTANCE.formatAmount(itemChange.get((short) +1), true) + granularity.perStr(),
            left + 20, top, PRODUCTION_GREEN);
        graphics.text(font, "-" + ItemResourceRendering.INSTANCE.formatAmount(itemChange.get((short) -1), true) + granularity.perStr(),
            left + 20, top + 10, CONSUMPTION_RED);*/
    }

    public void renderTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            TooltipRenderer.renderTooltip(graphics, this.tooltipLines, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (button == 0) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.onClick.run();
                return true;
            }
        }
        return false;
    }

    public boolean isMouseOver(final double mx, final double my) {
        return mx >= this.x && mx < this.x + this.width && my >= this.y && my < this.y + this.height;
    }

    private record ResourceContent(AEKey resourceKey, long net, Granularity granularity) {
    }
}
