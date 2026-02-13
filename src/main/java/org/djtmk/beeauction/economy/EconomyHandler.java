package org.djtmk.beeauction.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyHandler {
    private final Economy economy;

    public EconomyHandler(Economy economy) {
        this.economy = economy;
    }

    public boolean hasEnough(Player player, double amount) {
        if (economy == null || player == null) {
            return false;
        }
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || player == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || player == null || amount <= 0) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(UUID playerUuid, double amount) {
        if (economy == null || playerUuid == null || amount <= 0) {
            return false;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
        return response.transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) {
            return String.format("%,.2f", amount);
        }
        return economy.format(amount);
    }

    public double getBalance(Player player) {
        if (economy == null || player == null) {
            return 0.0;
        }
        return economy.getBalance(player);
    }
}
