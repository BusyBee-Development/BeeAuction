package org.djtmk.beeauction.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.djtmk.beeauction.BeeAuction;

import java.util.logging.Logger;

public class ConfigMigrator {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static final int EXPECTED_VERSION = 1;

    private final BeeAuction plugin;

    public ConfigMigrator(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public void migrate() {
        FileConfiguration config = plugin.getConfig();
        int currentVersion = config.getInt("config-version", 0);

        if (currentVersion < EXPECTED_VERSION) {
            log.info("[BeeAuction] Your config.yml is outdated. Auto-migration will be attempted.");
            // In the future, add migration logic here.
            // For now, we just update the version number.
            config.set("config-version", EXPECTED_VERSION);
            plugin.saveConfig();
            log.info("[BeeAuction] Config migration complete.");
        }
    }
}
