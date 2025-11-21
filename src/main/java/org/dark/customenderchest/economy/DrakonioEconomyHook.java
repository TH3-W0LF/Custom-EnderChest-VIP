package org.dark.customenderchest.economy;

import br.com.ystoreplugins.product.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;

/**
 * Hook direto e forçado para o yEconomias "drakonio"
 * Não usa Vault, conecta diretamente via API do yPlugins
 */
public final class DrakonioEconomyHook {

    private static EconomyProvider ECONOMY;

    /**
     * Configura e registra o provider de economia "drakonio"
     * 
     * @param plugin Instância do plugin CustomEnderChest
     * @return true se conectou com sucesso, false caso contrário
     */
    public static boolean setup(Plugin plugin) {
        // Verifica se o yEconomias existe
        if (Bukkit.getPluginManager().getPlugin("yEconomias") == null) {
            plugin.getLogger().severe("========================================");
            plugin.getLogger().severe("✘ yEconomias NÃO encontrado!");
            plugin.getLogger().severe("A economia Drakonio não pôde ser conectada!");
            plugin.getLogger().severe("========================================");
            return false;
        }

        try {
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("Conectando à economia 'drakonio'...");
            
            // Usa reflexão para criar o yEconomias (não disponível em tempo de compilação)
            Class<?> yEconomiasClass = Class.forName("br.com.ystoreplugins.product.economy.methods.yEconomias");
            Constructor<?> constructor = yEconomiasClass.getConstructor(String.class);
            ECONOMY = (EconomyProvider) constructor.newInstance("drakonio");
            
            // Registra no ServicesManager com prioridade MÁXIMA
            Bukkit.getServicesManager().register(
                    EconomyProvider.class,
                    ECONOMY,
                    plugin,
                    org.bukkit.plugin.ServicePriority.Highest
            );
            
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("✓✓✓ [ECONOMIA] Conectado com sucesso!");
            plugin.getLogger().info("✓ Provider: yEconomias -> drakonio");
            plugin.getLogger().info("✓ Nome: " + ECONOMY.getName());
            plugin.getLogger().info("========================================");
            
            return true;
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("========================================");
            plugin.getLogger().severe("✘ ERRO: Classe yEconomias não encontrada!");
            plugin.getLogger().severe("Caminho: br.com.ystoreplugins.product.economy.methods.yEconomias");
            plugin.getLogger().severe("Certifique-se de que o yPlugins/yEconomias está instalado.");
            plugin.getLogger().severe("========================================");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("========================================");
            plugin.getLogger().severe("✘ ERRO ao registrar a economia drakonio:");
            plugin.getLogger().severe(e.getMessage());
            plugin.getLogger().severe("========================================");
            e.printStackTrace();
            return false;
        }
    }

    // ======================
    // MÉTODOS DE USO DIRETO
    // ======================

    public static boolean has(String player, double amount) {
        return ECONOMY != null && ECONOMY.has(player, amount);
    }

    public static double get(String player) {
        return ECONOMY != null ? ECONOMY.get(player) : 0;
    }

    public static void add(String player, double amount) {
        if (ECONOMY != null) {
            ECONOMY.add(player, amount);
        }
    }

    public static void remove(String player, double amount) {
        if (ECONOMY != null) {
            ECONOMY.remove(player, amount);
        }
    }

    public static boolean isActive() {
        return ECONOMY != null;
    }
    
    public static EconomyProvider getProvider() {
        return ECONOMY;
    }
}
