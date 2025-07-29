package org.djtmk.beeauction.config;

// UPDATED: Added new message keys for the claim system and more specific questions.
public enum MessageEnum {
    // Auction messages
    AUCTION_STARTED,
    NEW_BID,
    OUTBID,
    WIN,
    CANCELLED,
    NO_AUCTION,
    AUCTION_ENDED,
    TIME_EXTENSION,

    // Error messages
    NO_PERMISSION,
    INVALID_AMOUNT,
    NOT_ENOUGH_MONEY,
    AUCTION_ALREADY_ACTIVE,
    INVALID_COMMAND,

    // Success messages
    RELOAD_SUCCESS,

    // Auction creation questions
    AUCTION_NAME_QUESTION,
    COMMAND_DISPLAY_NAME_QUESTION, // NEW

    // Claim system messages (NEW)
    CLAIM_SUCCESS,
    CLAIM_FAIL,
    CLAIM_INVENTORY_FULL,
    CLAIM_JOIN_NOTIFICATION;

    /**
     * Gets the configured message from messages.yml.
     * @return The formatted message string.
     */
    public String get() {
        return ConfigManager.getMessage(this.name());
    }

    /**
     * Gets the configured message from messages.yml and replaces placeholders.
     * @param placeholders A list of key-value pairs to replace (e.g., "player", "Steve", "amount", "100").
     * @return The formatted message string with placeholders replaced.
     */
    public String get(Object... placeholders) {
        return ConfigManager.getMessage(this.name(), placeholders);
    }
}
