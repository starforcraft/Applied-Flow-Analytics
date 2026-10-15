package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.widgets.IconButton;
import appeng.util.Icon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

abstract class FlowSideButtonWidget<T extends Enum<T>> extends IconButton {
    private final String translationKey;
    private final Supplier<T> getter;
    private final Function<T, Identifier> sprite;

    protected FlowSideButtonWidget(final String translationKey,
                                   final Supplier<T> getter,
                                   final Consumer<T> setter,
                                   final UnaryOperator<T> next,
                                   @Nullable final UnaryOperator<T> previous,
                                   final Function<T, Identifier> sprite) {
        super(button -> setter.accept(cycle(getter.get(), next, previous)));
        this.translationKey = "flow." + translationKey;
        this.getter = getter;
        this.sprite = sprite;
        this.setMessage(this.getTitle());
    }

    protected Identifier getSprite() {
        return this.sprite.apply(this.getter.get());
    }

    protected MutableComponent getTitle() {
        return createFlowAnalyticsTranslation("gui", this.translationKey);
    }

    protected List<MutableComponent> getSubText() {
        return List.of(createFlowAnalyticsTranslation("gui", this.translationKey + "." + this.getter.get().name().toLowerCase(Locale.ROOT))
            .withStyle(ChatFormatting.GRAY));
    }

    @Override
    @Nullable
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractContents(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().pushMatrix();
        graphics.nextStratum();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY() + 1 + (this.isHovered() ? 1 : 0), 16, 16);
        graphics.pose().popMatrix();
    }

    @Override
    public List<Component> getTooltipMessage() {
        final List<Component> lines = new ArrayList<>();
        lines.add(this.getTitle());
        lines.addAll(this.getSubText());
        return lines;
    }

    private static <T> T cycle(final T current, final UnaryOperator<T> next, @Nullable final UnaryOperator<T> previous) {
        if (previous != null && Minecraft.getInstance().screen instanceof AEBaseScreen<?> screen && screen.isHandlingRightClick()) {
            return previous.apply(current);
        }
        return next.apply(current);
    }
}
