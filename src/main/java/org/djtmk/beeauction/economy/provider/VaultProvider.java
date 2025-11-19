package org.djtmk.beeauction.economy.provider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.djtmk.beeauction.economy.EconomyProvider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VaultProvider implements EconomyProvider {

    private final Economy economy;

    public VaultProvider(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public CompletableFuture<Boolean> has(Player player, double amount) {
        return CompletableFuture.supplyAsync(() -> economy.has(player, amount));
    }

    @Override
    public CompletableFuture<Boolean> withdraw(Player player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (!response.transactionSuccess()) {
                // Simple rollback: deposit the amount back if withdrawal failed
                economy.depositPlayer(player, amount);
                return false;
            }
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> deposit(Player player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
            return response.transactionSuccess();
        });
    }

    @Override
    public CompletableFuture<Double> getBalance(Player player) {
        return CompletableFuture.supplyAsync(() -> economy.getBalance(player));
    }
}
