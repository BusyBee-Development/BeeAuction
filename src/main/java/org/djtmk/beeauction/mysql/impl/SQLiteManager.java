package org.djtmk.beeauction.mysql.impl;

import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.data.SQLiteHandler;
import org.djtmk.beeauction.mysql.AsyncDatabaseManager;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SQLiteManager implements AsyncDatabaseManager {

    private final BeeAuction plugin;
    private final SQLiteHandler sqLiteHandler;
    private final ExecutorService sqliteExecutor;

    public SQLiteManager(BeeAuction plugin) {
        this.plugin = plugin;
        this.sqLiteHandler = new SQLiteHandler(plugin);
        this.sqliteExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "BeeAuction-SQLite");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void initialize() {
        sqLiteHandler.initialize();
    }

    @Override
    public void shutdown() {
        sqliteExecutor.shutdown();
        try {
            if (!sqliteExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("SQLite executor did not terminate in time, forcing shutdown");
                sqliteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            plugin.getLogger().log(Level.WARNING, "Interrupted while waiting for SQLite executor shutdown", e);
            sqliteExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        sqLiteHandler.shutdown();
    }

    @Override
    public CompletableFuture<Void> saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward) {
        return CompletableFuture.runAsync(() -> sqLiteHandler.saveAuctionResult(playerName, playerUuid, amount, auctionType, reward), sqliteExecutor);
    }

    @Override
    public CompletableFuture<List<org.djtmk.beeauction.data.AuctionHistoryEntry>> getAuctionHistory() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sqLiteHandler.getAuctionHistory();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get auction history", e);
                return Collections.emptyList();
            }
        }, sqliteExecutor);
    }

    @Override
    public CompletableFuture<Void> addPendingReward(UUID playerUuid, ItemStack item, String reason) {
        return CompletableFuture.runAsync(() -> sqLiteHandler.addPendingReward(playerUuid, item, reason), sqliteExecutor);
    }

    @Override
    public CompletableFuture<List<ItemStack>> getAndRemovePendingRewards(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sqLiteHandler.getAndRemovePendingRewards(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get and remove pending rewards", e);
                return Collections.emptyList();
            }
        }, sqliteExecutor);
    }

    @Override
    public CompletableFuture<Boolean> hasPendingRewards(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sqLiteHandler.hasPendingRewards(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check for pending rewards", e);
                return false;
            }
        }, sqliteExecutor);
    }

    @Override
    public CompletableFuture<Integer> getAuctionsWonCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sqLiteHandler.getAuctionsWonCount(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get auctions won count", e);
                return 0;
            }
        }, sqliteExecutor);
    }
}
