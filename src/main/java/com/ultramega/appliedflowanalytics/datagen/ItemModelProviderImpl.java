package com.ultramega.appliedflowanalytics.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ItemModelProviderImpl extends ItemModelProvider {
    public ItemModelProviderImpl(final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.registerDisplayModel("flow_terminal");
        this.withExistingParent("flow_monitor", ResourceLocation.fromNamespaceAndPath("ae2", "item/storage_monitor"));
        this.withExistingParent("flow_level_emitter", ResourceLocation.fromNamespaceAndPath("ae2", "item/level_emitter"))
            .texture("emitter", createFlowAnalyticsIdentifier("part/flow_level_emitter"));
    }

    private void registerDisplayModel(final String name) {
        this.withExistingParent(name, ResourceLocation.fromNamespaceAndPath("ae2", "item/display_base"))
            .texture("front_bright", createFlowAnalyticsIdentifier("part/" + name + "_bright"))
            .texture("front_medium", createFlowAnalyticsIdentifier("part/" + name + "_medium"))
            .texture("front_dark", createFlowAnalyticsIdentifier("part/" + name + "_dark"));
    }
}
