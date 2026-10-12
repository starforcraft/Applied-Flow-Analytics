package com.ultramega.appliedflowanalytics.network;

import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.service.SnapshotService;

import java.util.Objects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public record MenuStateUpdateMessage(int containerId, long requestId, MenuState state) implements CustomPacketPayload {
    public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(createFlowAnalyticsIdentifier("guistate_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MenuStateUpdateMessage::containerId,
        ByteBufCodecs.VAR_LONG, MenuStateUpdateMessage::requestId,
        MenuState.STREAM_CODEC, MenuStateUpdateMessage::state,
        MenuStateUpdateMessage::new
    );

    public MenuStateUpdateMessage {
        Objects.requireNonNull(state);
    }

    public void handleServer(final ServerPlayer player) {
        if (!(player.containerMenu instanceof FlowTerminalContainerMenu menu)
            || menu.containerId != this.containerId || !menu.stillValid(player)) {
            return;
        }
        switch (this.state) {
            case MenuState.SnapshotRequest request -> SnapshotService.execute(player, menu, this.requestId, request.granularity(), request.allStored());
            case MenuState.DetailedRequest request -> SnapshotService.executeDetailed(player, menu, this.requestId, request.resource(), request.granularity());
            default -> {
                // Responses are only accepted by the client
            }
        }
    }

    @Override
    public Type<MenuStateUpdateMessage> type() {
        return TYPE;
    }
}
