package org.djtmk.beeauction.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.djtmk.beeauction.BeeAuction;

public class AdminJoinListener implements Listener {

    private final BeeAuction plugin;

    public AdminJoinListener(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // The permission should be specific to the plugin
        if (player.hasPermission("beeauction.admin")) {
            // Delay notification slightly to avoid chat spam on join
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getUpdateChecker().notifyPlayer(player);
            }, 40L); // 2-second delay
        }
    }
}
