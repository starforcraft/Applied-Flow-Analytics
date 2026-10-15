package com.ultramega.appliedflowanalytics.part;

import com.ultramega.appliedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.appliedflowanalytics.data.FlowSnapshotData;
import com.ultramega.appliedflowanalytics.network.MenuState;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.MonitorItemVisibility;
import com.ultramega.appliedflowanalytics.support.FlowSettings;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManager;
import appeng.helpers.IConfigInvHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.ConfigInventory;
import appeng.util.InteractionUtil;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class FlowMonitorPart extends AbstractFlowDisplayPart implements IConfigInvHost {
    public static final String ITEM_VISIBILITY_TAG = "item_visibility";
    public static final String FLOW_TEXT_TAG = "flow_text";
    public static final String GRANULARITY_TAG = "granularity";
    public static final String LINE_STYLE_TAG = "line_style";

    private final ConfigInventory filter = ConfigInventory.configTypes(1).changeListener(this::settingsChanged).build();
    private final IConfigManager configManager = IConfigManager.builder(this::settingsChanged)
        .registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE)
        .registerSetting(FlowSettings.ITEM_VISIBILITY, MonitorItemVisibility.SHOW)
        .registerSetting(FlowSettings.FLOW_TEXT, FlowDirection.NET)
        .registerSetting(FlowSettings.GRANULARITY, Granularity.SECOND)
        .registerSetting(FlowSettings.LINE_STYLE, LineStyle.EXACT)
        .build();
    private boolean loadingData;
    @Nullable
    private FlowSnapshotData displaySource;
    private long sourceRevision = -1;
    private boolean displayActive;
    private long displayRevision;
    private long[] displayInflow = new long[0];
    private long[] displayOutflow = new long[0];

    public FlowMonitorPart(final IPartItem<?> partItem) {
        super(partItem, false);
    }

    @Override
    protected void historyTick() {
        this.updateDisplay(false);
    }

    private void updateDisplay(final boolean force) {
        if (this.getHost() == null || this.isClientSide()) {
            return;
        }
        final var component = this.historyNode.getHistoryComponent().orElse(null);
        final FlowSnapshotData source = component == null ? null : component.getData().orElse(null);
        final long revision = source == null ? -1 : source.getRevision(this.getGranularity().getTickAmount());
        final boolean active = this.getMainNode().isActive() && this.redstoneAllowsDisplay();
        if (!force && active == this.displayActive && source == this.displaySource && revision == this.sourceRevision) {
            return;
        }
        final AEKey selected = this.getConfiguredResource();
        final FlowSnapshotData.Samples samples = component == null || selected == null
            ? new FlowSnapshotData.Samples(new long[0], new long[0])
            : component.getSamples(selected, this.getGranularity().getTickAmount());
        this.displaySource = source;
        this.sourceRevision = revision;
        if (force || active != this.displayActive || !Arrays.equals(samples.inflow(), this.displayInflow)
            || !Arrays.equals(samples.outflow(), this.displayOutflow)) {
            this.displayInflow = samples.inflow();
            this.displayOutflow = samples.outflow();
            this.displayActive = active;
            this.displayRevision++;
            this.getHost().markForUpdate();
        }
    }

    private boolean redstoneAllowsDisplay() {
        return switch (this.getRedstoneMode()) {
            case IGNORE, SIGNAL_PULSE -> true;
            case HIGH_SIGNAL -> this.getHost().hasRedstone();
            case LOW_SIGNAL -> !this.getHost().hasRedstone();
        };
    }

    private void settingsChanged() {
        if (!this.loadingData && this.getHost() != null) {
            this.getHost().markForSave();
            this.updateDisplay(true);
        }
    }

    @Override
    public ConfigInventory getConfig() {
        return this.filter;
    }

    @Nullable
    public AEKey getConfiguredResource() {
        return this.filter.getKey(0);
    }

    public MonitorItemVisibility getItemVisibility() {
        return this.configManager.getSetting(FlowSettings.ITEM_VISIBILITY);
    }

    public void setItemVisibility(final MonitorItemVisibility value) {
        this.configManager.putSetting(FlowSettings.ITEM_VISIBILITY, Objects.requireNonNull(value));
    }

    public FlowDirection getFlowText() {
        return this.configManager.getSetting(FlowSettings.FLOW_TEXT);
    }

    public void setFlowText(final FlowDirection value) {
        this.configManager.putSetting(FlowSettings.FLOW_TEXT, Objects.requireNonNull(value));
    }

    public Granularity getGranularity() {
        return this.configManager.getSetting(FlowSettings.GRANULARITY);
    }

    public void setGranularity(final Granularity value) {
        this.configManager.putSetting(FlowSettings.GRANULARITY, Objects.requireNonNull(value));
    }

    public LineStyle getLineStyle() {
        return this.configManager.getSetting(FlowSettings.LINE_STYLE);
    }

    public void setLineStyle(final LineStyle value) {
        this.configManager.putSetting(FlowSettings.LINE_STYLE, Objects.requireNonNull(value));
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    public RedstoneMode getRedstoneMode() {
        return this.configManager.getSetting(Settings.REDSTONE_CONTROLLED);
    }

    public void setRedstoneMode(final RedstoneMode value) {
        this.configManager.putSetting(Settings.REDSTONE_CONTROLLED,
            Objects.requireNonNull(value) == RedstoneMode.SIGNAL_PULSE ? RedstoneMode.IGNORE : value);
    }

    public boolean isDisplayActive() {
        return this.displayActive;
    }

    public long getDisplayRevision() {
        return this.displayRevision;
    }

    public long[] getDisplayInflow() {
        return this.displayInflow.clone();
    }

    public long[] getDisplayOutflow() {
        return this.displayOutflow.clone();
    }

    @Override
    public void writeConfiguration(final ValueOutput tag) {
        this.filter.writeToChildTag(tag, "filter");
        this.configManager.writeToNBT(tag);
    }

    @Override
    public void readConfiguration(final ValueInput tag) {
        this.loadingData = true;
        try {
            this.filter.readFromChildTag(tag, "filter");
            this.configManager.readFromNBT(tag);
        } finally {
            this.loadingData = false;
        }
        this.sourceRevision = -1;
        this.displayRevision++;
    }

    @Override
    public void writeToNBT(final ValueOutput tag) {
        super.writeToNBT(tag);
        this.writeConfiguration(tag);
    }

    @Override
    public void readFromNBT(final ValueInput tag) {
        super.readFromNBT(tag);
        this.readConfiguration(tag);
    }

    @Override
    public void writeToStream(final RegistryFriendlyByteBuf buffer) {
        super.writeToStream(buffer);
        final TagValueOutput tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, buffer.registryAccess());
        this.writeConfiguration(tag);
        buffer.writeNbt(tag.buildResult());
        buffer.writeBoolean(this.displayActive);
        MenuState.Codecs.SAMPLES.encode(buffer, this.displayInflow);
        MenuState.Codecs.SAMPLES.encode(buffer, this.displayOutflow);
    }

    @Override
    public boolean readFromStream(final RegistryFriendlyByteBuf buffer) {
        final boolean changed = super.readFromStream(buffer);
        final CompoundTag tag = buffer.readNbt();
        if (tag != null) {
            this.readConfiguration(TagValueInput.create(ProblemReporter.DISCARDING, buffer.registryAccess(), tag));
        }
        this.displayActive = buffer.readBoolean();
        this.displayInflow = MenuState.Codecs.SAMPLES.decode(buffer);
        this.displayOutflow = MenuState.Codecs.SAMPLES.decode(buffer);
        this.displayRevision++;
        return changed;
    }

    @Override
    public boolean onUseWithoutItem(final Player player, final Vec3 pos) {
        if (InteractionUtil.canWrenchRotate(player.getInventory().getSelectedItem())) {
            return super.onUseWithoutItem(player, pos);
        }
        if (!this.isClientSide()) {
            MenuOpener.open(FlowMonitorContainerMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }

}
