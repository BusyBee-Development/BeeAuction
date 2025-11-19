package org.djtmk.beeauction.mysql;

import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AsyncDatabaseManager {

    void initialize();

    void shutdown();

    CompletableFuture<Void> saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward);

    CompletableFuture<ResultSet> getAuctionHistory();

    CompletableFuture<Void> addPendingReward(UUID playerUuid, ItemStack item, String reason);

    CompletableFuture<List<ItemStack>> getAndRemovePendingRewards(UUID playerUuid);

    CompletableFuture<Boolean> hasPendingRewards(UUID playerUuid);
}
