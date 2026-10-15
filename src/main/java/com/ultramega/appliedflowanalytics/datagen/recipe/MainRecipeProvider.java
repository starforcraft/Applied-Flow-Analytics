package com.ultramega.appliedflowanalytics.datagen.recipe;

import com.ultramega.appliedflowanalytics.registry.ModItems;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class MainRecipeProvider extends RecipeProvider {
    public MainRecipeProvider(final HolderLookup.Provider lookupProvider, final RecipeOutput output) {
        super(lookupProvider, output);
    }

    @Override
    protected void buildRecipes() {
        this.registerFlowPart(this.output, "flow_terminal", ModItems.FLOW_TERMINAL.get(), AEParts.TERMINAL);
        this.registerFlowPart(this.output, "flow_monitor", ModItems.FLOW_MONITOR.get(), AEParts.STORAGE_MONITOR);
        this.registerFlowPart(this.output, "flow_level_emitter", ModItems.FLOW_LEVEL_EMITTER.get(), AEParts.LEVEL_EMITTER);
    }

    private void registerFlowPart(final RecipeOutput output, final String name, final ItemLike result, final ItemLike counterpart) {
        ShapelessRecipeBuilder.shapeless(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.MISC, result)
            .requires(counterpart)
            .requires(AEItems.CALCULATION_PROCESSOR)
            .requires(Items.COMPARATOR)
            .unlockedBy("has_counterpart", this.has(counterpart))
            .save(output, ResourceKey.create(Registries.RECIPE, createFlowAnalyticsIdentifier(name)));
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider lookupProvider, final RecipeOutput output) {
            return new MainRecipeProvider(lookupProvider, output);
        }

        @Override
        public String getName() {
            return "Applied Flow Analytics Recipes";
        }
    }
}
