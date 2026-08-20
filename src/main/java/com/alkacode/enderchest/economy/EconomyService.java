package com.alkacode.enderchest.economy;

import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

/**
 * Ponte para uma moeda do AlkaEconomy, resolvida por id configuravel em
 * {@code economias.yml} (chave {@code currency-id}, padrao "alkarion" - preserva o
 * comportamento original desse plugin, que nasceu em torno da economia Alkarion real
 * desta rede). Nao hardcoda mais a moeda: qualquer id valido no AlkaEconomy funciona,
 * inclusive uma criada depois de o plugin ja estar rodando (basta reload) - mesmo
 * padrao de resolucao por string usado no AlkaVips/AlkaMines (ver
 * feedback-currency-id-pattern). Consome o EconomyManager diretamente, sem Vault como
 * intermediario, ja que o AlkaEconomy e depend obrigatorio (plugin.yml).
 */
public class EconomyService {

    private static final String DEFAULT_CURRENCY_ID = "alkarion";

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private FileConfiguration economiasConfig;
    private String currencyId;
    private String currencyName;

    public EconomyService(JavaPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.economiasConfig = loadEconomiasConfig(plugin);
        resolveCurrency();
    }

    public void reload() {
        economiasConfig = loadEconomiasConfig(plugin);
        resolveCurrency();
    }

    private void resolveCurrency() {
        String requested = economiasConfig.getString("currency-id", DEFAULT_CURRENCY_ID).toLowerCase(Locale.ROOT);
        if (!economyManager.isValidCurrency(requested)) {
            plugin.getLogger().warning("Moeda '" + requested + "' configurada em economias.yml (currency-id) nao "
                    + "existe no AlkaEconomy - usando '" + DEFAULT_CURRENCY_ID + "'.");
            requested = DEFAULT_CURRENCY_ID;
        }
        String resolved = requested;
        this.currencyId = resolved;
        this.currencyName = economyManager.getCurrencies().stream()
                .filter(currency -> currency.id().equals(resolved))
                .findFirst()
                .map(CurrencyDefinition::name)
                .orElse(resolved);
    }

    private FileConfiguration loadEconomiasConfig(JavaPlugin plugin) {
        File economiasFile = new File(plugin.getDataFolder(), "economias.yml");
        if (!economiasFile.exists()) {
            try (InputStream in = plugin.getResource("economias.yml")) {
                if (in != null) {
                    Files.copy(in, economiasFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar economias.yml: " + e.getMessage());
            }
        }
        return YamlConfiguration.loadConfiguration(economiasFile);
    }

    public boolean has(UUID uuid, double amount) {
        return economyManager.has(uuid, currencyId, amount);
    }

    public double getBalance(UUID uuid) {
        return economyManager.getBalance(uuid, currencyId);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!economyManager.has(uuid, currencyId, amount)) {
            return false;
        }
        economyManager.removeBalance(uuid, currencyId, amount);
        return true;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public double getUpgradePrice(int tier) {
        return economiasConfig.getDouble("upgrade-prices." + tier, 1000.0 * tier);
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }
}
