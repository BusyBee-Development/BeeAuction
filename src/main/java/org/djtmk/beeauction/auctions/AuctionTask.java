package org.djtmk.beeauction.auctions;

import org.bukkit.boss.BarColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.util.BossBarUtil;

public class AuctionTask extends BukkitRunnable {
    private final BeeAuction plugin;
    private final Auction auction;
    private final AuctionManager auctionManager;

    /**
     * Constructor
     * @param plugin The plugin instance
     * @param auction The auction to handle
     * @param auctionManager The auction manager
     */
    public AuctionTask(BeeAuction plugin, Auction auction, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auction = auction;
        this.auctionManager = auctionManager;

        // Create the boss bar
        BossBarUtil.createAuctionBar(
                getBarTitle(),
                BarColor.GREEN
        );
    }

    @Override
    public void run() {
        // Check if the auction is still active
        if (!auction.isActive()) {
            cancel();
            BossBarUtil.removeAuctionBar();
            return;
        }

        // Check if the auction has ended
        int timeRemaining = auction.getTimeRemaining();
        if (timeRemaining <= 0) {
            // End the auction
            auction.end();

            // Remove the boss bar
            BossBarUtil.removeAuctionBar();

            // Cancel the task
            cancel();

            // Clear the active auction
            auctionManager.clearActiveAuction();

            return;
        }

        // Update the boss bar
        updateBossBar(timeRemaining);
    }

    private void updateBossBar(int timeRemaining) {
        // Calculate progress (0.0 - 1.0)
        double progress = (double) timeRemaining / auction.getDuration();

        // Update the boss bar
        BossBarUtil.updateAuctionBar(getBarTitle(), progress);

        // Update the color based on time remaining
        if (timeRemaining <= 5) {
            BossBarUtil.getAuctionBar().setColor(BarColor.RED);
        } else if (timeRemaining <= 15) {
            BossBarUtil.getAuctionBar().setColor(BarColor.YELLOW);
        }
    }

    private String getBarTitle() {
        StringBuilder title = new StringBuilder();

        // Add the item/command name
        String rewardName = auction.getRewardName();

        // Replace %player% with the highest bidder's name if available
        if (auction.getHighestBidder() != null && rewardName.contains("%player%")) {
            rewardName = rewardName.replace("%player%", auction.getHighestBidder().getName());
        } else {
            // If no highest bidder yet, replace with a generic placeholder
            rewardName = rewardName.replace("%player%", "Winner");
        }

        title.append("&6").append(rewardName);

        // Add the current bid and bidder
        title.append(" &7| &eCurrent Bid: &a").append(plugin.getEconomyHandler().format(auction.getCurrentBid()));

        if (auction.getHighestBidder() != null) {
            title.append(" &7| &eBidder: &a").append(auction.getHighestBidder().getName());
        }

        // Add the time remaining
        int timeRemaining = auction.getTimeRemaining();
        title.append(" &7| &eTime: &a").append(formatTime(timeRemaining));

        return title.toString();
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        return minutes + "m " + remainingSeconds + "s";
    }

    public void cancelTask() {
        cancel();
        BossBarUtil.removeAuctionBar();
    }
}
