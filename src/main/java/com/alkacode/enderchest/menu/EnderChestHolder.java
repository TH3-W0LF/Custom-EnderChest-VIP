package com.alkacode.enderchest.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Marcador unico de inventario do EnderChest - o plugin legado tinha duas classes
 * EnderChestHolder distintas (uma por sistema), o que deixava os listeners de
 * cada sistema cegos para os inventarios do outro. So existe uma agora.
 */
public record EnderChestHolder(UUID ownerUUID, int page, int maxPages, boolean isAdmin) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
