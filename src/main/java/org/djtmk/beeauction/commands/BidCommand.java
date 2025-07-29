package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.auctions.Auction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

public class BidCommand implements CommandExecutor {
    private final BeeAuction plugin;

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

        // Prevent owner from bidding on their own auction
        if (activeAuction.getOwnerUuid() != null && activeAuction.getOwnerUuid().equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "§cYou cannot bid on your own auction.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            // The placeBid method will handle sending the correct minimum bid message.
            // We just need to trigger it with an invalid amount.
            plugin.getAuctionManager().placeBid(player, -1);
            return true;
        }

        // The placeBid method now contains all validation logic (min amount, enough money, etc.)
        plugin.getAuctionManager().placeBid(player, amount);

        return true;
    }
}
