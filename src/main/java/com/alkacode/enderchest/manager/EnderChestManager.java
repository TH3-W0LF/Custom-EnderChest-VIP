package com.alkacode.enderchest.manager;

import com.alkacode.enderchest.database.EnderChestRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calculo de tier/paginas por permissao e o rastreamento do bloco fisico aberto (para
 * animar a tampa do bau). O menu de upgrades vive em EnderChestUpgradeGui (BaseGui do
 * AlkaCore) - esta classe so calcula o tier efetivo, que o menu consulta.
 */
public class EnderChestManager {

    private final JavaPlugin plugin;
    private final EnderChestRepository repository;
    private final Map<UUID, Location> openedBlocks = new HashMap<>();

    public EnderChestManager(JavaPlugin plugin, EnderChestRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void registerOpenedBlock(Player player, Block block) {
        openedBlocks.put(player.getUniqueId(), block.getLocation());
    }

    public void closeOpenedBlock(Player player) {
        Location loc = openedBlocks.remove(player.getUniqueId());
        if (loc != null) {
            Block block = loc.getBlock();
            if (block.getType() == Material.ENDER_CHEST && block.getState() instanceof EnderChest ec) {
                ec.close();
            }
        }
    }

    public int getEffectiveTier(Player player) {
        int dbTier = repository.getTier(player.getUniqueId());
        int permTier = 1;

        ConfigurationSection tiersConfig = plugin.getConfig().getConfigurationSection("permissions.tiers");
        if (tiersConfig != null) {
            String[] tierKeys = {"vip4", "vip3", "vip2", "vip1", "free"};
            for (String tierKey : tierKeys) {
                ConfigurationSection tierConfig = tiersConfig.getConfigurationSection(tierKey);
                if (tierConfig != null) {
                    String permission = tierConfig.getString("permission");
                    int pages = tierConfig.getInt("pages", 1);
                    if (permission != null && player.hasPermission(permission)) {
                        permTier = pages;
                        break;
                    }
                }
            }
        }

        return Math.max(1, Math.max(dbTier, permTier));
    }

    public int getMaxPages(Player player) {
        int tier = getEffectiveTier(player);
        return Math.max(1, tier);
    }
}
