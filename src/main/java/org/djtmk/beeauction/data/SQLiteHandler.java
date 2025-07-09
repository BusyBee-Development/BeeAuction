package org.djtmk.beeauction.data;

import org.djtmk.beeauction.BeeAuction;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLiteHandler implements DatabaseHandler {
    private final BeeAuction plugin;
    private File databaseFile;
    private Connection connection;

    public SQLiteHandler(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // Create data directory if it doesn't exist
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Set database file
        databaseFile = new File(dataFolder, "auctions.db");

        // Create tables
        createTables();
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing SQLite connection", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }

        try {
            // Use Paper/Spigot's built-in SQLite driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    @Override
    public void createTables() {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS auction_history (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "player_name VARCHAR(16), " +
                             "amount DOUBLE, " +
                             "auction_type VARCHAR(16), " +
                             "reward TEXT, " +
                             "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                             ")"
             )) {
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables", e);
        }
    }

    @Override
    public void saveAuctionResult(String playerName, double amount, String auctionType, String reward) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO auction_history (player_name, amount, auction_type, reward) VALUES (?, ?, ?, ?)"
             )) {
            statement.setString(1, playerName);
            statement.setDouble(2, amount);
            statement.setString(3, auctionType);
            statement.setString(4, reward);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save auction result", e);
        }
    }

    @Override
    public ResultSet getAuctionHistory() throws SQLException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM auction_history ORDER BY timestamp DESC LIMIT 10"
        );
        return statement.executeQuery();
    }
}
