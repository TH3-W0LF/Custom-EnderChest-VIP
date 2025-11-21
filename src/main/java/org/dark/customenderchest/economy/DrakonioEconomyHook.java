package org.dark.customenderchest.economy;

import br.com.ystoreplugins.product.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

public final class DrakonioEconomyHook {

    private static EconomyProvider ECONOMY;
    private static final String TARGET_ID = "drakonio"; // id exato da sua economia no yEconomias

    public static boolean setup(final Plugin plugin) {
        plugin.getLogger().info("[ECONOMY] Tentando conectar economia: " + TARGET_ID);

        // 1) Tenta via ServicesManager (se algum provider já estiver registrado)
        RegisteredServiceProvider<?> reg = Bukkit.getServicesManager().getRegistration(EconomyProvider.class);
        if (reg != null) {
            Object prov = reg.getProvider();
            if (prov instanceof EconomyProvider) {
                EconomyProvider ep = (EconomyProvider) prov;
                // tenta checar o nome do provider, se a implementação expuser getName()
                try {
                    String name = ep.getName();
                    if (name != null && name.toLowerCase().contains(TARGET_ID.toLowerCase())) {
                        ECONOMY = ep;
                        plugin.getLogger().info("[ECONOMY] Conectado via ServicesManager: " + name);
                        return true;
                    }
                } catch (Throwable ignored) {
                    // se getName lançar, apenas aceitamos o provider como fallback
                    ECONOMY = ep;
                    plugin.getLogger().info("[ECONOMY] Conectado via ServicesManager (fallback).");
                    return true;
                }
            }
        }

        // 2) Tenta encontrar o plugin yEconomias diretamente (nome exato)
        if (Bukkit.getPluginManager().getPlugin("yEconomias") != null) {
            // tenta instanciar diretamente a implementação do yEconomias, se disponível
            try {
                // classe oficial do yPlugins que disponibiliza o "yEconomias" provider; 
                // se estiver no classpath do servidor, isto funcionará:
                Class<?> clazz = Class.forName("br.com.ystoreplugins.product.economy.methods.yEconomias");
                Object instance = clazz.getConstructor(String.class).newInstance(TARGET_ID);
                if (instance instanceof EconomyProvider) {
                    ECONOMY = (EconomyProvider) instance;
                    // registra no ServicesManager para ser usado por outros plugins
                    Bukkit.getServicesManager().register(EconomyProvider.class, ECONOMY, plugin, org.bukkit.plugin.ServicePriority.Normal);
                    plugin.getLogger().info("[ECONOMY] Conectado diretamente (new yEconomias): " + TARGET_ID);
                    return true;
                }
            } catch (ClassNotFoundException cnf) {
                plugin.getLogger().warning("[ECONOMY] Classe yEconomias não encontrada no classpath.");
            } catch (Throwable t) {
                plugin.getLogger().severe("[ECONOMY] Erro ao instanciar yEconomias: " + t.getMessage());
                t.printStackTrace();
            }
        }

        // 3) Se ainda não conectou: agenda uma segunda tentativa em 2s (caso yEconomias ainda esteja inicializando)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ECONOMY != null) return;
                plugin.getLogger().info("[ECONOMY] Segunda tentativa de conexão com yEconomias...");
                // repete a lógica curta: ServicesManager -> direct construct
                RegisteredServiceProvider<?> reg2 = Bukkit.getServicesManager().getRegistration(EconomyProvider.class);
                if (reg2 != null && reg2.getProvider() instanceof EconomyProvider) {
                    ECONOMY = (EconomyProvider) reg2.getProvider();
                    plugin.getLogger().info("[ECONOMY] Conectado via ServicesManager (retry).");
                    return;
                }
                if (Bukkit.getPluginManager().getPlugin("yEconomias") != null) {
                    try {
                        Class<?> clazz = Class.forName("br.com.ystoreplugins.product.economy.methods.yEconomias");
                        Object instance = clazz.getConstructor(String.class).newInstance(TARGET_ID);
                        if (instance instanceof EconomyProvider) {
                            ECONOMY = (EconomyProvider) instance;
                            Bukkit.getServicesManager().register(EconomyProvider.class, ECONOMY, plugin, org.bukkit.plugin.ServicePriority.Normal);
                            plugin.getLogger().info("[ECONOMY] Conectado diretamente (retry new yEconomias).");
                        }
                    } catch (Throwable ignored) {}
                }
                if (ECONOMY == null) {
                    plugin.getLogger().severe("[ECONOMY] Falha ao conectar com a economia " + TARGET_ID + " após retry. Plugin será desativado.");
                    Bukkit.getPluginManager().disablePlugin(plugin);
                }
            }
        }.runTaskLater(plugin, 40L); // 40L = 2 segundos (20 ticks = 1s)

        // não retorna true agora; a segunda tentativa fará o enable/disable
        return false;
    }

    public static boolean has(String player, double amount) {
        return ECONOMY != null && ECONOMY.has(player, amount);
    }

    public static double get(String player) {
        return ECONOMY != null ? ECONOMY.get(player) : 0;
    }

    public static void add(String player, double amount) {
        if (ECONOMY != null) ECONOMY.add(player, amount);
    }

    public static void remove(String player, double amount) {
        if (ECONOMY != null) ECONOMY.remove(player, amount);
    }

    public static boolean isActive() {
        return ECONOMY != null;
    }
    
    public static EconomyProvider getProvider() {
        return ECONOMY;
    }
}
