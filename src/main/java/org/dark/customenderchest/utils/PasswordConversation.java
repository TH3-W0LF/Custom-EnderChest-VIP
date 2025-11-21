package org.dark.customenderchest.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.manager.EnderChestManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class PasswordConversation {

    private final CustomEnderChest plugin;
    private final EnderChestManager enderChestManager;
    private final DatabaseManager databaseManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PasswordConversation(CustomEnderChest plugin, EnderChestManager enderChestManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.enderChestManager = enderChestManager;
        this.databaseManager = databaseManager;
    }

    public void startPasswordEntry(Player player) {
        // Verificar Bloqueio
        long lockoutEnd = databaseManager.getLockoutEnd(player.getUniqueId());
        if (System.currentTimeMillis() < lockoutEnd) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(lockoutEnd - System.currentTimeMillis());
            player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.password-locked", "<red>Bloqueado por <time> min.")
                    .replace("<time>", String.valueOf(minutes))));
            enderChestManager.closeOpenedBlock(player);
            return;
        }

        // Iniciar Conversa
        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true) // Bloqueia outros chats
                .withLocalEcho(false) // NÃO mostra o que o player digita no chat dele (Segurança Visual)
                .withFirstPrompt(new PasswordPrompt())
                .withTimeout(plugin.getConfig().getInt("security.password-timeout", 30))
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.password-timeout", "<red>Tempo esgotado.")));
                        enderChestManager.closeOpenedBlock(player);
                    }
                });

        player.beginConversation(factory.buildConversation(player));
    }

    private class PasswordPrompt extends StringPrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            return plugin.getConfig().getString("messages.password-request", "§eDigite sua senha:");
        }

        @Override
        public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
            Player player = (Player) context.getForWhom();
            
            if (input == null || input.equalsIgnoreCase("cancelar")) {
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.operation-cancelled", "<red>Cancelado.")));
                enderChestManager.closeOpenedBlock(player);
                return Prompt.END_OF_CONVERSATION;
            }

            String storedHash = databaseManager.getPasswordHash(player.getUniqueId());
            if (storedHash == null) {
                // Erro de estado, abre direto
                enderChestManager.openEnderChest(player, player.getUniqueId(), 0, false);
                return Prompt.END_OF_CONVERSATION;
            }

            if (hash(input).equals(storedHash)) {
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.password-correct", "<green>Senha correta!")));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                databaseManager.resetFailedAttempts(player.getUniqueId());
                
                // Abrir Inventário (Sync)
                Bukkit.getScheduler().runTask(plugin, () -> 
                    enderChestManager.openEnderChest(player, player.getUniqueId(), 0, false));
                
                return Prompt.END_OF_CONVERSATION;
            } else {
                // Falha
                int fails = databaseManager.incrementFailedAttempts(player.getUniqueId());
                int maxAttempts = plugin.getConfig().getInt("security.max-attempts", 5);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);

                if (fails >= maxAttempts) {
                    applyPunishment(player);
                    return Prompt.END_OF_CONVERSATION;
                }

                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.password-incorrect", "<red>Incorreto")
                        .replace("<current>", String.valueOf(fails))
                        .replace("<max>", String.valueOf(maxAttempts))));
                
                // Retorna o mesmo prompt para tentar de novo
                return this; 
            }
        }
    }

    private void applyPunishment(Player player) {
        FileConfiguration config = plugin.getConfig();
        int currentLevel = databaseManager.getPunishmentLevel(player.getUniqueId()) + 1;
        
        // Buscar tempo na config (punishments.1, punishments.2, etc)
        // Se não tiver config específica para o nível, pega o maior nível definido ou um padrão
        int minutes = config.getInt("security.punishments." + currentLevel);
        if (minutes == 0) {
            // Tenta encontrar o maior nível configurado se passarmos do limite
            for (int i = currentLevel; i > 0; i--) {
                if (config.contains("security.punishments." + i)) {
                    minutes = config.getInt("security.punishments." + i);
                    break;
                }
            }
            if (minutes == 0) minutes = 60; // Fallback 1 hora
        }

        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
        databaseManager.applyLockout(player.getUniqueId(), endTime, currentLevel);
        enderChestManager.closeOpenedBlock(player);
        
        player.sendMessage(mm.deserialize(config.getString("messages.password-locked", "<red>Bloqueado.")
                .replace("<time>", String.valueOf(minutes))));
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

