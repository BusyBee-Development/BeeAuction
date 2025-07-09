package org.djtmk.beeauction.auctions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.AuctionEnum.AuctionType;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.ItemUtils;
import org.djtmk.beeauction.util.MessageUtil;

public class Auction {
    private final BeeAuction plugin;
    private final AuctionType type;
    private final ItemStack item;
    private final String command;
    private final double startPrice;
    private final int duration;
    private final String customName;
    private final String ownerName; // Store the owner's name

    private double currentBid;
    private Player highestBidder;
    private boolean active;
    private long endTime;

    /**
     * Constructor for an item auction
     * @param plugin The plugin instance
     * @param item The item being auctioned
     * @param startPrice The starting price
     * @param duration The duration in seconds
     * @param customName Optional custom name for the auction
     * @param ownerName The name of the player who started the auction
     */
    public Auction(BeeAuction plugin, ItemStack item, double startPrice, int duration, String customName, String ownerName) {
        this.plugin = plugin;
        this.type = AuctionType.ITEM;
        this.item = item;
        this.command = null;
        this.startPrice = startPrice;
        this.duration = duration;
        this.customName = customName;
        this.ownerName = ownerName;
        this.currentBid = startPrice;
        this.highestBidder = null;
        this.active = false;
    }

    /**
     * Constructor for an item auction (legacy constructor)
     * @param plugin The plugin instance
     * @param item The item being auctioned
     * @param startPrice The starting price
     * @param duration The duration in seconds
     * @param customName Optional custom name for the auction
     */
    public Auction(BeeAuction plugin, ItemStack item, double startPrice, int duration, String customName) {
        this(plugin, item, startPrice, duration, customName, "Unknown");
    }

    /**
     * Constructor for an item auction (legacy constructor)
     * @param plugin The plugin instance
     * @param item The item being auctioned
     * @param startPrice The starting price
     * @param duration The duration in seconds
     */
    public Auction(BeeAuction plugin, ItemStack item, double startPrice, int duration) {
        this(plugin, item, startPrice, duration, null, "Unknown");
    }

    /**
     * Constructor for a command auction
     * @param plugin The plugin instance
     * @param command The command to execute
     * @param startPrice The starting price
     * @param duration The duration in seconds
     * @param customName Optional custom name for the auction
     * @param ownerName The name of the player who started the auction
     */
    public Auction(BeeAuction plugin, String command, double startPrice, int duration, String customName, String ownerName) {
        this.plugin = plugin;
        this.type = AuctionType.COMMAND;
        this.item = null;
        this.command = command;
        this.startPrice = startPrice;
        this.duration = duration;
        this.customName = customName;
        this.ownerName = ownerName;
        this.currentBid = startPrice;
        this.highestBidder = null;
        this.active = false;
    }

    /**
     * Constructor for a command auction (legacy constructor)
     * @param plugin The plugin instance
     * @param command The command to execute
     * @param startPrice The starting price
     * @param duration The duration in seconds
     * @param customName Optional custom name for the auction
     */
    public Auction(BeeAuction plugin, String command, double startPrice, int duration, String customName) {
        this(plugin, command, startPrice, duration, customName, "Unknown");
    }

    /**
     * Constructor for a command auction (legacy constructor)
     * @param plugin The plugin instance
     * @param command The command to execute
     * @param startPrice The starting price
     * @param duration The duration in seconds
     */
    public Auction(BeeAuction plugin, String command, double startPrice, int duration) {
        this(plugin, command, startPrice, duration, null, "Unknown");
    }

    public void start() {
        active = true;
        endTime = System.currentTimeMillis() + (duration * 1000L);

        // Broadcast auction start message
        String rewardName = getRewardName();
        Bukkit.broadcastMessage(MessageEnum.AUCTION_STARTED.get("item", rewardName, "price", plugin.getEconomyHandler().format(startPrice)));
    }

