package org.djtmk.beeauction.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuctionChatListener implements Listener {
    private final BeeAuction plugin;
    private final Map<UUID, PendingAuction> pendingAuctions = new ConcurrentHashMap<>();
    private final BukkitTask cleanupTask;

    public AuctionChatListener(BeeAuction plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            pendingAuctions.forEach((uuid, pending) -> {
                if (now - pending.getCreationTime() > TimeUnit.MINUTES.toMillis(2)) {
                    pendingAuctions.remove(uuid);
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        MessageUtil.sendMessage(player, "§cYour auction creation has timed out and was cancelled.");
                    }
                    if (pending.isItemAuction() && pending.getItem() != null) {
                        plugin.getDatabaseManager().addPendingReward(uuid, pending.getItem(), "Auction creation timed out");
                    }
                }
            });
        }, 20 * 60, 20 * 60);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (pendingAuctions.containsKey(playerUUID)) {
            event.setCancelled(true);
            PendingAuction pending = pendingAuctions.get(playerUUID);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                pendingAuctions.remove(playerUUID);
                MessageUtil.sendMessage(player, "§cAuction creation cancelled.");
                if (pending.isItemAuction()) {
                    plugin.getDatabaseManager().addPendingReward(playerUUID, pending.getItem(), "Auction creation cancelled");
                    MessageUtil.sendMessage(player, "§aYour item has been returned to your /claim queue.");
                }
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> handleInput(player, pending, message));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PendingAuction pending = pendingAuctions.remove(event.getPlayer().getUniqueId());
        if (pending != null && pending.isItemAuction()) {
            plugin.getDatabaseManager().addPendingReward(event.getPlayer().getUniqueId(), pending.getItem(), "Auction creation cancelled (disconnect)");
        }
    }

    private void handleInput(Player player, PendingAuction pending, String message) {
        switch (pending.getStage()) {
            case AWAITING_DISPLAY_NAME:
                pending.setCommandDisplayName(message);
                pending.setStage(PendingStage.AWAITING_AUCTION_NAME);
                player.sendMessage(MessageEnum.AUCTION_NAME_QUESTION.get());
                break;
            case AWAITING_AUCTION_NAME:
                pending.setAuctionName(message);
                startAuctionFromPending(player, pending);
                break;
        }
    }

    private void startAuctionFromPending(Player player, PendingAuction pending) {
        pendingAuctions.remove(player.getUniqueId());
        if (pending.isItemAuction()) {
            plugin.getAuctionManager().startItemAuction(
                    pending.getItem(),
                    pending.getDuration(),
                    pending.getStartPrice(),
                    pending.getAuctionName(),
                    player
            );
        } else {
            plugin.getAuctionManager().startCommandAuction(
                    pending.getCommand(),
                    pending.getCommandDisplayName(),
                    pending.getDuration(),
                    pending.getStartPrice(),
                    pending.getAuctionName(),
                    player.getName()
            );
        }
    }

    public void addPendingItemAuction(Player player, ItemStack item, int duration, double startPrice) {
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(item, duration, startPrice));
    }

    public void addPendingCommandAuction(Player player, String command, int duration, double startPrice) {
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(command, duration, startPrice));
    }

    public void cancelCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }

    private enum PendingStage {
        AWAITING_AUCTION_NAME,
        AWAITING_DISPLAY_NAME
    }

    private static class PendingAuction {
        private final ItemStack item;
        private final String command;
        private final int duration;
        private final double startPrice;
        private final long creationTime;
        private PendingStage stage;

        private String auctionName;
        private String commandDisplayName;

        public PendingAuction(ItemStack item, int duration, double startPrice) {
            this.item = item;
            this.command = null;
            this.duration = duration;
            this.startPrice = startPrice;
            this.creationTime = System.currentTimeMillis();
            this.stage = PendingStage.AWAITING_AUCTION_NAME;
        }

        public PendingAuction(String command, int duration, double startPrice) {
            this.item = null;
            this.command = command;
            this.duration = duration;
            this.startPrice = startPrice;
            this.creationTime = System.currentTimeMillis();
            this.stage = PendingStage.AWAITING_DISPLAY_NAME;
        }

        public boolean isItemAuction() { return item != null; }
        public ItemStack getItem() { return item; }
        public String getCommand() { return command; }
        public int getDuration() { return duration; }
        public double getStartPrice() { return startPrice; }
        public long getCreationTime() { return creationTime; }
        public PendingStage getStage() { return stage; }
        public String getAuctionName() { return auctionName; }
        public String getCommandDisplayName() { return commandDisplayName; }

        public void setStage(PendingStage stage) { this.stage = stage; }
        public void setAuctionName(String auctionName) { this.auctionName = auctionName; }
        public void setCommandDisplayName(String commandDisplayName) { this.commandDisplayName = commandDisplayName; }
    }
}
