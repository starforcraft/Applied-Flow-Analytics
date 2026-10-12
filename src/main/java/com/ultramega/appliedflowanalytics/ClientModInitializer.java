package com.ultramega.appliedflowanalytics;

import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.appliedflowanalytics.registry.ModItems;
import com.ultramega.appliedflowanalytics.screen.FlowLevelEmitterScreen;
import com.ultramega.appliedflowanalytics.screen.FlowMonitorScreen;
import com.ultramega.appliedflowanalytics.screen.FlowTerminalScreen;

import appeng.api.util.AEColor;
import appeng.init.client.InitScreens;

import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientModInitializer {
    private ClientModInitializer() {
    }

    public static void register(final IEventBus eventBus) {
        eventBus.addListener(ClientModInitializer::registerScreens);
        eventBus.addListener(ClientModInitializer::registerItemColors);
    }

    private static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> FastColor.ARGB32.opaque(AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex)),
            ModItems.FLOW_TERMINAL.get(), ModItems.FLOW_MONITOR.get());
    }

    private static void registerScreens(final RegisterMenuScreensEvent event) {
        InitScreens.register(event, FlowTerminalContainerMenu.TYPE, FlowTerminalScreen::new, "/screens/appliedflowanalytics/flow_terminal.json");
        InitScreens.register(event, FlowMonitorContainerMenu.TYPE, FlowMonitorScreen::new, "/screens/appliedflowanalytics/flow_monitor.json");
        InitScreens.register(event, FlowLevelEmitterContainerMenu.TYPE, FlowLevelEmitterScreen::new, "/screens/appliedflowanalytics/flow_level_emitter.json");
    }

    public static void handleMenuState(final MenuStateUpdateMessage message) {
        final var minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu.containerId == message.containerId()
            && minecraft.screen instanceof FlowTerminalScreen screen) {
            screen.updateMenuState(message.requestId(), message.state());
        }
    }

}
