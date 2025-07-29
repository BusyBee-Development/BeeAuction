package org.djtmk.beeauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.djtmk.beeauction.auctions.AuctionManager;
import org.djtmk.beeauction.commands.BidCommand;
import org.djtmk.beeauction.commands.ClaimCommand;
import org.djtmk.beeauction.commands.GlobalAuctionCommand;
import org.djtmk.beeauction.commands.GlobalAuctionTabCompleter;
import org.djtmk.beeauction.config.ConfigManager;
import org.djtmk.beeauction.data.DatabaseManager;
import org.djtmk.beeauction.economy.EconomyHandler;
import org.djtmk.beeauction.listeners.AdminJoinListener;
import org.djtmk.beeauction.listeners.AuctionChatListener;
import org.djtmk.beeauction.listeners.ClaimListener;
import org.djtmk.beeauction.util.UpdateChecker;

import java.util.Objects;
import java.util.logging.Logger;

public final class BeeAuction extends JavaPlugin {
    private static BeeAuction instance;
    private static final Logger log = Logger.getLogger("Minecraft");
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private AuctionManager auctionManager;
    private EconomyHandler economyHandler;
    private AuctionChatListener auctionChatListener;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        auctionManager = new AuctionManager(this);

        registerListeners();
        registerCommands();

        updateChecker = new UpdateChecker(this, "beeauction"); // Replace with your Modrinth ID
        updateChecker.check();

        log.info(String.format("[%s] has been enabled! Version: %s", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.cancelAuction();
        }
        if (auctionChatListener != null) {
            auctionChatListener.cancelCleanupTask();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        log.info(String.format("[%s] has been disabled.", getDescription().getName()));
    }

    // This method now only looks for "Vault", which is what your fork identifies as.
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            log.severe(String.format("[%s] - No economy service found through Vault. Is an economy plugin (e.g. EssentialsX) installed?", getDescription().getName()));
            return false;
        }
        economyHandler = new EconomyHandler(rsp.getProvider());
        log.info(String.format("[%s] Successfully hooked into economy service: %s", getDescription().getName(), rsp.getProvider().getName()));
        return true;
    }

    private void registerListeners() {
        auctionChatListener = new AuctionChatListener(this);
        getServer().getPluginManager().registerEvents(new ClaimListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminJoinListener(this), this);
    }

    private void registerCommands() {
        String adminCommandName = configManager.getAdminCommandName();
        GlobalAuctionTabCompleter tabCompleter = new GlobalAuctionTabCompleter(this);
        Objects.requireNonNull(getCommand(adminCommandName)).setExecutor(new GlobalAuctionCommand(this, auctionChatListener));
        Objects.requireNonNull(getCommand(adminCommandName)).setTabCompleter(tabCompleter);

        String playerBidCommand = configManager.getPlayerBidCommand();
        Objects.requireNonNull(getCommand(playerBidCommand)).setExecutor(new BidCommand(this));

        Objects.requireNonNull(getCommand("claim")).setExecutor(new ClaimCommand(this));
    }

    public static BeeAuction getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public EconomyHandler getEconomyHandler() { return economyHandler; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
}
