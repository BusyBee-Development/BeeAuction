package org.djtmk.beeauction.data;

import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface DatabaseHandler {

    void initialize();
    void shutdown();
    Connection getConnection() throws SQLException;

    void createTables();
    void saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward);

    List<AuctionHistoryEntry> getAuctionHistory() throws SQLException;
    void addPendingReward(UUID playerUuid, ItemStack item, String reason);

    List<ItemStack> getAndRemovePendingRewards(UUID playerUuid) throws SQLException;

    boolean hasPendingRewards(UUID playerUuid) throws SQLException;
}
