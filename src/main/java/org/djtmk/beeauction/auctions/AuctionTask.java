package org.djtmk.beeauction.auctions;

import org.bukkit.boss.BarColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.util.BossBarUtil;

public class AuctionTask extends BukkitRunnable {
    private final BeeAuction plugin;
    private final Auction auction;
    private final AuctionManager auctionManager;

    public AuctionTask(BeeAuction plugin, Auction auction, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auction = auction;
        this.auctionManager = auctionManager;

        BossBarUtil.createAuctionBar(
                getBarTitle(),
                BarColor.GREEN
        );
    }

    @Override
    public void run() {
        if (!auction.isActive()) {
            this.cancelTask();
            return;
        }

        int timeRemaining = auction.getTimeRemaining();
        if (timeRemaining <= 0) {
            auction.end();
            auctionManager.clearActiveAuction();
            this.cancelTask();
            return;
        }

        updateBossBar(timeRemaining);
    }

    private void updateBossBar(int timeRemaining) {
        double progress = (double) timeRemaining / auction.getDuration();
        BossBarUtil.updateAuctionBar(getBarTitle(), progress);

        if (timeRemaining <= 10) {
            BossBarUtil.getAuctionBar().setColor(BarColor.RED);
        } else if (timeRemaining <= 30) {
            BossBarUtil.getAuctionBar().setColor(BarColor.YELLOW);
        } else {
            BossBarUtil.getAuctionBar().setColor(BarColor.GREEN);
        }
    }

    private String getBarTitle() {
        StringBuilder title = new StringBuilder();
        String rewardName = auction.getRewardName();

        if (auction.getHighestBidder() != null && rewardName.contains("%player%")) {
            rewardName = rewardName.replace("%player%", auction.getHighestBidder().getName());
        } else {
            rewardName = rewardName.replace("%player%", "Winner");
        }

        title.append("§eAuction: §f").append(rewardName);
        title.append(" §7| §eBid: §a").append(plugin.getEconomyHandler().format(auction.getCurrentBid()));

        if (auction.getHighestBidder() != null) {
            title.append(" §7| §eBy: §f").append(auction.getHighestBidder().getName());
        }

        title.append(" §7| §eTime: §f").append(formatTime(auction.getTimeRemaining()));

        return title.toString();
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%dm %ds", minutes, remainingSeconds);
    }

    public void cancelTask() {
        cancel();
        BossBarUtil.removeAuctionBar();
    }
}