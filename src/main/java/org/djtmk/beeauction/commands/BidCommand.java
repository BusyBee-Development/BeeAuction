package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.auctions.Auction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.InputSanitizer;
import org.djtmk.beeauction.util.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BidCommand implements CommandExecutor {
    private final BeeAuction plugin;

    private final Map<UUID, Long> lastBidTime = new ConcurrentHashMap<>();
    private static final long BID_COOLDOWN_MS = 500; // 500ms between bids
    private static final double DEFAULT_MAX_BID = 1_000_000_000.0; // 1 billion

    public BidCommand(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("auction.bid")) {
            MessageUtil.sendMessage(player, MessageEnum.NO_PERMISSION.get());
            return true;
        }

        Auction activeAuction = plugin.getAuctionManager().getActiveAuction();
        if (activeAuction == null || !activeAuction.isActive()) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return true;
        }

        if (args.length == 0) {
            String bidCommand = plugin.getConfigManager().getPlayerBidCommand();
            MessageUtil.sendMessage(player, "§cUsage: /" + bidCommand + " <amount>");
            return true;
        }

        if (activeAuction.getOwnerUuid() != null && activeAuction.getOwnerUuid().equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "§cYou cannot bid on your own auction.");
            return true;
        }

        UUID playerUuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastBid = lastBidTime.get(playerUuid);

        if (lastBid != null && (currentTime - lastBid) < BID_COOLDOWN_MS) {
            MessageUtil.sendMessage(player, "§cPlease wait before bidding again.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            plugin.getBidManager().placeBid(activeAuction, player, -1);
            return true;
        }

        double maxBid = plugin.getConfigManager().getConfig().getDouble("auction.max-bid-amount", DEFAULT_MAX_BID);
        if (!InputSanitizer.isValidAmount(amount, 0.01, maxBid)) {
            MessageUtil.sendMessage(player, "§cInvalid bid amount. Maximum bid is " + maxBid);
            return true;
        }

        lastBidTime.put(playerUuid, currentTime);

        plugin.getBidManager().placeBid(activeAuction, player, amount);

        return true;
    }
}
