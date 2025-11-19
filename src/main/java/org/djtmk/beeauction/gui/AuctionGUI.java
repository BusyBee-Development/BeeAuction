package org.djtmk.beeauction.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AuctionGUI {

    private final Player player;
    private int page = 0;

    public AuctionGUI(Player player) {
        this.player = player;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 54, "BeeAuction - Page " + (page + 1));

        // Add items to the inventory...

        // Add navigation buttons
        ItemStack prevPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevPage.getItemMeta();
        prevMeta.setDisplayName("Previous Page");
        prevPage.setItemMeta(prevMeta);

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName("Next Page");
        nextPage.setItemMeta(nextMeta);

        inv.setItem(45, prevPage);
        inv.setItem(53, nextPage);

        player.openInventory(inv);
    }

    public void nextPage() {
        page++;
        open();
    }

    public void prevPage() {
        if (page > 0) {
            page--;
            open();
        }
    }
}
