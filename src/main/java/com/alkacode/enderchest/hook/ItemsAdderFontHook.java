package com.alkacode.enderchest.hook;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao soft com o ItemsAdder (softdepend) - resolve os placeholders de font-image
 * (:offset_-16::ui_enderchest:) do titulo da GUI do EnderChest em texto real via a API
 * oficial dev.lone.itemsadder.api.FontImages.FontImageWrapper#replaceFontImages, que e
 * o metodo documentado pra isso (a versao anterior deste hook tentava resolver via
 * PlaceholderAPI, o que nunca funcionou - o font-image do ItemsAdder nao passa por PAPI).
 *
 * Reflexao pura - mesmo motivo documentado no AlkaVipsHook (evitar LinkageError por
 * mismatch de versao/classpath). Se o ItemsAdder nao estiver presente ou a chamada
 * falhar, replaceFontImages() devolve a string original sem alteracao - o EnderChestMenu
 * ja trata esse caso como "sem imagem custom", caindo no titulo de texto padrao.
 */
public final class ItemsAdderFontHook {

    private final Method replaceMethod;
    private final Logger logger;

    private ItemsAdderFontHook(Method replaceMethod, Logger logger) {
        this.replaceMethod = replaceMethod;
        this.logger = logger;
    }

    public static ItemsAdderFontHook tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return null;
        }
        try {
            Class<?> wrapperClass = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            Method replace = wrapperClass.getMethod("replaceFontImages", String.class);
            logger.info("ItemsAdder detectado - GUI do EnderChest vai usar a imagem de fundo custom.");
            return new ItemsAdderFontHook(replace, logger);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "ItemsAdder encontrado mas a API de font-images nao carregou "
                    + "(versao incompativel?) - GUI do EnderChest ficara com titulo de texto simples.", t);
            return null;
        }
    }

    /** Resolve os placeholders de font-image (:nome:, :offset_N:) em texto real, ou
     * devolve a string original se a chamada falhar. Nunca lanca. */
    public String replaceFontImages(String raw) {
        try {
            return (String) replaceMethod.invoke(null, raw);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook do ItemsAdder falhou em replaceFontImages - titulo cai pro padrao.", t);
            return raw;
        }
    }
}
