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
        if (!auction.isActive()) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return false;
        }

        double minIncrement = plugin.getConfigManager().getConfig().getDouble("auction.min-bid-increment", 1.0);
        double requiredBid = (auction.getHighestBidder() == null) ? auction.getCurrentBid() : auction.getCurrentBid() + minIncrement;

        if (amount < requiredBid) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_AMOUNT.get("amount", plugin.getEconomyManager().getProviderName()));
            return false;
        }

        if (!plugin.getEconomyManager().has(player, amount).join()) {
            MessageUtil.sendMessage(player, MessageEnum.NOT_ENOUGH_MONEY.get());
            return false;
        }

        return auction.placeBid(player, amount);
    }
}
