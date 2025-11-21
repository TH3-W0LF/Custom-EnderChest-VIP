package org.dark.customenderchest.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.utils.ItemBuilder;

import java.util.*;

public class EnderChestManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Location> openedBlocks = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    private org.dark.customenderchest.economy.DrakonioEconomy drakonioEconomy;

    public EnderChestManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    public void setDrakonioEconomy(org.dark.customenderchest.economy.DrakonioEconomy economy) {
        this.drakonioEconomy = economy;
    }
    
    public void registerOpenedBlock(Player player, Block block) {
        openedBlocks.put(player.getUniqueId(), block.getLocation());
    }
    
    public void closeOpenedBlock(Player player) {
        Location loc = openedBlocks.remove(player.getUniqueId());
        if (loc != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.ENDER_CHEST && block.getState() instanceof EnderChest ec) {
                ec.close();
            }
        }
    }
    
    public int getEffectiveTier(Player player) {
        int dbTier = databaseManager.getPurchasedTier(player.getUniqueId());
        int permTier = 0;
        
        if (player.hasPermission("meuplugin.vip.drakkar")) permTier = 5; // 5 Paginas
        else if (player.hasPermission("meuplugin.vip.lorde")) permTier = 3; // 3 Paginas
        else if (player.hasPermission("meuplugin.vip.cavaleiro")) permTier = 2; // 2 Paginas
        else if (player.hasPermission("meuplugin.vip.escudeiro")) permTier = 1; // 36 Slots
        
        return Math.max(dbTier, permTier);
    }

    public int getMaxPages(Player player) {
        int tier = getEffectiveTier(player);
        if (tier <= 1) return 1; 
        return tier;
    }
    
    public void openUpgradeMenu(Player player) {
        Inventory upgradeInv = Bukkit.createInventory(new UpgradeHolder(), 54, 
                Component.text("Loja de Upgrades").color(NamedTextColor.DARK_PURPLE));
        
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        for (int i = 0; i < 54; i++) upgradeInv.setItem(i, glass);
        
        ConfigurationSection upgradeConfig = plugin.getConfig().getConfigurationSection("upgrades");

        upgradeInv.setItem(49, ItemBuilder.fromConfig(upgradeConfig.getConfigurationSection("back-button")));
        
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(mm.deserialize("<gradient:#FFAA00:#FFE055><bold>Loja de Upgrades</bold></gradient>").decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(List.of(
            mm.deserialize(" <gray>Aqui você pode comprar").decoration(TextDecoration.ITALIC, false),
            mm.deserialize(" <#b57edc>Cristais de Tier</#b57edc> <gray>para evoluir").decoration(TextDecoration.ITALIC, false),
            mm.deserialize(" <gray>o seu <white>EnderChest</white> ou de amigos.").decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<gradient:#FF4500:#8B0000> ⚔ Exclusivo Drakkar MC ⚔ </gradient>").decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gradient:#00FFFF:#008080> 🛠 Dev: MestreBR </gradient>").decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize(" <italic><dark_gray>Dúvidas? Entre em contato com a Staff.").decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(infoMeta);
        upgradeInv.setItem(4, info);
        
        ConfigurationSection tiers = upgradeConfig.getConfigurationSection("tiers");
        int currentTier = getEffectiveTier(player);
        
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int tierLevel = Integer.parseInt(key);
                int slot = tiers.getInt(key);
                
                ItemStack tierItem = ItemBuilder.fromConfig(upgradeConfig.getConfigurationSection("tier-item"));
                ItemMeta tm = tierItem.getItemMeta();
                
                // Se o jogador já tiver esse tier, mostra diferente (Encantado ou aviso)
                if (tierLevel <= currentTier) {
                    tm.displayName(mm.deserialize("<green>Tier " + tierLevel + " (Adquirido)").decoration(TextDecoration.ITALIC, false));
                    tm.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    tm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } else {
                    if (tm.hasDisplayName()) {
                        tm.displayName(tm.displayName().replaceText(b -> b.matchLiteral("<tier>").replacement(key)));
                    }
                }
                
                if (tm.hasLore()) {
                    List<Component> newLore = new ArrayList<>();
                    if (tm.lore() != null) {
                        for (Component l : tm.lore()) {
                            newLore.add(l.replaceText(b -> b.matchLiteral("<tier>").replacement(key)));
                        }
                    }
                    
                    // Adiciona informações de preço
                    if (drakonioEconomy != null && tierLevel > currentTier) {
                        double price = drakonioEconomy.getUpgradePrice(tierLevel);
                        String currencyName = drakonioEconomy.getCurrencyName();
                        newLore.add(Component.empty());
                        newLore.add(mm.deserialize("<yellow>💰 Preço: <gold>" + String.format("%.0f", price) + " " + currencyName).decoration(TextDecoration.ITALIC, false));
                        
                        double balance = drakonioEconomy.getBalance(player.getUniqueId());
                        if (balance >= price) {
                            newLore.add(mm.deserialize("<green>✔ Você pode comprar!").decoration(TextDecoration.ITALIC, false));
                        } else {
                            newLore.add(mm.deserialize("<red>✘ Saldo insuficiente").decoration(TextDecoration.ITALIC, false));
                        }
                    } else if (tierLevel <= currentTier) {
                        newLore.add(Component.empty());
                        newLore.add(mm.deserialize("<green>✔ Já adquirido!").decoration(TextDecoration.ITALIC, false));
                    }
                    
                    tm.lore(newLore);
                }
                tierItem.setItemMeta(tm);
                
                upgradeInv.setItem(slot, tierItem);
            }
        }
        
        player.openInventory(upgradeInv);
    }

    public void openEnderChest(Player viewer, UUID targetUUID, int pageIndex, boolean isEditing) {
        int tier = 0;
        if (viewer.getUniqueId().equals(targetUUID)) {
            tier = getEffectiveTier(viewer);
        } else {
            tier = databaseManager.getPurchasedTier(targetUUID);
            if (tier == 0) tier = 1; // Fallback admin
        }
        
        int maxPages = (tier <= 1) ? 1 : tier;

        if (pageIndex < 0 || pageIndex >= maxPages) return;

        Inventory inv;
        EnderChestHolder holder = new EnderChestHolder(targetUUID, pageIndex, maxPages, isEditing);

        if (maxPages == 1) {
            int size = 27; // Tier 0
            if (tier == 1) size = 36; // Tier 1
            inv = Bukkit.createInventory(holder, size, Component.text("EnderChest").color(NamedTextColor.LIGHT_PURPLE));
        } else {
            inv = Bukkit.createInventory(holder, 54, 
                    Component.text("EnderChest (Página " + (pageIndex + 1) + "/" + maxPages + ")")
                            .color(NamedTextColor.DARK_PURPLE));
        }

        ItemStack[] items = databaseManager.loadPage(targetUUID, pageIndex);
        int maxSlots = inv.getSize();
        if (maxPages > 1) maxSlots = 45; // Se tem paginação, 45 slots são itens
        
        for (int i = 0; i < Math.min(items.length, maxSlots); i++) {
            if (items[i] != null) inv.setItem(i, items[i]);
        }
        
        if (maxPages > 1) {
            setupControlBar(inv, pageIndex, maxPages);
        }

        viewer.openInventory(inv);
    }

    private void setupControlBar(Inventory inv, int pageIndex, int maxPages) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ConfigurationSection gui = plugin.getConfig().getConfigurationSection("gui.buttons");
        
        // Botão Voltar (Slot 45)
        if (pageIndex > 0) {
            inv.setItem(45, ItemBuilder.fromConfig(gui.getConfigurationSection("previous-page")));
        }

        // Botão Próxima (Slot 53)
        if (pageIndex < maxPages - 1) {
            inv.setItem(53, ItemBuilder.fromConfig(gui.getConfigurationSection("next-page")));
        }

        // Botão Auto-Organizar (Slot 49)
        inv.setItem(49, ItemBuilder.fromConfig(gui.getConfigurationSection("sort")));
        
        // Botão Senha (Slot 48)
        inv.setItem(48, ItemBuilder.fromConfig(gui.getConfigurationSection("password")));
        
        // Botão Upgrade (Slot 50)
        inv.setItem(50, ItemBuilder.fromConfig(gui.getConfigurationSection("upgrade")));
    }
    
    public void sortInventory(Inventory inv) {
        // Adaptação para inventários menores que 54 slots
        int inventorySize = inv.getSize();
        int storageSize = (inventorySize == 54) ? 45 : inventorySize;

        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < storageSize; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                contents.add(item);
            }
            inv.setItem(i, null); 
        }

        contents = stackItems(contents);

        contents.sort((i1, i2) -> {
            if (i1.getType() != i2.getType()) {
                return i1.getType().compareTo(i2.getType());
            }
            return i1.displayName().toString().compareTo(i2.displayName().toString());
        });

        for (int i = 0; i < contents.size() && i < storageSize; i++) {
            inv.setItem(i, contents.get(i));
        }
    }

    private List<ItemStack> stackItems(List<ItemStack> items) {
        List<ItemStack> stacked = new ArrayList<>();
        for (ItemStack item : items) {
            boolean merged = false;
            for (ItemStack s : stacked) {
                if (s.isSimilar(item)) {
                    int room = s.getMaxStackSize() - s.getAmount();
                    if (room > 0) {
                        int transfer = Math.min(room, item.getAmount());
                        s.setAmount(s.getAmount() + transfer);
                        item.setAmount(item.getAmount() - transfer);
                        if (item.getAmount() <= 0) {
                            merged = true;
                            break;
                        }
                    }
                }
            }
            if (!merged || item.getAmount() > 0) {
                stacked.add(item);
            }
        }
        return stacked;
    }
}
