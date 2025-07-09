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

public class AuctionManager {
    private final BeeAuction plugin;
    private Auction activeAuction;
    private AuctionTask auctionTask;
    private BukkitTask scheduledTask;
    private Map<String, Boolean> startedAuctions = new HashMap<>();
    private DayOfWeek lastCheckedDay;

    public AuctionManager(BeeAuction plugin) {
        this.plugin = plugin;
        this.lastCheckedDay = LocalDateTime.now().getDayOfWeek();

        // Schedule auto auctions if enabled
        if (plugin.getConfigManager().isScheduleEnabled()) {
            scheduleAutoAuctions();
        }
    }

    /**
     * Start an item auction
     * @param item The item to auction
     * @param duration The duration in seconds
     * @param startPrice The starting price
     * @param customName Optional custom name for the auction
     * @param ownerName The name of the player who started the auction
     * @return True if the auction was started
     */
    public boolean startItemAuction(ItemStack item, int duration, double startPrice, String customName, String ownerName) {
        // Check if there's already an active auction
        if (activeAuction != null && activeAuction.isActive()) {
            return false;
        }

        // Create the auction
        activeAuction = new Auction(plugin, item, startPrice, duration, customName, ownerName);

        // Start the auction
        activeAuction.start();

        // Start the auction task
        auctionTask = new AuctionTask(plugin, activeAuction, this);
        auctionTask.runTaskTimer(plugin, 0, 20); // Run every second

        return true;
    }

    /**
     * Start an item auction
     * @param item The item to auction
     * @param duration The duration in seconds
     * @param startPrice The starting price
     * @param customName Optional custom name for the auction
     * @return True if the auction was started
     */
    public boolean startItemAuction(ItemStack item, int duration, double startPrice, String customName) {
        return startItemAuction(item, duration, startPrice, customName, "Unknown");
    }

    /**
     * Start an item auction (legacy method for backward compatibility)
     * @param item The item to auction
     * @param duration The duration in seconds
     * @param startPrice The starting price
     * @return True if the auction was started
     */
    public boolean startItemAuction(ItemStack item, int duration, double startPrice) {
        return startItemAuction(item, duration, startPrice, null, "Unknown");
    }

    /**
     * Start a command auction
     * @param command The command to execute
     * @param duration The duration in seconds
     * @param startPrice The starting price
     * @param customName Optional custom name for the auction
     * @param ownerName The name of the player who started the auction
     * @return True if the auction was started
     */
    public boolean startCommandAuction(String command, int duration, double startPrice, String customName, String ownerName) {
        // Check if there's already an active auction
        if (activeAuction != null && activeAuction.isActive()) {
            return false;
        }

        // Create the auction
        activeAuction = new Auction(plugin, command, startPrice, duration, customName, ownerName);

        // Start the auction
        activeAuction.start();

        // Start the auction task
        auctionTask = new AuctionTask(plugin, activeAuction, this);
        auctionTask.runTaskTimer(plugin, 0, 20); // Run every second

        return true;
    }

    /**
     * Start a command auction
     * @param command The command to execute
     * @param duration The duration in seconds
     * @param startPrice The starting price
     * @param customName Optional custom name for the auction
     * @return True if the auction was started
     */
    public boolean startCommandAuction(String command, int duration, double startPrice, String customName) {
        return startCommandAuction(command, duration, startPrice, customName, "Unknown");
    }

    /**
     * Start a command auction (legacy method for backward compatibility)
     * @param command The command to execute
     * @param duration The duration in seconds
     * @param startPrice The starting price
     * @return True if the auction was started
     */
    public boolean startCommandAuction(String command, int duration, double startPrice) {
        return startCommandAuction(command, duration, startPrice, null, "Unknown");
    }

    public boolean cancelAuction() {
        if (activeAuction == null || !activeAuction.isActive()) {
            return false;
        }

        // Cancel the auction
        activeAuction.cancel();

        // Cancel the task
        if (auctionTask != null) {
            auctionTask.cancelTask();
            auctionTask = null;
        }

        // Clear the active auction
        activeAuction = null;

        return true;
    }

