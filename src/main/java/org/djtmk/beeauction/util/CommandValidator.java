package org.djtmk.beeauction.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.djtmk.beeauction.BeeAuction;

import java.util.List;
import java.util.logging.Logger;

/**
 * Validates commands for scheduled auctions to prevent malicious command execution.
 * Supports whitelist and blacklist modes for flexible security.
 */
public class CommandValidator {

    private final BeeAuction plugin;
    private final Logger logger;

    public CommandValidator(BeeAuction plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Validates a command against the configured whitelist/blacklist.
     *
     * @param command the command to validate (without leading slash)
     * @return true if the command is allowed, false otherwise
     */
    public boolean isCommandAllowed(String command) {
        if (command == null || command.isEmpty()) {
            logger.warning("Attempted to validate null or empty command");
            return false;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();

        // Check if validation is enabled
        boolean validationEnabled = config.getBoolean("schedule.command-validation.enabled", true);
        if (!validationEnabled) {
            logger.warning("Command validation is DISABLED. This is a security risk!");
            return true;
        }

        // Normalize command (trim and lowercase for comparison)
        String normalizedCommand = command.trim().toLowerCase();

        // Always check blacklist first (regardless of mode)
        List<String> blacklist = config.getStringList("schedule.command-validation.blacklist");
        for (String blockedPrefix : blacklist) {
            if (normalizedCommand.startsWith(blockedPrefix.toLowerCase())) {
                logger.warning("Blocked blacklisted command: " + InputSanitizer.sanitizeForLogging(command));
                return false;
            }
        }

        // Check whitelist mode
        boolean whitelistMode = config.getBoolean("schedule.command-validation.whitelist-mode", false);

        if (whitelistMode) {
            // Whitelist mode: command must start with an allowed prefix
            List<String> whitelist = config.getStringList("schedule.command-validation.whitelist");

            for (String allowedPrefix : whitelist) {
                if (normalizedCommand.startsWith(allowedPrefix.toLowerCase())) {
                    logger.info("Approved whitelisted command: " + InputSanitizer.sanitizeForLogging(command));
                    return true;
                }
            }

            // Not in whitelist
            logger.warning("Blocked non-whitelisted command: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        } else {
            // Blacklist mode: allow all commands except blacklisted ones
            // (blacklist was already checked above)
            logger.info("Approved command (blacklist mode): " + InputSanitizer.sanitizeForLogging(command));
            return true;
        }
    }

    /**
     * Validates a command and logs a detailed reason if blocked.
     *
     * @param command the command to validate
     * @param context additional context for logging (e.g., "scheduled auction", "reward")
     * @return true if allowed, false otherwise
     */
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

    /**
     * Checks if a command contains potentially dangerous patterns.
     * This is an additional check beyond whitelist/blacklist.
     *
     * @param command the command to check
     * @return true if the command appears safe
     */
    public boolean isSafeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        String normalized = command.toLowerCase();

        // Check for command chaining attempts
        if (normalized.contains(";") || normalized.contains("&&") ||
            normalized.contains("||") || normalized.contains("|")) {
            logger.warning("Command contains chaining operators: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        }

        // Check for command substitution attempts
        if (normalized.contains("$(") || normalized.contains("`")) {
            logger.warning("Command contains substitution operators: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        }

        // Check for redirection attempts
        if (normalized.contains(">") || normalized.contains("<")) {
            logger.warning("Command contains redirection operators: " + InputSanitizer.sanitizeForLogging(command));
            return false;
        }

        return true;
    }
}
