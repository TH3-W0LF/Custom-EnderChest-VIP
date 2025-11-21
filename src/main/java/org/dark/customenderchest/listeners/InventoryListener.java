package org.dark.customenderchest.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.economy.DrakonioEconomy;
import org.dark.customenderchest.manager.EnderChestHolder;
import org.dark.customenderchest.manager.EnderChestManager;
import org.dark.customenderchest.manager.UpgradeHolder;
import org.dark.customenderchest.utils.LogUtils;

import java.util.Arrays;
import java.util.List;

public class InventoryListener implements Listener {

    private final CustomEnderChest plugin;
    private final EnderChestManager enderChestManager;
    private final DatabaseManager databaseManager;
    private DrakonioEconomy drakonioEconomy; // Não final pois será injetado depois
    private final MiniMessage mm = MiniMessage.miniMessage();

    public InventoryListener(CustomEnderChest plugin, EnderChestManager enderChestManager, DatabaseManager databaseManager, DrakonioEconomy drakonioEconomy) {
        this.plugin = plugin;
        this.enderChestManager = enderChestManager;
        this.databaseManager = databaseManager;
        this.drakonioEconomy = drakonioEconomy;
    }
    
    public void setDrakonioEconomy(DrakonioEconomy drakonioEconomy) {
        this.drakonioEconomy = drakonioEconomy;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof UpgradeHolder) {
            event.setCancelled(true); 
            
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getSlot();
            
            if (slot == 49) { // Voltar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                enderChestManager.openEnderChest(player, player.getUniqueId(), 0, false);
                return;
            }
            
            // Lógica de Compra
            ConfigurationSection tiers = plugin.getConfig().getConfigurationSection("upgrades.tiers");
            if (tiers != null) {
                int tierFound = -1;
                for(String key : tiers.getKeys(false)) {
                    if(tiers.getInt(key) == slot) {
                        tierFound = Integer.parseInt(key);
                        break;
                    }
                }
                
                if (tierFound != -1) {
                    // Verifica se a economia está configurada
                    if (drakonioEconomy == null || !drakonioEconomy.isConnected()) {
                        player.sendMessage(mm.deserialize("<red>✘ Sistema de economia não está disponível! Tente novamente em alguns segundos."));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                        plugin.getLogger().severe("Jogador " + player.getName() + " tentou comprar mas a economia não está conectada!");
                        return;
                    }
                    
                    // Permite comprar mesmo se já tiver, para poder presentear outros jogadores
                    double cost = drakonioEconomy.getUpgradePrice(tierFound);
                    
                    if (drakonioEconomy.has(player.getUniqueId(), cost)) {
                        drakonioEconomy.withdraw(player.getUniqueId(), cost);
                        
                        // Cria o item do zero baseado na config
                        ConfigurationSection tierItemConfig = plugin.getConfig().getConfigurationSection("upgrades.tier-item");
                        ItemStack item = new ItemStack(Material.valueOf(tierItemConfig.getString("material", "PAPER")));
                        ItemMeta meta = item.getItemMeta();
                        
                        // Define o CustomModelData
                        if (tierItemConfig.contains("custom_model_data")) {
                            int cmd = tierItemConfig.getInt("custom_model_data");
                            if (cmd > 0) {
                                meta.setCustomModelData(cmd);
                            }
                        }
                        
                        // Define o nome do item
                        meta.displayName(mm.deserialize("<#b57edc>Cristal de Tier " + tierFound).decoration(TextDecoration.ITALIC, false));
                        
                        // Define a lore do item
                        meta.lore(List.of(
                            mm.deserialize("<gray>Use este item para liberar").decoration(TextDecoration.ITALIC, false),
                            mm.deserialize("<gray>a Página " + tierFound + " do EnderChest.").decoration(TextDecoration.ITALIC, false),
                            Component.empty(),
                            mm.deserialize("<yellow>⚠ Clique com botão direito para usar!").decoration(TextDecoration.ITALIC, false)
                        ));
                        
                        item.setItemMeta(meta);
                        player.getInventory().addItem(item);
                        
                        plugin.getLogger().info("Item criado para " + player.getName() + ": Material=" + item.getType() + 
                            ", CMD=" + (meta.hasCustomModelData() ? meta.getCustomModelData() : "nenhum") +
                            ", Nome=" + PlainTextComponentSerializer.plainText().serialize(meta.displayName()));
                        
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                        
                        String msg = drakonioEconomy.getMessage("purchase-success");
                        if (msg.isEmpty()) {
                            msg = "<green>✔ Você comprou o Cristal de Tier <tier>!";
                        }
                        msg = msg.replace("<tier>", String.valueOf(tierFound));
                        player.sendMessage(mm.deserialize(msg));
                        
                    } else {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                        
                        String msg = drakonioEconomy.getMessage("purchase-fail");
                        if (msg.isEmpty()) {
                            msg = "<red>✘ Saldo insuficiente! Necessário: <price> <currency>";
                        }
                        msg = msg.replace("<price>", String.format("%.0f", cost))
                                 .replace("<currency>", drakonioEconomy.getCurrencyName());
                        player.sendMessage(mm.deserialize(msg));
                    }
                }
            }
            
            return;
        }

        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (isShulkerBox(event.getCurrentItem()) || isShulkerBox(event.getCursor())) {
             if (event.getClickedInventory() == event.getView().getTopInventory() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                 if (event.getClickedInventory() == event.getView().getBottomInventory() && event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                     // Seguro
                 } else {
                     event.setCancelled(true);
                     player.sendMessage(Component.text("Você não pode colocar Shulker Boxes no EnderChest!", NamedTextColor.RED));
                     player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                     return;
                 }
             }
        }

