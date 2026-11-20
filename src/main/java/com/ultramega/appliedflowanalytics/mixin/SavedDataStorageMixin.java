package com.ultramega.appliedflowanalytics.mixin;

import com.ultramega.appliedflowanalytics.data.FlowHistoryStorage;
import com.ultramega.appliedflowanalytics.data.FlowSnapshotData;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

@Mixin(SavedDataStorage.class)
public abstract class SavedDataStorageMixin implements FlowHistoryStorage {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    // Keep histories out of the vanilla cache, which materializes a tag tree before scheduling IO.
    @Unique
    private final Map<Path, FlowSnapshotData> appliedflowanalytics$histories = new HashMap<>();

    @Invoker("getDataFile")
    protected abstract Path appliedflowanalytics$getDataFile(Identifier id);

    @Override
    public FlowSnapshotData appliedflowanalytics$getHistory(final UUID networkId) {
        final Path path = this.appliedflowanalytics$getDataFile(createFlowAnalyticsIdentifier("network_history/" + networkId));
        return this.appliedflowanalytics$histories.computeIfAbsent(path, file -> FlowSnapshotData.open(file, this.registries));
    }

    @Inject(method = "collectDirtyTagsToSave", at = @At("HEAD"))
    private void appliedflowanalytics$scheduleHistorySaves(final CallbackInfoReturnable<?> callback) {
        this.appliedflowanalytics$histories.forEach((path, history) -> history.save(path, this.registries));
    }

    // Both a flushed world save and SavedDataStorage.close() use this method.
    @Inject(method = "saveAndJoin", at = @At("RETURN"))
    private void appliedflowanalytics$awaitHistorySaves(final CallbackInfo callback) {
        if (!this.appliedflowanalytics$histories.isEmpty()) {
            FlowSnapshotData.flushPendingSaves();
        }
    }
}
