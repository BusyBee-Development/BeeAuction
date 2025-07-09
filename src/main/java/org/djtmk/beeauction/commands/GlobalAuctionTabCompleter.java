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
    private List<String> COMMANDS;
    private List<String> AUCTION_TYPES;
    private static final List<String> EXAMPLE_TIMES = Arrays.asList("30", "60", "120", "300", "600");
    private static final List<String> EXAMPLE_PRICES = Arrays.asList("100", "500", "1000", "5000");
    private static final List<String> EXAMPLE_COMMANDS = Arrays.asList(
            "give %player% diamond 5",
            "give %player% emerald 10",
            "eco give %player% 1000",
            "crate give %player% rare 1"
    );

    public GlobalAuctionTabCompleter(BeeAuction plugin) {
        this.plugin = plugin;
        // Initialize command lists from config
        updateCommandLists();
    }

    private void updateCommandLists() {
        // Get subcommands from config
        String startCmd = plugin.getConfigManager().getAdminSubcommandStart();
        String cancelCmd = plugin.getConfigManager().getAdminSubcommandCancel();
        String reloadCmd = plugin.getConfigManager().getAdminSubcommandReload();
        COMMANDS = Arrays.asList(startCmd, cancelCmd, reloadCmd);

        // Get auction types from config
        String itemType = plugin.getConfigManager().getAdminAuctionTypeItem();
        String commandType = plugin.getConfigManager().getAdminAuctionTypeCommand();
        AUCTION_TYPES = Arrays.asList(itemType, commandType);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Check permission
        if (!sender.hasPermission("globalauction.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            StringUtil.copyPartialMatches(args[0], COMMANDS, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            // Second argument for "start" - auction type
            StringUtil.copyPartialMatches(args[1], AUCTION_TYPES, completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            // Third argument for "start" - time
            StringUtil.copyPartialMatches(args[2], EXAMPLE_TIMES, completions);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("start")) {
            // Fourth argument for "start" - price
            StringUtil.copyPartialMatches(args[3], EXAMPLE_PRICES, completions);
        } else if (args.length == 5 && args[0].equalsIgnoreCase("start") && args[1].equalsIgnoreCase("command")) {
            // Fifth argument for "start command" - command examples
            StringUtil.copyPartialMatches(args[4], EXAMPLE_COMMANDS, completions);
        }

        Collections.sort(completions);
        return completions;
    }
}
