package org.djtmk.beeauction.data;

import org.djtmk.beeauction.BeeAuction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {
    private final BeeAuction plugin;
    private DatabaseHandler databaseHandler;

    public DatabaseManager(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Only use SQLite
        databaseHandler = new SQLiteHandler(plugin);

        // Initialize the database
        try {
            databaseHandler.initialize();
            plugin.getLogger().info("Database initialized with SQLite handler");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    public void shutdown() {
        if (databaseHandler != null) {
            databaseHandler.shutdown();
        }
    }

    /**
     * Save an auction result
     * @param playerName The name of the player who won the auction
     * @param amount The amount the player paid
     * @param auctionType The type of auction (ITEM or COMMAND)
     * @param reward The reward (item or command)
     */
    public void saveAuctionResult(String playerName, double amount, String auctionType, String reward) {
        if (databaseHandler != null) {
            databaseHandler.saveAuctionResult(playerName, amount, auctionType, reward);
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

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }
}
