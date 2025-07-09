package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

public class BidCommand implements CommandExecutor {
    private final BeeAuction plugin;

    public BidCommand(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("auction.bid")) {
            MessageUtil.sendMessage(player, MessageEnum.NO_PERMISSION.get());
            return true;
        }

        // Get custom bid command from config
        String bidCommand = plugin.getConfigManager().getPlayerBidCommand();

        // Check if there are any arguments
        if (args.length == 0) {
            MessageUtil.sendMessage(player, "§cUsage: /" + bidCommand + " <amount>");
            return true;
        }

        // Check if there's an active auction
        if (!plugin.getAuctionManager().hasActiveAuction()) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return true;
        }

        // Get the bid amount
        double amount;
        try {
            amount = Double.parseDouble(args[0]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_AMOUNT.get(
                    "amount", plugin.getEconomyHandler().format(plugin.getAuctionManager().getActiveAuction().getCurrentBid() + 1)
            ));
            return true;
        }

        // Place the bid
        plugin.getAuctionManager().placeBid(player, amount);

        return true;
    }
}