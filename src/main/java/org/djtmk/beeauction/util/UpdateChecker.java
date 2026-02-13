package org.djtmk.beeauction.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String modrinthProjectId;
    private String latestVersion;
    private String downloadUrl;

    public UpdateChecker(JavaPlugin plugin, String modrinthProjectId) {
        this.plugin = plugin;
        this.modrinthProjectId = modrinthProjectId;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + modrinthProjectId + "/version");

                if (!url.getProtocol().equals("https")) {
                    plugin.getLogger().severe("Update check must use HTTPS. Aborting.");
                    return;
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", plugin.getName() + "/UpdateChecker");

                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(false);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    String redirectUrl = connection.getHeaderField("Location");
                    if (redirectUrl != null && !redirectUrl.startsWith("https://")) {
                        plugin.getLogger().warning("Update check redirect to non-HTTPS URL blocked: " + redirectUrl);
                        return;
                    }
                    plugin.getLogger().info("Update check redirected, please update the Modrinth project ID.");
                    return;
                }

                if (responseCode != 200) {
                    plugin.getLogger().warning("Could not check for updates. Modrinth API returned status: " + responseCode);
                    return;
                }

                JsonArray versions = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonArray();
                if (versions.size() > 0) {
                    JsonObject latest = versions.get(0).getAsJsonObject();
                    String newVersion = latest.get("version_number").getAsString();

                    if (isNewer(plugin.getDescription().getVersion(), newVersion)) {
                        this.latestVersion = newVersion;
                        this.downloadUrl = "https://modrinth.com/project/" + modrinthProjectId + "/version/" + newVersion;
                        plugin.getLogger().info("A new version of " + plugin.getName() + " is available: " + newVersion);
                    } else {
                        plugin.getLogger().info(plugin.getName() + " is up to date.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("An error occurred while checking for updates: " + e.getMessage());
            }
        });
    }

    public void notifyPlayer(Player player) {
        if (latestVersion != null && downloadUrl != null) {
            Component message = Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(plugin.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text("A new version is available: ", NamedTextColor.YELLOW))
                    .append(Component.text(latestVersion, NamedTextColor.GREEN))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to open the download page", NamedTextColor.AQUA)))
                    .clickEvent(ClickEvent.openUrl(downloadUrl))
                    .build();

            player.sendMessage(message);
        }
    }

    private boolean isNewer(String currentVersion, String latestVersion) {
        currentVersion = currentVersion.replaceAll("[^\\d.]", "");
        latestVersion = latestVersion.replaceAll("[^\\d.]", "");

        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false;
    }
}
