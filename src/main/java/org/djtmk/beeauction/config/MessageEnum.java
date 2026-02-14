package org.djtmk.beeauction.config;

public enum MessageEnum {

    AUCTION_STARTED,
    NEW_BID,
    OUTBID,
    WIN,
    WIN_MESSAGE,
    LOSE_MESSAGE,
    AUCTION_CANCELLED,
    NO_AUCTION,
    AUCTION_ENDED,
    TIME_EXTENSION,
    NO_PERMISSION,
    INVALID_AMOUNT,
    NOT_ENOUGH_MONEY,
    AUCTION_ALREADY_ACTIVE,
    INVALID_COMMAND,
    RELOAD_SUCCESS,
    AUCTION_NAME_QUESTION,
    COMMAND_DISPLAY_NAME_QUESTION,
    CLAIM_SUCCESS,
    CLAIM_FAIL,
    CLAIM_INVENTORY_FULL,
    CLAIM_JOIN_NOTIFICATION;

    public String get() {
        return ConfigManager.getMessage(this.name());
    }
    public String get(Object... placeholders) {
        return ConfigManager.getMessage(this.name(), placeholders);
    }
}
