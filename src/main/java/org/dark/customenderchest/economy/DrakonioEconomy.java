package org.dark.customenderchest.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

public class DrakonioEconomy {

    private final JavaPlugin plugin;
    private Economy vaultEconomy;
    private String currencyName;
    private FileConfiguration economiasConfig;

    public DrakonioEconomy(JavaPlugin plugin) {
        this.plugin = plugin;
        loadEconomiasConfig();
        reloadConfig();
        setupEconomy();
    }
    
    private void loadEconomiasConfig() {
        File economiasFile = new File(plugin.getDataFolder(), "economias.yml");
        
        if (!economiasFile.exists()) {
            try (InputStream in = plugin.getResource("economias.yml")) {
                if (in != null) {
                    Files.copy(in, economiasFile.toPath());
                    plugin.getLogger().info("Arquivo economias.yml criado com sucesso!");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Não foi possível criar economias.yml: " + e.getMessage());
            }
        }
        
        economiasConfig = YamlConfiguration.loadConfiguration(economiasFile);
    }

    public void reloadConfig() {
        this.currencyName = economiasConfig.getString("display-name", "Money");
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("======================================");
            plugin.getLogger().warning("⚠ Vault não encontrado!");
            plugin.getLogger().warning("O sistema de economia não funcionará.");
            plugin.getLogger().warning("======================================");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("======================================");
            plugin.getLogger().warning("⚠ Nenhum provedor de economia encontrado no Vault!");
            plugin.getLogger().warning("Certifique-se de que um plugin de economia está instalado.");
            plugin.getLogger().warning("======================================");
            return;
        }

        vaultEconomy = rsp.getProvider();
        plugin.getLogger().info("======================================");
        plugin.getLogger().info("✓ Sistema de economia inicializado!");
        plugin.getLogger().info("✓ Usando Vault: " + vaultEconomy.getName());
        plugin.getLogger().info("======================================");
    }

    public boolean has(UUID uuid, double amount) {
        if (vaultEconomy == null) {
            return false;
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return vaultEconomy.has(player, amount);
    }

    public void withdraw(UUID uuid, double amount) {
        if (amount < 0) return;
        
        if (vaultEconomy == null) {
            plugin.getLogger().severe("✘ ERRO: Tentou retirar " + amount + " mas o Vault não está conectado!");
            return;
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        double balanceBefore = vaultEconomy.getBalance(player);
        vaultEconomy.withdrawPlayer(player, amount);
        double balanceAfter = vaultEconomy.getBalance(player);
        
        plugin.getLogger().info("======================================");
        plugin.getLogger().info("RETIRADA DE " + currencyName.toUpperCase());
        plugin.getLogger().info("Jogador: " + player.getName());
        plugin.getLogger().info("Saldo anterior: " + balanceBefore);
        plugin.getLogger().info("Valor retirado: " + amount);
        plugin.getLogger().info("Saldo atual: " + balanceAfter);
        plugin.getLogger().info("======================================");
    }

    public double getBalance(UUID uuid) {
        if (vaultEconomy == null) {
            return 0.0;
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return vaultEconomy.getBalance(player);
    }

    // Métodos auxiliares
    public void deposit(UUID uuid, double amount) {
        if (amount < 0) return;
        
        if (vaultEconomy == null) {
            plugin.getLogger().severe("✘ ERRO: Tentou depositar " + amount + " mas o Vault não está conectado!");
            return;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        vaultEconomy.depositPlayer(player, amount);
    }
    
    public String getCurrencyName() {
        if (vaultEconomy != null) {
            return vaultEconomy.currencyNameSingular();
        }
        return currencyName;
    }
    
    public double getUpgradePrice(int tier) {
        return economiasConfig.getDouble("upgrade-prices." + tier, 1000.0 * tier);
    }
    
    public String getMessage(String key) {
        return economiasConfig.getString("messages." + key, "");
    }
    
    public boolean isConnected() {
        return vaultEconomy != null;
    }
    
    public void reconnect() {
        plugin.getLogger().info("Tentando reconectar ao Vault...");
        setupEconomy();
    }
}
