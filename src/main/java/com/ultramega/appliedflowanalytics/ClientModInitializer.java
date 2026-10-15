package com.ultramega.appliedflowanalytics;

import com.ultramega.appliedflowanalytics.container.FlowLevelEmitterContainerMenu;
import com.ultramega.appliedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.appliedflowanalytics.part.FlowMonitorPart;
import com.ultramega.appliedflowanalytics.screen.FlowLevelEmitterScreen;
import com.ultramega.appliedflowanalytics.screen.FlowMonitorRenderer;
import com.ultramega.appliedflowanalytics.screen.FlowMonitorScreen;
import com.ultramega.appliedflowanalytics.screen.FlowTerminalScreen;

import appeng.client.InitScreens;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class ClientModInitializer {
    private ClientModInitializer() {
    }

    public static void register(final IEventBus eventBus) {
        eventBus.addListener(ClientModInitializer::registerPayloads);
        eventBus.addListener(ClientModInitializer::registerScreens);
        eventBus.addListener(ClientModInitializer::registerPartRenderers);
    }

    private static void registerPayloads(final RegisterClientPayloadHandlersEvent event) {
        event.register(MenuStateUpdateMessage.TYPE, (message, context) -> handleMenuState(message));
    }

    private static void registerPartRenderers(final RegisterPartRendererEvent event) {
        event.register(FlowMonitorPart.class, FlowMonitorRenderer.INSTANCE);
    }

    private static void registerScreens(final RegisterMenuScreensEvent event) {
        InitScreens.register(event, FlowTerminalContainerMenu.TYPE, FlowTerminalScreen::new, "/screens/appliedflowanalytics/flow_terminal.json");
        InitScreens.register(event, FlowMonitorContainerMenu.TYPE, FlowMonitorScreen::new, "/screens/appliedflowanalytics/flow_monitor.json");
        InitScreens.register(event, FlowLevelEmitterContainerMenu.TYPE, FlowLevelEmitterScreen::new, "/screens/appliedflowanalytics/flow_level_emitter.json");
    }

    private static void handleMenuState(final MenuStateUpdateMessage message) {
        final var minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu.containerId == message.containerId()
            && minecraft.screen instanceof FlowTerminalScreen screen) {
            screen.updateMenuState(message.requestId(), message.state());
        }
    }

}
