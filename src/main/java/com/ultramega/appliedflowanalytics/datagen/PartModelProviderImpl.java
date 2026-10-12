package com.ultramega.appliedflowanalytics.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class PartModelProviderImpl extends ModelProvider<BlockModelBuilder> {
    public PartModelProviderImpl(final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, MOD_ID, "part", BlockModelBuilder::new, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.registerTerminalModel("off");
        this.registerTerminalModel("on");
        this.registerStorageMonitorModel("off");
        this.registerStorageMonitorModel("on");
        this.registerLevelEmitterModel("off");
        this.registerLevelEmitterModel("on");
    }

    private void registerTerminalModel(final String state) {
        final BlockModelBuilder model = "off".equals(state)
            ? this.withExistingParent("flow_terminal_off", ResourceLocation.fromNamespaceAndPath("ae2", "part/display_off"))
            : this.getBuilder("flow_terminal_on");
        model.texture("lightsBright", createFlowAnalyticsIdentifier("part/flow_terminal_bright"))
            .texture("lightsMedium", createFlowAnalyticsIdentifier("part/flow_terminal_medium"))
            .texture("lightsDark", createFlowAnalyticsIdentifier("part/flow_terminal_dark"));
        if ("on".equals(state)) {
            this.registerDisplayLayer(model, "lightsBright", 3);
            this.registerDisplayLayer(model, "lightsMedium", 2);
            this.registerDisplayLayer(model, "lightsDark", 1);
        }
    }

    private void registerDisplayLayer(final BlockModelBuilder model, final String texture, final int tintIndex) {
        model.element()
            .from(2, 2, 0)
            .to(14, 14, 2)
            .face(Direction.NORTH)
            .texture("#" + texture)
            .tintindex(tintIndex)
            .emissivity(15, 15);
    }

    private void registerStorageMonitorModel(final String state) {
        this.withExistingParent("flow_monitor_" + state, ResourceLocation.fromNamespaceAndPath("ae2", "part/storage_monitor_" + state));
    }

    private void registerLevelEmitterModel(final String state) {
        final ResourceLocation texture = createFlowAnalyticsIdentifier("part/flow_level_emitter");
        this.withExistingParent("flow_level_emitter_" + state, ResourceLocation.fromNamespaceAndPath("ae2", "part/level_emitter_base_" + state))
            .texture("emitter", texture)
            .texture("particle", texture);
    }

    @Override
    public String getName() {
        return MOD_ID + " Part Models";
    }
}
