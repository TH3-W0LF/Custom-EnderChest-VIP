package org.dark.customenderchest.manager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record EnderChestHolder(UUID ownerUUID, int currentPage, int maxPages, boolean isEditing) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return null; // Não usado diretamente, apenas marcador
    }
}

