package org.djtmk.beeauction.data;

import java.sql.Timestamp;
import java.util.UUID;

public class AuctionHistoryEntry {
    private final int id;
    private final String playerName;
    private final UUID playerUuid;
    private final double amount;
    private final String auctionType;
    private final String reward;
    private final Timestamp timestamp;

    public AuctionHistoryEntry(int id, String playerName, UUID playerUuid, double amount,
                                String auctionType, String reward, Timestamp timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.amount = amount;
        this.auctionType = auctionType;
        this.reward = reward;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getPlayerName() { return playerName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public double getAmount() { return amount; }
    public String getAuctionType() { return auctionType; }
    public String getReward() { return reward; }
    public Timestamp getTimestamp() { return timestamp; }
}
