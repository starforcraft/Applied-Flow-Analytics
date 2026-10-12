package com.ultramega.appliedflowanalytics.mixin;

import com.ultramega.appliedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.appliedflowanalytics.network.FlowStorageAccess;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkStorage.class, remap = false)
public abstract class NetworkStorageMixin implements FlowStorageAccess {
    @Unique
    @Nullable
    private FlowHistoryNetworkComponent flowanalytics$history;

    @Override
    public void flowAnalytics$setFlowHistory(final FlowHistoryNetworkComponent component) {
        this.flowanalytics$history = component;
    }

    @Inject(method = "insert", at = @At("RETURN"))
    private void flowanalytics$insert(final AEKey what,
                                      final long amount,
                                      final Actionable type,
                                      final IActionSource src,
                                      final CallbackInfoReturnable<Long> result) {
        if (type == Actionable.MODULATE && result.getReturnValueJ() > 0 && this.flowanalytics$history != null) {
            this.flowanalytics$history.recordChange(what, result.getReturnValueJ());
        }
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void flowanalytics$extract(final AEKey what,
                                       final long amount,
                                       final Actionable mode,
                                       final IActionSource source,
                                       final CallbackInfoReturnable<Long> result) {
        if (mode == Actionable.MODULATE && result.getReturnValueJ() > 0 && this.flowanalytics$history != null) {
            this.flowanalytics$history.recordChange(what, -result.getReturnValueJ());
        }
    }
}
