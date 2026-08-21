package com.alkacode.enderchest.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.enderchest.economy.EconomyService;
import com.alkacode.enderchest.manager.EnderChestManager;
import com.alkacode.enderchest.service.EnderChestService;
import com.alkacode.enderchest.util.ItemBuilder;
import com.alkacode.enderchest.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Loja de upgrades do EnderChest - estende {@link BaseGui} do AlkaCore (Core registra
 * o GuiListener globalmente, este plugin nunca registra o proprio). Diferente do menu
 * principal do EC (que continua com InventoryHolder proprio - ver EnderChestHolder),
 * aqui todos os slots sao botoes/informativos, sem conteudo editavel pelo jogador,
 * entao o contrato "handleClick sempre cancela" do BaseGui se encaixa sem nenhuma
 * perda de funcionalidade.
 *
 * Comprar um tier aqui NAO desbloqueia o tier direto - da ao jogador um "Cristal de
 * Tier" fisico que precisa ser usado (clique direito) pra ativar, mesmo fluxo de duas
 * etapas do menu antigo (ver EnderChestListener#onTierCrystalInteract).
 */
public class EnderChestUpgradeGui extends BaseGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final EnderChestManager enderChestManager;
    private final EconomyService economyService;
    private final EnderChestService enderChestService;
    private final Messages messages;

    public EnderChestUpgradeGui(JavaPlugin plugin, Player player, EnderChestManager enderChestManager,
                                 EconomyService economyService, EnderChestService enderChestService,
                                 Messages messages) {
        super(plugin, player, "<dark_purple>Loja de Upgrades", 6, "ec_upgrade");
        this.enderChestManager = enderChestManager;
        this.economyService = economyService;
        this.enderChestService = enderChestService;
        this.messages = messages;
    }

    @Override
    public void render() {
        ConfigurationSection upgradeConfig = plugin.getConfig().getConfigurationSection("upgrades");
        if (upgradeConfig == null) {
            return;
        }

        ConfigurationSection infoItem = upgradeConfig.getConfigurationSection("info-item");
        if (infoItem != null) {
            setItem(infoItem.getInt("slot", 4), ItemBuilder.fromConfig(infoItem));
        }

        ConfigurationSection backButton = upgradeConfig.getConfigurationSection("back-button");
        if (backButton != null) {
            setItem(backButton.getInt("slot", 49), ItemBuilder.fromConfig(backButton), e -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                player.closeInventory();
                enderChestService.openEnderChestDirect(player, 0);
            });
        }

        int currentTier = enderChestManager.getEffectiveTier(player);
        int nextTier = currentTier + 1;

        ConfigurationSection tiers = upgradeConfig.getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int tierLevel = Integer.parseInt(key);
                int slot = tiers.getInt(key);
                setItem(slot, buildTierItem(upgradeConfig, key, tierLevel, currentTier, nextTier),
                        tierLevel == nextTier ? e -> attemptPurchase(tierLevel) : null);
            }
        }

        fill(glass(upgradeConfig));
    }

    private void attemptPurchase(int tier) {
        int currentTier = enderChestManager.getEffectiveTier(player);
        int nextTier = currentTier + 1;
        if (tier != nextTier) {
            // estado mudou entre o render e o clique (ex: outro admin alterou o tier) - re-renderiza em vez de agir sobre um slot obsoleto.
            refresh();
            return;
        }

        double cost = economyService.getUpgradePrice(tier);
        if (!economyService.has(player.getUniqueId(), cost)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage(messages.get("upgrades.purchase-fail",
                    "<price>", String.format("%.0f", cost), "<currency>", economyService.getCurrencyName()));
            return;
        }

        economyService.withdraw(player.getUniqueId(), cost);
        player.getInventory().addItem(buildTierCrystal(tier));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        player.sendMessage(messages.get("upgrades.purchase-success", "<tier>", String.valueOf(tier)));

        refresh();
    }

    private ItemStack buildTierCrystal(int tier) {
        ConfigurationSection tierItemConfig = plugin.getConfig().getConfigurationSection("upgrades.tier-item");
        ConfigurationSection crystalConfig = plugin.getConfig().getConfigurationSection("upgrades.tier-crystal");
        ItemStack item = new ItemStack(Material.valueOf(tierItemConfig.getString("material", "PAPER")));
        ItemMeta meta = item.getItemMeta();

        if (tierItemConfig.contains("custom_model_data")) {
            int cmd = tierItemConfig.getInt("custom_model_data");
            if (cmd > 0) meta.setCustomModelData(cmd);
        }

        String tierStr = String.valueOf(tier);
        meta.displayName(MM.deserialize(crystalConfig.getString("name", "").replace("<tier>", tierStr))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : crystalConfig.getStringList("lore")) {
            lore.add(line.isEmpty() ? Component.empty()
                    : MM.deserialize(line.replace("<tier>", tierStr)).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTierItem(ConfigurationSection upgradeConfig, String key, int tierLevel, int currentTier, int nextTier) {
        ItemStack tierItem = ItemBuilder.fromConfig(upgradeConfig.getConfigurationSection("tier-item"));
        ItemMeta meta = tierItem.getItemMeta();
        ConfigurationSection states = upgradeConfig.getConfigurationSection("tier-item.states");

        if (tierLevel <= currentTier) {
            String name = states.getString("acquired.name", "<green>Tier <tier> (Adquirido)").replace("<tier>", key);
            meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (tierLevel == nextTier) {
            if (meta.hasDisplayName()) {
                meta.displayName(meta.displayName().replaceText(b -> b.matchLiteral("<tier>").replacement(key)));
            }
        } else {
            tierItem.setType(Material.BARRIER);
            meta = tierItem.getItemMeta();
            String name = states.getString("locked.name", "<red><st>Tier <tier></st> <gray>(Bloqueado)").replace("<tier>", key);
            meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }

        if (meta.hasLore()) {
            List<Component> newLore = new ArrayList<>();
            if (meta.lore() != null) {
                for (Component line : meta.lore()) {
                    newLore.add(line.replaceText(b -> b.matchLiteral("<tier>").replacement(key)));
                }
            }

            if (tierLevel == nextTier) {
                double price = economyService.getUpgradePrice(tierLevel);
                String currencyName = economyService.getCurrencyName();
                newLore.add(Component.empty());
                String priceLine = states.getString("next.price-line", "<yellow>Preco: <gold><price> <currency>")
                        .replace("<price>", String.format("%.0f", price))
                        .replace("<currency>", currencyName);
                newLore.add(MM.deserialize(priceLine).decoration(TextDecoration.ITALIC, false));

                double balance = economyService.getBalance(player.getUniqueId());
                String statusLine = balance >= price
                        ? states.getString("next.lore-can-buy", "<green>Voce pode comprar!")
                        : states.getString("next.lore-cant-buy", "<red>Saldo insuficiente");
                newLore.add(MM.deserialize(statusLine).decoration(TextDecoration.ITALIC, false));
            } else if (tierLevel <= currentTier) {
                for (String line : states.getStringList("acquired.lore-extra")) {
                    newLore.add(line.isEmpty() ? Component.empty() : MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
            } else {
                for (String line : states.getStringList("locked.lore-extra")) {
                    String replaced = line.replace("<current-tier>", String.valueOf(currentTier));
                    newLore.add(replaced.isEmpty() ? Component.empty() : MM.deserialize(replaced).decoration(TextDecoration.ITALIC, false));
                }
            }

            meta.lore(newLore);
        }
        tierItem.setItemMeta(meta);
        return tierItem;
    }

    private ItemStack glass(ConfigurationSection upgradeConfig) {
        String matName = upgradeConfig.getString("border.material", "GRAY_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        return glass;
    }
}
