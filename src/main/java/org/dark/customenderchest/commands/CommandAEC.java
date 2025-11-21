package org.dark.customenderchest.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.manager.EnderChestManager;
import org.dark.customenderchest.utils.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandAEC implements CommandExecutor, TabCompleter {

    private final EnderChestManager enderChestManager;
    private final DatabaseManager databaseManager;

    public CommandAEC(EnderChestManager enderChestManager, DatabaseManager databaseManager) {
        this.enderChestManager = enderChestManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Apenas jogadores podem usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            enderChestManager.openEnderChest(player, player.getUniqueId(), 0, true);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("ver")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Uso: /aec ver <nick>", NamedTextColor.RED));
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(Component.text("Jogador nunca jogou neste servidor ou não encontrado.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Abrindo EnderChest de " + target.getName() + " (Modo Edição)...", NamedTextColor.GREEN));
            LogUtils.log("Admin " + player.getName() + " abriu o EC de " + target.getName() + " (" + target.getUniqueId() + ")");
            enderChestManager.openEnderChest(player, target.getUniqueId(), 0, true);

        } else if (sub.equals("resetpass")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Uso: /aec resetpass <nick>", NamedTextColor.RED));
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            databaseManager.removePassword(target.getUniqueId());
            player.sendMessage(Component.text("Senha de " + target.getName() + " foi removida.", NamedTextColor.GREEN));
            LogUtils.log("Admin " + player.getName() + " resetou a senha de " + target.getName());

        } else if (sub.equals("settier")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("Uso: /aec settier <nick> <nivel>", NamedTextColor.RED));
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            int tier;
            try {
                tier = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("O nível deve ser um número inteiro.", NamedTextColor.RED));
                return true;
            }

            databaseManager.setPurchasedTier(target.getUniqueId(), tier);
            player.sendMessage(Component.text("Tier de " + target.getName() + " definido para " + tier + ".", NamedTextColor.GREEN));
            LogUtils.log("Admin " + player.getName() + " definiu o tier de " + target.getName() + " para " + tier);

        } else {
            player.sendMessage(Component.text("Comando desconhecido.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("ver");
            completions.add("resetpass");
            completions.add("settier");
            return completions;
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
