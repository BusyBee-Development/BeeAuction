package org.djtmk.beeauction.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.api.BeeAuctionAPI;
import org.djtmk.beeauction.auctions.Auction;
import org.djtmk.beeauction.config.ConfigManager;

public class PlaceholderHook extends PlaceholderExpansion {

    private final BeeAuction plugin;

    public PlaceholderHook(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "beeauction";
    }

    @Override
    public String getAuthor() {
        return "djtmk";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        // Get configurable placeholder messages
        String activeTrue = ConfigManager.getMessage("placeholders.active-true");
        String activeFalse = ConfigManager.getMessage("placeholders.active-false");
        String noAuctionItem = ConfigManager.getMessage("placeholders.no-auction-item");
        String noAuctionBid = ConfigManager.getMessage("placeholders.no-auction-bid");
        String noAuctionBidder = ConfigManager.getMessage("placeholders.no-auction-bidder");
        String noAuctionTime = ConfigManager.getMessage("placeholders.no-auction-time");

        // Support both old and new placeholder names for backward compatibility
        if (identifier.equals("active_listings") || identifier.equals("active")) {
            return BeeAuctionAPI.hasActiveAuction() ? activeTrue : activeFalse;
        }

        // Player-specific placeholder: auctions won count
        if (identifier.equals("auctions_won")) {
            if (player == null) {
                return "0";
            }
            return String.valueOf(plugin.getDatabaseManager().getAuctionsWonCount(player.getUniqueId()));
        }

        Auction auction = BeeAuctionAPI.getActiveAuction();
        if (auction == null) {
            // Return configurable fallback messages when no auction is active
            switch (identifier) {
                case "highest_bid":
                case "current_bid":
                    return noAuctionBid;
                case "highest_bidder":
                case "bidder":
                    return noAuctionBidder;
                case "item_name":
                case "item":
                    return noAuctionItem;
                case "time_remaining":
                case "time_left":
                    return noAuctionTime;
                default:
                    return null;
            }
        }

        switch (identifier) {
            case "highest_bid":
            case "current_bid":
                return String.valueOf(auction.getCurrentBid());
            case "highest_bidder":
            case "bidder":
                return auction.getHighestBidder() != null ? auction.getHighestBidder().getName() : "None";
            case "item_name":
            case "item":
                return auction.getRewardName();
            case "time_remaining":
            case "time_left":
                return String.valueOf(auction.getTimeRemaining());
            default:
                return null;
        }
    }
}
