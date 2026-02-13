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

        // FIXED: Check if player is in creation mode before processing
        if (!creationManager.hasPendingAuction(player)) {
            return; // Not in creation mode, let chat proceed normally
        }

        // Cancel event on async thread (this is safe)
        event.setCancelled(true);

        // FIXED: Schedule auction creation to main thread
        // This prevents IllegalStateException when firing AuctionStartEvent
        // AsyncPlayerChatEvent runs on async thread, but Bukkit events must be fired synchronously
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
