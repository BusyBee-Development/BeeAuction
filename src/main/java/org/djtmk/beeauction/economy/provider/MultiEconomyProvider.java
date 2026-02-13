package org.djtmk.beeauction.economy.provider;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.djtmk.beeauction.economy.EconomyProvider;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class MultiEconomyProvider implements EconomyProvider {

    private static final Logger log = Logger.getLogger("Minecraft");
    private EconomyProvider activeProvider;

    public MultiEconomyProvider() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                activeProvider = new VaultProvider(rsp.getProvider());
                log.info("[BeeAuction] Vault found, using as economy provider.");
                return;
            }
        }
        log.warning("[BeeAuction] No supported economy provider found.");
    }

    @Override
    public String getName() {
        return activeProvider != null ? activeProvider.getName() : "None";
    }

    @Override
    public CompletableFuture<Boolean> has(Player player, double amount) {
        if (activeProvider == null) return CompletableFuture.completedFuture(false);
        return activeProvider.has(player, amount);
    }

    @Override
    public CompletableFuture<Boolean> withdraw(Player player, double amount) {
        if (activeProvider == null) return CompletableFuture.completedFuture(false);
        return activeProvider.withdraw(player, amount);
    }

    @Override
    public CompletableFuture<Boolean> deposit(Player player, double amount) {
        if (activeProvider == null) return CompletableFuture.completedFuture(false);
        return activeProvider.deposit(player, amount);
    }

    @Override
    public CompletableFuture<Double> getBalance(Player player) {
        if (activeProvider == null) return CompletableFuture.completedFuture(0.0);
        return activeProvider.getBalance(player);
    }

    public boolean isAvailable() {
        return activeProvider != null;
    }
}
