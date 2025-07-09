package org.djtmk.beeauction.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.BeeAuction;

public class EconomyHandler {
    private Economy economy;
    private final BeeAuction plugin;

    public EconomyHandler(BeeAuction plugin) {
        this.plugin = plugin;
    }

    public EconomyHandler(Economy economy) {
        this.economy = economy;
        this.plugin = BeeAuction.getInstance();
    }

    public void setEconomy(Economy economy) {
        this.economy = economy;
    }

    public Economy getEconomy() {
        return economy;
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
        if (economy == null || player == null) {
            return false;
        }

        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) {
            return String.format("%.2f", amount);
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
