package org.dark.customenderchest.economy;

import br.com.ystoreplugins.product.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

public class DrakonioEconomy {

    private final JavaPlugin plugin;
    private EconomyProvider yEconomy;
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
        this.currencyName = economiasConfig.getString("display-name", "Drakonio");
    }

    private void setupEconomy() {
        // Usa o provider do hook direto (já foi registrado no onEnable)
        yEconomy = DrakonioEconomyHook.getProvider();
        
        if (yEconomy != null) {
            plugin.getLogger().info("======================================");
            plugin.getLogger().info("✓ Sistema de economia inicializado!");
            plugin.getLogger().info("✓ Usando provider do DrakonioEconomyHook");
            plugin.getLogger().info("✓ Nome: " + yEconomy.getName());
            plugin.getLogger().info("======================================");
        } else {
            plugin.getLogger().warning("⚠ Provider não encontrado no hook!");
        }
    }

    public boolean has(UUID uuid, double amount) {
        if (yEconomy == null) {
            plugin.getLogger().severe("✘ ERRO: Economia 'drakonio' não está conectada!");
            return false;
        }
        
        String playerName = getPlayerName(uuid);
        boolean has = yEconomy.has(playerName, amount);
        double balance = yEconomy.get(playerName);
        plugin.getLogger().info("Verificando saldo de " + playerName + ": " + balance + " " + currencyName + " (precisa: " + amount + ") = " + has);
        return has;
    }

    public void withdraw(UUID uuid, double amount) {
        if (amount < 0) return;
        
        if (yEconomy == null) {
            plugin.getLogger().severe("✘ ERRO: Tentou retirar " + amount + " mas a economia 'drakonio' não está conectada!");
            return;
        }
        
        String playerName = getPlayerName(uuid);
        double balanceBefore = yEconomy.get(playerName);
        yEconomy.remove(playerName, amount);
        double balanceAfter = yEconomy.get(playerName);
        
        plugin.getLogger().info("======================================");
        plugin.getLogger().info("RETIRADA DE " + currencyName.toUpperCase());
        plugin.getLogger().info("Jogador: " + playerName);
        plugin.getLogger().info("Saldo anterior: " + balanceBefore);
        plugin.getLogger().info("Valor retirado: " + amount);
        plugin.getLogger().info("Saldo atual: " + balanceAfter);
        plugin.getLogger().info("======================================");
    }

    public double getBalance(UUID uuid) {
        if (yEconomy == null) {
            plugin.getLogger().severe("✘ ERRO: Economia 'drakonio' não está conectada!");
            return 0.0;
        }
        
        String playerName = getPlayerName(uuid);
        double balance = yEconomy.get(playerName);
        plugin.getLogger().info("Consultando saldo de " + playerName + ": " + balance + " " + currencyName);
        return balance;
    }

    // Métodos auxiliares
    public void deposit(UUID uuid, double amount) {
        if (amount < 0) return;
        
        if (yEconomy == null) {
            plugin.getLogger().severe("✘ ERRO: Tentou depositar " + amount + " mas a economia 'drakonio' não está conectada!");
            return;
        }

        yEconomy.add(getPlayerName(uuid), amount);
    }
    
    private String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName(); // yEconomias usa String
    }

    public String getCurrencyName() {
        return currencyName;
    }
    
    public double getUpgradePrice(int tier) {
        return economiasConfig.getDouble("upgrade-prices." + tier, 1000.0 * tier);
    }
    
    public String getMessage(String key) {
        return economiasConfig.getString("messages." + key, "");
    }
    
    public boolean isConnected() {
        return yEconomy != null;
    }
    
    public void reconnect() {
        plugin.getLogger().info("Tentando reconectar ao yEconomias...");
        setupEconomy();
    }
}
