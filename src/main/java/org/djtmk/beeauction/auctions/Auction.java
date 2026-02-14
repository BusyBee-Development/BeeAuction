package org.djtmk.beeauction.auctions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.AuctionEnum.AuctionType;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.events.AuctionBidEvent;
import org.djtmk.beeauction.util.InputSanitizer;
import org.djtmk.beeauction.util.ItemUtils;
import org.djtmk.beeauction.util.MessageUtil;

import java.util.Map;
import java.util.UUID;

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
    private volatile double currentBid;
    private volatile Player highestBidder;
    private volatile boolean active;
    private volatile long endTime;

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

    public Auction(BeeAuction plugin, String command, String commandDisplayName, double startPrice, int duration, String customName, String ownerName) {
        this.plugin = plugin;
        this.type = AuctionType.COMMAND;
        this.item = null;
        this.command = command;
        this.commandDisplayName = commandDisplayName;
        this.startPrice = startPrice;
        this.duration = duration;
        this.customName = customName;
        this.ownerName = ownerName;
        this.ownerUuid = null;
        this.currentBid = startPrice;
        this.highestBidder = null;
        this.active = false;
    }

    public void start() {
        active = true;
        endTime = System.currentTimeMillis() + (duration * 1000L);
        String rewardName = getRewardName();
        String formattedPrice = MessageUtil.formatPrice(startPrice);
        Bukkit.broadcastMessage(MessageEnum.AUCTION_STARTED.get("item", rewardName, "price", formattedPrice));
    }

    public synchronized void end() {
        if (!active) {
            return;
        }
        active = false;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (highestBidder == null) {
                Bukkit.broadcastMessage(MessageEnum.AUCTION_CANCELLED.get("reason", "No bids were placed."));
                if (type == AuctionType.ITEM && item != null && ownerUuid != null) {
                    returnItemToOwner("Your auctioned item was returned (no bids).");
                }
                return;
            }

            handleWinner();
            handlePayment();
            broadcastEndMessage();
        });
    }

    public synchronized void cancel() {
        if (!active) {
            return;
        }
        active = false;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage(MessageEnum.AUCTION_CANCELLED.get("reason", "The auction was cancelled by an admin."));

            if (highestBidder != null) {
                plugin.getEconomyManager().deposit(highestBidder, currentBid);
                if (highestBidder.isOnline()) {
                    MessageUtil.sendMessage(highestBidder, "§aYour bid of " + MessageUtil.formatPrice(currentBid) + " was refunded.");
                }
            }

            if (type == AuctionType.ITEM && item != null && ownerUuid != null) {
                returnItemToOwner("Your auctioned item was returned because the auction was cancelled.");
            }
        });
    }

    public synchronized boolean placeBid(Player player, double amount) {
        if (!active) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return false;
        }

        double minIncrement = plugin.getConfigManager().getConfig().getDouble("auction.min-bid-increment", 1.0);
        double requiredBid = (highestBidder == null) ? currentBid : currentBid + minIncrement;

        if (amount < requiredBid) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_AMOUNT.get("amount", MessageUtil.formatPrice(requiredBid)));
            return false;
        }

        if (!plugin.getEconomyManager().has(player, amount).join()) {
            MessageUtil.sendMessage(player, MessageEnum.NOT_ENOUGH_MONEY.get());
            return false;
        }

        AuctionBidEvent bidEvent = new AuctionBidEvent(this, player, amount);
        Bukkit.getPluginManager().callEvent(bidEvent);
        if (bidEvent.isCancelled()) {
            return false;
        }

        boolean withdrawn = plugin.getEconomyManager().withdraw(player, amount).join();
        if (!withdrawn) {
            MessageUtil.sendMessage(player, "§cFailed to withdraw funds. Please try again.");
            plugin.getLogger().warning("Failed to withdraw " + amount + " from player " + player.getName() + " for auction bid");
            return false;
        }

        Player previousBidder = highestBidder;
        double previousBid = currentBid;
        if (previousBidder != null) {
            boolean refunded = plugin.getEconomyManager().deposit(previousBidder, previousBid).join();
            if (!refunded) {
                plugin.getLogger().severe("CRITICAL: Failed to refund " + previousBid + " to " + previousBidder.getName() +
                        " after accepting bid from " + player.getName() + ". Manual intervention required.");
            } else if (previousBidder.isOnline()) {
                MessageUtil.sendMessage(previousBidder, MessageEnum.OUTBID.get("player", player.getName()));
            }
        }

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
                "amount", MessageUtil.formatPrice(amount),
                "time_extension", timeExtensionText
        ));

        return true;
    }

    private void handleWinner() {
        if (highestBidder.isOnline()) {
            if (type == AuctionType.ITEM && item != null) {
                Map<Integer, ItemStack> couldNotFit = highestBidder.getInventory().addItem(item.clone());
                if (!couldNotFit.isEmpty()) {
                    // FIXED: Add error handling for database futures
                    plugin.getDatabaseManager().addPendingReward(highestBidder.getUniqueId(), couldNotFit.get(0), "Auction win (inventory full)")
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    plugin.getLogger().severe("Failed to save pending reward for " + highestBidder.getName() + ": " + error.getMessage());
                                }
                            });
                    MessageUtil.sendMessage(highestBidder, "§eYour inventory was full! The won item has been sent to your /claim queue.");
                }
                MessageUtil.sendMessage(highestBidder, MessageEnum.WIN_MESSAGE.get("item", getRewardName(), "amount", MessageUtil.formatPrice(currentBid)));
            } else if (type == AuctionType.COMMAND && command != null) {
                // SECURITY FIX: Sanitize player name to prevent command injection
                String sanitizedPlayerName = InputSanitizer.sanitizePlayerName(highestBidder.getName());
                String formattedCommand = command.replace("%player%", sanitizedPlayerName);

                plugin.getLogger().info("Executing auction command: " + InputSanitizer.sanitizeForLogging(formattedCommand) + " for player: " + highestBidder.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                MessageUtil.sendMessage(highestBidder, MessageEnum.WIN_MESSAGE.get("item", getRewardName(), "amount", MessageUtil.formatPrice(currentBid)));
            }
        } else {
            if (type == AuctionType.ITEM && item != null) {
                plugin.getDatabaseManager().addPendingReward(highestBidder.getUniqueId(), item, "Auction win")
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                plugin.getLogger().severe("Failed to save pending reward for " + highestBidder.getName() + ": " + error.getMessage());
                            }
                        });
            }
        }
    }

    private void handlePayment() {
        if (ownerUuid == null) return;

        double taxRate = plugin.getConfigManager().getConfig().getDouble("auction.sales-tax-rate", 0.0);
        double tax = currentBid * taxRate;
        double finalAmount = currentBid - tax;

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null) {
            plugin.getEconomyManager().deposit(owner, finalAmount);
            if (owner.isOnline()) {
                MessageUtil.sendMessage(owner, "§aYou have received " + MessageUtil.formatPrice(finalAmount) + " for your auction (after tax).");
            }
        }
    }

    private void returnItemToOwner(String reason) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null && owner.isOnline()) {
            Map<Integer, ItemStack> couldNotFit = owner.getInventory().addItem(item.clone());
            if (!couldNotFit.isEmpty()) {
                // FIXED: Add error handling for database futures
                plugin.getDatabaseManager().addPendingReward(ownerUuid, couldNotFit.get(0), "Auction item returned (inventory full)")
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                plugin.getLogger().severe("Failed to return item to owner " + ownerUuid + ": " + error.getMessage());
                            }
                        });
            }
        } else {
            plugin.getDatabaseManager().addPendingReward(ownerUuid, item.clone(), "Auction item returned")
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            plugin.getLogger().severe("Failed to return item to owner " + ownerUuid + ": " + error.getMessage());
                        }
                    });
        }
        if (owner != null && owner.isOnline()) {
            MessageUtil.sendMessage(owner, "§a" + reason);
        }
    }

    private void broadcastEndMessage() {
        String finalRewardName = getRewardName().replace("%player%", highestBidder.getName());
        Bukkit.broadcastMessage(MessageEnum.AUCTION_ENDED.get(
                "player", highestBidder.getName(),
                "amount", MessageUtil.formatPrice(currentBid),
                "item", finalRewardName
        ));
        plugin.getDatabaseManager().saveAuctionResult(
                highestBidder.getName(),
                highestBidder.getUniqueId(),
                currentBid,
                type.name(),
                finalRewardName
        ).whenComplete((result, error) -> {
            if (error != null) {
                plugin.getLogger().severe("Failed to save auction result: " + error.getMessage());
            }
        });
    }

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
            return command;
        }
        return "Unknown Reward";
    }

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
