package org.djtmk.beeauction.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionChatListener implements Listener {
    private final BeeAuction plugin;
    private final Map<UUID, PendingAuction> pendingAuctions = new HashMap<>();

    public AuctionChatListener(BeeAuction plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if the player has a pending auction
        if (pendingAuctions.containsKey(playerUUID)) {
            // Cancel the chat event to prevent the message from being broadcast
            event.setCancelled(true);

            // Get the pending auction
            PendingAuction pendingAuction = pendingAuctions.get(playerUUID);
            String auctionName = event.getMessage();

            // Remove the pending auction
            pendingAuctions.remove(playerUUID);

            // Start the auction with the provided name
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (pendingAuction.isItemAuction()) {
                    plugin.getAuctionManager().startItemAuction(
                            pendingAuction.getItem(),
                            pendingAuction.getDuration(),
                            pendingAuction.getStartPrice(),
                            auctionName,
                            pendingAuction.getOwnerName()
                    );
                } else {
                    plugin.getAuctionManager().startCommandAuction(
                            pendingAuction.getCommand(),
                            pendingAuction.getDuration(),
                            pendingAuction.getStartPrice(),
                            auctionName,
                            pendingAuction.getOwnerName()
                    );
                }
            });
        }
    }

    public void addPendingItemAuction(Player player, ItemStack item, int duration, double startPrice) {
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(item, duration, startPrice, player.getName()));
    }

    public void addPendingCommandAuction(Player player, String command, int duration, double startPrice) {
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(command, duration, startPrice, player.getName()));
    }

    public void removePendingAuction(Player player) {
        pendingAuctions.remove(player.getUniqueId());
    }

    private static class PendingAuction {
        private final ItemStack item;
        private final String command;
        private final int duration;
        private final double startPrice;
        private final String ownerName;

        // Constructor for item auction
        public PendingAuction(ItemStack item, int duration, double startPrice, String ownerName) {
            this.item = item;
            this.command = null;
            this.duration = duration;
            this.startPrice = startPrice;
            this.ownerName = ownerName;
        }

        // Constructor for command auction
        public PendingAuction(String command, int duration, double startPrice, String ownerName) {
            this.item = null;
            this.command = command;
            this.duration = duration;
            this.startPrice = startPrice;
            this.ownerName = ownerName;
        }

        public boolean isItemAuction() {
            return item != null;
        }

        public ItemStack getItem() {
            return item;
        }

        public String getCommand() {
            return command;
        }

        public int getDuration() {
            return duration;
        }

        public double getStartPrice() {
            return startPrice;
        }

        public String getOwnerName() {
            return ownerName;
        }
    }
}
