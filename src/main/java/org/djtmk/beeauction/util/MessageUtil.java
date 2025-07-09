package org.djtmk.beeauction.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }

        // First translate standard color codes
        String colored = ChatColor.translateAlternateColorCodes('&', message);

        // Then replace hex colors
        Matcher matcher = HEX_PATTERN.matcher(colored);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }

        matcher.appendTail(buffer);

        return buffer.toString();
    }

    public static void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        player.sendMessage(colorize(message));
    }

    public static String formatPrice(double price) {
        return String.format("%,.2f", price);
    }
}
