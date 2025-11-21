package org.dark.customenderchest.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.manager.EnderChestManager;
import org.dark.customenderchest.utils.PasswordConversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandEC implements CommandExecutor, TabCompleter {

    private final EnderChestManager enderChestManager;
    private final DatabaseManager databaseManager;
    private final PasswordConversation passwordConversation;

    public CommandEC(EnderChestManager enderChestManager, DatabaseManager databaseManager, PasswordConversation passwordConversation) {
        this.enderChestManager = enderChestManager;
        this.databaseManager = databaseManager;
        this.passwordConversation = passwordConversation;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Apenas jogadores podem usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            if (databaseManager.getPasswordHash(player.getUniqueId()) != null) {
                passwordConversation.startPasswordEntry(player);
            } else {
                enderChestManager.openEnderChest(player, player.getUniqueId(), 0, false);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("senha")) {
            // Exige confirmação: /ec senha <senha> <confirmacao>
            if (args.length < 3) {
                player.sendMessage(Component.text("Uso correto: /ec senha <senha> <repetir_senha>", NamedTextColor.RED));
                return true;
            }
            
            String pass1 = args[1];
            String pass2 = args[2];

            if (!pass1.equals(pass2)) {
                player.sendMessage(Component.text("As senhas não coincidem! Tente novamente.", NamedTextColor.RED));
                return true;
            }

            databaseManager.setPassword(player.getUniqueId(), PasswordConversation.hash(pass1));
            player.sendMessage(Component.text("Senha definida com sucesso! Não a esqueça.", NamedTextColor.GREEN));
            
        } else if (sub.equals("remover")) {
            if (databaseManager.getPasswordHash(player.getUniqueId()) == null) {
                player.sendMessage(Component.text("Você não tem uma senha definida.", NamedTextColor.RED));
                return true;
            }
            databaseManager.removePassword(player.getUniqueId());
            player.sendMessage(Component.text("Senha removida! Seu baú está desprotegido.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Comando desconhecido.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("senha");
            completions.add("remover");
            return completions;
        }
        return Collections.emptyList();
    }
}
