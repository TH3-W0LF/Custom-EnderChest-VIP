package org.dark.customenderchest.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static ItemStack fromConfig(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);

        String matName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Nome
        String name = section.getString("name");
        if (name != null) {
            meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }

        // Lore
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> MM.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList()));
        }

        // Custom Model Data (ItemsAdder)
        if (section.contains("custom_model_data")) {
            meta.setCustomModelData(section.getInt("custom_model_data"));
        }

        // Flags
        meta.addItemFlags(ItemFlag.values());

        item.setItemMeta(meta);
        return item;
    }
}