        if (holder.maxPages() > 1 && event.getClickedInventory() == event.getView().getTopInventory()) {
            int slot = event.getSlot();

            if (slot >= 45) {
                event.setCancelled(true);

                if (slot == 45) { 
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    enderChestManager.openEnderChest(player, holder.ownerUUID(), holder.currentPage() - 1, holder.isEditing());
                } else if (slot == 53) { 
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    enderChestManager.openEnderChest(player, holder.ownerUUID(), holder.currentPage() + 1, holder.isEditing());
                } else if (slot == 49) { 
                    player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1, 1);
                    enderChestManager.sortInventory(event.getInventory());
                    player.sendMessage(Component.text("Itens organizados!", NamedTextColor.GREEN));
                } else if (slot == 48) { 
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    player.closeInventory();
                    
                    player.sendMessage(Component.empty());
                    player.sendMessage(mm.deserialize("<gradient:#FFD700:#FFA500><bold>SISTEMA DE SEGURANÇA</bold></gradient>"));
                    player.sendMessage(Component.empty());
                    player.sendMessage(mm.deserialize("<gray>Para definir sua senha, utilize o comando:"));
                    player.sendMessage(mm.deserialize("<yellow>/ec senha <senha> <repetir_senha>"));
                    player.sendMessage(Component.empty());
                    player.sendMessage(mm.deserialize("<red><bold>⚠ AVISO:</bold> <gray>Errar a senha <red>5 vezes <gray>resultará em"));
                    player.sendMessage(mm.deserialize("<red>bloqueio temporário <gray>do seu EnderChest."));
                    player.sendMessage(Component.empty());
                    
                } else if (slot == 50) { 
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
                    enderChestManager.openUpgradeMenu(player);
                }
            }
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        
        Player player = event.getPlayer();
        
        // Verifica se é um Cristal de Tier
        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("upgrades.tier-item");
        if (itemConfig == null) {
            plugin.getLogger().warning("Configuração 'upgrades.tier-item' não encontrada!");
            return;
        }
        
        // Verifica o material primeiro
        String configMaterial = itemConfig.getString("material", "PAPER");
        if (item.getType() != Material.valueOf(configMaterial)) return;
        
        // Log de debug
        plugin.getLogger().info("Item detectado: " + item.getType());
        plugin.getLogger().info("Tem ItemMeta: " + item.hasItemMeta());
        plugin.getLogger().info("Tem DisplayName: " + item.getItemMeta().hasDisplayName());
        
