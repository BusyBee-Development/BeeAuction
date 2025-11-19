package org.djtmk.beeauction.auctions;

import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.mysql.AsyncDatabaseManager;

import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuctionHistoryManager {

    private final AsyncDatabaseManager dbManager;

    public AuctionHistoryManager(BeeAuction plugin) {
        this.dbManager = plugin.getDatabaseManager();
    }

    public CompletableFuture<Void> logAuction(String playerName, UUID playerUuid, double amount, String auctionType, String reward) {
        return dbManager.saveAuctionResult(playerName, playerUuid, amount, auctionType, reward);
    }

    public CompletableFuture<ResultSet> getHistory() {
        return dbManager.getAuctionHistory();
    }
}