    /**
     * Place a bid on the active auction
     * @param player The player placing the bid
     * @param amount The bid amount
     * @return True if the bid was successful
     */
    public boolean placeBid(Player player, double amount) {
        if (activeAuction == null || !activeAuction.isActive()) {
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

    /**
     * Schedule auto auctions
     * This method is called when the plugin is enabled and when the config is reloaded
     */
    public void scheduleAutoAuctions() {
        plugin.getLogger().info("Scheduling auto auctions...");

        // Check if scheduled auctions are enabled in the config
        if (!plugin.getConfigManager().isScheduleEnabled()) {
            plugin.getLogger().info("Scheduled auctions are disabled in config.yml");

            // Cancel any existing scheduled task
            if (scheduledTask != null) {
                scheduledTask.cancel();
                scheduledTask = null;
                plugin.getLogger().info("Cancelled existing scheduled auction task");
            }

            return;
        }

        // Cancel any existing scheduled task
        if (scheduledTask != null) {
            scheduledTask.cancel();
            plugin.getLogger().info("Cancelled existing scheduled auction task");
        }

        // Reset the tracking map
        startedAuctions.clear();

        // Reset the last checked day
        lastCheckedDay = LocalDateTime.now().getDayOfWeek();

        // Schedule a task to check for auto auctions every 10 seconds for more precise timing
        scheduledTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkScheduledAuctions, 20 * 10, 20 * 10);

        // Immediately check for scheduled auctions
        Bukkit.getScheduler().runTask(plugin, this::checkScheduledAuctions);
    }

    private void checkScheduledAuctions() {
        // Get the current day and time
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // Check if it's a new day, reset the tracking if it is
        if (lastCheckedDay != currentDay) {
            startedAuctions.clear();
            lastCheckedDay = currentDay;
        }

        // Get the scheduled auctions
        ConfigurationSection config = plugin.getConfigManager().getConfig();
        ConfigurationSection scheduleSection = config.getConfigurationSection("schedule");

        if (scheduleSection == null) {
            plugin.getLogger().warning("No 'schedule' section found in config.yml");
            return;
        }

        List<Map<?, ?>> auctionsList = scheduleSection.getMapList("auctions");

        if (auctionsList == null || auctionsList.isEmpty()) {
            plugin.getLogger().warning("No auctions found in schedule.auctions list");
            return;
        }

        // Check each scheduled auction
        for (Map<?, ?> auctionMap : auctionsList) {
            try {
                // Get the day
                String dayString = (String) auctionMap.get("day");
                if (dayString == null) {
                    plugin.getLogger().warning("Scheduled auction missing 'day' field");
                    continue;
                }

                try {
                    Day day = Day.valueOf(dayString);

                    // Convert to DayOfWeek
                    DayOfWeek scheduledDay = DayOfWeek.valueOf(day.name());

                    // Check if it's the right day
                    if (scheduledDay != currentDay) {
                        continue;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid day in scheduled auction: " + dayString);
                    continue;
                }

                // Get the time
                Object timeValue = auctionMap.get("time");
                String timeString;

                if (timeValue == null) {
                    plugin.getLogger().warning("Scheduled auction missing 'time' field");
                    continue;
                } else if (timeValue instanceof Integer) {
                    // Convert Integer to String in format HH:mm
                    int timeInt = (Integer) timeValue;
                    int hours = timeInt / 100;
                    int minutes = timeInt % 100;
                    timeString = String.format("%02d:%02d", hours, minutes);
                } else if (timeValue instanceof String) {
                    timeString = (String) timeValue;
                } else {
                    plugin.getLogger().warning("Unexpected time format in scheduled auction: " + timeValue + " (type: " + timeValue.getClass().getName() + ")");
                    timeString = String.valueOf(timeValue);
                }

                LocalTime scheduledTime;
                try {
                    scheduledTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
                } catch (DateTimeParseException e) {
                    plugin.getLogger().warning("Invalid time format in scheduled auction: " + timeString + 
                            ". Expected format: HH:mm (e.g. 08:25)");
                    continue;
                }

                // Create a unique key for this auction
                String auctionKey = dayString + "_" + timeString;

                // Check if this auction has already been started today
                if (startedAuctions.containsKey(auctionKey) && startedAuctions.get(auctionKey)) {
                    continue;
                }

                // Check if it's the right time (current time should be at or after scheduled time)
                int timeDifference = currentTime.toSecondOfDay() - scheduledTime.toSecondOfDay();

                // Only start if current time is at or after scheduled time (with a small buffer of 5 seconds)
                // or if we're within 30 seconds after the scheduled time to avoid missing it
                if (timeDifference < -5 || timeDifference > 30) {
                    continue;
                }

                // Get the reward
                Map<?, ?> rewardMap = (Map<?, ?>) auctionMap.get("reward");
                if (rewardMap == null) {
                    plugin.getLogger().warning("Scheduled auction missing 'reward' field");
                    continue;
                }

                // Get the reward type
                String rewardType = (String) rewardMap.get("type");
                if (rewardType == null) {
                    plugin.getLogger().warning("Scheduled auction missing 'reward.type' field");
                    continue;
                }

                AuctionType type;
                try {
                    type = AuctionType.valueOf(rewardType);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid reward type in scheduled auction: " + rewardType);
                    continue;
                }

                // Get the start price
                double startPrice = rewardMap.get("start_price") instanceof Number ? 
                        ((Number) rewardMap.get("start_price")).doubleValue() : 100;

                // Get the auction name
                String auctionName = (String) auctionMap.get("name");
                // If no name is provided, use a default name
                if (auctionName == null || auctionName.isEmpty()) {
                    auctionName = "Scheduled Auction";
                }

                // Start the auction
                if (type == AuctionType.ITEM) {
                    // Get the item
                    Map<?, ?> itemMap = (Map<?, ?>) rewardMap.get("item");
                    if (itemMap == null) {
                        plugin.getLogger().warning("Scheduled item auction missing 'reward.item' field");
                        continue;
                    }

                    // Create a temporary ConfigurationSection for the item
                    ConfigurationSection itemSection = config.createSection("temp_item");
                    for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            itemSection.set((String) entry.getKey(), entry.getValue());
                        }
                    }

                    ItemStack item = ItemUtils.deserializeItem(itemSection);
                    config.set("temp_item", null); // Clean up

                    if (item != null) {
                        boolean success = startItemAuction(item, 300, startPrice, auctionName); // 5 minutes
                        if (!success) {
                            plugin.getLogger().warning("Failed to start item auction");
                        }
                    } else {
                        plugin.getLogger().warning("Failed to create item for auction");
                    }
                } else if (type == AuctionType.COMMAND) {
                    // Get the command
                    String command = (String) rewardMap.get("command");

                    if (command == null || command.isEmpty()) {
                        plugin.getLogger().warning("Scheduled command auction missing or empty 'reward.command' field");
                        continue;
                    }

                    boolean success = startCommandAuction(command, 300, startPrice, auctionName); // 5 minutes
                    if (!success) {
                        plugin.getLogger().warning("Failed to start command auction");
                    }
                }

                // Mark this auction as started
                startedAuctions.put(auctionKey, true);

                // Only start one auction at a time
                break;
            } catch (IllegalArgumentException | DateTimeParseException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid scheduled auction configuration", e);
            }
        }
    }
}
