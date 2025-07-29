package org.djtmk.beeauction.auctions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.AuctionEnum.AuctionType;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.ItemUtils;
import org.djtmk.beeauction.util.MessageUtil;

import java.util.Map;
import java.util.UUID;

// UPDATED: This class has been significantly refactored for robustness and new features.
public class Auction {
    private final BeeAuction plugin;
    private final AuctionType type;
    private final ItemStack item;
    private final String command;
    private final String commandDisplayName;
    private final double startPrice;
    private final int duration;
    private final String customName;
    private final String ownerName;
    private final UUID ownerUuid;

    private double currentBid;
    private Player highestBidder;
    private boolean active;
    private long endTime;

    // UPDATED: Main constructor for player-started item auctions
    public Auction(BeeAuction plugin, ItemStack item, double startPrice, int duration, String customName, Player owner) {
        this.plugin = plugin;
        this.type = AuctionType.ITEM;
        this.item = item.clone();
        this.command = null;
        this.commandDisplayName = null;
        this.startPrice = startPrice;
        this.duration = duration;
        this.customName = customName;
        this.ownerName = owner.getName();
        this.ownerUuid = owner.getUniqueId();
        this.currentBid = startPrice;
        this.highestBidder = null;
        this.active = false;
    }

    // UPDATED: Main constructor for server-started command auctions
    public Auction(BeeAuction plugin, String command, String commandDisplayName, double startPrice, int duration, String customName, String ownerName) {
        this.plugin = plugin;
        this.type = AuctionType.COMMAND;
        this.item = null;
        this.command = command;
        this.commandDisplayName = commandDisplayName;
        this.startPrice = startPrice;
        this.duration = duration;
        this.customName = customName;
        this.ownerName = ownerName; // e.g., "Console" for scheduled auctions
        this.ownerUuid = null; // No specific owner UUID for console auctions
        this.currentBid = startPrice;
        this.highestBidder = null;
        this.active = false;
    }

    public void start() {
        active = true;
        endTime = System.currentTimeMillis() + (duration * 1000L);
        String rewardName = getRewardName();
        Bukkit.broadcastMessage(MessageEnum.AUCTION_STARTED.get("item", rewardName, "price", plugin.getEconomyHandler().format(startPrice)));
    }

    // UPDATED: Refactored for clarity and offline handling
    public void end() {
        active = false;

        if (highestBidder == null) {
            Bukkit.broadcastMessage(MessageEnum.CANCELLED.get("reason", "No bids were placed."));
            if (type == AuctionType.ITEM && item != null && ownerUuid != null) {
                returnItemToOwner("Your auctioned item was returned (no bids).");
            }
            return;
        }

        handleWinner();
        handlePayment();
        broadcastEndMessage();
    }

    // UPDATED: Refactored for clarity and offline handling
    public void cancel() {
        active = false;
        Bukkit.broadcastMessage(MessageEnum.CANCELLED.get("reason", "The auction was cancelled by an admin."));

        if (highestBidder != null) {
            plugin.getEconomyHandler().deposit(highestBidder.getUniqueId(), currentBid);
            if (highestBidder.isOnline()) {
                MessageUtil.sendMessage(highestBidder, "§aYour bid of " + plugin.getEconomyHandler().format(currentBid) + " was refunded.");
            }
        }

        if (type == AuctionType.ITEM && item != null && ownerUuid != null) {
            returnItemToOwner("Your auctioned item was returned because the auction was cancelled.");
        }
    }

    // UPDATED: Includes minimum bid increment logic
    public boolean placeBid(Player player, double amount) {
        if (!active) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return false;
        }

        double minIncrement = plugin.getConfigManager().getConfig().getDouble("auction.min-bid-increment", 1.0);
        double requiredBid = (highestBidder == null) ? startPrice : currentBid + minIncrement;

