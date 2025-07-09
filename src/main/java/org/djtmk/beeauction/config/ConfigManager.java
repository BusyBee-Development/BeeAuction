package org.djtmk.beeauction.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.djtmk.beeauction.BeeAuction;
import org.djtmk.beeauction.config.AuctionEnum.StorageType;
import org.djtmk.beeauction.util.MessageUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private final BeeAuction plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private File configFile;
    private File messagesFile;

    private static ConfigManager instance;

    public ConfigManager(BeeAuction plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public void loadConfigs() {
        // Create config directory if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Load main config
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load messages config
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Compare with defaults
        matchConfigWithDefaults();
    }

    public void reloadConfigs() {
        // Reload main config
        config = YamlConfiguration.loadConfiguration(configFile);

        // Reload messages config
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Compare with defaults
        matchConfigWithDefaults();
    }

    private void matchConfigWithDefaults() {
        // Match main config with defaults
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                }
            }

            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
            }
        }

        // Match messages config with defaults
        InputStream defaultMessagesStream = plugin.getResource("messages.yml");
        if (defaultMessagesStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultMessagesStream, StandardCharsets.UTF_8));

            for (String key : defaultMessages.getKeys(true)) {
                if (!messagesConfig.contains(key)) {
                    messagesConfig.set(key, defaultMessages.get(key));
                }
            }

            try {
                messagesConfig.save(messagesFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save messages to " + messagesFile, e);
            }
        }
    }

    public static String getMessage(String key) {
        if (instance == null || instance.messagesConfig == null) {
            return "§cMessage not found: " + key;
        }

        String message = instance.messagesConfig.getString(key.toLowerCase().replace('_', '-'));
        if (message == null) {
            return "§cMessage not found: " + key;
        }

        return MessageUtil.colorize(message);
    }

    public static String getMessage(String key, Object... placeholders) {
        String message = getMessage(key);

        if (placeholders.length % 2 != 0) {
            return message;
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);
            message = message.replace("{" + placeholder + "}", value);
        }

        return message;
    }

    public StorageType getStorageType() {
        // Always return SQLITE as it's the only supported storage type now
        return StorageType.SQLITE;
    }
    public boolean isScheduleEnabled() {
        return config.getBoolean("schedule.enabled", false);
    }

    // Command configuration methods

    // Admin command methods
    public String getAdminCommandName() {
        return config.getString("commands.admin.name", "globalauction");
    }

    public List<String> getAdminCommandAliases() {
        return config.getStringList("commands.admin.aliases");
    }

    public String getAdminSubcommandStart() {
        return config.getString("commands.admin.subcommands.start", "start");
    }

    public String getAdminSubcommandCancel() {
        return config.getString("commands.admin.subcommands.cancel", "cancel");
    }

    public String getAdminSubcommandReload() {
        return config.getString("commands.admin.subcommands.reload", "reload");
    }

    public String getAdminAuctionTypeItem() {
        return config.getString("commands.admin.auction-types.item", "item");
    }

    public String getAdminAuctionTypeCommand() {
        return config.getString("commands.admin.auction-types.command", "command");
    }

    // Player command methods
    public String getPlayerBidCommand() {
        return config.getString("commands.player.bid", "podbij");
    }

    public List<String> getPlayerBidCommandAliases() {
        return config.getStringList("commands.player.bid-aliases");
    }

    public FileConfiguration getConfig() {
        return config;
    }
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
