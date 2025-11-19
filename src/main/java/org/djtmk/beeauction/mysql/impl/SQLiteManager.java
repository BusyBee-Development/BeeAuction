package org.djtmk.beeauction.mysql.impl;

import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.data.SQLiteHandler;
import org.djtmk.beeauction.mysql.AsyncDatabaseManager;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SQLiteManager implements AsyncDatabaseManager {

    private final BeeAuction plugin;
    private final SQLiteHandler sqLiteHandler;

    public SQLiteManager(BeeAuction plugin) {
        this.plugin = plugin;
        this.sqLiteHandler = new SQLiteHandler(plugin);
    }

    @Override
    public void initialize() {
        sqLiteHandler.initialize();
    }

    @Override
    public void shutdown() {
        sqLiteHandler.shutdown();
    }

    @Override
    public CompletableFuture<Void> saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward) {
        return CompletableFuture.runAsync(() -> sqLiteHandler.saveAuctionResult(playerName, playerUuid, amount, auctionType, reward));
    }

    @Override
    public CompletableFuture<ResultSet> getAuctionHistory() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sqLiteHandler.getAuctionHistory();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get auction history", e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> addPendingReward(UUID playerUuid, ItemStack item, String reason) {
        return CompletableFuture.runAsync(() -> sqLiteHandler.addPendingReward(playerUuid, item, reason));
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
        });
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
        });
    }
}
