package com.ultramega.appliedflowanalytics.screen;

import com.ultramega.appliedflowanalytics.container.FlowTerminalContainerMenu;
import com.ultramega.appliedflowanalytics.data.FlowEstimate;
import com.ultramega.appliedflowanalytics.network.MenuState;
import com.ultramega.appliedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.appliedflowanalytics.screen.components.FlowGraph;
import com.ultramega.appliedflowanalytics.screen.components.FlowItemButton;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.GranularitySideButtonWidget;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyleSideButtonWidget;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.ResourceViewSideButtonWidget;
import com.ultramega.appliedflowanalytics.support.ResourceRendering;
import com.ultramega.appliedflowanalytics.util.TickScheduler;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.localization.GuiText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowTerminalScreen extends AEBaseScreen<FlowTerminalContainerMenu> {
    private static final int PRODUCTION_GREEN = 0xff00ff00;
    private static final int CONSUMPTION_RED = 0xffff0000;
    private static final int ESTIMATES_BLUE = 0xff66ddff;
    private static final int WHITE = 0xffffffff;
    private static final int GRAY = 0xffa0a0a0;

    private static final int ITEM_HEADER_Y = 11;
    private static final int ITEM_NAME_Y = 15;
    private static final int GRAPH_X = 10;
    private static final int GRAPH_BOTTOM = 134;

    // Simple generation constants
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_SPACING = ROW_HEIGHT - 1;
    private static final int INNER_X = 8;
    private static final int INNER_Y = 21;
    private static final int INNER_WIDTH = 221;
    private static final int INNER_HEIGHT = 178;
    private static final int NUMBER_OF_COLS = 3;

    // Detailed generation constants
    private static final int TEXT_LINE_HEIGHT = 12;

    private final List<FlowItemButton> itemButtons = new ArrayList<>();
    private final FlowGraph graph = new FlowGraph();
    private final MenuRequestTracker requests = new MenuRequestTracker();
    private FlowEstimate cachedEstimate = FlowEstimate.EMPTY;
    @Nullable
    private AEKey cachedEstimateResource;
    private String[] estimateLabels = new String[0];

    private int innerLeft = this.leftPos + INNER_X;
    private int innerTop = this.topPos + INNER_Y;

    private final Button doneButton;
    private final Scrollbar scrollbar;
    private final AETextField searchField;
    private final SettingToggleButton<SortDir> sortDirectionButton;
    private final SettingToggleButton<SortOrder> sortOrderButton;
    private TickScheduler tickScheduler;
    private int refreshIntervalTicks;
    private Granularity displayedGranularity;
    private ResourceView displayedResourceView;

    private Map<AEKey, Map<Short, Long>> lastSnapshot = new HashMap<>();

    private boolean initialSnapshotReceived;
    private boolean buttonsDirty = true;
    @Nullable
    private ListLayout buttonLayout;

    private boolean showingDetails = false;
    @Nullable
    private AEKey selectedResource;

    public FlowTerminalScreen(final FlowTerminalContainerMenu container,
                              final Inventory inventory,
                              final Component text, final ScreenStyle style) {
        super(container, inventory, text, style);
        this.setTextContent(TEXT_ID_DIALOG_TITLE, text);
        this.doneButton = this.widgets.addButton("done", Component.translatable("gui.done"), this::onClose);
        this.searchField = this.widgets.addTextField("search");
        this.searchField.setMaxLength(128);
        this.searchField.setPlaceholder(GuiText.SearchPlaceholder.text());
        this.searchField.setTooltipMessage(List.of(createFlowAnalyticsTranslation("gui", "flow.search")));
        this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.BIG);
        this.scrollbar.setCaptureMouseWheel(false);

        this.sortDirectionButton = this.addToLeftToolbar(new SettingToggleButton<>(Settings.SORT_DIRECTION,
            container.getSortingDirection(), (button, backwards) -> container.setSortingDirection(button.getNextValue(backwards))));
        this.sortOrderButton = this.addToLeftToolbar(new SettingToggleButton<>(Settings.SORT_BY,
            container.getSortingType(), (button, backwards) -> container.setSortingType(button.getNextValue(backwards))));
        this.addToLeftToolbar(new ResourceViewSideButtonWidget(container, this::onResourceViewChanged));
        this.addToLeftToolbar(new GranularitySideButtonWidget(container, this::onGranularityChanged));
        this.addToLeftToolbar(new LineStyleSideButtonWidget(container));

        this.displayedGranularity = container.getGranularity();
        this.displayedResourceView = container.getResourceView();
        this.resetRefreshScheduler();
    }

    @Override
    public void init() {
        super.init();
        this.buttonLayout = null;
        this.rebuildItemButtonsIfNeeded();
        this.updateControlVisibility();
        this.requestCurrentGenerationStats();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.displayedGranularity != this.getMenu().getGranularity()) {
            this.onGranularityChanged();
        }
        if (this.displayedResourceView != this.getMenu().getResourceView()) {
            this.onResourceViewChanged();
        }
        if (this.refreshIntervalTicks != this.getRefreshIntervalTicks()) {
            this.resetRefreshScheduler();
        }
        if (this.tickScheduler.shouldRun()) {
            this.requestCurrentGenerationStats();
        }
    }

    private int getRefreshIntervalTicks() {
        return Math.max(5, this.getMenu().getGranularity().getTickAmount());
    }

    private void resetRefreshScheduler() {
        this.refreshIntervalTicks = this.getRefreshIntervalTicks();
        this.tickScheduler = new TickScheduler(this.refreshIntervalTicks);
    }

    private void renderDetailedGenerationStats(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        final Granularity granularity = this.getMenu().getGranularity();

        graphics.pose().pushMatrix();
        graphics.nextStratum();

        this.graph.renderItem(graphics, this.leftPos + GRAPH_X, this.topPos + ITEM_HEADER_Y);
        graphics.text(this.font, this.graph.getItemName(), this.leftPos + 35, this.topPos + ITEM_NAME_Y, WHITE);

        final int graphLeft = this.leftPos + GRAPH_X;
        final int graphBottom = this.topPos + GRAPH_BOTTOM;
        final int graphWidth = this.graph.getGraphSize().x;
        final int graphHeight = this.graph.getGraphSize().y;
        this.graph.setGraphPos(graphLeft, graphBottom);
        if (this.graph.isLoading()) {
            final Component loadingText = createFlowAnalyticsTranslation("gui", "flow.loading");
            graphics.centeredText(this.font, loadingText, graphLeft + graphWidth / 2, graphBottom - graphHeight / 2 - this.font.lineHeight / 2, WHITE);
            graphics.pose().popMatrix();
            return;
        }

        this.graph.drawGraphs(graphics);

        // Main stats
        final AEKey itemKey = this.graph.getItemKey();

        final int productionRowShift = 2;
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.inflow"),
            graphLeft + productionRowShift, graphBottom + TEXT_LINE_HEIGHT + 2, PRODUCTION_GREEN);
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.maximum", this.formatAmount(itemKey, this.graph.getMaxProduction()) + granularity.perStr()),
            graphLeft + productionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.minimum", this.formatAmount(itemKey, this.graph.getMinProduction()) + granularity.perStr()),
            graphLeft + productionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        final int consumptionRowShift = 78;
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.outflow"),
            graphLeft + consumptionRowShift, graphBottom + TEXT_LINE_HEIGHT + 2, CONSUMPTION_RED);
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.maximum", this.formatAmount(itemKey, this.graph.getMaxConsumption()) + granularity.perStr()),
            graphLeft + consumptionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.minimum", this.formatAmount(itemKey, this.graph.getMinConsumption()) + granularity.perStr()),
            graphLeft + consumptionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        // Reference lines
        graphics.text(this.font, createFlowAnalyticsTranslation("gui", "flow.average"),
            graphLeft + graphWidth + 8, graphBottom - graphHeight - 14, WHITE);

        final double incAvg = this.graph.getAvgProduction();
        final double decAvg = this.graph.getAvgConsumption();
        final int zeroY = (int) Math.round(this.graph.getGuiY(0));
        final int incAvgY = (int) Math.round(this.graph.getGuiY(incAvg));
        final int decAvgY = (int) Math.round(this.graph.getGuiY(-decAvg));
        final int averageLabelX = graphLeft + graphWidth + 3;
        // Leave room for the zero label, even when one or both averages are zero.
        final int incLabelY = Math.clamp(incAvgY - 10, graphBottom - graphHeight, zeroY - 11);
        final int decLabelY = Math.clamp(decAvgY + 2, zeroY + 2, graphBottom - this.font.lineHeight);
        graphics.text(this.font, "+" + this.formatAmount(itemKey, (long) incAvg) + granularity.perStr(),
            averageLabelX, incLabelY, PRODUCTION_GREEN);
        graphics.text(this.font, "-" + this.formatAmount(itemKey, (long) decAvg) + granularity.perStr(),
            averageLabelX, decLabelY, CONSUMPTION_RED);
        graphics.text(this.font, "0", averageLabelX, zeroY - 4, GRAY);

        // Estimates
        final int estimatesRowShift = 160;
        graphics.text(this.font, createFlowAnalyticsTranslation("gui", "flow.estimates"),
            graphLeft + estimatesRowShift, graphBottom + 25, ESTIMATES_BLUE);
        final FlowEstimate estimate = this.graph.getEstimate();
        this.updateEstimateLabels(itemKey, estimate);
        graphics.text(this.font, this.estimateLabels[0], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 3, WHITE);
        graphics.text(this.font, this.estimateLabels[1], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 4, WHITE);
        graphics.text(this.font, this.estimateLabels[2], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 5, WHITE);
        graphics.text(this.font, this.estimateLabels[3], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 6, WHITE);

        // Overall stats
        final long net = Arrays.stream(this.graph.getNetData()).sum();
        graphics.text(this.font,
            createFlowAnalyticsTranslation("gui", "flow.total_stored", this.formatAmount(itemKey, this.graph.getTotalStored())), graphLeft + productionRowShift,
            graphBottom + 4 + TEXT_LINE_HEIGHT * 5, WHITE);
        graphics.text(this.font, createFlowAnalyticsTranslation("gui", "flow.net_timeframe", (net > 0 ? "+" : "") + this.formatAmount(itemKey, net)),
            graphLeft + productionRowShift, graphBottom + 4 + TEXT_LINE_HEIGHT * 6, WHITE);

        graphics.pose().popMatrix();
    }

    private void renderSimpleGenerationStats(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {

        this.innerLeft = this.leftPos + INNER_X;
        this.innerTop = this.topPos + INNER_Y;
        graphics.enableScissor(this.innerLeft, this.innerTop,
            this.innerLeft + INNER_WIDTH, this.innerTop + INNER_HEIGHT);

        final FlowItemButton hoveredButton = this.findHoveredItemButton(mouseX, mouseY);
        for (final FlowItemButton button : this.itemButtons) {
            if (button != hoveredButton) {
                button.render(graphics, false);
            }
        }
        // Draw the hovered button last so its shared borders stay highlighted
        if (hoveredButton != null) {
            hoveredButton.render(graphics, true);
        }

        graphics.disableScissor();

    }

    private void rebuildItemButtonsIfNeeded() {
        if (!this.initialSnapshotReceived || this.showingDetails) {
            return;
        }
        final ListLayout layout = this.currentListLayout();
        if (!this.buttonsDirty && layout.equals(this.buttonLayout)) {
            return;
        }
        this.innerLeft = this.leftPos + INNER_X;
        this.innerTop = this.topPos + INNER_Y;
        final var sorted = this.lastSnapshot.entrySet().stream().sorted(this.getResourceComparator()).toList();
        final List<Map.Entry<AEKey, Map<Short, Long>>> filtered = new ArrayList<>();
        for (final var entry : sorted) {
            final ResourceRendering rendering = new ResourceRendering(entry.getKey());
            if (layout.query().isEmpty()
                || rendering.getDisplayName(entry.getKey()).getString().toLowerCase(Locale.ROOT).contains(layout.query())) {
                filtered.add(entry);
            }
        }
        final int rows = (filtered.size() + NUMBER_OF_COLS - 1) / NUMBER_OF_COLS;
        final int contentHeight = rows == 0 ? 0 : (rows - 1) * ROW_SPACING + ROW_HEIGHT;
        final int maxOffset = Math.max(0, contentHeight - INNER_HEIGHT);
        this.scrollbar.setRange(0, maxOffset, ROW_SPACING);
        final int offset = this.scrollbar.getCurrentScroll();
        this.itemButtons.clear();
        for (int i = 0; i < filtered.size(); i++) {
            final int y = this.innerTop + (i / NUMBER_OF_COLS) * ROW_SPACING - offset;
            if (y >= this.innerTop + INNER_HEIGHT) {
                break;
            }
            if (y + ROW_HEIGHT <= this.innerTop) {
                continue;
            }
            final var entry = filtered.get(i);
            final AEKey resource = entry.getKey();
            final ResourceRendering rendering = new ResourceRendering(resource);

            final int column = i % NUMBER_OF_COLS;
            final int columnLeft = column * (INNER_WIDTH - 1) / NUMBER_OF_COLS;
            final int columnRight = (column + 1) * (INNER_WIDTH - 1) / NUMBER_OF_COLS;
            final int rowWidth = columnRight - columnLeft + 1;
            final int x = this.innerLeft + columnLeft;
            this.itemButtons.add(new FlowItemButton(x, y, rowWidth, ROW_HEIGHT,
                resource, entry.getValue(), layout.granularity(), () -> this.openDetails(resource),
                List.of(rendering.getDisplayName(resource))));
        }
        this.buttonLayout = this.currentListLayout();
        this.buttonsDirty = false;
    }

    private ListLayout currentListLayout() {
        return new ListLayout(this.searchField.getValue().toLowerCase(Locale.ROOT), this.getMenu().getSortingType(),
            this.getMenu().getSortingDirection(), this.getMenu().getGranularity(), this.leftPos, this.topPos,
            this.scrollbar.getCurrentScroll());
    }

    private Comparator<Map.Entry<AEKey, Map<Short, Long>>> getResourceComparator() {
        final Comparator<Map.Entry<AEKey, Map<Short, Long>>> byName = Comparator.comparing(
            entry -> entry.getKey().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER);
        final Comparator<Map.Entry<AEKey, Map<Short, Long>>> comparator = switch (this.getMenu().getSortingType()) {
            case NAME -> byName;
            case MOD -> Comparator.<Map.Entry<AEKey, Map<Short, Long>>, String>comparing(
                entry -> entry.getKey().getModId(), String.CASE_INSENSITIVE_ORDER).thenComparing(byName);
            case AMOUNT -> Comparator.<Map.Entry<AEKey, Map<Short, Long>>>comparingLong(
                entry -> entry.getValue().getOrDefault((short) 1, 0L) - entry.getValue().getOrDefault((short) -1, 0L))
                .thenComparing(byName);
        };
        return this.getMenu().getSortingDirection() == SortDir.DESCENDING ? comparator.reversed() : comparator;
    }

    private void requestCurrentGenerationStats() {
        final Player player = this.getMenu().entity;
        if (player.containerMenu == this.getMenu()) {
            final int granularity = this.getMenu().getGranularity().getTickAmount();
            final MenuState state;
            if (this.showingDetails) {
                final AEKey resource = this.selectedResource;
                if (resource == null) {
                    return;
                }
                state = new MenuState.DetailedRequest(resource, granularity);
            } else {
                state = new MenuState.SnapshotRequest(granularity, this.getMenu().getResourceView() == ResourceView.ALL_STORED);
            }
            final long requestId = this.requests.tryStartRequest();
            if (requestId == -1) {
                return;
            }
            this.getMenu().sendMenuStateUpdate(player, requestId, state);
        }
    }

    private void openDetails(final AEKey resource) {
        this.selectedResource = resource;
        this.showingDetails = true;
        this.graph.beginLoading(resource, this.getMenu().getGranularity());
        this.requests.invalidate();
        this.updateControlVisibility();
        this.requestCurrentGenerationStats();
    }

    private void returnToList() {
        this.showingDetails = false;
        this.requests.invalidate();
        this.updateControlVisibility();
        this.requestCurrentGenerationStats();
    }

    private void onResourceViewChanged() {
        this.displayedResourceView = this.getMenu().getResourceView();
        this.lastSnapshot = new HashMap<>();
        this.buttonsDirty = true;
        this.itemButtons.clear();
        this.scrollbar.setCurrentScroll(0);
        this.requests.invalidate();
        this.requestCurrentGenerationStats();
    }

    private void onGranularityChanged() {
        this.displayedGranularity = this.getMenu().getGranularity();
        this.resetRefreshScheduler();
        this.requests.invalidate();
        final AEKey resource = this.selectedResource;
        if (this.showingDetails && resource != null) {
            this.graph.beginLoading(resource, this.getMenu().getGranularity());
        } else {
            this.lastSnapshot = new HashMap<>();
            this.buttonsDirty = true;
        }
        this.requestCurrentGenerationStats();
    }

    public void updateMenuState(final long requestId, final MenuState state) {
        final MenuRequestTracker.Completion completion = this.requests.complete(requestId);
        if (completion == MenuRequestTracker.Completion.IGNORED) {
            return;
        }
        if (completion == MenuRequestTracker.Completion.OBSOLETE) {
            // Navigation is coalesced while waiting; now fetch only the latest requested view
            this.requestCurrentGenerationStats();
            return;
        }
        if (state instanceof MenuState.DetailedSnapshot(Map<ResourceChangeGranularityKey, long[]> data)) {
            final AEKey resource = this.selectedResource;
            if (!this.showingDetails || resource == null
                || data.keySet().stream().anyMatch(key -> key.granularity() != this.getMenu().getGranularity().getTickAmount()
                    || !key.resourceKey().equals(resource))) {
                return;
            }
            final Map<ResourceChangeGranularityKey, long[]> graphData = data.isEmpty()
                ? Map.of(new ResourceChangeGranularityKey(resource, (short) 0, this.getMenu().getGranularity().getTickAmount()), new long[]{0}) : data;
            this.graph.setData(graphData, this.getMenu().getGranularity());
        } else if (state instanceof MenuState.Snapshot(int granularity, boolean allStored, Map<AEKey, Map<Short, Long>> data)
            && !this.showingDetails
            && granularity == this.getMenu().getGranularity().getTickAmount()
            && allStored == (this.getMenu().getResourceView() == ResourceView.ALL_STORED)) {
            this.buttonsDirty |= !this.lastSnapshot.equals(data);
            this.lastSnapshot = data;
            this.initialSnapshotReceived = true;
            this.rebuildItemButtonsIfNeeded();
            this.updateControlVisibility();
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final double mouseX = event.x();
        final double mouseY = event.y();
        final int button = event.button();
        if (!this.initialSnapshotReceived) {
            return false;
        }
        this.rebuildItemButtonsIfNeeded();
        if (this.showingDetails) {
            // back button
            if (button == 3) {
                this.returnToList();
                return true;
            }
            if (this.graph.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } else {
            // forward button
            if (button == 4) {
                final AEKey resource = this.selectedResource;
                if (resource != null) {
                    this.openDetails(resource);
                }
                return true;
            }
            final FlowItemButton hoveredButton = this.findHoveredItemButton(mouseX, mouseY);
            if (hoveredButton != null && hoveredButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.showingDetails) {
            if (this.graph.mouseReleased(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double z, final double delta) {
        final boolean didScroll = this.scrollbar.isVisible() && delta != 0
            && this.isHoveringOverArea(x, y)
            && this.scrollbar.onMouseWheel(new Point((int) (x - this.leftPos), (int) (y - this.topPos)), delta);
        return didScroll || super.mouseScrolled(x, y, z, delta);
    }

    @Override
    public void onClose() {
        if (this.showingDetails) {
            this.returnToList();
            return;
        }
        this.requests.invalidate();
        super.onClose();
    }

    @Nullable
    private FlowItemButton findHoveredItemButton(final double mouseX, final double mouseY) {
        if (!this.isHoveringOverArea(mouseX, mouseY)) {
            return null;
        }
        for (final FlowItemButton button : this.itemButtons.reversed()) {
            if (button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    private boolean isHoveringOverArea(final double x, final double y) {
        return !this.showingDetails && this.isHovering(INNER_X, INNER_Y, INNER_WIDTH, INNER_HEIGHT, x, y);
    }

    private String formatAmount(final AEKey resourceKey, final long amount) {
        return new ResourceRendering(resourceKey).formatAmount(amount, true);
    }

    private void updateEstimateLabels(final AEKey resourceKey, final FlowEstimate estimate) {
        if (estimate.equals(this.cachedEstimate) && resourceKey.equals(this.cachedEstimateResource)) {
            return;
        }
        this.cachedEstimate = estimate;
        this.cachedEstimateResource = resourceKey;
        this.estimateLabels = new String[]{
            this.formatEstimate(resourceKey, estimate, Granularity.SECOND),
            this.formatEstimate(resourceKey, estimate, Granularity.MINUTE),
            this.formatEstimate(resourceKey, estimate, Granularity.HOUR),
            this.formatEstimate(resourceKey, estimate, Granularity.DAY)
        };
    }

    private String formatEstimate(final AEKey resourceKey, final FlowEstimate estimate, final Granularity target) {
        if (!estimate.available()) {
            return "—" + target.perStr();
        }
        final double amount = estimate.forTicks(target.getTickAmount());
        final double magnitude = Math.abs(amount);
        final String formatted;
        if (resourceKey instanceof AEItemKey && magnitude < 1000) {
            formatted = FlowEstimate.formatNumber(magnitude);
        } else if (resourceKey instanceof AEFluidKey && magnitude < 1000) {
            formatted = FlowEstimate.formatNumber((1.0 / resourceKey.getAmountPerUnit()) * magnitude) + " B";
        } else if (magnitude > 0 && magnitude < 1000 && magnitude != Math.rint(magnitude)) {
            formatted = FlowEstimate.formatNumber(magnitude) + " × " + this.formatAmount(resourceKey, 1);
        } else {
            formatted = this.formatAmount(resourceKey, Math.round(magnitude));
        }
        return (amount < 0 ? "-" : amount > 0 ? "+" : "") + formatted + target.perStr();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.sortDirectionButton.set(this.getMenu().getSortingDirection());
        this.sortOrderButton.set(this.getMenu().getSortingType());
        this.rebuildItemButtonsIfNeeded();
        this.updateControlVisibility();
    }

    private void updateControlVisibility() {
        final boolean showListControls = this.initialSnapshotReceived && !this.showingDetails;
        this.searchField.visible = showListControls;
        this.searchField.active = showListControls;
        if (!showListControls) {
            this.searchField.setFocused(false);
        }
        this.doneButton.visible = showListControls;
        this.scrollbar.setVisible(showListControls);
        this.setTextHidden(TEXT_ID_DIALOG_TITLE, this.showingDetails);
    }

    @Override
    public void drawBG(final GuiGraphicsExtractor graphics,
                       final int offsetX,
                       final int offsetY,
                       final int mouseX,
                       final int mouseY,
                       final float partialTicks) {
        if (this.showingDetails) {
            this.style.getImage("detail").dest(offsetX, offsetY).blit(graphics);
        } else {
            super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        }
        if (!this.initialSnapshotReceived) {
            graphics.centeredText(this.font, createFlowAnalyticsTranslation("gui", "flow.loading"),
                offsetX + this.imageWidth / 2, offsetY + this.imageHeight / 2, WHITE);
            return;
        }
        this.graph.lineStyle = this.getMenu().getLineStyle();
        if (this.showingDetails) {
            this.renderDetailedGenerationStats(graphics, mouseX, mouseY);
        } else {
            this.renderSimpleGenerationStats(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        if (this.showingDetails) {
            this.graph.renderTooltip(graphics, mouseX, mouseY);
            final Component heading = createFlowAnalyticsTranslation("gui", "flow.estimates");
            if (!this.graph.isLoading() && this.isHovering(GRAPH_X + 160, GRAPH_BOTTOM + 25,
                this.font.width(heading), this.font.lineHeight, mouseX, mouseY)) {
                final FlowEstimate estimate = this.graph.getEstimate();
                graphics.setTooltipForNextFrame(this.font, estimate.available()
                    ? createFlowAnalyticsTranslation("gui", "flow.estimates_window", estimate.observationWindow())
                    : createFlowAnalyticsTranslation("gui", "flow.estimates_empty"), mouseX, mouseY);
            }
        } else {
            final FlowItemButton hoveredButton = this.findHoveredItemButton(mouseX, mouseY);
            if (hoveredButton != null) {
                hoveredButton.renderTooltip(graphics, mouseX, mouseY);
            }
        }
    }

    private record ListLayout(String query, SortOrder sortingType, SortDir sortingDirection,
                              Granularity granularity, int left, int top, int scrollOffset) {
    }
}