    public void end() {
        active = false;

        // If no bids, cancel the auction
        if (highestBidder == null) {
            // Return the item to the owner if this is an item auction
            if (type == AuctionType.ITEM && item != null && ownerName != null) {
                // Try to find the owner online
                Player owner = Bukkit.getPlayerExact(ownerName);
                if (owner != null && owner.isOnline()) {
                    // Give the item back to the owner
                    owner.getInventory().addItem(item.clone());
                    MessageUtil.sendMessage(owner, "§aYour auctioned item has been returned to you as there were no bids.");
                } else {
                    // Owner is offline, log this situation
                    plugin.getLogger().warning("Could not return item to offline owner: " + ownerName);
                    // TODO: Consider storing the item in a database or other storage for later retrieval
                }
            }

            Bukkit.broadcastMessage(MessageEnum.CANCELLED.get());
            return;
        }

        // Give the reward to the highest bidder
        if (highestBidder.isOnline()) {
            if (type == AuctionType.ITEM && item != null) {
                // Give the item
                highestBidder.getInventory().addItem(item.clone());

                // Send win message
                MessageUtil.sendMessage(highestBidder, MessageEnum.WIN.get("item", ItemUtils.getItemDisplayName(item)));

                // Save to database
                plugin.getDatabaseManager().saveAuctionResult(
                        highestBidder.getName(),
                        currentBid,
                        type.name(),
                        ItemUtils.getItemDisplayName(item)
                );
            } else if (type == AuctionType.COMMAND && command != null) {
                // Execute the command
                String formattedCommand = command.replace("%player%", highestBidder.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);

                // Get the reward name with player placeholder replaced
                String rewardName = getRewardName().replace("%player%", highestBidder.getName());

                // Send win message
                MessageUtil.sendMessage(highestBidder, MessageEnum.WIN.get("item", rewardName));

                // Save to database
                plugin.getDatabaseManager().saveAuctionResult(
                        highestBidder.getName(),
                        currentBid,
                        type.name(),
                        rewardName
                );
            }
        }

        // Get the reward name with player placeholder replaced
        String rewardName = getRewardName().replace("%player%", highestBidder.getName());

        // Broadcast auction end message
        Bukkit.broadcastMessage(MessageEnum.AUCTION_ENDED.get(
                "player", highestBidder.getName(),
                "amount", plugin.getEconomyHandler().format(currentBid),
                "item", rewardName
        ));
    }

    public void cancel() {
        active = false;

        // Refund the highest bidder
        if (highestBidder != null && highestBidder.isOnline()) {
            plugin.getEconomyHandler().deposit(highestBidder, currentBid);
            MessageUtil.sendMessage(highestBidder, MessageEnum.CANCELLED.get());
        }

        // Return the item to the owner if this is an item auction
        if (type == AuctionType.ITEM && item != null && ownerName != null) {
            // Try to find the owner online
            Player owner = Bukkit.getPlayerExact(ownerName);
            if (owner != null && owner.isOnline()) {
                // Give the item back to the owner
                owner.getInventory().addItem(item.clone());
                MessageUtil.sendMessage(owner, "§aYour auctioned item has been returned to you.");
            } else {
                // Owner is offline, log this situation
                plugin.getLogger().warning("Could not return item to offline owner: " + ownerName);
                // TODO: Consider storing the item in a database or other storage for later retrieval
            }
        }

        // Broadcast cancellation message
        Bukkit.broadcastMessage(MessageEnum.CANCELLED.get());
    }

    /**
     * Place a bid
     * @param player The player placing the bid
     * @param amount The bid amount
     * @return True if the bid was successful
     */
    public boolean placeBid(Player player, double amount) {
        // Check if the auction is active
        if (!active) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return false;
        }

        // Get the minimum bid amount from config
        double minBidAmount = plugin.getConfigManager().getConfig().getDouble("auction.min-bid-amount", 100);

