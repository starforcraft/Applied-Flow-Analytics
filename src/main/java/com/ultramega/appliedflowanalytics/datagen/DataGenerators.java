package com.ultramega.appliedflowanalytics.datagen;

import com.ultramega.appliedflowanalytics.datagen.recipe.MainRecipeProvider;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent event) {
        final var generator = event.getGenerator();
        final var output = generator.getPackOutput();
        final var existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new PartModelProviderImpl(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ItemModelProviderImpl(output, existingFileHelper));
        generator.addProvider(event.includeServer(), new MainRecipeProvider(output, event.getLookupProvider()));
    }
}
