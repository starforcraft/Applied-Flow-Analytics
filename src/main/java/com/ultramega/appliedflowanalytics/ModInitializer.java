package com.ultramega.appliedflowanalytics;

import com.ultramega.appliedflowanalytics.config.ServerConfigImpl;
import com.ultramega.appliedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.appliedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.appliedflowanalytics.registry.ModDataComponents;
import com.ultramega.appliedflowanalytics.registry.ModItems;
import com.ultramega.appliedflowanalytics.registry.ModMenus;

import appeng.api.networking.GridServices;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

@Mod(MOD_ID)
public final class ModInitializer {
    public ModInitializer(final IEventBus eventBus, final ModContainer modContainer) {
        final ServerConfigImpl serverConfig = new ServerConfigImpl();
        modContainer.registerConfig(ModConfig.Type.SERVER, serverConfig.getSpec());
        Platform.setServerConfigProvider(() -> serverConfig);

        ModDataComponents.register(eventBus);
        ModItems.register(eventBus);
        ModMenus.register(eventBus);

        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener((final ServerTickEvent.Post event) -> AppliedFlowAnalyticsMod.tick());
        NeoForge.EVENT_BUS.addListener((final ServerStoppedEvent event) -> AppliedFlowAnalyticsMod.serverStopped());

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientModInitializer.register(eventBus);
        }
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> GridServices.register(FlowHistoryNetworkComponent.class, FlowHistoryNetworkComponent.class));
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1").executesOn(HandlerThread.MAIN);

        registrar.playBidirectional(
            MenuStateUpdateMessage.TYPE,
            MenuStateUpdateMessage.STREAM_CODEC,
            (message, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    message.handleServer(player);
                }
            });
    }
}
