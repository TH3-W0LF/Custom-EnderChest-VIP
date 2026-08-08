package com.alkacode.enderchest.listener;

import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.economy.EconomyService;
import com.alkacode.enderchest.gui.EnderChestUpgradeGui;
import com.alkacode.enderchest.manager.EnderChestManager;
import com.alkacode.enderchest.menu.EnderChestHolder;
import com.alkacode.enderchest.service.EnderChestService;
import com.alkacode.enderchest.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Listener do ciclo do EnderChest customizado (bloco fisico, GUI de conteudo, cristais
 * de tier). A loja de upgrades NAO passa mais por aqui - virou {@link EnderChestUpgradeGui}
 * (BaseGui do AlkaCore), cujos cliques o GuiListener global do Core ja trata.
 *
 * O menu de conteudo do EC continua com {@link EnderChestHolder} proprio (nao um
 * BaseGui): o jogador precisa mover itens livremente nos slots 0-44, e o GuiListener
 * do Core cancela incondicionalmente qualquer clique fora do inventario do proprio
 * BaseGui (inclusive shift-click do inventario do jogador PARA dentro do bau) - isso
 * quebraria o deposito rapido por shift-click, que hoje funciona. Ver decisao
 * registrada na migracao para o AlkaCore.
 */
public class EnderChestListener implements Listener {

    private final JavaPlugin plugin;
    private final EnderChestService service;
    private final EnderChestManager enderChestManager;
    private final EnderChestRepository repository;
    private final EconomyService economyService;
    private final Messages messages;

    public EnderChestListener(JavaPlugin plugin, EnderChestService service, EnderChestManager enderChestManager,
                               EnderChestRepository repository, EconomyService economyService, Messages messages) {
        this.plugin = plugin;
        this.service = service;
        this.enderChestManager = enderChestManager;
        this.repository = repository;
        this.economyService = economyService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().isBlock()) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);

        enderChestManager.registerOpenedBlock(player, block);
        if (block.getState() instanceof EnderChest ec) {
            ec.open();
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> service.openEnderChest(player, 0));
    }

    @EventHandler
    public void onTierCrystalInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        Player player = event.getPlayer();

        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("upgrades.tier-item");
        if (itemConfig == null) return;

        String configMaterial = itemConfig.getString("material", "PAPER");
        if (item.getType() != Material.valueOf(configMaterial)) return;

        if (itemConfig.contains("custom_model_data")) {
            int configCMD = itemConfig.getInt("custom_model_data");
            if (configCMD > 0) {
                if (!item.getItemMeta().hasCustomModelData()) return;
                if (item.getItemMeta().getCustomModelData() != configCMD) return;
            }
        }

        if (!item.getItemMeta().hasDisplayName()) return;

        String displayName;
        try {
            displayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());
        } catch (Exception e) {
            displayName = item.getItemMeta().getDisplayName();
        }

        if (!displayName.contains("Tier")) return;

        try {
            String[] parts = displayName.split(" ");
            int tier = -1;

            for (int i = parts.length - 1; i >= 0; i--) {
                try {
                    tier = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
                    if (tier >= 2 && tier <= 29) break;
                } catch (NumberFormatException ignored) {
                }
            }

            if (tier == -1 || tier < 2 || tier > 29) return;

            event.setCancelled(true);

            int currentTier = enderChestManager.getEffectiveTier(player);
            int nextTier = currentTier + 1;

            if (tier <= currentTier) {
                player.sendMessage(messages.get("upgrades.tier-already-owned"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            if (tier != nextTier) {
                player.sendMessage(messages.get("upgrades.tier-locked-crystal",
                        "<next>", String.valueOf(nextTier), "<current>", String.valueOf(currentTier)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            repository.setTier(player.getUniqueId(), tier);

            player.sendMessage(messages.get("upgrades.tier-activated", "<tier>", String.valueOf(tier)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);

            item.setAmount(item.getAmount() - 1);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao processar Cristal de Tier: " + e.getMessage());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isShulkerBox(event.getCurrentItem()) || isShulkerBox(event.getCursor())) {
            if (event.getClickedInventory() == event.getView().getTopInventory() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                player.sendMessage(messages.get("enderchest.shulker-blocked"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                return;
            }
        }

        int slot = event.getSlot();
        if (slot < 45 || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (slot == 53 && holder.page() < holder.maxPages() - 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            service.openEnderChestDirect(player, holder.page() + 1);
        } else if (slot == 45 && holder.page() > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            service.openEnderChestDirect(player, holder.page() - 1);
        } else if (slot == 49) {
            player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1, 1);
            service.sortInventory(event.getInventory());
            player.sendMessage(messages.get("enderchest.sorted"));
        } else if (slot == 48) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            player.closeInventory();

            for (Component line : messages.getList("password.security-info")) {
                player.sendMessage(line);
            }
        } else if (slot == 50) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
            player.closeInventory();
            new EnderChestUpgradeGui(plugin, player, enderChestManager, economyService, service, messages).open();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
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
        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID ownerUUID = holder.ownerUUID();

        // Cada pagina e uma unidade opaca de 45 slots - a posicao exata do jogador e
        // preservada, mesmo que paginas anteriores estejam 100% vazias. Nao ha merge
        // com outras paginas nem remocao de slots vazios no meio do array.
        org.bukkit.inventory.Inventory inv = event.getInventory();
        ItemStack[] pageItems = new ItemStack[EnderChestRepository.PAGE_SIZE];
        for (int i = 0; i < EnderChestRepository.PAGE_SIZE; i++) {
            ItemStack item = inv.getItem(i);
            pageItems[i] = (item != null && item.getType() != Material.AIR) ? item.clone() : null;
        }

        service.getRepository().savePage(ownerUUID, holder.page(), pageItems);

        enderChestManager.closeOpenedBlock(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.getRepository().clearCache(event.getPlayer().getUniqueId());
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getType().name().endsWith("SHULKER_BOX");
    }
}
