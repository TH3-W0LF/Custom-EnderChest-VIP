package com.alkacode.enderchest.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao soft com o AlkaVips (softdepend) - so pra saber quanto tempo falta pro
 * VIP ativo do jogador expirar (AlkaVipsAPI#getActiveVip -> PlayerVip#remainingMillis),
 * usada no aviso de "vai perder paginas extras" (ver EnderChestListener/EnderChestMenu).
 * As paginas em si continuam concedidas 100% por permissao (permissions.tiers no
 * config.yml) - este hook nunca decide acesso, so informa quanto tempo resta.
 *
 * Reflexao pura - NUNCA importar com.alkacode.vips.* direto aqui (mesmo motivo
 * documentado no AlkaVipsHook/AlkaMinesHook do AlkaDrop: um mismatch de versao entre
 * os dois jars vira LinkageError sem isso). O CompletableFuture devolvido pela API e
 * usado direto (e um tipo do JDK, nao do AlkaVips), mas com timeout curto no get().
 */
public final class AlkaVipsHook {

    private final Object api;
    private final Method getActiveVipMethod;
    private final Method remainingMillisMethod;
    private final Logger logger;

    private AlkaVipsHook(Object api, Method getActiveVipMethod, Method remainingMillisMethod, Logger logger) {
        this.api = api;
        this.getActiveVipMethod = getActiveVipMethod;
        this.remainingMillisMethod = remainingMillisMethod;
        this.logger = logger;
    }

    public static AlkaVipsHook tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("AlkaVips") == null) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("com.alkacode.vips.api.AlkaVipsAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return null;
            }
            Class<?> playerVipClass = Class.forName("com.alkacode.vips.model.PlayerVip");

            Method getActiveVip = apiClass.getMethod("getActiveVip", UUID.class);
            Method remainingMillis = playerVipClass.getMethod("remainingMillis");

            logger.info("AlkaVips detectado - aviso de expiracao de paginas VIP vai usar o tempo restante real.");
            return new AlkaVipsHook(registration.getProvider(), getActiveVip, remainingMillis, logger);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AlkaVips encontrado mas a API nao carregou (versao incompativel?) - "
                    + "aviso de expiracao de paginas VIP ficara indisponivel.", t);
            return null;
        }
    }

    /**
     * Millis restantes do VIP ativo do jogador, ou vazio se ele nao tiver VIP ativo, o
     * VIP for permanente (nunca expira - remainingMillis() negativo) ou a chamada
     * falhar por qualquer motivo. Nunca lanca.
     */
    public Optional<Long> activeVipRemainingMillis(Player player) {
        try {
            CompletableFuture<?> future = (CompletableFuture<?>) getActiveVipMethod.invoke(api, player.getUniqueId());
            Object playerVip = future.get(50, TimeUnit.MILLISECONDS);
            if (playerVip == null) {
                return Optional.empty();
            }
            long remaining = (long) remainingMillisMethod.invoke(playerVip);
            return remaining < 0 ? Optional.empty() : Optional.of(remaining);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook do AlkaVips falhou em activeVipRemainingMillis de " + player.getName()
                    + " - aviso de expiracao ignorado.", t);
            return Optional.empty();
        }
    }
}
