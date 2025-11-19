package org.djtmk.beeauction.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.djtmk.beeauction.auctions.Auction;

public class AuctionBidEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Auction auction;
    private final Player player;
    private final double bidAmount;
    private boolean isCancelled;

    public AuctionBidEvent(Auction auction, Player player, double bidAmount) {
        this.auction = auction;
        this.player = player;
        this.bidAmount = bidAmount;
    }

    public Auction getAuction() {
        return auction;
    }

    public Player getPlayer() {
        return player;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
