package com.ultramega.appliedflowanalytics.container;

import com.ultramega.appliedflowanalytics.part.FlowLevelEmitterPart;

import appeng.api.stacks.GenericStack;
import appeng.menu.implementations.MenuTypeBuilder;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowLevelEmitterContainerMenu extends AbstractFlowPartMenu<FlowLevelEmitterPart> {
    public static final MenuType<FlowLevelEmitterContainerMenu> TYPE = MenuTypeBuilder
        .create(FlowLevelEmitterContainerMenu::new, FlowLevelEmitterPart.class)
        .withInitialData((host, buffer) -> {
            GenericStack.writeBuffer(host.getFilter().getStack(0), buffer);
            buffer.writeDouble(host.getAmount());
        }, (host, menu, buffer) -> {
            host.getFilter().setStack(0, GenericStack.readBuffer(buffer));
            menu.initializeAmount(buffer.readDouble());
        })
        .build(createFlowAnalyticsIdentifier("flow_level_emitter"));

    private double currentAmount;

    public FlowLevelEmitterContainerMenu(final MenuType<FlowLevelEmitterContainerMenu> type, final int id,
                                         final Inventory inventory, final FlowLevelEmitterPart host) {
        super(type, id, inventory, host);

        this.registerClientAction("amount", Double.class, this::setAmount);
        this.addFilterAndInventory(inventory, host.getFilter(), 98);
    }

    public void initializeAmount(final double value) {
        this.currentAmount = value;
    }

    public double getAmount() {
        return this.isClientSide() ? this.currentAmount : this.host.getAmount();
    }

    public void setAmount(final double value) {
        if (!Double.isFinite(value) || !this.stillValid(this.getPlayer())) {
            return;
        }
        if (this.isClientSide()) {
            if (value != this.currentAmount) {
                this.currentAmount = value;
                this.sendClientAction("amount", value);
            }
        } else {
            this.host.setAmount(value);
        }
    }
}
