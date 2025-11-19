package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.auctions.AuctionCreationManager;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.MessageUtil;

public class GlobalAuctionCommand implements CommandExecutor {
    private final BeeAuction plugin;
    private final AuctionCreationManager creationManager;

    public GlobalAuctionCommand(BeeAuction plugin, AuctionCreationManager creationManager) {
        this.plugin = plugin;
        this.creationManager = creationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auction.admin")) {
            String noPermMsg = MessageEnum.NO_PERMISSION.get();
            if (sender instanceof Player) {
                MessageUtil.sendMessage((Player) sender, noPermMsg);
            } else {
                sender.sendMessage(MessageUtil.colorize(noPermMsg));
            }
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
        String cancelCmd = plugin.getConfigManager().getAdminSubcommandCancel();
        String reloadCmd = plugin.getConfigManager().getAdminSubcommandReload();
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can start auctions from in-game.");
            return;
        }
        Player player = (Player) sender;

        if (!creationManager.canCreateAuction(player)) {
            player.sendMessage("§cYou must wait before creating another auction.");
            return;
        }

        if (plugin.getAuctionManager().hasActiveAuction()) {
            MessageUtil.sendMessage(player, MessageEnum.AUCTION_ALREADY_ACTIVE.get());
            return;
        }

        if (args.length < 4) {
            sendUsage(player);
            return;
        }

        String itemType = plugin.getConfigManager().getAdminAuctionTypeItem();
        String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();
        String type = args[1].toLowerCase();

        int duration;
        double startPrice;
        try {
            duration = Integer.parseInt(args[2]);
            startPrice = Double.parseDouble(args[3]);
            if (duration <= 0 || startPrice < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, MessageEnum.INVALID_COMMAND.get());
            return;
        }

        if (type.equals(itemType)) {
            handleItemAuctionStart(player, duration, startPrice);
        } else if (type.equals(commandType)) {
            handleCommandAuctionStart(player, args, duration, startPrice);
        } else {
            sendUsage(player);
        }
    }

    private void handleItemAuctionStart(Player player, int duration, double startPrice) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            MessageUtil.sendMessage(player, "§cYou must hold an item in your hand to auction it.");
            return;
        }

        player.getInventory().setItemInMainHand(null);
        creationManager.startItemAuctionCreation(player, item, duration, startPrice);
    }

    private void handleCommandAuctionStart(Player player, String[] args, int duration, double startPrice) {
        if (args.length < 5) {
            sendUsage(player);
            return;
        }

        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            commandBuilder.append(args[i]).append(" ");
        }
        String auctionCommand = commandBuilder.toString().trim();

        creationManager.startCommandAuctionCreation(player, auctionCommand, duration, startPrice);
    }

    private void handleCancel(CommandSender sender) {
        if (!plugin.getAuctionManager().cancelAuction()) {
            sender.sendMessage("§c" + MessageEnum.NO_AUCTION.get());
        } else {
            sender.sendMessage("§aAuction successfully cancelled.");
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getAuctionManager().scheduleAutoAuctions();
        sender.sendMessage("§a" + MessageEnum.RELOAD_SUCCESS.get());
    }

    private void sendUsage(CommandSender sender) {
        String adminCmd = plugin.getConfigManager().getAdminCommandName();
        String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
        String cancelCmd = plugin.getConfigManager().getAdminSubcommandCancel();
        String reloadCmd = plugin.getConfigManager().getAdminSubcommandReload();
        String itemType = plugin.getConfigManager().getAdminAuctionTypeItem();
        String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();

        sender.sendMessage(MessageUtil.colorize("&6&lGlobalAuction Commands:"));
        sender.sendMessage(MessageUtil.colorize(" &e/" + adminCmd + " " + startCmd + " " + itemType + " <time> <start_price>"));
        sender.sendMessage(MessageUtil.colorize("   &7- Starts an auction for the item in your hand."));
        sender.sendMessage(MessageUtil.colorize(" &e/" + adminCmd + " " + startCmd + " " + commandType + " <time> <start_price> <command>"));
        sender.sendMessage(MessageUtil.colorize("   &7- Starts an auction for a command."));
        sender.sendMessage(MessageUtil.colorize(" &e/" + adminCmd + " " + cancelCmd));
        sender.sendMessage(MessageUtil.colorize("   &7- Cancels the active auction."));
        sender.sendMessage(MessageUtil.colorize(" &e/" + adminCmd + " " + reloadCmd));
        sender.sendMessage(MessageUtil.colorize("   &7- Reloads the plugin's configuration files."));
    }
}
