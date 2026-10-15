package com.ultramega.appliedflowanalytics.datagen;

import com.ultramega.appliedflowanalytics.datagen.model.ModelProviders;
import com.ultramega.appliedflowanalytics.datagen.recipe.MainRecipeProvider;

import appeng.datagen.providers.models.AE2ModelProvider;

import net.minecraft.data.DataGenerator.PackGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent.Client event) {
        final PackGenerator pack = event.getGenerator().getVanillaPack(true);
        pack.addProvider(AE2ModelProvider.create(MOD_ID, ModelProviders::new));
        pack.addProvider(output -> new MainRecipeProvider.Runner(output, event.getLookupProvider()));
    }
}
