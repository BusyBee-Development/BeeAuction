package org.djtmk.beeauction.data;

import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.util.ItemUtils;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

// UPDATED: Implements new methods for offline reward storage.
public class SQLiteHandler implements DatabaseHandler {
    private final BeeAuction plugin;
    private File databaseFile;
    private Connection connection;

    public SQLiteHandler(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        databaseFile = new File(dataFolder, "auctions.db");
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
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found", e);
            }
        }
        return connection;
    }

    @Override
    public void createTables() {
        String historyTable = "CREATE TABLE IF NOT EXISTS auction_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " + // UPDATED
                "amount DOUBLE NOT NULL, " +
                "auction_type VARCHAR(16) NOT NULL, " +
                "reward TEXT NOT NULL, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        // NEW: Table for storing items for offline players.
        String rewardsTable = "CREATE TABLE IF NOT EXISTS pending_rewards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "item_data TEXT NOT NULL, " +
                "reason TEXT, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(historyTable);
            stmt.execute(rewardsTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
        }
    }

    @Override
    public void saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward) {
        String sql = "INSERT INTO auction_history(player_name, player_uuid, amount, auction_type, reward) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setString(2, playerUuid.toString()); // UPDATED
            pstmt.setDouble(3, amount);
            pstmt.setString(4, auctionType);
            pstmt.setString(5, reward);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save auction result", e);
        }
    }

    @Override
    public ResultSet getAuctionHistory() throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM auction_history ORDER BY timestamp DESC LIMIT 10");
        return stmt.executeQuery();
    }

    @Override
    public void addPendingReward(UUID playerUuid, ItemStack item, String reason) {
        String sql = "INSERT INTO pending_rewards(player_uuid, item_data, reason) VALUES(?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, ItemUtils.serializeItemToBase64(item));
            pstmt.setString(3, reason);
            pstmt.executeUpdate();
        } catch (SQLException | IllegalStateException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not add pending reward to database", e);
        }
    }

    @Override
    public List<ItemStack> getAndRemovePendingRewards(UUID playerUuid) throws SQLException {
        String selectSql = "SELECT id, item_data FROM pending_rewards WHERE player_uuid = ?";
        String deleteSql = "DELETE FROM pending_rewards WHERE id = ?";
        List<ItemStack> items = new ArrayList<>();
        List<Integer> idsToRemove = new ArrayList<>();

        try (Connection conn = getConnection(); PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, playerUuid.toString());
            ResultSet rs = selectStmt.executeQuery();
            while (rs.next()) {
                idsToRemove.add(rs.getInt("id"));
                try {
                    ItemStack item = ItemUtils.deserializeItemFromBase64(rs.getString("item_data"));
                    items.add(item);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not deserialize a pending item reward.", e);
                }
            }
            if (!idsToRemove.isEmpty()) {
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    for (Integer id : idsToRemove) {
                        deleteStmt.setInt(1, id);
                        deleteStmt.addBatch();
                    }
                    deleteStmt.executeBatch();
                }
            }
        }
        return items;
    }

    @Override
    public boolean hasPendingRewards(UUID playerUuid) throws SQLException {
        String sql = "SELECT 1 FROM pending_rewards WHERE player_uuid = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
