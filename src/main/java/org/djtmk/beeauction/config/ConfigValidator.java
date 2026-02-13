package org.djtmk.beeauction.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.djtmk.beeauction.BeeAuction;

import java.util.logging.Logger;

public class ConfigValidator {

    private static final Logger log = Logger.getLogger("Minecraft");
    private final BeeAuction plugin;
    private boolean isValid = true;

    public ConfigValidator(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public boolean validate() {
        FileConfiguration config = plugin.getConfig();

        if (config.getDouble("auction.min-bid-amount") <= 0) {
            log.severe("[BeeAuction] Invalid config: 'auction.min-bid-amount' must be a positive number.");
            isValid = false;
        }
        if (config.getDouble("auction.min-bid-increment") <= 0) {
            log.severe("[BeeAuction] Invalid config: 'auction.min-bid-increment' must be a positive number.");
            isValid = false;
        }
        if (config.getDouble("auction.sales-tax-rate") < 0 || config.getDouble("auction.sales-tax-rate") > 1) {
            log.severe("[BeeAuction] Invalid config: 'auction.sales-tax-rate' must be between 0 and 1.");
            isValid = false;
        }

        String dbType = config.getString("database.type", "sqlite");
        if (!dbType.equalsIgnoreCase("sqlite") && !dbType.equalsIgnoreCase("mysql")) {
            log.severe("[BeeAuction] Invalid config: 'database.type' must be either 'sqlite' or 'mysql'.");
            isValid = false;
        }

        if (!isValid) {
            log.severe("[BeeAuction] Disabling plugin due to critical configuration errors.");
        }

        return isValid;
    }
}
