package org.djtmk.beeauction.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DatabaseHandler {

    void initialize();

    void shutdown();

    Connection getConnection() throws SQLException;

    void createTables();
    
    /**
     * Save an auction result
     * @param playerName The name of the player who won the auction
     * @param amount The amount the player paid
     * @param auctionType The type of auction (ITEM or COMMAND)
     * @param reward The reward (item or command)
     */
    void saveAuctionResult(String playerName, double amount, String auctionType, String reward);

    ResultSet getAuctionHistory() throws SQLException;
}
