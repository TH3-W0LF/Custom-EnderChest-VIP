package org.dark.customenderchest.manager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public record UpgradeHolder() implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}

