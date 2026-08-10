package com.alkacode.enderchest.service;

import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.hook.AlkaVipsHook;
import com.alkacode.enderchest.manager.EnderChestManager;
import com.alkacode.enderchest.menu.EnderChestMenu;
import com.alkacode.enderchest.util.Messages;
import com.alkacode.enderchest.util.PasswordGate;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ponto unico de entrada para abrir o EnderChest customizado - o comando /ec e o
 * clique fisico no bau chamam exatamente este metodo, entao nunca divergem em qual
 * storage ou GUI usam (essa divergencia era a causa da bug de "enderchest diferente"
 * no plugin legado).
 */
public class EnderChestService {
    private final EnderChestMenu menu;
    private final EnderChestRepository repository;
    private final PasswordGate passwordGate;
    private final EnderChestManager enderChestManager;

    public EnderChestService(JavaPlugin plugin, EnderChestRepository repository,
                              PasswordGate passwordGate, EnderChestManager enderChestManager,
                              Messages messages, Supplier<AlkaVipsHook> alkaVipsHookSupplier) {
        this.repository = repository;
        this.passwordGate = passwordGate;
        this.enderChestManager = enderChestManager;
        this.menu = new EnderChestMenu(plugin, repository, enderChestManager, messages, alkaVipsHookSupplier);
    }

    public void openEnderChest(Player player) {
        openEnderChest(player, 0);
    }

    public void openEnderChest(Player player, int page) {
        if (repository.getPasswordHash(player.getUniqueId()) != null) {
            passwordGate.startPasswordEntry(player);
        } else {
            menu.open(player, page);
        }
    }

    public void openEnderChestDirect(Player player, int page) {
        menu.open(player, page);
    }

    public void openEnderChestAdmin(Player admin, UUID targetUUID, int page) {
        menu.open(admin, targetUUID, page, true);
    }

    public EnderChestRepository getRepository() {
        return repository;
    }

    public EnderChestManager getEnderChestManager() {
        return enderChestManager;
    }

    public void sortInventory(org.bukkit.inventory.Inventory inv) {
        menu.sortInventory(inv);
    }
}
