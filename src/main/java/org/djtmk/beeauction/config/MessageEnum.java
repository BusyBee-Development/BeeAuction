package org.djtmk.beeauction.config;

/**
 * Enum for message configuration
 */
public enum MessageEnum {
    AUCTION_STARTED,
    NEW_BID,
    OUTBID,
    WIN,
    CANCELLED,
    NO_AUCTION,
    NO_PERMISSION,
    INVALID_AMOUNT,
    NOT_ENOUGH_MONEY,
    AUCTION_ALREADY_ACTIVE,
    RELOAD_SUCCESS,
    INVALID_COMMAND,
    AUCTION_ENDED,
    AUCTION_NAME_QUESTION,
    TIME_EXTENSION;

    public String get() {
        return ConfigManager.getMessage(this.name());
    }
    public String get(Object... placeholders) {
        return ConfigManager.getMessage(this.name(), placeholders);
    }
}
