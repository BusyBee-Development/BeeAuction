package org.djtmk.beeauction.auctions;

import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;


public class BidManager {

    private final BeeAuction plugin;

    public BidManager(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public boolean placeBid(Auction auction, Player player, double amount) {

        return auction.placeBid(player, amount);
    }
}
