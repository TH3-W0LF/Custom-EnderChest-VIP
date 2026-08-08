package com.alkacode.enderchest.command;

import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.service.EnderChestService;
import com.alkacode.enderchest.util.MasterKeyItem;
import com.alkacode.enderchest.util.Messages;
import com.alkacode.enderchest.util.PasswordGate;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandEC implements CommandExecutor, TabCompleter {

    private static final String BYPASS_PERMISSION = "alkaenderchest.senha.remover.bypass";
    private static final String ADMIN_PERMISSION = "alkaenderchest.admin";

    private final JavaPlugin plugin;
    private final EnderChestService enderChestService;
    private final EnderChestRepository repository;
    private final PasswordGate passwordGate;
    private final Messages messages;

    public CommandEC(JavaPlugin plugin, EnderChestService enderChestService, EnderChestRepository repository,
                      PasswordGate passwordGate, Messages messages) {
        this.plugin = plugin;
        this.enderChestService = enderChestService;
        this.repository = repository;
        this.passwordGate = passwordGate;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }

        if (args.length == 0) {
            enderChestService.openEnderChest(player, 0);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "senha" -> handleSetPassword(player, args);
            case "remover" -> handleRemovePassword(player);
            case "chavemestra" -> handleGiveMasterKey(player, args);
            case "entrar" -> handleEnterPassword(player, args);
            default -> player.sendMessage(messages.get("general.unknown-command"));
        }

        return true;
    }

    /**
     * Resposta ao prompt aberto por PasswordGate#startPasswordEntry - so aceita se o
     * jogador realmente tem um prompt pendente (clicou no bau primeiro). Comando, nunca
     * chat: AsyncChatEvent/AsyncPlayerChatEvent podem ser interceptados por outros
     * plugins de chat (ex: chat local) mesmo com o evento cancelado, ja que cancelar
     * nao impede que outro listener rode e faca seu proprio broadcast.
     */
    private void handleEnterPassword(Player player, String[] args) {
        if (!passwordGate.isPending(player.getUniqueId())) {
            player.sendMessage(messages.get("password.entrar-no-pending"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(messages.get("password.entrar-usage"));
            return;
        }

        passwordGate.handleAttempt(player, args[1]);
    }

    private void handleSetPassword(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(messages.get("password.set-usage"));
            return;
        }

        String pass1 = args[1];
        String pass2 = args[2];

        if (!pass1.equals(pass2)) {
            player.sendMessage(messages.get("password.mismatch"));
            return;
        }

        repository.setPassword(player.getUniqueId(), PasswordGate.hash(pass1));
        player.sendMessage(messages.get("password.set"));
    }

    /**
     * Ordem de verificacao (prioridade): 1) permissao de bypass -> remove de graca;
     * 2) Chave Mestra na mao principal (identificada por PDC, nao por nome/material)
     * -> consome 1 unidade e remove; 3) nenhum dos dois -> bloqueia a acao.
     */
    private void handleRemovePassword(Player player) {
        if (repository.getPasswordHash(player.getUniqueId()) == null) {
            player.sendMessage(messages.get("password.not-set"));
            return;
        }

        if (player.hasPermission(BYPASS_PERMISSION)) {
            repository.removePassword(player.getUniqueId());
            player.sendMessage(messages.get("password.removed"));
            return;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (MasterKeyItem.isMasterKey(plugin, inHand)) {
            int newAmount = inHand.getAmount() - 1;
            if (newAmount <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                inHand.setAmount(newAmount);
                player.getInventory().setItemInMainHand(inHand);
            }

            repository.removePassword(player.getUniqueId());
            player.sendMessage(messages.get("password.masterkey-used"));
            return;
        }

        player.sendMessage(messages.get("password.masterkey-required"));
    }

    private void handleGiveMasterKey(Player sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(messages.get("general.no-permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(messages.get("password.masterkey-usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("password.masterkey-target-not-found"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("password.masterkey-invalid-amount"));
            return;
        }
        if (amount <= 0) {
            sender.sendMessage(messages.get("password.masterkey-invalid-amount"));
            return;
        }

        target.getInventory().addItem(MasterKeyItem.create(plugin, amount));
        sender.sendMessage(messages.get("password.masterkey-given", "<amount>", String.valueOf(amount)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("senha");
            completions.add("remover");
            completions.add("entrar");
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                completions.add("chavemestra");
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("chavemestra")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
