package org.djtmk.beeauction.util;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;

public class BossBarUtil {
    private static BossBar auctionBar;
    private static final BeeAuction plugin = BeeAuction.getInstance();

    /**
     * Create a boss bar for an auction
     * @param title The title of the boss bar
     * @param color The color of the boss bar
     * @return The created boss bar
     */
    public static BossBar createAuctionBar(String title, BarColor color) {
        // Remove existing boss bar if it exists
        if (auctionBar != null) {
            auctionBar.removeAll();
        }

        // Create new boss bar
        auctionBar = Bukkit.createBossBar(
                MessageUtil.colorize(title),
                color,
                BarStyle.SOLID
        );

        // Add all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            auctionBar.addPlayer(player);
        }

        return auctionBar;
    }

    public static void updateAuctionBar(String title, double progress) {
        if (auctionBar == null) {
            return;
        }

        auctionBar.setTitle(MessageUtil.colorize(title));
        auctionBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

        // Add any new players that joined
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!auctionBar.getPlayers().contains(player)) {
                auctionBar.addPlayer(player);
            }
        }
    }

    public static void removeAuctionBar() {
        if (auctionBar != null) {
            auctionBar.removeAll();
            auctionBar = null;
        }
    }

    public static void addPlayer(Player player) {
        if (auctionBar != null && player != null) {
            auctionBar.addPlayer(player);
        }
    }

    public static void removePlayer(Player player) {
        if (auctionBar != null && player != null) {
            auctionBar.removePlayer(player);
        }
    }

    public static BossBar getAuctionBar() {
        return auctionBar;
    }
}
