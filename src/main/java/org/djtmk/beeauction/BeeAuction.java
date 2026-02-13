package org.djtmk.beeauction;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.djtmk.beeauction.api.BeeAuctionAPI;
import org.djtmk.beeauction.auctions.AuctionCreationManager;
import org.djtmk.beeauction.auctions.AuctionManager;
import org.djtmk.beeauction.auctions.BidManager;
import org.djtmk.beeauction.commands.BidCommand;
import org.djtmk.beeauction.commands.ClaimCommand;
import org.djtmk.beeauction.commands.GlobalAuctionCommand;
import org.djtmk.beeauction.commands.GlobalAuctionTabCompleter;
import org.djtmk.beeauction.config.ConfigManager;
import org.djtmk.beeauction.hooks.PlaceholderHook;
import org.djtmk.beeauction.mysql.AsyncDatabaseManager;
import org.djtmk.beeauction.mysql.DatabaseManagerFactory;
import org.djtmk.beeauction.economy.EconomyManager;
import org.djtmk.beeauction.listeners.AdminJoinListener;
import org.djtmk.beeauction.listeners.AuctionCreationListener;
import org.djtmk.beeauction.listeners.ClaimListener;
import org.djtmk.beeauction.listeners.GUIListener;
import org.djtmk.beeauction.util.UpdateChecker;

import java.util.Objects;
import java.util.logging.Logger;

public final class BeeAuction extends JavaPlugin {
    private static BeeAuction instance;
    private static final Logger log = Logger.getLogger("Minecraft");
    private ConfigManager configManager;
    private AsyncDatabaseManager databaseManager;
    private AuctionManager auctionManager;
    private EconomyManager economyManager;
    private AuctionCreationListener auctionCreationListener;
    private BidManager bidManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        if (!configManager.loadConfigs()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        economyManager = new EconomyManager();
        if (!economyManager.isAvailable()) {
            log.severe(String.format("[%s] - Disabled due to no supported economy plugin found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        log.info(String.format("[%s] Successfully hooked into economy service: %s", getDescription().getName(), economyManager.getProviderName()));

        databaseManager = new DatabaseManagerFactory(this).createDatabaseManager();
        databaseManager.initialize();

        auctionManager = new AuctionManager(this);
        auctionCreationListener = new AuctionCreationListener(this);
        bidManager = new BidManager(this);

        BeeAuctionAPI.setPlugin(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            log.info("[BeeAuction] PlaceholderAPI hook enabled.");
        }

        registerListeners();
        registerCommands();

        int pluginId = 29513;
        try {
            new Metrics(this, pluginId);
            log.info(String.format("[%s] bStats metrics enabled.", getDescription().getName()));
        } catch (Exception e) {
            log.warning(String.format("[%s] Failed to initialize bStats: %s", getDescription().getName(), e.getMessage()));
        }

        updateChecker = new UpdateChecker(this, "beeauction");
        updateChecker.check();

        log.info(String.format("[%s] has been enabled! Version: %s", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.cancelAuction();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        log.info(String.format("[%s] has been disabled.", getDescription().getName()));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ClaimListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminJoinListener(this), this);
        // GUI disabled - uncomment to re-enable
        // getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(auctionCreationListener, this);
    }

    private void registerCommands() {
        String adminCommandName = configManager.getAdminCommandName();
        GlobalAuctionTabCompleter tabCompleter = new GlobalAuctionTabCompleter(this);
        Objects.requireNonNull(getCommand(adminCommandName)).setExecutor(new GlobalAuctionCommand(this, auctionCreationListener.getCreationManager()));
        Objects.requireNonNull(getCommand(adminCommandName)).setTabCompleter(tabCompleter);

        String playerBidCommand = configManager.getPlayerBidCommand();
        Objects.requireNonNull(getCommand(playerBidCommand)).setExecutor(new BidCommand(this));

        Objects.requireNonNull(getCommand("claim")).setExecutor(new ClaimCommand(this));
    }

    public static BeeAuction getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public AsyncDatabaseManager getDatabaseManager() { return databaseManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public AuctionCreationManager getAuctionCreationManager() { return auctionCreationListener.getCreationManager(); }
    public BidManager getBidManager() { return bidManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
}
