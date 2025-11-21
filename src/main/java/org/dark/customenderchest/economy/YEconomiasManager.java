package org.dark.customenderchest.economy;

import br.com.ystoreplugins.product.economy.EconomyProvider;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de múltiplas economias do yEconomias
 * Permite acessar economias específicas pelo plugin-id
 */
public class YEconomiasManager {
    
    private static final Map<String, EconomyProvider> providers = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * Inicializa e mapeia todos os providers de economia disponíveis
     * IMPORTANTE: Funciona tanto com yEconomias standalone quanto como módulo do yPlugins
     */
    public static void initialize() {
        if (initialized) return;
        
        try {
            Bukkit.getLogger().info("[YEconomiasManager] Buscando providers de economia no ServicesManager...");
            
            // Busca DIRETAMENTE no ServicesManager (funciona com yEconomias standalone ou como módulo)
            var registrations = Bukkit.getServicesManager().getRegistrations(EconomyProvider.class);
            
            Bukkit.getLogger().info("[YEconomiasManager] Encontrados " + registrations.size() + " registros de EconomyProvider");
            
            if (registrations.isEmpty()) {
                Bukkit.getLogger().warning("[YEconomiasManager] ⚠ Nenhum provider encontrado!");
                Bukkit.getLogger().warning("[YEconomiasManager] Certifique-se de que o yEconomias/yPlugins está instalado.");
                initialized = true;
                return;
            }
            
            registrations.forEach(registration -> {
                try {
                    EconomyProvider provider = registration.getProvider();
                    String name = provider.getName();
                    providers.put(name.toLowerCase(), provider);
                    Bukkit.getLogger().info("[YEconomiasManager] ✓ Provider registrado: '" + name + "'");
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[YEconomiasManager] Erro ao registrar provider: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            initialized = true;
            Bukkit.getLogger().info("[YEconomiasManager] ========================================");
            Bukkit.getLogger().info("[YEconomiasManager] Total de providers registrados: " + providers.size());
            Bukkit.getLogger().info("[YEconomiasManager] ========================================");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[YEconomiasManager] ERRO CRÍTICO ao inicializar: " + e.getMessage());
            e.printStackTrace();
            initialized = true; // Marca como inicializado para não tentar novamente
        }
    }
    
    /**
     * Obtém um provider específico pelo plugin-id
     * @param pluginId O ID do plugin da economia (ex: "yeconomias-drakonio")
     * @return O provider ou null se não encontrado
     */
    public static EconomyProvider getProvider(String pluginId) {
        if (!initialized) {
            initialize();
        }
        return providers.get(pluginId.toLowerCase());
    }
    
    /**
     * Lista todos os providers disponíveis
     */
    public static Map<String, EconomyProvider> getAllProviders() {
        if (!initialized) {
            initialize();
        }
        return new HashMap<>(providers);
    }
    
    /**
     * Verifica se existe um provider com o ID especificado
     */
    public static boolean hasProvider(String pluginId) {
        if (!initialized) {
            initialize();
        }
        return providers.containsKey(pluginId.toLowerCase());
    }
}

