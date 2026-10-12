package com.ultramega.appliedflowanalytics.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> FLOW_SETTINGS = COMPONENTS.register("flow_settings",
        () -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());

    private ModDataComponents() {
    }

    public static void register(final IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
