package org.djtmk.beeauction.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.djtmk.beeauction.auctions.Auction;

public class AuctionStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Auction auction;

    public AuctionStartEvent(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
