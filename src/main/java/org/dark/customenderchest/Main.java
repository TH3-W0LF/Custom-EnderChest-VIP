package org.dark.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.dark.customenderchest.commands.CommandAEC;
import org.dark.customenderchest.commands.CommandEC;
import org.dark.customenderchest.commands.CommandEconomy;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.economy.DrakonioEconomy;
import org.dark.customenderchest.economy.DrakonioEconomyHook;
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
    private BukkitTask waitTask;

    @Override
    public void onEnable() {
        getLogger().info("CustomEnderChest-VIP iniciando... aguardando yPlugins/yEconomias...");

        // NÃO carregar imediatamente — esperar o ecossistema yPlugins subir
        waitTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            int tentativas = 0;

            @Override
            public void run() {
                tentativas++;

                Plugin yplugins = getServer().getPluginManager().getPlugin("yPlugins");
                Plugin yeconomias = getServer().getPluginManager().getPlugin("yEconomias");

                // O yPlugins primeiro inicia, depois baixa as economias por IP, e só então habilita o yEconomias
                if (yplugins != null && yplugins.isEnabled() && 
                    yeconomias != null && yeconomias.isEnabled()) {

                    getLogger().info("yPlugins e yEconomias confirmados! Iniciando carregamento da economia...");
                    iniciarPluginAposYPlugins();
                    waitTask.cancel();
                    return;
                }

                if (tentativas >= 80) { // 80 tentativas = 80 × 0.5s = 40 segundos
                    getLogger().severe("========================================");
                    getLogger().severe("Tempo máximo atingido (40 segundos).");
                    getLogger().severe("yPlugins/yEconomias não iniciaram a tempo.");
                    getLogger().severe("Plugin será desabilitado.");
                    getLogger().severe("========================================");
                    Bukkit.getPluginManager().disablePlugin(Main.this);
                    waitTask.cancel();
                }
            }
        }, 10L, 10L); // roda a cada 10 ticks (0.5s)
    }

    private void iniciarPluginAposYPlugins() {
        getLogger().info("========================================");
        getLogger().info("Iniciando CustomEnderChest VIP...");
        getLogger().info("========================================");

        // Carregar configurações
        saveDefaultConfig();
        LogUtils.init(this);

        // Setup da economia (com retry automático)
        boolean ok = DrakonioEconomyHook.setup(this);
        if (!ok) {
            getLogger().warning("[CustomEnderChest] Economia não conectada na primeira tentativa. Aguardando retry...");
        } else {
            getLogger().info("[CustomEnderChest] Economia drakonio conectada com sucesso.");
        }

        // Inicializar componentes
        databaseManager = new DatabaseManager(this);
        enderChestManager = new EnderChestManager(this, databaseManager);
        PasswordConversation passwordConversation = new PasswordConversation(this, enderChestManager, databaseManager);
        
        // Cria o inventoryListener sem a economia (será injetada depois)
        inventoryListener = new InventoryListener(this, enderChestManager, databaseManager, null);
        
        // Registrar eventos
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        getServer().getPluginManager().registerEvents(new EnderChestBlockListener(enderChestManager, databaseManager, passwordConversation), this);

        // Registrar comandos
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
        // Cancela a task de espera se ainda estiver rodando
        if (waitTask != null && !waitTask.isCancelled()) {
            waitTask.cancel();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("CustomEnderChest desativado.");
    }
}

