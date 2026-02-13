package org.djtmk.beeauction.util;

/**
 * Utility class for sanitizing user input to prevent security exploits.
 * Handles chat injection, color code abuse, Unicode exploits, and command injection.
 */
public class InputSanitizer {

    // Maximum lengths for various input types
    private static final int MAX_AUCTION_NAME_LENGTH = 64;
    private static final int MAX_COMMAND_DISPLAY_NAME_LENGTH = 64;
    private static final int MAX_CHAT_INPUT_LENGTH = 256;

    /**
     * Sanitizes auction names entered by players.
     * Removes control characters, Unicode exploits, and enforces length limits.
     * Allows basic color codes (&0-9, &a-f, &r) but strips potentially harmful ones.
     *
     * @param input the raw auction name from player input
     * @return sanitized auction name safe for display and storage
     */
    public static String sanitizeAuctionName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Remove control characters (including newlines, tabs, etc.)
        input = input.replaceAll("\\p{C}", "");

        // Remove Unicode direction override characters (RTL/LTR exploits)
        // U+202E (Right-to-Left Override), U+202D (Left-to-Right Override)
        // U+200E (Left-to-Right Mark), U+200F (Right-to-Left Mark)
        input = input.replaceAll("[\\u202E\\u202D\\u200E\\u200F]", "");

        // Remove zero-width characters that can be used for visual exploits
        // U+200B (Zero Width Space), U+200C (Zero Width Non-Joiner), U+200D (Zero Width Joiner)
        // U+FEFF (Zero Width No-Break Space)
        input = input.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");

        // Strip formatting codes that can be abused (&k for obfuscation can cause lag)
        // Keep basic colors (0-9, a-f) and reset (r)
        // Remove: k (obfuscated), l (bold), m (strikethrough), n (underline), o (italic)
        input = input.replaceAll("&[klmno]", "");

        // Enforce maximum length (prevent excessive memory usage)
        if (input.length() > MAX_AUCTION_NAME_LENGTH) {
            input = input.substring(0, MAX_AUCTION_NAME_LENGTH);
        }

        // Trim whitespace
        return input.trim();
    }

    /**
     * Sanitizes command display names for scheduled auctions.
     *
     * @param input the raw display name from configuration
     * @return sanitized display name
     */
    public static String sanitizeCommandDisplayName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Apply same sanitization as auction names
        input = input.replaceAll("\\p{C}", "");
        input = input.replaceAll("[\\u202E\\u202D\\u200E\\u200F]", "");
        input = input.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        input = input.replaceAll("&[klmno]", "");

        if (input.length() > MAX_COMMAND_DISPLAY_NAME_LENGTH) {
            input = input.substring(0, MAX_COMMAND_DISPLAY_NAME_LENGTH);
        }

        return input.trim();
    }

    /**
     * Sanitizes general chat input during auction creation.
     * More restrictive than auction names.
     *
     * @param input the raw chat message from player
     * @return sanitized chat input
     */
    public static String sanitizeChatInput(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Remove all control characters
        input = input.replaceAll("\\p{C}", "");

        // Remove Unicode exploits
        input = input.replaceAll("[\\u202E\\u202D\\u200E\\u200F]", "");
        input = input.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");

        // Strip ALL color codes from player chat input (they can type them later with &)
        // This prevents fake system messages
        input = input.replaceAll("&[0-9a-fk-or]", "");

        // Enforce length limit
        if (input.length() > MAX_CHAT_INPUT_LENGTH) {
            input = input.substring(0, MAX_CHAT_INPUT_LENGTH);
        }

        return input.trim();
    }

    /**
     * Sanitizes player names for use in commands to prevent command injection.
     * Only allows alphanumeric characters and underscores (standard Minecraft format).
     *
     * @param playerName the player name to sanitize
     * @return sanitized player name containing only safe characters
     */
    public static String sanitizePlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return "";
        }

        // Only allow letters, numbers, and underscores
        // Minecraft usernames are 3-16 characters: alphanumeric + underscore
        return playerName.replaceAll("[^a-zA-Z0-9_]", "");
    }

    /**
     * Validates that a string contains only safe characters for file paths.
     * Prevents directory traversal attacks.
     *
     * @param input the input to validate
     * @return true if the input is safe for file operations
     */
    public static boolean isSafeFilename(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Check for directory traversal patterns
        if (input.contains("..") || input.contains("/") || input.contains("\\")) {
            return false;
        }

        // Check for null bytes (can be used to bypass file extension checks)
        if (input.contains("\0")) {
            return false;
        }

        // Only allow safe characters: alphanumeric, dash, underscore, period
        return input.matches("[a-zA-Z0-9._-]+");
    }

    /**
     * Validates a double value is within reasonable bounds for auction amounts.
     *
     * @param value the value to validate
     * @param minValue the minimum allowed value (inclusive)
     * @param maxValue the maximum allowed value (inclusive)
     * @return true if the value is within bounds
     */
    public static boolean isValidAmount(double value, double minValue, double maxValue) {
        // Check for NaN and infinity
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return false;
        }

        // Check bounds
        return value >= minValue && value <= maxValue;
    }

    /**
     * Sanitizes a command string for logging purposes.
     * Redacts potentially sensitive information.
     *
     * @param command the command to sanitize for logging
     * @return sanitized command safe for logs
     */
    public static String sanitizeForLogging(String command) {
        if (command == null) {
            return "null";
        }

        // Redact common sensitive patterns
        command = command.replaceAll("password[=:]\\S+", "password=***");
        command = command.replaceAll("token[=:]\\S+", "token=***");
        command = command.replaceAll("key[=:]\\S+", "key=***");

        // Limit length for logging
        if (command.length() > 200) {
            return command.substring(0, 200) + "... (truncated)";
        }

        return command;
    }
}
