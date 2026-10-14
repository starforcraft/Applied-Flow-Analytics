package com.ultramega.appliedflowanalytics.container;

import com.ultramega.appliedflowanalytics.part.FlowMonitorPart;

import appeng.api.stacks.GenericStack;
import appeng.menu.implementations.MenuTypeBuilder;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowMonitorContainerMenu extends AbstractFlowPartMenu<FlowMonitorPart> {
    public static final MenuType<FlowMonitorContainerMenu> TYPE = MenuTypeBuilder
        .create(FlowMonitorContainerMenu::new, FlowMonitorPart.class)
        .withInitialData((host, buffer) -> GenericStack.writeBuffer(host.getConfig().getStack(0), buffer),
            (host, menu, buffer) -> host.getConfig().setStack(0, GenericStack.readBuffer(buffer)))
        .buildUnregistered(createFlowAnalyticsIdentifier("flow_monitor"));

    public FlowMonitorContainerMenu(final MenuType<FlowMonitorContainerMenu> type,
                                    final int id,
                                    final Inventory inventory,
                                    final FlowMonitorPart host) {
        super(type, id, inventory, host);

        this.addFilterAndInventory(inventory, host.getConfig(), 55);
    }
}
