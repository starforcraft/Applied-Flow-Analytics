package com.ultramega.appliedflowanalytics.mixin;

import java.io.File;
import java.util.Map;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DimensionDataStorage.class)
public interface DimensionDataStorageAccessor {
    @Accessor("cache")
    Map<String, SavedData> appliedflowanalytics$getCache();

    @Invoker("getDataFile")
    File appliedflowanalytics$getDataFile(String name);
}
