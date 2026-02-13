package org.djtmk.beeauction.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.auctions.AuctionCreationManager;

public class AuctionCreationListener implements Listener {

    private final BeeAuction plugin;
    private final AuctionCreationManager creationManager;

    public AuctionCreationListener(BeeAuction plugin) {
        this.plugin = plugin;
        this.creationManager = new AuctionCreationManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!creationManager.hasPendingAuction(player)) {
            return;
        }

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            creationManager.handleChatInput(player, message);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        creationManager.removePendingAuction(event.getPlayer());
    }

    public AuctionCreationManager getCreationManager() {
        return creationManager;
    }
}
