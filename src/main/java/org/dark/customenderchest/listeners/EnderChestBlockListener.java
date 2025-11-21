package org.dark.customenderchest.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.dark.customenderchest.database.DatabaseManager;
import org.dark.customenderchest.manager.EnderChestManager;
import org.dark.customenderchest.utils.PasswordConversation;

public class EnderChestBlockListener implements Listener {

    private final EnderChestManager enderChestManager;
    private final DatabaseManager databaseManager;
    private final PasswordConversation passwordConversation;

    public EnderChestBlockListener(EnderChestManager enderChestManager, DatabaseManager databaseManager, PasswordConversation passwordConversation) {
        this.enderChestManager = enderChestManager;
        this.databaseManager = databaseManager;
        this.passwordConversation = passwordConversation;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) return;
        
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        
        Player player = event.getPlayer();
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().isBlock()) return; 

        event.setCancelled(true);
        
        enderChestManager.registerOpenedBlock(player, block);

        player.playSound(block.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
        if (block.getState() instanceof EnderChest ec) {
            ec.open(); 
        }

        if (databaseManager.getPasswordHash(player.getUniqueId()) != null) {
            passwordConversation.startPasswordEntry(player);
        } else {
            enderChestManager.openEnderChest(player, player.getUniqueId(), 0, false);
        }
    }
}
