package org.djtmk.beeauction.auctions;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.AuctionEnum.AuctionType;
import org.djtmk.beeauction.config.MessageEnum;
import org.djtmk.beeauction.events.AuctionStartEvent;
import org.djtmk.beeauction.util.CommandValidator;
import org.djtmk.beeauction.util.ItemUtils;
import org.djtmk.beeauction.util.MessageUtil;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AuctionManager {
    private final BeeAuction plugin;
    private final Cache<String, Auction> auctionCache;
    private AuctionTask auctionTask;
    private BukkitTask scheduledTask;
    private final Map<String, Boolean> startedAuctions = new HashMap<>();
    private DayOfWeek lastCheckedDay;
    private static final String ACTIVE_AUCTION_KEY = "active_auction";
    private final CommandValidator commandValidator; // SECURITY FIX

    public AuctionManager(BeeAuction plugin) {
        this.plugin = plugin;
        this.lastCheckedDay = LocalDateTime.now().getDayOfWeek();
        this.commandValidator = new CommandValidator(plugin); // SECURITY FIX

        this.auctionCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();

        if (plugin.getConfigManager().isScheduleEnabled()) {
            scheduleAutoAuctions();
        }
    }

    public boolean startItemAuction(ItemStack item, int duration, double startPrice, String customName, Player owner) {
        if (hasActiveAuction()) {
            return false;
        }
        Auction auction = new Auction(plugin, item, startPrice, duration, customName, owner);
        startAuction(auction);
        return true;
    }

    public boolean startCommandAuction(String command, String displayName, int duration, double startPrice, String customName, String ownerName) {
        if (hasActiveAuction()) {
            return false;
        }
        Auction auction = new Auction(plugin, command, displayName, startPrice, duration, customName, ownerName);
        startAuction(auction);
        return true;
    }

    private void startAuction(Auction auction) {
        AuctionStartEvent startEvent = new AuctionStartEvent(auction);
        Bukkit.getPluginManager().callEvent(startEvent);

        auction.start();
        auctionCache.put(ACTIVE_AUCTION_KEY, auction);
        auctionTask = new AuctionTask(plugin, auction, this);
        auctionTask.runTaskTimerAsynchronously(plugin, 0, 20);
    }

    public boolean cancelAuction() {
        Auction activeAuction = getActiveAuction();
        if (activeAuction == null) {
            return false;
        }

        activeAuction.cancel();
        if (auctionTask != null) {
            auctionTask.cancelTask();
            auctionTask = null;
        }
        clearActiveAuction();
        return true;
    }

    public boolean placeBid(Player player, double amount) {
        Auction activeAuction = getActiveAuction();
        if (activeAuction == null) {
            MessageUtil.sendMessage(player, MessageEnum.NO_AUCTION.get());
            return false;
        }
        return activeAuction.placeBid(player, amount);
    }

    public void clearActiveAuction() {
        auctionCache.invalidate(ACTIVE_AUCTION_KEY);
        auctionTask = null;
    }

    public boolean hasActiveAuction() {
        Auction auction = auctionCache.getIfPresent(ACTIVE_AUCTION_KEY);
        return auction != null && auction.isActive();
    }

    public Auction getActiveAuction() {
        return auctionCache.getIfPresent(ACTIVE_AUCTION_KEY);
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

        if (hasActiveAuction()) return;

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
                if (timeDifference < 0 || timeDifference > 30) continue;

                Bukkit.getScheduler().runTask(plugin, () -> startScheduledAuction(auctionMap));
                startedAuctions.put(auctionKey, true);
                break;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Invalid scheduled auction configuration", e);
            }
        }
    }

    private void startScheduledAuction(Map<?, ?> auctionMap) {
        if (hasActiveAuction()) return;

        Map<?, ?> rewardMap = (Map<?, ?>) auctionMap.get("reward");
        AuctionType type = AuctionType.valueOf(((String) rewardMap.get("type")).toUpperCase());
        double startPrice = ((Number) rewardMap.get("start_price")).doubleValue();
        String auctionName = (String) auctionMap.get("name");
        int duration = auctionMap.containsKey("duration") ? ((Number) auctionMap.get("duration")).intValue() : 300;

        if (type == AuctionType.ITEM) {
            ConfigurationSection itemSection = plugin.getConfigManager().getConfig().createSection("temp_item", (Map<?, ?>) rewardMap.get("item"));
            ItemStack item = ItemUtils.deserializeItem(itemSection);
            if (item != null) {
                String displayName = ItemUtils.getItemDisplayName(item);
                String command = "give %player% " + item.getType().getKey().getKey() + " " + item.getAmount();

                // SECURITY FIX: Validate command before starting auction
                if (!commandValidator.validateAndLog(command, "scheduled item auction: " + auctionName)) {
                    plugin.getLogger().severe("Skipping scheduled auction '" + auctionName + "' due to dangerous command");
                    return;
                }
                if (!commandValidator.isSafeCommand(command)) {
                    plugin.getLogger().severe("Skipping scheduled auction '" + auctionName + "' due to unsafe command patterns");
                    return;
                }

                startCommandAuction(command, displayName, duration, startPrice, auctionName, "Server");
            }
        } else if (type == AuctionType.COMMAND) {
            String command = (String) rewardMap.get("command");
            String displayName = (String) rewardMap.get("display-name");
            if (command != null && !command.isEmpty()) {
                // SECURITY FIX: Validate command before starting auction
                if (!commandValidator.validateAndLog(command, "scheduled command auction: " + auctionName)) {
                    plugin.getLogger().severe("Skipping scheduled auction '" + auctionName + "' due to dangerous command");
                    return;
                }
                if (!commandValidator.isSafeCommand(command)) {
                    plugin.getLogger().severe("Skipping scheduled auction '" + auctionName + "' due to unsafe command patterns");
                    return;
                }

                startCommandAuction(command, displayName, duration, startPrice, auctionName, "Server");
            }
        }
    }
}
