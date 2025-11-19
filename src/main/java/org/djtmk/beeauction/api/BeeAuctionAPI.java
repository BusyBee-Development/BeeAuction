package org.djtmk.beeauction.api;

import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.auctions.Auction;

import java.util.UUID;

public class BeeAuctionAPI {

    private static BeeAuction plugin;

    public static void setPlugin(BeeAuction plugin) {
        BeeAuctionAPI.plugin = plugin;
    }

    public static Auction getActiveAuction() {
        return plugin.getAuctionManager().getActiveAuction();
    }

    public static Player getHighestBidder() {
        Auction auction = getActiveAuction();
        return auction != null ? auction.getHighestBidder() : null;
    }

    public static double getHighestBid() {
        Auction auction = getActiveAuction();
        return auction != null ? auction.getCurrentBid() : 0;
    }

    public static boolean hasActiveAuction() {
        return plugin.getAuctionManager().hasActiveAuction();
    }
}
