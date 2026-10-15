package com.ultramega.appliedflowanalytics.registry;

import com.ultramega.appliedflowanalytics.part.FlowLevelEmitterPart;
import com.ultramega.appliedflowanalytics.part.FlowMonitorPart;
import com.ultramega.appliedflowanalytics.part.FlowTerminalPart;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.items.parts.PartItem;

import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<Item, PartItem<FlowTerminalPart>> FLOW_TERMINAL =
        register("flow_terminal", FlowTerminalPart.class, FlowTerminalPart::new);
    public static final DeferredHolder<Item, PartItem<FlowMonitorPart>> FLOW_MONITOR =
        register("flow_monitor", FlowMonitorPart.class, FlowMonitorPart::new);
    public static final DeferredHolder<Item, PartItem<FlowLevelEmitterPart>> FLOW_LEVEL_EMITTER =
        register("flow_level_emitter", FlowLevelEmitterPart.class, FlowLevelEmitterPart::new);

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

    private static <P extends IPart> DeferredHolder<Item, PartItem<P>> register(final String name, final Class<P> partClass, final Function<IPartItem<P>, P> factory) {
        return ITEMS.register(name, () -> new PartItem<>(
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, createFlowAnalyticsIdentifier(name))),
            partClass,
            factory
        ));
    }

    public static void register(final IEventBus eventBus) {
        ITEMS.register(eventBus);
        TABS.register(eventBus);
    }
}
