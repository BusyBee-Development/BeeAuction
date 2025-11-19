package org.djtmk.beeauction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.MessageEnum;

import java.util.List;
import java.util.Map;

public class ClaimCommand implements CommandExecutor {
    private final BeeAuction plugin;

    public ClaimCommand(BeeAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getDatabaseManager().getAndRemovePendingRewards(player.getUniqueId()).thenAccept(rewards -> {
            if (rewards.isEmpty()) {
                player.sendMessage(MessageEnum.CLAIM_FAIL.get());
                return;
            }

            player.sendMessage(MessageEnum.CLAIM_SUCCESS.get("count", String.valueOf(rewards.size())));
            Map<Integer, ItemStack> couldNotFit = player.getInventory().addItem(rewards.toArray(new ItemStack[0]));

            if (!couldNotFit.isEmpty()) {
                player.sendMessage(MessageEnum.CLAIM_INVENTORY_FULL.get());
                for (ItemStack leftover : couldNotFit.values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
        });
        return true;
    }
}