        // Verifica CustomModelData se configurado
        if (itemConfig.contains("custom_model_data")) {
            int configCMD = itemConfig.getInt("custom_model_data");
            if (configCMD > 0) {
                if (!item.getItemMeta().hasCustomModelData()) {
                    plugin.getLogger().info("Item não tem CustomModelData");
                    return;
                }
                if (item.getItemMeta().getCustomModelData() != configCMD) {
                    plugin.getLogger().info("CustomModelData diferente: " + item.getItemMeta().getCustomModelData() + " vs " + configCMD);
                    return;
                }
                plugin.getLogger().info("CustomModelData correto: " + configCMD);
            }
        }
        
        // Extrai o Tier do nome do item
        if (!item.getItemMeta().hasDisplayName()) {
            plugin.getLogger().info("Item não tem DisplayName");
            return;
        }
        
        // Tenta diferentes formas de pegar o displayName
        String displayName;
        try {
            displayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());
        } catch (Exception e) {
            // Fallback para método legado
            displayName = item.getItemMeta().getDisplayName();
        }
        
        plugin.getLogger().info("DisplayName: " + displayName);
        
        // Verifica se o nome contém "Cristal" e "Tier"
        if (!displayName.contains("Tier")) {
            plugin.getLogger().info("Nome não contém 'Tier'");
            return;
        }
        
        try {
            // Pega o último número encontrado no nome
            String[] parts = displayName.split(" ");
            int tier = -1;
            
            for (int i = parts.length - 1; i >= 0; i--) {
                try {
                    tier = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
                    if (tier >= 2 && tier <= 11) break;
                } catch (NumberFormatException ignored) {
                }
            }
            
            plugin.getLogger().info("Tier detectado: " + tier);
            
            if (tier == -1 || tier < 2 || tier > 11) {
                plugin.getLogger().warning("Tier inválido: " + tier);
                return;
            }
            
            event.setCancelled(true);
            
            int currentTier = enderChestManager.getEffectiveTier(player);
            plugin.getLogger().info("Tier atual do jogador: " + currentTier);
            
            if (tier <= currentTier) {
                String msg = drakonioEconomy.getMessage("tier-already-owned");
                if (msg.isEmpty()) {
                    msg = "<yellow>⚠ Você já possui este Tier ou superior!";
                }
                player.sendMessage(mm.deserialize(msg));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }
            
            // Aplica o Tier
            databaseManager.setPurchasedTier(player.getUniqueId(), tier);
            plugin.getLogger().info("Tier " + tier + " aplicado para " + player.getName());
            
            String msg = drakonioEconomy.getMessage("tier-activated");
            if (msg.isEmpty()) {
                msg = "<gradient:#00FF00:#00AA00>✔ Tier <tier> ativado! Seu EnderChest foi expandido.</gradient>";
            }
            msg = msg.replace("<tier>", String.valueOf(tier));
            
            player.sendMessage(mm.deserialize(msg));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            
            // Remove o item da mão do jogador
            item.setAmount(item.getAmount() - 1);
            
        } catch (Exception e) {
            plugin.getLogger().severe("ERRO ao processar Cristal de Tier: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof UpgradeHolder) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof EnderChestHolder)) return;
        
        if (isShulkerBox(event.getOldCursor()) || isShulkerBox(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        
        if (event.getInventory().getSize() == 54) {
            for (int slot : event.getRawSlots()) {
                if (slot >= 45 && slot < 54) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof UpgradeHolder) {
            return; 
        }

        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        ItemStack[] itemsToSave;

        if (holder.maxPages() > 1) {
            itemsToSave = Arrays.copyOfRange(inv.getContents(), 0, 45);
        } else {
            itemsToSave = inv.getContents();
        }

        databaseManager.savePage(holder.ownerUUID(), holder.currentPage(), itemsToSave);
        enderChestManager.closeOpenedBlock(player);

        if (holder.isEditing()) {
            LogUtils.log("Admin " + player.getName() + " fechou/editou o EC de " + holder.ownerUUID());
            player.sendMessage(Component.text("EnderChest salvo.", NamedTextColor.YELLOW));
        }
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getType().name().endsWith("SHULKER_BOX");
    }
}
