package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

public class GlobalAuctionCommand implements CommandExecutor {
    private final BeeAuction plugin;
    private final org.djtmk.beeauction.listeners.AuctionChatListener chatListener;

    public GlobalAuctionCommand(BeeAuction plugin) {
        this.plugin = plugin;
        this.chatListener = new org.djtmk.beeauction.listeners.AuctionChatListener(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("auction.admin")) {
            MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, MessageEnum.NO_PERMISSION.get());
            return true;
        }

        // Check if there are any arguments
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // Get custom subcommands from config
        String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
        String cancelCmd = plugin.getConfigManager().getAdminSubcommandCancel();
        String reloadCmd = plugin.getConfigManager().getAdminSubcommandReload();

        // Handle subcommands
        String subCmd = args[0].toLowerCase();
        if (subCmd.equals(startCmd)) {
            handleStart(sender, args);
        } else if (subCmd.equals(cancelCmd)) {
            handleCancel(sender);
        } else if (subCmd.equals(reloadCmd)) {
            handleReload(sender);
        } else {
            sendUsage(sender);
        }

        return true;
    }

    private void handleStart(CommandSender sender, String[] args) {
        // Check if there's already an active auction
        if (plugin.getAuctionManager().hasActiveAuction()) {
            MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, MessageEnum.AUCTION_ALREADY_ACTIVE.get());
            return;
        }

        // Check if there are enough arguments
        if (args.length < 4) {
            sendUsage(sender);
            return;
        }

        // Get custom auction types from config
        String itemType = plugin.getConfigManager().getAdminAuctionTypeItem();
        String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();

        // Get the auction type
        String type = args[1].toLowerCase();

        // Get the duration
        int duration;
        try {
            duration = Integer.parseInt(args[2]);
            if (duration <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, MessageEnum.INVALID_COMMAND.get());
            return;
        }

        // Get the start price
        double startPrice;
        try {
            startPrice = Double.parseDouble(args[3]);
            if (startPrice <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, MessageEnum.INVALID_COMMAND.get());
            return;
        }

        // Handle item auction
        if (type.equals(itemType)) {
            // Check if the sender is a player
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(null, "§cOnly players can start item auctions");
                return;
            }

            // Get the item in hand
            Player player = (Player) sender;
            ItemStack item = player.getInventory().getItemInMainHand();

            // Check if the item is valid
            if (item == null || item.getType().isAir()) {
                MessageUtil.sendMessage(player, "§cYou must hold an item to auction");
                return;
            }

            // Ask for auction name
            MessageUtil.sendMessage(player, MessageEnum.AUCTION_NAME_QUESTION.get());
            chatListener.addPendingItemAuction(player, item.clone(), duration, startPrice);

            // The auction will be started by the chat listener when the player enters a name
            // We'll remove the item from the player's hand now
            item.setAmount(0);
            player.getInventory().setItemInMainHand(item);
        }
        // Handle command auction
        else if (type.equals(commandType)) {
            // Check if there are enough arguments
            if (args.length < 5) {
                sendUsage(sender);
                return;
            }

            // Check if the sender is a player
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(null, "§cOnly players can start command auctions");
                return;
            }

            Player player = (Player) sender;

            // Get the command
            StringBuilder commandBuilder = new StringBuilder();
            for (int i = 4; i < args.length; i++) {
                commandBuilder.append(args[i]).append(" ");
            }
            String auctionCommand = commandBuilder.toString().trim();

            // Ask for auction name
            MessageUtil.sendMessage(player, MessageEnum.AUCTION_NAME_QUESTION.get());
            chatListener.addPendingCommandAuction(player, auctionCommand, duration, startPrice);

            // The auction will be started by the chat listener when the player enters a name
        }
        // Invalid auction type
        else {
            sendUsage(sender);
        }
    }

    private void handleCancel(CommandSender sender) {
        // Check if there's an active auction
        if (!plugin.getAuctionManager().hasActiveAuction()) {
            MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, MessageEnum.NO_AUCTION.get());
            return;
        }

        // Cancel the auction
        plugin.getAuctionManager().cancelAuction();

        // Send success message
        MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, "§aAuction cancelled");
    }

    private void handleReload(CommandSender sender) {
        // Reload the config
        plugin.getConfigManager().reloadConfigs();

        // Reschedule auto auctions
        if (plugin.getConfigManager().isScheduleEnabled()) {
            plugin.getAuctionManager().scheduleAutoAuctions();
            plugin.getLogger().info("Rescheduled auto auctions after config reload");
        }

        // Send success message
        MessageUtil.sendMessage(sender instanceof Player ? (Player) sender : null, MessageEnum.RELOAD_SUCCESS.get());
    }

    private void sendUsage(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Get custom commands from config
            String adminCmd = plugin.getConfigManager().getAdminCommandName();
            String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
            String cancelCmd = plugin.getConfigManager().getAdminSubcommandCancel();
            String reloadCmd = plugin.getConfigManager().getAdminSubcommandReload();
            String itemType = plugin.getConfigManager().getAdminAuctionTypeItem();
            String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();

            MessageUtil.sendMessage(player, "§6GlobalAuction Commands:");
            MessageUtil.sendMessage(player, "§e/" + adminCmd + " " + startCmd + " " + itemType + " <time> <start_price> §7- Start an item auction with the item in your hand");
            MessageUtil.sendMessage(player, "§e/" + adminCmd + " " + startCmd + " " + commandType + " <time> <start_price> <command> §7- Start a command auction");
            MessageUtil.sendMessage(player, "§e/" + adminCmd + " " + cancelCmd + " §7- Cancel the active auction");
            MessageUtil.sendMessage(player, "§e/" + adminCmd + " " + reloadCmd + " §7- Reload the config");
            MessageUtil.sendMessage(player, "§7Note: You will be asked to provide a name for the auction after starting it.");
            MessageUtil.sendMessage(player, "");
        }
    }
}
