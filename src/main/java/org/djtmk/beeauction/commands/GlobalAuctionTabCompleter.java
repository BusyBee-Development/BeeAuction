package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.djtmk.beeauction.BeeAuction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GlobalAuctionTabCompleter implements TabCompleter {
    private final BeeAuction plugin;
    private List<String> subcommands;
    private List<String> auctionTypes;

    public GlobalAuctionTabCompleter(BeeAuction plugin) {
        this.plugin = plugin;
        updateCommandLists();
    }

    public void updateCommandLists() {
        String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
        String cancelCmd = plugin.getConfigManager().getAdminSubcommandCancel();
        String reloadCmd = plugin.getConfigManager().getAdminSubcommandReload();
        this.subcommands = Arrays.asList(startCmd, cancelCmd, reloadCmd);

        String itemType = plugin.getConfigManager().getAdminAuctionTypeItem();
        String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();
        this.auctionTypes = Arrays.asList(itemType, commandType);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("auction.admin")) {
            return Collections.emptyList();
        }

        final List<String> completions = new ArrayList<>();
        final String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
        final String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], subcommands, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase(startCmd)) {
            StringUtil.copyPartialMatches(args[1], auctionTypes, completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase(startCmd)) {
            StringUtil.copyPartialMatches(args[2], Arrays.asList("60", "300", "600"), completions);
        } else if (args.length == 4 && args[0].equalsIgnoreCase(startCmd)) {
            StringUtil.copyPartialMatches(args[3], Arrays.asList("100", "1000", "10000"), completions);
        } else if (args.length >= 5 && args[0].equalsIgnoreCase(startCmd) && args[1].equalsIgnoreCase(commandType)) {
            StringUtil.copyPartialMatches(args[4], Arrays.asList("give %player% diamond 1", "eco give %player% 1000"), completions);
        }

        Collections.sort(completions);
        return completions;
    }
}
