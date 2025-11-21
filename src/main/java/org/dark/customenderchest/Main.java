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

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private EnderChestManager enderChestManager;
    private DrakonioEconomy drakonioEconomy;
    private InventoryListener inventoryListener;
    private CommandEconomy commandEconomy;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("CustomEnderChest VIP 1.0 - Iniciando...");
        getLogger().info("========================================");

        // Carregar configurações
        saveDefaultConfig();
        LogUtils.init(this);

        // Inicializar componentes
        databaseManager = new DatabaseManager(this);
        enderChestManager = new EnderChestManager(this, databaseManager);
        PasswordConversation passwordConversation = new PasswordConversation(this, enderChestManager, databaseManager);
        
        // Inicializar economia (Vault)
        drakonioEconomy = new DrakonioEconomy(this);
        enderChestManager.setDrakonioEconomy(drakonioEconomy);
        
        // Cria o inventoryListener
        inventoryListener = new InventoryListener(this, enderChestManager, databaseManager, drakonioEconomy);
        
        // Registrar eventos
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        getServer().getPluginManager().registerEvents(new EnderChestBlockListener(enderChestManager, databaseManager, passwordConversation), this);

        // Registrar comandos
        getCommand("ec").setExecutor(new CommandEC(enderChestManager, databaseManager, passwordConversation));
        getCommand("aec").setExecutor(new CommandAEC(enderChestManager, databaseManager));
        
        // Comando de debug da economia
        commandEconomy = new CommandEconomy(this, drakonioEconomy);
        getCommand("ececonomy").setExecutor(commandEconomy);
        
        if (drakonioEconomy.isConnected()) {
            getLogger().info("========================================");
            getLogger().info("CustomEnderChest VIP 1.0 carregado com sucesso!");
            getLogger().info("========================================");
        } else {
            getLogger().warning("⚠ Vault não encontrado. Sistema de economia não funcionará.");
            getLogger().warning("Instale o Vault e um plugin de economia compatível.");
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("CustomEnderChest desativado.");
    }
}
