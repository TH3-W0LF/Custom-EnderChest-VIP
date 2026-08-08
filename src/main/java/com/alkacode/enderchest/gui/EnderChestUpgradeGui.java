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

        setItem(4, buildInfoItem());

        ConfigurationSection backButton = upgradeConfig.getConfigurationSection("back-button");
        if (backButton != null) {
            setItem(49, ItemBuilder.fromConfig(backButton), e -> {
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

        fill(glass());
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
        ItemStack item = new ItemStack(Material.valueOf(tierItemConfig.getString("material", "PAPER")));
        ItemMeta meta = item.getItemMeta();

        if (tierItemConfig.contains("custom_model_data")) {
            int cmd = tierItemConfig.getInt("custom_model_data");
            if (cmd > 0) meta.setCustomModelData(cmd);
        }

        meta.displayName(MM.deserialize("<#b57edc>Cristal de Tier " + tier).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                MM.deserialize("<gray>Use este item para liberar").decoration(TextDecoration.ITALIC, false),
                MM.deserialize("<gray>a Pagina " + tier + " do EnderChest.").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MM.deserialize("<yellow>Clique com botao direito para usar!").decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(MM.deserialize("<gradient:#FFAA00:#FFE055><bold>Loja de Upgrades</bold></gradient>").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                MM.deserialize(" <gray>Aqui voce pode comprar").decoration(TextDecoration.ITALIC, false),
                MM.deserialize(" <#b57edc>Cristais de Tier</#b57edc> <gray>para evoluir").decoration(TextDecoration.ITALIC, false),
                MM.deserialize(" <gray>o seu <white>EnderChest</white> ou de amigos.").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MM.deserialize("<gradient:#FF4500:#8B0000>Exclusivo AlkaCode</gradient>").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                MM.deserialize(" <italic><dark_gray>Duvidas? Entre em contato com a Staff.").decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(meta);
        return info;
    }

    private ItemStack buildTierItem(ConfigurationSection upgradeConfig, String key, int tierLevel, int currentTier, int nextTier) {
        ItemStack tierItem = ItemBuilder.fromConfig(upgradeConfig.getConfigurationSection("tier-item"));
        ItemMeta meta = tierItem.getItemMeta();

        if (tierLevel <= currentTier) {
            meta.displayName(MM.deserialize("<green>Tier " + tierLevel + " (Adquirido)").decoration(TextDecoration.ITALIC, false));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (tierLevel == nextTier) {
            if (meta.hasDisplayName()) {
                meta.displayName(meta.displayName().replaceText(b -> b.matchLiteral("<tier>").replacement(key)));
            }
        } else {
            tierItem.setType(Material.BARRIER);
            meta = tierItem.getItemMeta();
            meta.displayName(MM.deserialize("<red><st>Tier " + tierLevel + "</st> <gray>(Bloqueado)").decoration(TextDecoration.ITALIC, false));
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
                newLore.add(MM.deserialize("<yellow>Preco: <gold>" + String.format("%.0f", price) + " " + currencyName).decoration(TextDecoration.ITALIC, false));

                double balance = economyService.getBalance(player.getUniqueId());
                if (balance >= price) {
                    newLore.add(MM.deserialize("<green>Voce pode comprar!").decoration(TextDecoration.ITALIC, false));
                } else {
                    newLore.add(MM.deserialize("<red>Saldo insuficiente").decoration(TextDecoration.ITALIC, false));
                }
            } else if (tierLevel <= currentTier) {
                newLore.add(Component.empty());
                newLore.add(MM.deserialize("<green>Ja adquirido!").decoration(TextDecoration.ITALIC, false));
            } else {
                newLore.add(Component.empty());
                newLore.add(MM.deserialize("<red>Bloqueado").decoration(TextDecoration.ITALIC, false));
                newLore.add(MM.deserialize("<gray>Complete o Tier " + currentTier + " primeiro").decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(newLore);
        }
        tierItem.setItemMeta(meta);
        return tierItem;
    }

    private ItemStack glass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        return glass;
    }
}
