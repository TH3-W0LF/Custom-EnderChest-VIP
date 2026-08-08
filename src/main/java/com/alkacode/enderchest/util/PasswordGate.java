package com.alkacode.enderchest.util;

import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.manager.EnderChestManager;
import com.alkacode.enderchest.service.EnderChestService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Porta de entrada da senha do EnderChest via comando (/ec entrar <senha>), nunca via
 * chat livre nem GUI. Ja tentamos as duas: bigorna (Bukkit.createInventory com
 * InventoryType.ANVIL nao gera uma bigorna funcional no Paper moderno - quebrava com
 * ClassCastException) e chat com AsyncChatEvent cancelado (vazava pra quem tem "chat
 * local" no servidor - cancelar o evento nao impede outros plugins de rodar seu
 * proprio listener e fazer broadcast por conta propria; ignoreCancelled=false e o
 * padrao, entao qualquer plugin que nao cheque event.isCancelled() ainda processa a
 * mensagem, e nao existe prioridade "antes de LOWEST" pra garantir que rodamos primeiro
 * se o outro plugin tambem estiver em LOWEST). Comando nunca passa pelo pipeline de
 * chat, entao nenhum plugin de chat (local ou nao) tem como interceptar.
 */
public class PasswordGate {

    private final JavaPlugin plugin;
    private final EnderChestRepository repository;
    private final EnderChestManager enderChestManager;
    private final Messages messages;

    // jogadores com um prompt de senha em aberto aguardando /ec entrar <senha>.
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private EnderChestService enderChestService;

    public PasswordGate(JavaPlugin plugin, EnderChestRepository repository,
                         EnderChestManager enderChestManager, Messages messages) {
        this.plugin = plugin;
        this.repository = repository;
        this.enderChestManager = enderChestManager;
        this.messages = messages;
    }

    public void setEnderChestService(EnderChestService enderChestService) {
        this.enderChestService = enderChestService;
    }

    public EnderChestService getEnderChestService() {
        return enderChestService;
    }

    public void startPasswordEntry(Player player) {
        long lockoutEnd = repository.getLockoutUntil(player.getUniqueId());
        if (System.currentTimeMillis() < lockoutEnd) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(lockoutEnd - System.currentTimeMillis());
            player.sendMessage(messages.get("password.locked", "<time>", String.valueOf(minutes)));
            enderChestManager.closeOpenedBlock(player);
            return;
        }

        beginPasswordPrompt(player);
    }

    private void beginPasswordPrompt(Player player) {
        UUID uuid = player.getUniqueId();
        pending.add(uuid);

        int timeoutSeconds = plugin.getConfig().getInt("security.password-timeout-seconds", 30);
        player.sendMessage(messages.get("password.prompt-command", "<time>", String.valueOf(timeoutSeconds)));
        // action bar: dificil de perder mesmo se o jogador nao olhar o chat; a mensagem
        // de chat acima continua existindo pra ficar no historico caso a action bar suma.
        player.sendActionBar(messages.getNoPrefix("password.prompt-actionbar", "<time>", String.valueOf(timeoutSeconds)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // se ainda estava pendente, o jogador nunca rodou /ec entrar - trata como
            // abandono e libera o estado do bloco.
            if (pending.remove(uuid)) {
                player.sendMessage(messages.get("password.timeout"));
                enderChestManager.closeOpenedBlock(player);
            }
        }, timeoutSeconds * 20L);
    }

    /** True se esse jogador tem um prompt de senha em aberto aguardando /ec entrar. */
    public boolean isPending(UUID uuid) {
        return pending.contains(uuid);
    }

    /** Chamado pelo CommandEC (subcomando "entrar") com a senha digitada. */
    public void handleAttempt(Player player, String input) {
        if (!pending.remove(player.getUniqueId())) {
            return;
        }

        if (input.equalsIgnoreCase("cancelar")) {
            handleAbandoned(player);
            return;
        }

        String storedHash = repository.getPasswordHash(player.getUniqueId());
        boolean correct = storedHash == null || hash(input).equals(storedHash);

        if (correct) {
            handleCorrect(player);
            enderChestService.openEnderChestDirect(player, 0);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
            handleWrongAttempt(player);
        }
    }

    public void handleWrongAttempt(Player player) {
        int fails = repository.incrementFailedAttempts(player.getUniqueId());
        int maxAttempts = plugin.getConfig().getInt("security.max-attempts", 5);

        if (fails >= maxAttempts) {
            applyPunishment(player);
        } else {
            player.sendMessage(messages.get("password.incorrect",
                    "<current>", String.valueOf(fails), "<max>", String.valueOf(maxAttempts)));
            enderChestManager.closeOpenedBlock(player);
        }
    }

    public void handleCorrect(Player player) {
        player.sendMessage(messages.get("password.correct"));
        repository.resetFailedAttempts(player.getUniqueId());
    }

    public void handleAbandoned(Player player) {
        player.sendMessage(messages.get("password.cancelled"));
        enderChestManager.closeOpenedBlock(player);
    }

    private void applyPunishment(Player player) {
        int currentLevel = repository.getPunishmentLevel(player.getUniqueId()) + 1;

        int minutes = plugin.getConfig().getInt("security.punishments." + currentLevel);
        if (minutes == 0) {
            for (int i = currentLevel; i > 0; i--) {
                if (plugin.getConfig().contains("security.punishments." + i)) {
                    minutes = plugin.getConfig().getInt("security.punishments." + i);
                    break;
                }
            }
            if (minutes == 0) minutes = 60;
        }

        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
        repository.applyLockout(player.getUniqueId(), endTime, currentLevel);
        enderChestManager.closeOpenedBlock(player);

        player.sendMessage(messages.get("password.locked", "<time>", String.valueOf(minutes)));
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
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
