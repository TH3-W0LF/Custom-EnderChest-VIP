package com.alkacode.enderchest.command;

import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.economy.EconomyService;
import com.alkacode.enderchest.service.EnderChestService;
import com.alkacode.enderchest.util.LogUtils;
import com.alkacode.enderchest.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandAEC implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final EnderChestRepository repository;
    private final EnderChestService enderChestService;
    private final EconomyService economyService;
    private final Messages messages;

    public CommandAEC(JavaPlugin plugin, EnderChestRepository repository, EnderChestService enderChestService,
                       EconomyService economyService, Messages messages) {
        this.plugin = plugin;
        this.repository = repository;
        this.enderChestService = enderChestService;
        this.economyService = economyService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }

        if (args.length == 0) {
            enderChestService.openEnderChestAdmin(player, player.getUniqueId(), 0);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("ver")) {
            if (args.length < 2) {
                player.sendMessage(messages.get("admin.ec-view-usage"));
                return true;
            }
            String targetName = args[1];

            Player onlineTarget = Bukkit.getPlayer(targetName);
            UUID targetUUID;
            String displayName;

            if (onlineTarget != null) {
                targetUUID = onlineTarget.getUniqueId();
                displayName = onlineTarget.getName();
            } else {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
                    player.sendMessage(messages.get("admin.ec-view-not-found"));
                    return true;
                }
                targetUUID = offlineTarget.getUniqueId();
                displayName = offlineTarget.getName();
                if (displayName == null) displayName = targetName;
            }

            player.sendMessage(messages.get("admin.ec-view-opening", "<player>", displayName));
            LogUtils.log("Admin " + player.getName() + " abriu o EC de " + displayName + " (" + targetUUID + ")");
            enderChestService.openEnderChestAdmin(player, targetUUID, 0);

        } else if (sub.equals("resetpass")) {
            if (args.length < 2) {
                player.sendMessage(messages.get("admin.resetpass-usage"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            repository.removePassword(target.getUniqueId());
            player.sendMessage(messages.get("admin.resetpass-done", "<player>", String.valueOf(target.getName())));
            LogUtils.log("Admin " + player.getName() + " resetou a senha de " + target.getName());

        } else if (sub.equals("settier")) {
            if (args.length < 3) {
                player.sendMessage(messages.get("admin.settier-usage"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            int tier;
            try {
                tier = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(messages.get("admin.settier-invalid"));
                return true;
            }

            repository.setTier(target.getUniqueId(), tier);
            player.sendMessage(messages.get("admin.settier-done", "<player>", String.valueOf(target.getName()), "<tier>", String.valueOf(tier)));
            LogUtils.log("Admin " + player.getName() + " definiu o tier de " + target.getName() + " para " + tier);

        } else if (sub.equals("reload")) {
            plugin.reloadConfig();
            messages.load();
            economyService.reload();
            player.sendMessage(messages.get("admin.reload-done"));
            LogUtils.log("Admin " + player.getName() + " recarregou a configuracao do AlkaEnderChest.");

        } else {
            player.sendMessage(messages.get("general.unknown-command"));
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
            completions.add("reload");
            return completions;
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
