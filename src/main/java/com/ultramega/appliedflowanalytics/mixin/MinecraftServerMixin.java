package com.ultramega.appliedflowanalytics.mixin;

import com.ultramega.appliedflowanalytics.data.FlowSnapshotData;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void appliedflowanalytics$flushHistory(final boolean suppressLog,
                                                   final boolean flush,
                                                   final boolean forced,
                                                   final CallbackInfoReturnable<Boolean> callback) {
        if (flush) {
            FlowSnapshotData.flushPendingSaves();
        }
    }
}
