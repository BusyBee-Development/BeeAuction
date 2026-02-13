package org.djtmk.beeauction.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.djtmk.beeauction.BeeAuction;

import java.util.List;
import java.util.logging.Logger;

public class CommandValidator {

    private final BeeAuction plugin;
    private final Logger logger;

    public CommandValidator(BeeAuction plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isCommandAllowed(String command) {
        if (command == null || command.isEmpty()) {
            logger.warning("Attempted to validate null or empty command");
            return false;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();

        boolean validationEnabled = config.getBoolean("schedule.command-validation.enabled", true);
        if (!validationEnabled) {
            logger.warning("Command validation is DISABLED. This is a security risk!");
            return true;
        }

        String normalizedCommand = command.trim().toLowerCase();

        List<String> blacklist = config.getStringList("schedule.command-validation.blacklist");
        for (String blockedPrefix : blacklist) {
            if (normalizedCommand.startsWith(blockedPrefix.toLowerCase())) {
                logger.warning("Blocked blacklisted command: " + InputSanitizer.sanitizeForLogging(command));
                return false;
            }
        }

        boolean whitelistMode = config.getBoolean("schedule.command-validation.whitelist-mode", false);

        if (whitelistMode) {
            List<String> whitelist = config.getStringList("schedule.command-validation.whitelist");

            for (String allowedPrefix : whitelist) {
                if (normalizedCommand.startsWith(allowedPrefix.toLowerCase())) {
                    logger.info("Approved whitelisted command: " + InputSanitizer.sanitizeForLogging(command));
                    return true;
                }
            }

            logger.warning("Blocked non-whitelisted command: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        } else {
            logger.info("Approved command (blacklist mode): " + InputSanitizer.sanitizeForLogging(command));
            return true;
        }
    }

    public boolean validateAndLog(String command, String context) {
        boolean allowed = isCommandAllowed(command);

        if (!allowed) {
            logger.severe("SECURITY: Blocked dangerous command in " + context + ": " +
                    InputSanitizer.sanitizeForLogging(command));
            logger.severe("Please review your auction configuration for security issues.");
        } else {
            logger.info("Validated command for " + context + ": " +
                    InputSanitizer.sanitizeForLogging(command));
        }

        return allowed;
    }

    public boolean isSafeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        String normalized = command.toLowerCase();
        if (normalized.contains(";") || normalized.contains("&&") ||
            normalized.contains("||") || normalized.contains("|")) {
            logger.warning("Command contains chaining operators: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        }

        if (normalized.contains("$(") || normalized.contains("`")) {
            logger.warning("Command contains substitution operators: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        }

        if (normalized.contains(">") || normalized.contains("<")) {
            logger.warning("Command contains redirection operators: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        }

        return true;
    }
}
