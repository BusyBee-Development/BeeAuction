package org.djtmk.beeauction.auctions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.util.InputSanitizer;
import org.djtmk.beeauction.util.ItemUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionCreationManager {

    private final BeeAuction plugin;
    private final Map<UUID, PendingAuction> pendingAuctions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAuctionCreation = new ConcurrentHashMap<>();

    public AuctionCreationManager(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public void startItemAuctionCreation(Player player, ItemStack item, int duration, double startPrice) {
        if (isDuped(item)) {
            player.sendMessage("§cYou cannot auction this item.");
            return;
        }
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(item, duration, startPrice));
        player.sendMessage("Please enter a name for this auction in chat (or type 'cancel').");
    }

    public void startCommandAuctionCreation(Player player, String command, int duration, double startPrice) {
        pendingAuctions.put(player.getUniqueId(), new PendingAuction(command, duration, startPrice));
        player.sendMessage("Please enter a display name for the command reward (e.g., '&b5 Diamonds', or type 'cancel').");
    }

    public void handleChatInput(Player player, String message) {
        PendingAuction pending = pendingAuctions.get(player.getUniqueId());
        if (pending == null) {
            return;
        }

        if (message.equalsIgnoreCase("cancel")) {
            pendingAuctions.remove(player.getUniqueId());
            player.sendMessage("Auction creation cancelled.");
            if (pending.isItemAuction()) {
                plugin.getDatabaseManager().addPendingReward(player.getUniqueId(), pending.getItem(), "Auction creation cancelled")
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                plugin.getLogger().severe("Failed to return item to " + player.getName() + " after cancel: " + error.getMessage());
                            }
                        });
                player.sendMessage("Your item has been returned to your /claim queue.");
            }
            return;
        }

        String sanitizedInput = InputSanitizer.sanitizeChatInput(message);

        if (sanitizedInput.isEmpty()) {
            player.sendMessage("§cInvalid input. Please enter a valid name (or type 'cancel').");
            return;
        }

        switch (pending.getStage()) {
            case AWAITING_DISPLAY_NAME:
                String sanitizedDisplayName = InputSanitizer.sanitizeCommandDisplayName(sanitizedInput);
                pending.setCommandDisplayName(sanitizedDisplayName);
                pending.setStage(PendingStage.AWAITING_AUCTION_NAME);
                player.sendMessage("Please enter a name for this auction in chat (or type 'cancel').");
                break;
            case AWAITING_AUCTION_NAME:
                String sanitizedAuctionName = InputSanitizer.sanitizeAuctionName(sanitizedInput);
                pending.setAuctionName(sanitizedAuctionName);
                createAuction(player, pending);
                break;
        }
    }

    private void createAuction(Player player, PendingAuction pending) {
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
        lastAuctionCreation.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean hasPendingAuction(Player player) {
        return pendingAuctions.containsKey(player.getUniqueId());
    }
    public void removePendingAuction(Player player) {
        pendingAuctions.remove(player.getUniqueId());
    }

    public boolean canCreateAuction(Player player) {
        if (!lastAuctionCreation.containsKey(player.getUniqueId())) {
            return true;
        }
        long lastCreation = lastAuctionCreation.get(player.getUniqueId());
        return System.currentTimeMillis() - lastCreation > 30000; // 30 seconds
    }

    private boolean isDuped(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().has(ItemUtils.LEGACY_ITEM_KEY, PersistentDataType.BYTE);
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
        private PendingStage stage;
        private String auctionName;
        private String commandDisplayName;

        public PendingAuction(ItemStack item, int duration, double startPrice) {
            this.item = item;
            this.command = null;
            this.duration = duration;
            this.startPrice = startPrice;
            this.stage = PendingStage.AWAITING_AUCTION_NAME;
        }

        public PendingAuction(String command, int duration, double startPrice) {
            this.item = null;
            this.command = command;
            this.duration = duration;
            this.startPrice = startPrice;
            this.stage = PendingStage.AWAITING_DISPLAY_NAME;
        }

        public boolean isItemAuction() { return item != null; }
        public ItemStack getItem() { return item; }
        public String getCommand() { return command; }
        public int getDuration() { return duration; }
        public double getStartPrice() { return startPrice; }
        public PendingStage getStage() { return stage; }
        public String getAuctionName() { return auctionName; }
        public String getCommandDisplayName() { return commandDisplayName; }

        public void setStage(PendingStage stage) { this.stage = stage; }
        public void setAuctionName(String auctionName) { this.auctionName = auctionName; }
        public void setCommandDisplayName(String commandDisplayName) { this.commandDisplayName = commandDisplayName; }
    }
}
