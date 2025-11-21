package org.dark.customenderchest.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customenderchest.economy.DrakonioEconomy;
import org.jetbrains.annotations.NotNull;

public class CommandEconomy implements CommandExecutor {

    private final JavaPlugin plugin;
    private DrakonioEconomy drakonioEconomy;

    public CommandEconomy(JavaPlugin plugin, DrakonioEconomy drakonioEconomy) {
        this.plugin = plugin;
        this.drakonioEconomy = drakonioEconomy;
    }
    
    public void setDrakonioEconomy(DrakonioEconomy drakonioEconomy) {
        this.drakonioEconomy = drakonioEconomy;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("meuplugin.admin")) {
            sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reconnect")) {
            sender.sendMessage(Component.text("Tentando reconectar ao Vault...", NamedTextColor.YELLOW));
            if (drakonioEconomy != null) {
                drakonioEconomy.reconnect();
            } else {
                sender.sendMessage(Component.text("Sistema de economia não inicializado!", NamedTextColor.RED));
            }
            return true;
        }

        sender.sendMessage(Component.text("========== DEBUG ECONOMIA ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.empty());
        
        // Status do Vault
        boolean vaultInstalled = Bukkit.getPluginManager().getPlugin("Vault") != null;
        sender.sendMessage(Component.text("Vault instalado: " + (vaultInstalled ? "✓ SIM" : "✘ NÃO"),
            vaultInstalled ? NamedTextColor.GREEN : NamedTextColor.RED));
        
        // Status da economia
        boolean connected = drakonioEconomy != null && drakonioEconomy.isConnected();
        sender.sendMessage(Component.text("Economia conectada: " + (connected ? "✓ SIM" : "✘ NÃO"),
            connected ? NamedTextColor.GREEN : NamedTextColor.RED));
        
        if (connected) {
            sender.sendMessage(Component.text("Nome da moeda: " + drakonioEconomy.getCurrencyName(), NamedTextColor.AQUA));
            
            if (sender instanceof Player player) {
                double balance = drakonioEconomy.getBalance(player.getUniqueId());
                sender.sendMessage(Component.text("Seu saldo: " + balance, NamedTextColor.YELLOW));
            }
        }
        
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Providers disponíveis:", NamedTextColor.GOLD));
        
        // Busca no ServicesManager do Vault
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            sender.sendMessage(Component.text("  ✘ Nenhum provider encontrado!", NamedTextColor.RED));
            sender.sendMessage(Component.text("  Instale um plugin de economia compatível com Vault.", NamedTextColor.GRAY));
        } else {
            Economy economy = rsp.getProvider();
            sender.sendMessage(Component.text("  → " + economy.getName() + " (" + economy.getClass().getSimpleName() + ")", 
                NamedTextColor.GREEN));
        }
        
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Comandos:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /ececonomy - Mostra este menu", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /ececonomy reconnect - Tenta reconectar", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("===================================", NamedTextColor.GOLD));

        return true;
    }
}
