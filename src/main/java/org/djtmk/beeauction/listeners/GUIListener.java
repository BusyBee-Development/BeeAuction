package org.djtmk.beeauction.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.djtmk.beeauction.gui.AuctionGUI;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("BeeAuction")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        AuctionGUI gui = new AuctionGUI(player);

        if (event.getSlot() == 45) {
            gui.prevPage();
        } else if (event.getSlot() == 53) {
            gui.nextPage();
        }
    }
}
