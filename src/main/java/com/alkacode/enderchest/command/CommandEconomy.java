package com.alkacode.enderchest.command;

import com.alkacode.enderchest.economy.EconomyService;
import com.alkacode.enderchest.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandEconomy implements CommandExecutor {

    private final EconomyService economyService;
    private final Messages messages;

    public CommandEconomy(EconomyService economyService, Messages messages) {
        this.economyService = economyService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("meuplugin.admin")) {
            sender.sendMessage(messages.get("general.no-permission"));
            return true;
        }

        boolean alkaEconomyInstalled = Bukkit.getPluginManager().getPlugin("AlkaEconomy") != null;

        sender.sendMessage(messages.get("economy-debug.header"));
        sender.sendMessage(messages.get("economy-debug.alkaeconomy-status",
                "<status>", alkaEconomyInstalled ? "SIM" : "NAO"));
        sender.sendMessage(messages.get("economy-debug.currency", "<currency>", economyService.getCurrencyName()));

        if (sender instanceof Player player) {
            double balance = economyService.getBalance(player.getUniqueId());
            sender.sendMessage(messages.get("economy-debug.balance", "<balance>", economyService.format(balance)));
        }

        sender.sendMessage(messages.get("economy-debug.footer"));

        return true;
    }
}
