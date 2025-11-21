package org.dark.customenderchest.commands;

import br.com.ystoreplugins.product.economy.EconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.economy.DrakonioEconomy;
import org.dark.customenderchest.economy.YEconomiasManager;
import org.jetbrains.annotations.NotNull;

public class CommandEconomy implements CommandExecutor {

    private final CustomEnderChest plugin;
    private DrakonioEconomy drakonioEconomy;

    public CommandEconomy(CustomEnderChest plugin, DrakonioEconomy drakonioEconomy) {
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
            sender.sendMessage(Component.text("Tentando reconectar ao yEconomias...", NamedTextColor.YELLOW));
            if (drakonioEconomy != null) {
                drakonioEconomy.reconnect();
            } else {
                sender.sendMessage(Component.text("Sistema de economia não inicializado!", NamedTextColor.RED));
            }
            return true;
        }

        sender.sendMessage(Component.text("========== DEBUG ECONOMIA ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.empty());
        
        // Status do yEconomias (pode estar standalone ou como módulo do yPlugins)
        boolean yPluginsInstalled = plugin.getServer().getPluginManager().getPlugin("yPlugins") != null;
        boolean yEconomiasStandalone = plugin.getServer().getPluginManager().getPlugin("yEconomias") != null;
        
        if (yPluginsInstalled) {
            sender.sendMessage(Component.text("yPlugins detectado: ✓ SIM (yEconomias como módulo)", NamedTextColor.GREEN));
        } else if (yEconomiasStandalone) {
            sender.sendMessage(Component.text("yEconomias detectado: ✓ SIM (standalone)", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("yEconomias/yPlugins: ✘ NÃO DETECTADO", NamedTextColor.RED));
        }
        
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
        
        // Busca diretamente no ServicesManager
        var registrations = plugin.getServer().getServicesManager().getRegistrations(EconomyProvider.class);
        sender.sendMessage(Component.text("Total no ServicesManager: " + registrations.size(), NamedTextColor.GRAY));
        
        if (registrations.isEmpty()) {
            sender.sendMessage(Component.text("  ✘ Nenhum provider encontrado!", NamedTextColor.RED));
            sender.sendMessage(Component.text("  Verifique se o hook foi executado nos logs.", NamedTextColor.GRAY));
        } else {
            registrations.forEach(reg -> {
                EconomyProvider provider = reg.getProvider();
                String name = provider.getName();
                String className = provider.getClass().getSimpleName();
                boolean isDrakonio = name.toLowerCase().equals("drakonio");
                
                sender.sendMessage(Component.text("  → " + name + " (" + className + ")", 
                    isDrakonio ? NamedTextColor.GREEN : NamedTextColor.WHITE));
            });
        }
        
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Comandos:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /ececonomy - Mostra este menu", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /ececonomy reconnect - Tenta reconectar", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("===================================", NamedTextColor.GOLD));

        return true;
    }
}

