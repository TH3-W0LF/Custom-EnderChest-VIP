package org.dark.customenderchest.economy;

import br.com.ystoreplugins.product.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

public final class DrakonioEconomyHook {

    private static EconomyProvider ECONOMY;

    private static final String TARGET_ID = "drakonio"; // ID da economia no yEconomias

    public static boolean setup(final Plugin plugin) {

        plugin.getLogger().info("[ECONOMY] Tentando conectar economia: " + TARGET_ID);

        // 1) Tenta via ServicesManager (caso já esteja registrado)

        RegisteredServiceProvider<?> reg = Bukkit.getServicesManager().getRegistration(EconomyProvider.class);

        if (reg != null && reg.getProvider() instanceof EconomyProvider) {

            ECONOMY = (EconomyProvider) reg.getProvider();

            plugin.getLogger().info("[ECONOMY] Conectado via ServicesManager (provider existente).");

            return true;

        }

        // 2) Tenta instanciar pelo yEconomias diretamente

        if (Bukkit.getPluginManager().getPlugin("yEconomias") != null) {

            try {

                Class<?> clazz = Class.forName("br.com.ystoreplugins.product.economy.methods.yEconomias");

                Object instance = clazz.getConstructor(String.class).newInstance(TARGET_ID);

                if (instance instanceof EconomyProvider) {

                    ECONOMY = (EconomyProvider) instance;

                    Bukkit.getServicesManager().register(

                            EconomyProvider.class,

                            ECONOMY,

                            plugin,

                            org.bukkit.plugin.ServicePriority.Normal

                    );

                    plugin.getLogger().info("[ECONOMY] Conectado diretamente (new yEconomias): " + TARGET_ID);

                    return true;

                }

            } catch (ClassNotFoundException cnf) {

                plugin.getLogger().warning("[ECONOMY] Classe yEconomias não encontrada no classpath.");

            } catch (Throwable t) {

                plugin.getLogger().severe("[ECONOMY] Erro ao instanciar yEconomias: " + t.getMessage());

                t.printStackTrace();

            }

        } else {

            plugin.getLogger().info("[ECONOMY] yEconomias não está presente (ainda).");

        }

        // 3) Retry após 2s (caso yEconomias esteja inicializando)

        new BukkitRunnable() {

            @Override

            public void run() {

                if (ECONOMY != null) return;

                plugin.getLogger().info("[ECONOMY] Retry: tentando reconectar yEconomias -> " + TARGET_ID);

                RegisteredServiceProvider<?> r2 =

                        Bukkit.getServicesManager().getRegistration(EconomyProvider.class);

                if (r2 != null && r2.getProvider() instanceof EconomyProvider) {

                    ECONOMY = (EconomyProvider) r2.getProvider();

                    plugin.getLogger().info("[ECONOMY] Conectado via ServicesManager (retry).");

                    return;

                }

                if (Bukkit.getPluginManager().getPlugin("yEconomias") != null) {

                    try {

                        Class<?> clazz = Class.forName("br.com.ystoreplugins.product.economy.methods.yEconomias");

                        Object instance = clazz.getConstructor(String.class).newInstance(TARGET_ID);

                        if (instance instanceof EconomyProvider) {

                            ECONOMY = (EconomyProvider) instance;

                            Bukkit.getServicesManager().register(

                                    EconomyProvider.class,

                                    ECONOMY,

                                    plugin,

                                    org.bukkit.plugin.ServicePriority.Normal

                            );

                            plugin.getLogger().info("[ECONOMY] Conectado diretamente (retry new yEconomias).");

                            return;

                        }

                    } catch (Throwable ignored) {}

                }

                plugin.getLogger().severe("[ECONOMY] Falha ao conectar com a economia " + TARGET_ID + " após retry. Plugin será desativado.");

                Bukkit.getPluginManager().disablePlugin(plugin);

            }

        }.runTaskLater(plugin, 40L); // 2 segundos

        return false;

    }

    // MÉTODOS DE USO

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