        if (amount < requiredBid) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_AMOUNT.get("amount", plugin.getEconomyHandler().format(requiredBid)));
            return false;
        }

        if (!plugin.getEconomyHandler().hasEnough(player, amount)) {
            MessageUtil.sendMessage(player, MessageEnum.NOT_ENOUGH_MONEY.get());
            return false;
        }

        if (highestBidder != null) {
            plugin.getEconomyHandler().deposit(highestBidder.getUniqueId(), currentBid);
            if (highestBidder.isOnline()) {
                MessageUtil.sendMessage(highestBidder, MessageEnum.OUTBID.get("player", player.getName()));
            }
        }

        plugin.getEconomyHandler().withdraw(player, amount);

        currentBid = amount;
        highestBidder = player;

        int timeExtension = plugin.getConfigManager().getConfig().getInt("auction.bid-time-extension", 30);
        int timeThreshold = plugin.getConfigManager().getConfig().getInt("auction.bid-time-threshold", 60);
        boolean timeExtended = false;
        if (getTimeRemaining() < timeThreshold) {
            endTime += timeExtension * 1000L;
            timeExtended = true;
        }

        String timeExtensionText = timeExtended ? MessageEnum.TIME_EXTENSION.get("seconds", String.valueOf(timeExtension)) : "";
        Bukkit.broadcastMessage(MessageEnum.NEW_BID.get(
                "player", player.getName(),
                "amount", plugin.getEconomyHandler().format(amount),
                "time_extension", timeExtensionText
        ));

        return true;
    }

    // NEW: Helper method to handle giving the reward
    private void handleWinner() {
        if (highestBidder.isOnline()) {
            if (type == AuctionType.ITEM && item != null) {
                Map<Integer, ItemStack> couldNotFit = highestBidder.getInventory().addItem(item.clone());
                if (!couldNotFit.isEmpty()) {
                    plugin.getDatabaseManager().addPendingReward(highestBidder.getUniqueId(), couldNotFit.get(0), "Auction win (inventory full)");
                    MessageUtil.sendMessage(highestBidder, "§eYour inventory was full! The won item has been sent to your /claim queue.");
                }
                MessageUtil.sendMessage(highestBidder, MessageEnum.WIN.get("item", getRewardName()));
            } else if (type == AuctionType.COMMAND && command != null) {
                String formattedCommand = command.replace("%player%", highestBidder.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                MessageUtil.sendMessage(highestBidder, MessageEnum.WIN.get("item", getRewardName()));
            }
        } else {
            // Handle offline winner by adding item to their claim queue
            if (type == AuctionType.ITEM && item != null) {
                plugin.getDatabaseManager().addPendingReward(highestBidder.getUniqueId(), item, "Auction win");
            }
            // Note: Command rewards for offline players are generally not given.
        }
    }

    // NEW: Helper method to handle paying the seller
    private void handlePayment() {
        if (ownerUuid == null) return; // No seller to pay (e.g., console auction)

        double taxRate = plugin.getConfigManager().getConfig().getDouble("auction.sales-tax-rate", 0.0);
        double tax = currentBid * taxRate;
        double finalAmount = currentBid - tax;

        plugin.getEconomyHandler().deposit(ownerUuid, finalAmount);

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null && owner.isOnline()) {
            MessageUtil.sendMessage(owner, "§aYou have received " + plugin.getEconomyHandler().format(finalAmount) + " for your auction (after tax).");
        }
    }

    // NEW: Helper method to return item to owner (online or offline)
    private void returnItemToOwner(String reason) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null && owner.isOnline()) {
            Map<Integer, ItemStack> couldNotFit = owner.getInventory().addItem(item.clone());
            if (!couldNotFit.isEmpty()) {
                plugin.getDatabaseManager().addPendingReward(ownerUuid, couldNotFit.get(0), "Auction item returned (inventory full)");
            }
        } else {
            plugin.getDatabaseManager().addPendingReward(ownerUuid, item.clone(), "Auction item returned");
        }
        if (owner != null && owner.isOnline()) {
            MessageUtil.sendMessage(owner, "§a" + reason);
        }
    }

    // NEW: Helper method to broadcast end message and save result
    private void broadcastEndMessage() {
        String finalRewardName = getRewardName().replace("%player%", highestBidder.getName());
        Bukkit.broadcastMessage(MessageEnum.AUCTION_ENDED.get(
                "player", highestBidder.getName(),
                "amount", plugin.getEconomyHandler().format(currentBid),
                "item", finalRewardName
        ));
        plugin.getDatabaseManager().saveAuctionResult(
                highestBidder.getName(),
                highestBidder.getUniqueId(),
                currentBid,
                type.name(),
                finalRewardName
        );
    }

    // UPDATED: getRewardName is now simpler and more reliable
    public String getRewardName() {
        if (customName != null && !customName.isEmpty()) {
            return MessageUtil.colorize(customName);
        }
        if (type == AuctionType.ITEM && item != null) {
            return ItemUtils.getItemDisplayName(item);
        } else if (type == AuctionType.COMMAND && command != null) {
            if (commandDisplayName != null && !commandDisplayName.isEmpty()) {
                return MessageUtil.colorize(commandDisplayName);
            }
            return command; // Fallback to the raw command
        }
        return "Unknown Reward";
    }

    // --- Standard Getters ---
    public int getTimeRemaining() {
        if (!active) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }
    public boolean isActive() { return active; }
    public AuctionType getType() { return type; }
    public ItemStack getItem() { return item; }
    public String getCommand() { return command; }
    public double getCurrentBid() { return currentBid; }
    public Player getHighestBidder() { return highestBidder; }
    public int getDuration() { return duration; }
    public String getOwnerName() { return ownerName; }
    public UUID getOwnerUuid() { return ownerUuid; }
}
