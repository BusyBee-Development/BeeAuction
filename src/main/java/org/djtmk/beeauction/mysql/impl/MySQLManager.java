package org.djtmk.beeauction.mysql.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.mysql.AsyncDatabaseManager;
import org.djtmk.beeauction.util.ItemUtils;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MySQLManager implements AsyncDatabaseManager {

    private final BeeAuction plugin;
    private HikariDataSource dataSource;

    public MySQLManager(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("database.host") + ":" + plugin.getConfig().getInt("database.port") + "/" + plugin.getConfig().getString("database.database"));
        config.setUsername(plugin.getConfig().getString("database.username"));
        config.setPassword(plugin.getConfig().getString("database.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        createTables();
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() {
        String historyTable = "CREATE TABLE IF NOT EXISTS auction_history (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "auction_type VARCHAR(16) NOT NULL, " +
                "reward TEXT NOT NULL, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        String rewardsTable = "CREATE TABLE IF NOT EXISTS pending_rewards (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
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
    public CompletableFuture<Void> saveAuctionResult(String playerName, UUID playerUuid, double amount, String auctionType, String reward) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO auction_history(player_name, player_uuid, amount, auction_type, reward) VALUES(?,?,?,?,?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                pstmt.setString(2, playerUuid.toString());
                pstmt.setDouble(3, amount);
                pstmt.setString(4, auctionType);
                pstmt.setString(5, reward);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save auction result", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<org.djtmk.beeauction.data.AuctionHistoryEntry>> getAuctionHistory() {
        return CompletableFuture.supplyAsync(() -> {
            // FIXED: Process ResultSet inside try-with-resources to prevent resource leak
            String sql = "SELECT * FROM auction_history ORDER BY timestamp DESC LIMIT 10";
            List<org.djtmk.beeauction.data.AuctionHistoryEntry> entries = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    org.djtmk.beeauction.data.AuctionHistoryEntry entry = new org.djtmk.beeauction.data.AuctionHistoryEntry(
                            rs.getInt("id"),
                            rs.getString("player_name"),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getDouble("amount"),
                            rs.getString("auction_type"),
                            rs.getString("reward"),
                            rs.getTimestamp("timestamp")
                    );
                    entries.add(entry);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get auction history", e);
            }
            return entries;
        });
    }

    @Override
    public CompletableFuture<Void> addPendingReward(UUID playerUuid, ItemStack item, String reason) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO pending_rewards(player_uuid, item_data, reason) VALUES(?,?,?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                pstmt.setString(2, ItemUtils.serializeItemToBase64(item));
                pstmt.setString(3, reason);
                pstmt.executeUpdate();
            } catch (SQLException | IllegalStateException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add pending reward to database", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ItemStack>> getAndRemovePendingRewards(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSql = "SELECT id, item_data FROM pending_rewards WHERE player_uuid = ? FOR UPDATE";
            String deleteSql = "DELETE FROM pending_rewards WHERE id = ?";
            List<ItemStack> items = new ArrayList<>();
            List<Integer> idsToRemove = new ArrayList<>();

            Connection conn = null;
            try {
                conn = getConnection();
                conn.setAutoCommit(false);

                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
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

                    conn.commit();
                } catch (SQLException e) {
                    if (conn != null) {
                        conn.rollback();
                    }
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get and remove pending rewards", e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to restore connection state", e);
                    }
                }
            }
            return items;
        });
    }

    @Override
    public CompletableFuture<Boolean> hasPendingRewards(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM pending_rewards WHERE player_uuid = ? LIMIT 1";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check for pending rewards", e);
            }
            return false;
        });
    }
}