        // Check if the bid meets the minimum amount
        if (amount < minBidAmount) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_AMOUNT.get("amount", plugin.getEconomyHandler().format(minBidAmount)));
            return false;
        }

        // Check if the bid is high enough
        if (amount <= currentBid) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_AMOUNT.get("amount", plugin.getEconomyHandler().format(currentBid + 1)));
            return false;
        }

        // Check if the player has enough money
        if (!plugin.getEconomyHandler().hasEnough(player, amount)) {
            MessageUtil.sendMessage(player, MessageEnum.NOT_ENOUGH_MONEY.get());
            return false;
        }

        // Refund the previous highest bidder
        if (highestBidder != null && highestBidder.isOnline()) {
            plugin.getEconomyHandler().deposit(highestBidder, currentBid);
            MessageUtil.sendMessage(highestBidder, MessageEnum.OUTBID.get("player", player.getName()));
        }

        // Withdraw the bid amount from the player
        plugin.getEconomyHandler().withdraw(player, amount);

        // Update the highest bid
        currentBid = amount;
        highestBidder = player;

        // Get the time extension and threshold from config
        int timeExtension = plugin.getConfigManager().getConfig().getInt("auction.bid-time-extension", 30);
        int timeThreshold = plugin.getConfigManager().getConfig().getInt("auction.bid-time-threshold", 60);

        // Get current remaining time
        int remainingTime = getTimeRemaining();

        // Only add time to the auction if remaining time is below the threshold
        boolean timeExtended = false;
        if (remainingTime < timeThreshold) {
            endTime += timeExtension * 1000L;
            timeExtended = true;
        }

        // Prepare time extension message part
        String timeExtensionText = timeExtended ? 
            MessageEnum.TIME_EXTENSION.get("seconds", String.valueOf(timeExtension)) : 
            "";

        // Broadcast the new bid with conditional time extension info
        Bukkit.broadcastMessage(MessageEnum.NEW_BID.get(
                "player", player.getName(),
                "amount", plugin.getEconomyHandler().format(amount),
                "time_extension", timeExtensionText
        ));

        return true;
    }

    public String getRewardName() {
        // If a custom name is provided, use it
        if (customName != null && !customName.isEmpty()) {
            return MessageUtil.colorize(customName);
        }

        // Otherwise, use the default logic
        if (type == AuctionType.ITEM && item != null) {
            return ItemUtils.getItemDisplayName(item);
        } else if (type == AuctionType.COMMAND && command != null) {
            // Check if the command contains a custom item name with color codes
            // Example: "eco give %player% 1000 &a&l1000_&8$"
            // We want to extract "&a&l1000_&8$" as the item name
            if (command.contains("&")) {
                // Find the last occurrence of a space before a color code
                int lastSpaceBeforeColor = -1;
                for (int i = 0; i < command.length(); i++) {
                    if (command.charAt(i) == ' ' && i + 1 < command.length() && command.charAt(i + 1) == '&') {
                        lastSpaceBeforeColor = i;
                    }
                }

                if (lastSpaceBeforeColor != -1) {
                    // Extract the custom item name with color codes
                    String extractedName = command.substring(lastSpaceBeforeColor + 1);
                    return MessageUtil.colorize(extractedName);
                }
            }

            // If no custom item name with color codes, use the original extraction logic
            // Extract item name from command
            // Example command: "give %player% diamond 5"
            // We want to extract "5 Diamond" as the item name
            String[] parts = command.split(" ");
            if (parts.length >= 3) {
                // Try to extract amount and item name
                String itemName = "";
                String amount = "";

                // Check if the last part is a number (amount)
                try {
                    int lastPartAsInt = Integer.parseInt(parts[parts.length - 1]);
                    amount = parts[parts.length - 1];
                    // Item name is the part before the amount
                    itemName = parts[parts.length - 2];
                } catch (NumberFormatException e) {
                    // Last part is not a number, assume it's part of the item name
                    itemName = parts[parts.length - 1];
                    // Check if second-to-last part is a number
                    if (parts.length >= 4) {
                        try {
                            int secondToLastPartAsInt = Integer.parseInt(parts[parts.length - 2]);
                            amount = parts[parts.length - 2];
                            // Update item name to be the part before the amount
                            itemName = parts[parts.length - 3];
                        } catch (NumberFormatException ex) {
                            // Second-to-last part is not a number, assume it's part of the item name
                            if (parts.length >= 3) {
                                itemName = parts[parts.length - 2] + " " + itemName;
                            }
                        }
                    }
                }

                // Format the item name (capitalize first letter of each word)
                itemName = ItemUtils.formatMaterialName(itemName);

                // Combine amount and item name
                String result;
                if (!amount.isEmpty()) {
                    result = amount + " " + itemName;
                } else {
                    result = itemName;
                }

                // Process color codes in the result
                return MessageUtil.colorize(result);
            }

            // Fallback to the command if we couldn't extract an item name
            return MessageUtil.colorize(command);
        }
        return "Unknown";
    }

    public int getTimeRemaining() {
        if (!active) {
            return 0;
        }

        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }

    public boolean isActive() {
        return active;
    }
    public AuctionType getType() {
        return type;
    }
    public ItemStack getItem() {
        return item;
    }
    public String getCommand() {
        return command;
    }
    public double getStartPrice() {
        return startPrice;
    }
    public double getCurrentBid() {
        return currentBid;
    }
    public Player getHighestBidder() {
        return highestBidder;
    }
    public int getDuration() {
        return duration;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
