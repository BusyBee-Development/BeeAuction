package org.djtmk.beeauction.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.api.BeeAuctionAPI;
import org.djtmk.beeauction.auctions.Auction;

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
        // Support both old and new placeholder names for backward compatibility
        if (identifier.equals("active_listings") || identifier.equals("active")) {
            return BeeAuctionAPI.hasActiveAuction() ? "true" : "false";
        }

        Auction auction = BeeAuctionAPI.getActiveAuction();
        if (auction == null) {
            return "";
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
