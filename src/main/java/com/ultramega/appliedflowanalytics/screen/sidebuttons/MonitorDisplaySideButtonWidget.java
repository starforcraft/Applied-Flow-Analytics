package com.ultramega.appliedflowanalytics.screen.sidebuttons;

import com.ultramega.appliedflowanalytics.container.AbstractFlowPartMenu;

import appeng.api.config.Setting;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

public final class MonitorDisplaySideButtonWidget<T extends Enum<T>> extends FlowSideButtonWidget<T> {
    public MonitorDisplaySideButtonWidget(final String translation,
                                          final AbstractFlowPartMenu<?> menu,
                                          final Setting<T> setting,
                                          final T[] values,
                                          final Function<T, ResourceLocation> sprite) {
        super(translation, () -> menu.getConfigManager().getSetting(setting), value -> menu.setSetting(setting, value),
            current -> values[(current.ordinal() + 1) % values.length],
            current -> values[(current.ordinal() + values.length - 1) % values.length], sprite);
    }
}
