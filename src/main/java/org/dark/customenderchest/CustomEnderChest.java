package org.dark.customenderchest;

import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customenderchest.commands.CommandAEC;
import org.dark.customenderchest.commands.CommandEC;
import org.dark.customenderchest.commands.CommandEconomy;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.economy.DrakonioEconomy;
import org.dark.customenderchest.listeners.EnderChestBlockListener;
import org.dark.customenderchest.listeners.InventoryListener;
import org.dark.customenderchest.manager.EnderChestManager;
import org.dark.customenderchest.utils.LogUtils;
import org.dark.customenderchest.utils.PasswordConversation;

public class CustomEnderChest extends JavaPlugin {

    private DatabaseManager databaseManager;
    private EnderChestManager enderChestManager;
    private DrakonioEconomy drakonioEconomy;
    private InventoryListener inventoryListener;
    private CommandEconomy commandEconomy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LogUtils.init(this);

        getLogger().info("========================================");
        getLogger().info("CustomEnderChest VIP 1.0 - Iniciando...");
        getLogger().info("========================================");

        // REGISTRA O HOOK DIRETO DO yEconomias (SEM VAULT) - COM RETRY AUTOMÁTICO
        boolean ok = org.dark.customenderchest.economy.DrakonioEconomyHook.setup(this);
        if (!ok) {
            // O setup pode retornar false porque a conexão será tentada em retry (2 segundos);
            // O hook fará o disable automaticamente se falhar após o retry
            getLogger().info("[ECONOMY] Aguardando retry automático em 2 segundos...");
        }
        
        databaseManager = new DatabaseManager(this);
        enderChestManager = new EnderChestManager(this, databaseManager);
        PasswordConversation passwordConversation = new PasswordConversation(this, enderChestManager, databaseManager);
        
        // Cria o inventoryListener sem a economia (será injetada depois)
        inventoryListener = new InventoryListener(this, enderChestManager, databaseManager, null);
        
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        getServer().getPluginManager().registerEvents(new EnderChestBlockListener(enderChestManager, databaseManager, passwordConversation), this);

        getCommand("ec").setExecutor(new CommandEC(enderChestManager, databaseManager, passwordConversation));
        getCommand("aec").setExecutor(new CommandAEC(enderChestManager, databaseManager));
        
        // Aguarda 3 segundos para garantir que o hook conectou (incluindo retry)
        getServer().getScheduler().runTaskLater(this, () -> {
            // Inicializa a economia (hook já deve ter conectado)
            drakonioEconomy = new DrakonioEconomy(this);
            enderChestManager.setDrakonioEconomy(drakonioEconomy);
            inventoryListener.setDrakonioEconomy(drakonioEconomy);
            
            // Comando de debug da economia
            commandEconomy = new CommandEconomy(this, drakonioEconomy);
            getCommand("ececonomy").setExecutor(commandEconomy);
            
            if (drakonioEconomy.isConnected()) {
                getLogger().info("========================================");
                getLogger().info("CustomEnderChest VIP 1.0 carregado com sucesso!");
                getLogger().info("========================================");
            } else {
                getLogger().warning("Economia ainda não conectada. O hook tentará novamente...");
            }
        }, 60L); // 3 segundos (60 ticks)
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("CustomEnderChest desativado.");
    }
}
