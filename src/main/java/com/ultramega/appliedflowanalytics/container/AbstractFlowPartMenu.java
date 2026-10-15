package com.ultramega.appliedflowanalytics.container;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigManagerBuilder;
import appeng.api.util.IConfigurableObject;
import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import appeng.parts.AEBasePart;
import appeng.util.ConfigInventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public abstract class AbstractFlowPartMenu<T extends AEBasePart & IConfigurableObject> extends AEBaseMenu implements IConfigurableObject {
    protected final T host;
    private final IConfigManager clientConfigManager;
    private boolean settingsSent;

    protected AbstractFlowPartMenu(final MenuType<?> type, final int id, final Inventory inventory, final T host) {
        super(type, id, inventory, host);
        this.host = host;

        final IConfigManagerBuilder builder = IConfigManager.builder(() -> { });
        for (final Setting<?> setting : host.getConfigManager().getSettings()) {
            registerSetting(builder, setting, host.getConfigManager());
        }
        this.clientConfigManager = builder.build();
    }

    private static <V extends Enum<V>> void registerSetting(final IConfigManagerBuilder builder,
                                                           final Setting<V> setting,
                                                           final IConfigManager source) {
        builder.registerSetting(setting, source.getSetting(setting));
    }

    public T getHost() {
        return this.host;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.isClientSide() ? this.clientConfigManager : this.host.getConfigManager();
    }

    public <V extends Enum<V>> void setSetting(final Setting<V> setting, final V value) {
        if (!this.stillValid(this.getPlayer())) {
            return;
        }
        this.getConfigManager().putSetting(setting, value);
        if (this.isClientSide()) {
            ClientPacketDistributor.sendToServer(new ConfigValuePacket(setting, value));
        }
    }

    protected void addFilterAndInventory(final Inventory inventory, final ConfigInventory filter, final int inventoryY) {
        this.addSlot(new FakeSlot(filter.createMenuWrapper(), 0), SlotSemantics.CONFIG).x = 80;
        this.slots.getFirst().y = 20;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18), SlotSemantics.PLAYER_INVENTORY);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, inventoryY + 58), SlotSemantics.PLAYER_HOTBAR);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (!this.isClientSide() && this.stillValid(this.getPlayer())) {
            final IConfigManager serverConfigManager = this.host.getConfigManager();
            for (final Setting<?> setting : serverConfigManager.getSettings()) {
                if (!this.settingsSent || serverConfigManager.getSetting(setting) != this.clientConfigManager.getSetting(setting)) {
                    setting.copy(serverConfigManager, this.clientConfigManager);
                    this.sendPacketToClient(new ConfigValuePacket(setting, serverConfigManager));
                }
            }
            this.settingsSent = true;
        }
    }
}
