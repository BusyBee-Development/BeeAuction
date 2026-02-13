package org.djtmk.beeauction.economy;

import org.djtmk.beeauction.economy.provider.MultiEconomyProvider;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class EconomyManager {

    private final EconomyProvider provider;
    public EconomyManager() {
        this.provider = new MultiEconomyProvider();
    }
    public boolean isAvailable() {
        return ((MultiEconomyProvider) provider).isAvailable();
    }
    public String getProviderName() {
        return provider.getName();
    }
    public CompletableFuture<Boolean> has(Player player, double amount) {
        return provider.has(player, amount);
    }

    public CompletableFuture<Boolean> withdraw(Player player, double amount) {
        return provider.withdraw(player, amount);
    }

    public CompletableFuture<Boolean> deposit(Player player, double amount) {
        return provider.deposit(player, amount);
    }
    public CompletableFuture<Double> getBalance(Player player) {
        return provider.getBalance(player);
    }
}
