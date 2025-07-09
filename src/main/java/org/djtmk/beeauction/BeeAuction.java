package org.djtmk.beeauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.djtmk.beeauction.auctions.AuctionManager;
import org.djtmk.beeauction.commands.GlobalAuctionCommand;
import org.djtmk.beeauction.commands.GlobalAuctionTabCompleter;
import org.djtmk.beeauction.commands.BidCommand;
import org.djtmk.beeauction.config.ConfigManager;
import org.djtmk.beeauction.data.DatabaseManager;
import org.djtmk.beeauction.economy.EconomyHandler;

import java.util.logging.Logger;

public final class BeeAuction extends JavaPlugin {
    private static BeeAuction instance;
    private static final Logger log = Logger.getLogger("Minecraft");
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private AuctionManager auctionManager;
    private EconomyHandler economyHandler;

    @Override
    public void onEnable() {
        // Set instance
        instance = this;

        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Setup economy
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize auction manager
        auctionManager = new AuctionManager(this);

        // Register commands
        String adminCommandName = configManager.getAdminCommandName();
        String playerBidCommand = configManager.getPlayerBidCommand();

        // Register admin command
        if (getCommand(adminCommandName) != null) {
            getCommand(adminCommandName).setExecutor(new GlobalAuctionCommand(this));
            getCommand(adminCommandName).setTabCompleter(new GlobalAuctionTabCompleter(this));
        } else {
            log.warning("Failed to register admin command: " + adminCommandName + ". Using default command.");
            getCommand("globalauction").setExecutor(new GlobalAuctionCommand(this));
            getCommand("globalauction").setTabCompleter(new GlobalAuctionTabCompleter(this));
        }

        // Register player bid command
        if (getCommand(playerBidCommand) != null) {
            getCommand(playerBidCommand).setExecutor(new BidCommand(this));
        } else {
            log.warning("Failed to register player bid command: " + playerBidCommand + ". Using default command.");
            getCommand("podbij").setExecutor(new BidCommand(this));
        }

        // Log startup
        log.info(String.format("[%s] - Enabled version %s", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        // Cancel any active auctions
        if (auctionManager != null) {
            auctionManager.cancelAuction();
        }

        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        // Log shutdown
        log.info(String.format("[%s] - Disabled version %s", getDescription().getName(), getDescription().getVersion()));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economyHandler = new EconomyHandler(rsp.getProvider());
        return true;
    }

    public static BeeAuction getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public EconomyHandler getEconomyHandler() {
        return economyHandler;
    }
}
