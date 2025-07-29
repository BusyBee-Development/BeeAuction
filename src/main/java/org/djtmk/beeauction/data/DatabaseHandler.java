package org.djtmk.beeauction.data;

import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

// UPDATED: Interface now includes methods for handling pending rewards for offline players.
public interface DatabaseHandler {

    void initialize();

    void shutdown();

    Connection getConnection() throws SQLException;

    void createTables();

    // UPDATED: Now uses UUID for better player tracking.
    void saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward);

    ResultSet getAuctionHistory() throws SQLException;

    // NEW: Add a pending reward (item) for an offline player.
    void addPendingReward(UUID playerUuid, ItemStack item, String reason);

    // NEW: Get and remove all pending rewards for a player when they claim them.
    List<ItemStack> getAndRemovePendingRewards(UUID playerUuid) throws SQLException;

    // NEW: Check if a player has pending rewards to claim (for join notification).
    boolean hasPendingRewards(UUID playerUuid) throws SQLException;
}
