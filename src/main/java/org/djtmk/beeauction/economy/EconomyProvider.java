package org.djtmk.beeauction.economy;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface EconomyProvider {

    String getName();

    CompletableFuture<Boolean> has(Player player, double amount);

    CompletableFuture<Boolean> withdraw(Player player, double amount);

    CompletableFuture<Boolean> deposit(Player player, double amount);

    CompletableFuture<Double> getBalance(Player player);
}
