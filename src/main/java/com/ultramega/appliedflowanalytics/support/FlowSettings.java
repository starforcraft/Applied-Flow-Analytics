package com.ultramega.appliedflowanalytics.support;

import com.ultramega.appliedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.MonitorItemVisibility;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.ResourceView;

import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.util.IConfigManager;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

public final class FlowSettings {
    private static final Map<String, Setting<?>> SETTINGS = new HashMap<>();

    public static final Setting<Granularity> GRANULARITY = register("granularity", Granularity.class);
    public static final Setting<LineStyle> LINE_STYLE = register("line_style", LineStyle.class);
    public static final Setting<ResourceView> RESOURCE_VIEW = register("resource_view", ResourceView.class);

    public static final Setting<MonitorItemVisibility> ITEM_VISIBILITY = register("item_visibility", MonitorItemVisibility.class);
    public static final Setting<FlowDirection> FLOW_TEXT = register("flow_text", FlowDirection.class);
    public static final Setting<FlowDirection> FLOW_DIRECTION = register("flow_direction", FlowDirection.class);

    private FlowSettings() {
    }

    private static synchronized <T extends Enum<T>> Setting<T> register(final String name, final Class<T> enumClass) {
        Preconditions.checkState(!SETTINGS.containsKey(name));
        final Setting<T> setting = new Setting<>(name, enumClass);
        SETTINGS.put(name, setting);
        return setting;
    }

    public static IConfigManager createConfigManager(final Runnable onChange) {
        return IConfigManager.builder(onChange)
            .registerSetting(Settings.SORT_DIRECTION, SortDir.DESCENDING)
            .registerSetting(Settings.SORT_BY, SortOrder.AMOUNT)
            .registerSetting(GRANULARITY, Granularity.SECOND)
            .registerSetting(LINE_STYLE, LineStyle.EXACT)
            .registerSetting(RESOURCE_VIEW, ResourceView.CHANGED)
            .build();
    }
}
