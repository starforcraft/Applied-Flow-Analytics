package com.ultramega.appliedflowanalytics.registry;

import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MOD_ID);

    static {
        MENUS.register("flow_terminal", () -> FlowTerminalContainerMenu.TYPE);
        MENUS.register("flow_monitor", () -> FlowMonitorContainerMenu.TYPE);
        MENUS.register("flow_level_emitter", () -> FlowLevelEmitterContainerMenu.TYPE);
    }

    private ModMenus() {
    }

    public static void register(final IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
