package org.djtmk.beeauction.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

public class ClaimListener implements Listener {
    private final BeeAuction plugin;

    public ClaimListener(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check for pending rewards asynchronously to avoid login lag
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (plugin.getDatabaseManager().hasPendingRewards(player.getUniqueId())) {
                MessageUtil.sendMessage(player, MessageEnum.CLAIM_JOIN_NOTIFICATION.get());
            }
        }, 60L); // 3-second delay
    }
}