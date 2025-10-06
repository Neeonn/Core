package io.github.divinerealms.core.gui;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class InventoryGUI implements InventoryHandler, InventoryHolder {
  @Getter private final Inventory inventory;
  @Getter private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();

  public InventoryGUI() {
    this.inventory = this.createInventory();
  }

  public void addButton(int slot, InventoryButton button) {
    this.buttonMap.put(slot, button);
  }

  public void decorate(Player player) {
    this.buttonMap.forEach((slot, button) -> {
      if (slot < 0 || slot >= this.inventory.getSize()) return;

      Function<Player, ItemStack> creator = button.getIconCreator();
      if (creator == null) return;

      ItemStack stack = creator.apply(player);
      if (stack == null) return;

      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
        if (meta.hasDisplayName()) meta.setDisplayName(applyPlaceholders(player, meta.getDisplayName()));
        if (meta.hasLore()) {
          List<String> newLore = new ArrayList<>();
          for (String line : meta.getLore()) newLore.add(applyPlaceholders(player, line));
          meta.setLore(newLore);
        }
        stack.setItemMeta(meta);
      }

      this.inventory.setItem(slot, stack);
    });
  }

  public String applyPlaceholders(Player player, String text) {
    if (player == null) return text;
    try {
      return PlaceholderAPI.setPlaceholders(player, text);
    } catch (Throwable ignored) {
      return text;
    }
  }

  @Override
  public void onClick(InventoryClickEvent event) {
    event.setCancelled(true);
    int slot = event.getSlot();
    InventoryButton button = this.buttonMap.get(slot);
    if (button != null && button.getEventConsumer() != null) button.getEventConsumer().accept(event);
  }

  @Override
  public void onOpen(InventoryOpenEvent event) {
    this.decorate((Player) event.getPlayer());
  }

  @Override
  public void onClose(InventoryCloseEvent event) {
  }

  public void clear() {
    this.buttonMap.clear();
    this.inventory.clear();
  }

  protected abstract Inventory createInventory();
}