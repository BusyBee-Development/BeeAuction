package org.djtmk.beeauction.data;

import org.djtmk.beeauction.BeeAuction;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

// UPDATED: Wraps the new database methods.
public class DatabaseManager {
    private final BeeAuction plugin;
    private DatabaseHandler databaseHandler;

    public DatabaseManager(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        databaseHandler = new SQLiteHandler(plugin);
        databaseHandler.initialize();
        plugin.getLogger().info("Database initialized successfully.");
    }

    public void shutdown() {
        if (databaseHandler != null) {
            databaseHandler.shutdown();
        }
    }

    public void saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward) {
        if (databaseHandler != null) {
            databaseHandler.saveAuctionResult(playerName, playerUuid, amount, auctionType, reward);
        }
    }

    public ResultSet getAuctionHistory() {
        if (databaseHandler != null) {
            try {
                return databaseHandler.getAuctionHistory();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get auction history", e);
            }
        }
        return null;
    }

    // NEW wrapper methods for pending rewards
    public void addPendingReward(UUID playerUuid, ItemStack item, String reason) {
        if (databaseHandler != null) {
            databaseHandler.addPendingReward(playerUuid, item, reason);
        }
    }

    public List<ItemStack> getAndRemovePendingRewards(UUID playerUuid) {
        if (databaseHandler != null) {
            try {
                return databaseHandler.getAndRemovePendingRewards(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get and remove pending rewards", e);
            }
        }
        return Collections.emptyList();
    }

    public boolean hasPendingRewards(UUID playerUuid) {
        if (databaseHandler != null) {
            try {
                return databaseHandler.hasPendingRewards(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check for pending rewards", e);
            }
        }
        return false;
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }
}
