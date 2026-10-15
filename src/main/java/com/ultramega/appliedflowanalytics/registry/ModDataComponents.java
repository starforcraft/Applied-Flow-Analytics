package com.ultramega.appliedflowanalytics.registry;

import java.util.UUID;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> FLOW_THRESHOLD = COMPONENTS.register("flow_threshold",
        () -> DataComponentType.<Double>builder().persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> HISTORY_ID = COMPONENTS.register("history_id",
        () -> DataComponentType.<UUID>builder().persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
            .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString)).build());

    private ModDataComponents() {
    }

    public static void register(final IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
