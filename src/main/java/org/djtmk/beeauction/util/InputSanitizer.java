package org.djtmk.beeauction.util;

public class InputSanitizer {

    private static final int MAX_AUCTION_NAME_LENGTH = 64;
    private static final int MAX_COMMAND_DISPLAY_NAME_LENGTH = 64;
    private static final int MAX_CHAT_INPUT_LENGTH = 256;

    public static String sanitizeAuctionName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        input = input.replaceAll("\\p{C}", "");
        input = input.replaceAll("[\\u202E\\u202D\\u200E\\u200F]", "");
        input = input.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        input = input.replaceAll("&[klmno]", "");
        if (input.length() > MAX_AUCTION_NAME_LENGTH) {
            input = input.substring(0, MAX_AUCTION_NAME_LENGTH);
        }

        return input.trim();
    }

    public static String sanitizeCommandDisplayName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        input = input.replaceAll("\\p{C}", "");
        input = input.replaceAll("[\\u202E\\u202D\\u200E\\u200F]", "");
        input = input.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        input = input.replaceAll("&[klmno]", "");

        if (input.length() > MAX_COMMAND_DISPLAY_NAME_LENGTH) {
            input = input.substring(0, MAX_COMMAND_DISPLAY_NAME_LENGTH);
        }

        return input.trim();
    }

    public static String sanitizeChatInput(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        input = input.replaceAll("\\p{C}", "");
        input = input.replaceAll("[\\u202E\\u202D\\u200E\\u200F]", "");
        input = input.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        input = input.replaceAll("&[0-9a-fk-or]", "");
        if (input.length() > MAX_CHAT_INPUT_LENGTH) {
            input = input.substring(0, MAX_CHAT_INPUT_LENGTH);
        }

        return input.trim();
    }

    public static String sanitizePlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return "";
        }
        return playerName.replaceAll("[^a-zA-Z0-9_]", "");
    }

    public static boolean isSafeFilename(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        if (input.contains("..") || input.contains("/") || input.contains("\\")) {
            return false;
        }

        if (input.contains("\0")) {
            return false;
        }

        return input.matches("[a-zA-Z0-9._-]+");
    }

    public static boolean isValidAmount(double value, double minValue, double maxValue) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return false;
        }

        return value >= minValue && value <= maxValue;
    }

    public static String sanitizeForLogging(String command) {
        if (command == null) {
            return "null";
        }

        command = command.replaceAll("password[=:]\\S+", "password=***");
        command = command.replaceAll("token[=:]\\S+", "token=***");
        command = command.replaceAll("key[=:]\\S+", "key=***");

        if (command.length() > 200) {
            return command.substring(0, 200) + "... (truncated)";
        }

        return command;
    }
}
