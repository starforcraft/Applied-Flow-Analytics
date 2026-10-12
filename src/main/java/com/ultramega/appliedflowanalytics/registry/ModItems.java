package com.ultramega.appliedflowanalytics.registry;

import com.ultramega.appliedflowanalytics.part.FlowLevelEmitterPart;
import com.ultramega.appliedflowanalytics.part.FlowMonitorPart;
import com.ultramega.appliedflowanalytics.part.FlowTerminalPart;

import appeng.items.parts.PartItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<Item, PartItem<FlowTerminalPart>> FLOW_TERMINAL = ITEMS.register("flow_terminal",
        () -> new PartItem<>(new Item.Properties(), FlowTerminalPart.class, FlowTerminalPart::new));
    public static final DeferredHolder<Item, PartItem<FlowMonitorPart>> FLOW_MONITOR = ITEMS.register("flow_monitor",
        () -> new PartItem<>(new Item.Properties(), FlowMonitorPart.class, FlowMonitorPart::new));
    public static final DeferredHolder<Item, PartItem<FlowLevelEmitterPart>> FLOW_LEVEL_EMITTER = ITEMS.register("flow_level_emitter",
        () -> new PartItem<>(new Item.Properties(), FlowLevelEmitterPart.class, FlowLevelEmitterPart::new));

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.appliedflowanalytics"))
            .icon(() -> new ItemStack(FLOW_TERMINAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(FLOW_TERMINAL.get());
                output.accept(FLOW_MONITOR.get());
                output.accept(FLOW_LEVEL_EMITTER.get());
            }).build());
    }

    private ModItems() {
    }

    public static void register(final IEventBus eventBus) {
        ITEMS.register(eventBus);
        TABS.register(eventBus);
    }
}
