package io.github.divinerealms.core.listeners;

import io.github.divinerealms.core.gui.InventoryGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class GUIListener implements Listener {
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getInventory().getHolder() instanceof InventoryGUI) {
      InventoryGUI gui = (InventoryGUI) event.getInventory().getHolder();
      gui.onClick(event);
    }
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (event.getInventory().getHolder() instanceof InventoryGUI) {
      InventoryGUI gui = (InventoryGUI) event.getInventory().getHolder();
      gui.onOpen(event);
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getInventory().getHolder() instanceof InventoryGUI) {
      InventoryGUI gui = (InventoryGUI) event.getInventory().getHolder();
      gui.onClose(event);
    }
  }
}