package org.djtmk.beeauction.auctions;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.AuctionEnum.AuctionType;
import org.djtmk.beeauction.config.AuctionEnum.Day;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.util.ItemUtils;
import org.djtmk.beeauction.util.MessageUtil;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

// UPDATED: This class now uses the new Auction constructors.
public class AuctionManager {
    private final BeeAuction plugin;
    private Auction activeAuction;
    private AuctionTask auctionTask;
    private BukkitTask scheduledTask;
    private final Map<String, Boolean> startedAuctions = new HashMap<>();
    private DayOfWeek lastCheckedDay;

    public AuctionManager(BeeAuction plugin) {
        this.plugin = plugin;
        this.lastCheckedDay = LocalDateTime.now().getDayOfWeek();

        if (plugin.getConfigManager().isScheduleEnabled()) {
            scheduleAutoAuctions();
        }
    }

    // UPDATED: Now takes a Player object for owner information.
    public boolean startItemAuction(ItemStack item, int duration, double startPrice, String customName, Player owner) {
        if (hasActiveAuction()) {
            return false;
        }
        activeAuction = new Auction(plugin, item, startPrice, duration, customName, owner);
        startAuction(activeAuction);
        return true;
    }

    // UPDATED: Now takes a display name for the command reward.
    public boolean startCommandAuction(String command, String displayName, int duration, double startPrice, String customName, String ownerName) {
        if (hasActiveAuction()) {
            return false;
        }
        activeAuction = new Auction(plugin, command, displayName, startPrice, duration, customName, ownerName);
        startAuction(activeAuction);
        return true;
    }

    // NEW: Central method to start any auction.
    private void startAuction(Auction auction) {
        auction.start();
        auctionTask = new AuctionTask(plugin, auction, this);
        auctionTask.runTaskTimer(plugin, 0, 20);
    }

    public boolean cancelAuction() {
        if (!hasActiveAuction()) {
            return false;
        }

        activeAuction.cancel();
        if (auctionTask != null) {
            auctionTask.cancelTask();
            auctionTask = null;
        }
        activeAuction = null;
        return true;
    }

    public boolean placeBid(Player player, double amount) {
        if (!hasActiveAuction()) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return false;
        }
        return activeAuction.placeBid(player, amount);
    }

    public void clearActiveAuction() {
        activeAuction = null;
        auctionTask = null;
    }

    public boolean hasActiveAuction() {
        return activeAuction != null && activeAuction.isActive();
    }

    public Auction getActiveAuction() {
        return activeAuction;
    }

    public void scheduleAutoAuctions() {
        plugin.getLogger().info("Scheduling auto auctions...");
        if (!plugin.getConfigManager().isScheduleEnabled()) {
            plugin.getLogger().info("Scheduled auctions are disabled in config.yml");
            if (scheduledTask != null) {
                scheduledTask.cancel();
                scheduledTask = null;
            }
            return;
        }
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
        startedAuctions.clear();
        lastCheckedDay = LocalDateTime.now().getDayOfWeek();
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkScheduledAuctions, 20 * 10, 20 * 10);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkScheduledAuctions);
    }

    private void checkScheduledAuctions() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        if (lastCheckedDay != currentDay) {
            startedAuctions.clear();
            lastCheckedDay = currentDay;
        }

        if (hasActiveAuction()) return; // Don't start a scheduled auction if one is already running

        ConfigurationSection scheduleSection = plugin.getConfigManager().getConfig().getConfigurationSection("schedule");
        if (scheduleSection == null) return;

        List<Map<?, ?>> auctionsList = scheduleSection.getMapList("auctions");
        if (auctionsList.isEmpty()) return;

        for (Map<?, ?> auctionMap : auctionsList) {
            try {
                DayOfWeek scheduledDay = DayOfWeek.valueOf(((String) auctionMap.get("day")).toUpperCase());
                if (scheduledDay != currentDay) continue;

                String timeString = String.valueOf(auctionMap.get("time"));
                LocalTime scheduledTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));

                String auctionKey = scheduledDay + "_" + timeString;
                if (startedAuctions.getOrDefault(auctionKey, false)) continue;

                long timeDifference = currentTime.toSecondOfDay() - scheduledTime.toSecondOfDay();
                if (timeDifference < 0 || timeDifference > 30) continue; // Start within 30s of scheduled time

                // It's time to start this auction
                Bukkit.getScheduler().runTask(plugin, () -> {
                    startScheduledAuction(auctionMap);
                });
                startedAuctions.put(auctionKey, true);
                break;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Invalid scheduled auction configuration", e);
            }
        }
    }

    private void startScheduledAuction(Map<?, ?> auctionMap) {
        if (hasActiveAuction()) return; // Double-check on main thread

        Map<?, ?> rewardMap = (Map<?, ?>) auctionMap.get("reward");
        AuctionType type = AuctionType.valueOf(((String) rewardMap.get("type")).toUpperCase());
        double startPrice = ((Number) rewardMap.get("start_price")).doubleValue();
        String auctionName = (String) auctionMap.get("name");

        if (type == AuctionType.ITEM) {
            ConfigurationSection itemSection = plugin.getConfigManager().getConfig().createSection("temp_item", (Map<?, ?>) rewardMap.get("item"));
            ItemStack item = ItemUtils.deserializeItem(itemSection);
            if (item != null) {
                // For server-run item auctions, there's no player owner. The item simply ceases to exist if there are no bids.
                // We'll treat this like a command auction where the reward is the item itself.
                String displayName = ItemUtils.getItemDisplayName(item);
                String command = "give %player% " + item.getType().getKey().getKey() + " " + item.getAmount(); // This is a representation.
                startCommandAuction(command, displayName, 300, startPrice, auctionName, "Server");
            }
        } else if (type == AuctionType.COMMAND) {
            String command = (String) rewardMap.get("command");
            // UPDATED: Get the explicit display name from the config.
            String displayName = (String) rewardMap.get("display-name");
            if (command != null && !command.isEmpty()) {
                startCommandAuction(command, displayName, 300, startPrice, auctionName, "Server");
            }
        }
    }
}
