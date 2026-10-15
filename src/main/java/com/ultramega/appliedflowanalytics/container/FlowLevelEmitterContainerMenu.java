package com.ultramega.appliedflowanalytics.container;

import com.ultramega.appliedflowanalytics.part.FlowLevelEmitterPart;

import appeng.api.stacks.GenericStack;
import appeng.menu.guisync.ClientActionKey;
import appeng.menu.implementations.MenuTypeBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowLevelEmitterContainerMenu extends AbstractFlowPartMenu<FlowLevelEmitterPart> {
    public static final MenuType<FlowLevelEmitterContainerMenu> TYPE = MenuTypeBuilder
        .create(FlowLevelEmitterContainerMenu::new, FlowLevelEmitterPart.class)
        .withInitialData((host, buffer) -> {
            GenericStack.writeBuffer(host.getConfig().getStack(0), buffer);
            buffer.writeDouble(host.getAmount());
        }, (host, menu, buffer) -> {
            host.getConfig().setStack(0, GenericStack.readBuffer(buffer));
            menu.initializeAmount(buffer.readDouble());
        })
        .buildUnregistered(createFlowAnalyticsIdentifier("flow_level_emitter"));

    private static final ClientActionKey<Double> ACTION_AMOUNT = new ClientActionKey<>("amount");
    private double currentAmount;

    public FlowLevelEmitterContainerMenu(final MenuType<FlowLevelEmitterContainerMenu> type, final int id,
                                         final Inventory inventory, final FlowLevelEmitterPart host) {
        super(type, id, inventory, host);

        this.registerClientAction(ACTION_AMOUNT, ByteBufCodecs.DOUBLE, this::setAmount);
        this.addFilterAndInventory(inventory, host.getConfig(), 98);
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
                this.sendClientAction(ACTION_AMOUNT, value);
            }
        } else {
            this.host.setAmount(value);
        }
    }
}
