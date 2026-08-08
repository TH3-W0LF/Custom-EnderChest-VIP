package com.alkacode.enderchest.util;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Chave Mestra usada para forcar a remocao da senha de um EnderChest (/ec remover)
 * sem depender de permissao. O item e reconhecido por uma tag no
 * PersistentDataContainer, nao pelo nome/material - assim ele sobrevive a
 * renomeacao em bigorna e nao pode ser falsificado com um item comum renomeado.
 */
public final class MasterKeyItem {

    private static final String KEY_NAME = "alka_master_key";

    private MasterKeyItem() {
    }

    public static ItemStack create(JavaPlugin plugin, int amount) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("security.master-key");
        ItemStack item = ItemBuilder.fromConfig(section);
        item.setAmount(Math.max(1, amount));

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        return item;
    }

    public static boolean isMasterKey(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }

    private static NamespacedKey key(JavaPlugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }
}
