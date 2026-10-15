package com.ultramega.appliedflowanalytics.datagen.model;

import com.ultramega.appliedflowanalytics.registry.ModItems;

import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;
import appeng.api.util.AEColorVariant;
import appeng.client.api.model.parts.StaticPartModel;
import appeng.client.model.LevelEmitterPartModel;
import appeng.client.model.StatusIndicatorPartModel;
import appeng.client.render.AEColorItemTintSource;
import appeng.datagen.providers.models.ModelSubProvider;
import appeng.datagen.providers.models.PartModelOutput;

import java.util.Optional;

import net.minecraft.client.color.item.Constant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import static appeng.core.AppEng.makeId;
import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ModelProviders extends ModelSubProvider {
    private static final TextureSlot LIGHTS_BRIGHT = TextureSlot.create("lightsBright");
    private static final TextureSlot LIGHTS_MEDIUM = TextureSlot.create("lightsMedium");
    private static final TextureSlot LIGHTS_DARK = TextureSlot.create("lightsDark");
    private static final TextureSlot FRONT_BRIGHT = TextureSlot.create("front_bright");
    private static final TextureSlot FRONT_MEDIUM = TextureSlot.create("front_medium");
    private static final TextureSlot FRONT_DARK = TextureSlot.create("front_dark");
    private static final TextureSlot EMITTER = TextureSlot.create("emitter");

    private static final ModelTemplate TERMINAL_ON = template(
        "part/terminal_on", LIGHTS_BRIGHT, LIGHTS_MEDIUM, LIGHTS_DARK
    );
    private static final ModelTemplate TERMINAL_OFF = template(
        "part/display_off", LIGHTS_BRIGHT, LIGHTS_MEDIUM, LIGHTS_DARK
    );
    private static final ModelTemplate TERMINAL_ITEM = template(
        "item/display_base", FRONT_BRIGHT, FRONT_MEDIUM, FRONT_DARK
    );
    private static final ModelTemplate MONITOR_ON = template("part/storage_monitor_on");
    private static final ModelTemplate MONITOR_OFF = template("part/storage_monitor_off");
    private static final ModelTemplate MONITOR_ITEM = template("item/storage_monitor");
    private static final ModelTemplate EMITTER_ON = template(
        "part/level_emitter_base_on", EMITTER, TextureSlot.PARTICLE
    );
    private static final ModelTemplate EMITTER_OFF = template(
        "part/level_emitter_base_off", EMITTER, TextureSlot.PARTICLE
    );
    private static final ModelTemplate EMITTER_ITEM = template("item/level_emitter", EMITTER);

    public ModelProviders(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels,
                          final PartModelOutput partModels) {
        super(blockModels, itemModels, partModels);
    }

    @Override
    protected void register() {
        this.registerFlowTerminal();
        this.registerFlowMonitor();
        this.registerFlowLevelEmitter();
    }

    private void registerFlowTerminal() {
        final var part = ModItems.FLOW_TERMINAL.get();
        final Identifier model = IPartItem.getId(part).withPrefix("part/");
        final TextureMapping textures = new TextureMapping()
            .put(LIGHTS_BRIGHT, texture("flow_terminal_bright"))
            .put(LIGHTS_MEDIUM, texture("flow_terminal_medium"))
            .put(LIGHTS_DARK, texture("flow_terminal_dark"));
        final Identifier on = TERMINAL_ON.create(model.withSuffix("_on"), textures, this.modelOutput);
        final Identifier off = TERMINAL_OFF.create(model.withSuffix("_off"), textures, this.modelOutput);
        this.registerDisplay(part, on, off);

        final Identifier item = TERMINAL_ITEM.create(part, new TextureMapping()
            .put(FRONT_BRIGHT, texture("flow_terminal_bright"))
            .put(FRONT_MEDIUM, texture("flow_terminal_medium"))
            .put(FRONT_DARK, texture("flow_terminal_dark")), this.itemModels.modelOutput);
        this.registerTintedItem(part, item);
    }

    private void registerFlowMonitor() {
        final var part = ModItems.FLOW_MONITOR.get();
        final Identifier model = IPartItem.getId(part).withPrefix("part/");
        final Identifier on = MONITOR_ON.create(model.withSuffix("_on"), new TextureMapping(), this.modelOutput);
        final Identifier off = MONITOR_OFF.create(model.withSuffix("_off"), new TextureMapping(), this.modelOutput);
        this.registerDisplay(part, on, off);

        final Identifier item = MONITOR_ITEM.create(part, new TextureMapping(), this.itemModels.modelOutput);
        this.registerTintedItem(part, item);
    }

    private void registerFlowLevelEmitter() {
        final var part = ModItems.FLOW_LEVEL_EMITTER.get();
        final Identifier model = IPartItem.getId(part).withPrefix("part/");
        final TextureMapping textures = new TextureMapping()
            .put(EMITTER, texture("flow_level_emitter"))
            .put(TextureSlot.PARTICLE, texture("flow_level_emitter"));
        final Identifier on = EMITTER_ON.create(model.withSuffix("_on"), textures, this.modelOutput);
        final Identifier off = EMITTER_OFF.create(model.withSuffix("_off"), textures, this.modelOutput);
        this.partModels.composite(part,
            new LevelEmitterPartModel.Unbaked(on, off),
            statusIndicator("level_emitter_status"));

        final Identifier item = EMITTER_ITEM.create(part, new TextureMapping()
            .put(EMITTER, texture("flow_level_emitter")), this.itemModels.modelOutput);
        this.registerTintedItem(part, item);
    }

    private void registerDisplay(final IPartItem<?> part, final Identifier on, final Identifier off) {
        this.partModels.composite(part,
            new StaticPartModel.Unbaked(makeId("part/display_base")),
            new StatusIndicatorPartModel.Unbaked(on, on, off),
            statusIndicator("display_status"));
    }

    private void registerTintedItem(final Item item, final Identifier model) {
        this.itemModels.itemModelOutput.accept(item, ItemModelUtils.tintedModel(model,
            new Constant(-1),
            new AEColorItemTintSource(AEColor.TRANSPARENT, AEColorVariant.DARK),
            new AEColorItemTintSource(AEColor.TRANSPARENT, AEColorVariant.MEDIUM),
            new AEColorItemTintSource(AEColor.TRANSPARENT, AEColorVariant.BRIGHT),
            new AEColorItemTintSource(AEColor.TRANSPARENT, AEColorVariant.MEDIUM_BRIGHT)));
    }

    private static StatusIndicatorPartModel.Unbaked statusIndicator(final String name) {
        final Identifier model = makeId("part/" + name);
        return new StatusIndicatorPartModel.Unbaked(
            model.withSuffix("_has_channel"), model.withSuffix("_on"), model.withSuffix("_off"));
    }

    private static ModelTemplate template(final String parent, final TextureSlot... slots) {
        return new ModelTemplate(Optional.of(makeId(parent)), Optional.empty(), slots);
    }

    private static Material texture(final String name) {
        return new Material(createFlowAnalyticsIdentifier("part/" + name));
    }
}
