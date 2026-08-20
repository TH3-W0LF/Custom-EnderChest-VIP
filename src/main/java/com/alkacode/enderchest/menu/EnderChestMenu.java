package com.alkacode.enderchest.menu;

import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.hook.AlkaVipsHook;
import com.alkacode.enderchest.hook.ItemsAdderFontHook;
import com.alkacode.enderchest.manager.EnderChestManager;
import com.alkacode.enderchest.util.ItemBuilder;
import com.alkacode.enderchest.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class EnderChestMenu {
    private static final long THREE_DAYS_MILLIS = TimeUnit.DAYS.toMillis(3);

    private final EnderChestRepository repository;
    private final JavaPlugin plugin;
    private final EnderChestManager enderChestManager;
    private final Messages messages;
    private final Supplier<AlkaVipsHook> alkaVipsHookSupplier;
    private final Supplier<ItemsAdderFontHook> itemsAdderFontHookSupplier;

    public EnderChestMenu(JavaPlugin plugin, EnderChestRepository repository, EnderChestManager enderChestManager,
                           Messages messages, Supplier<AlkaVipsHook> alkaVipsHookSupplier,
                           Supplier<ItemsAdderFontHook> itemsAdderFontHookSupplier) {
        this.plugin = plugin;
        this.repository = repository;
        this.enderChestManager = enderChestManager;
        this.messages = messages;
        this.alkaVipsHookSupplier = alkaVipsHookSupplier;
        this.itemsAdderFontHookSupplier = itemsAdderFontHookSupplier;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        open(player, player.getUniqueId(), page, false);
    }

    public void open(Player viewer, UUID targetUUID, int page, boolean isAdmin) {
        int tier;
        if (viewer.getUniqueId().equals(targetUUID)) {
            tier = enderChestManager.getEffectiveTier(viewer);
        } else {
            tier = repository.getTier(targetUUID);
            if (tier == 0) tier = 1;
        }
        int maxPages = Math.max(1, tier);

        if (page < 0 || page >= maxPages) {
            page = 0;
        }

        ItemStack[] pageItems = repository.loadPage(targetUUID, page);

        String titlePlaceholder = ":offset_-49::ui_enderchest:";
        String title = "Ender Chest";

        if (isAdmin && !viewer.getUniqueId().equals(targetUUID)) {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);
            String targetName = targetPlayer.getName();
            if (targetName == null) targetName = targetUUID.toString();
            title = "Ender Chest de " + targetName;
        } else {
            ItemsAdderFontHook itemsAdderFontHook = itemsAdderFontHookSupplier.get();
            if (itemsAdderFontHook != null) {
                String processedTitle = itemsAdderFontHook.replaceFontImages(titlePlaceholder);
                if (processedTitle != null && !processedTitle.equals(titlePlaceholder) && !processedTitle.isEmpty()) {
                    title = processedTitle;
                }
            }
        }

        if (maxPages > 1) {
            title += " - Pag " + (page + 1) + "/" + maxPages;
        }

        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);
        Inventory inv = Bukkit.createInventory(
                new EnderChestHolder(targetUUID, page, maxPages, isAdmin),
                54,
                titleComponent
        );

        for (int slot = 0; slot < EnderChestRepository.PAGE_SIZE; slot++) {
            ItemStack item = pageItems[slot];
            if (item != null && !item.getType().isAir()) {
                inv.setItem(slot, item.clone());
            }
        }

        if (!isAdmin) {
            setupControlBar(inv, page, maxPages);
            setupVipWarning(inv, viewer);
        } else {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glass.getItemMeta();
            if (glassMeta != null) {
                glassMeta.displayName(Component.empty());
                glass.setItemMeta(glassMeta);
            }
            for (int i = 45; i < 54; i++) {
                inv.setItem(i, glass);
            }
        }

        viewer.openInventory(inv);
    }

    private void setupControlBar(Inventory inv, int pageIndex, int maxPages) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.empty());
            glass.setItemMeta(glassMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ConfigurationSection gui = plugin.getConfig().getConfigurationSection("gui.buttons");
        if (gui == null) return;

        if (pageIndex > 0) {
            ConfigurationSection prevPage = gui.getConfigurationSection("previous-page");
            if (prevPage != null) {
                inv.setItem(45, ItemBuilder.fromConfig(prevPage));
            }
        }

        if (pageIndex < maxPages - 1) {
            ConfigurationSection nextPage = gui.getConfigurationSection("next-page");
            if (nextPage != null) {
                inv.setItem(53, ItemBuilder.fromConfig(nextPage));
            }
        }

        ConfigurationSection sort = gui.getConfigurationSection("sort");
        if (sort != null) {
            inv.setItem(49, ItemBuilder.fromConfig(sort));
        }

        ConfigurationSection password = gui.getConfigurationSection("password");
        if (password != null) {
            inv.setItem(48, ItemBuilder.fromConfig(password));
        }

        ConfigurationSection upgrade = gui.getConfigurationSection("upgrade");
        if (upgrade != null) {
            inv.setItem(50, ItemBuilder.fromConfig(upgrade));
        }
    }

    /**
     * Item de aviso (slot 47, livre no controle atual) quando o VIP do jogador esta a
     * <= 3 dias de expirar E isso custaria paginas extra (ver
     * EnderChestManager#getPagesAtRisk) - so aparece se o AlkaVips estiver presente,
     * ja que so ele sabe a data real de expiracao (permissao pura nao carrega isso).
     */
    private void setupVipWarning(Inventory inv, Player viewer) {
        int pagesAtRisk = enderChestManager.getPagesAtRisk(viewer);
        if (pagesAtRisk <= 0) {
            return;
        }
        AlkaVipsHook alkaVipsHook = alkaVipsHookSupplier.get();
        if (alkaVipsHook == null) {
            return;
        }
        Optional<Long> remaining = alkaVipsHook.activeVipRemainingMillis(viewer);
        if (remaining.isEmpty() || remaining.get() > THREE_DAYS_MILLIS) {
            return;
        }
        long days = Math.max(1, (remaining.get() / TimeUnit.DAYS.toMillis(1)) + 1);

        ItemStack warning = new ItemStack(Material.CLOCK);
        ItemMeta meta = warning.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.getNoPrefix("vip-warning.gui-item-name",
                    "<days>", String.valueOf(days)));
            meta.lore(messages.getList("vip-warning.gui-item-lore",
                    "<days>", String.valueOf(days), "<pages>", String.valueOf(pagesAtRisk)));
            warning.setItemMeta(meta);
        }
        inv.setItem(47, warning);
    }

    public void sortInventory(Inventory inv) {
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
