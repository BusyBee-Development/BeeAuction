package org.djtmk.beeauction.auctions;

import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

public class BidManager {

    private final BeeAuction plugin;

    public BidManager(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public boolean placeBid(Auction auction, Player player, double amount) {
        // SECURITY FIX: All validation moved to Auction.placeBid() inside synchronized block
        // This prevents TOCTOU (Time-Of-Check-Time-Of-Use) race conditions
        // The validation here was happening OUTSIDE the lock, allowing race conditions
        return auction.placeBid(player, amount);
    }
}
