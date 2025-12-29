package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.commands.BukkitCommandWrapper;
import io.github.divinerealms.core.gui.InventoryButton;
import io.github.divinerealms.core.gui.InventoryGUI;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.main.ListenerManager;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Constants.GUI_COOLDOWN_DURATION_MS;

public class GUIManager {
  private final CoreManager coreManager;
  private final ConfigManager configManager;
  private final ListenerManager listenerManager;
  private final Logger logger;
  private final Plugin plugin;

  @Getter
  private final Map<String, InventoryGUI> menus = new HashMap<>();
  @Getter
  private final Map<String, String> menuCommands = new HashMap<>();
  @Getter
  private final Map<String, String> menuPermissions = new HashMap<>();
  private final Map<UUID, Long> userCooldowns = new HashMap<>();

  private CommandMap commandMap;

  public GUIManager(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.configManager = coreManager.getConfigManager();
    this.listenerManager = coreManager.getListenerManager();
    this.logger = coreManager.getLogger();
    this.plugin = coreManager.getPlugin();

    setupCommandMap();
    loadMenus();

    menus.keySet().forEach(menuKey -> {
      String permission = menuPermissions.get(menuKey);
      if (permission == null) {
        permission = "core.menu." + menuKey.toLowerCase();
      }

      registerMenuCommand(menuKey, permission);
    });

    listenerManager.enableMenuListener();
  }

  private void loadMenus() {
    var config = configManager.getConfig("menus.yml");
    if (config == null) {
      return;
    }

    var menusSection = config.getConfigurationSection("menus");
    if (menusSection == null) {
      return;
    }

    menusSection.getKeys(false).forEach(menuKey -> {
      ConfigurationSection menuSec = menusSection.getConfigurationSection(menuKey);
      if (menuSec == null) {
        return;
      }

      String title = menuSec.getString("title", "Menu");
      String command = menuSec.getString("command", menuKey.toLowerCase());
      String permission = menuSec.getString("permission", "core.menu." + menuKey.toLowerCase());
      int size = menuSec.getInt("size", 9);

      InventoryGUI template = new InventoryGUI() {
        @Override
        protected Inventory createInventory() {
          return Bukkit.createInventory(this, size, logger.color(title));
        }
      };

      menuCommands.put(menuKey, command);
      menuPermissions.put(menuKey, permission);

      var buttonSec = menuSec.getConfigurationSection("items");
      if (buttonSec == null) {
        return;
      }

      buttonSec.getKeys(false).forEach(buttonKey -> {
        int slot;
        try {
          slot = Integer.parseInt(buttonKey);
        } catch (NumberFormatException exception) {
          logger.info("&cInvalid slot key '" + buttonKey + "' in menu " + menuKey);
          return;
        }

        var itemSec = buttonSec.getConfigurationSection(buttonKey);
        if (itemSec == null) {
          return;
        }

        String materialRaw = itemSec.getString("material", "STONE");
        Material mat;
        short data = 0;

        if (materialRaw.contains(":")) {
          String[] parts = materialRaw.split(":");
          mat = Material.matchMaterial(parts[0]);
          if (mat == null) {
            mat = Material.STONE;
          }
          try {
            data = Short.parseShort(parts[1]);
          } catch (NumberFormatException ignored) {
          }
        } else {
          mat = Material.matchMaterial(materialRaw);
          if (mat == null) {
            mat = Material.STONE;
          }
        }

        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
          return;
        }

        String rawName = itemSec.getString("name", "");
        List<String> rawLore = itemSec.getStringList("lore");

        meta.setDisplayName(logger.color(rawName));

        if (rawLore != null && !rawLore.isEmpty()) {
          List<String> colored = new ArrayList<>();
          for (String line : rawLore) {
            colored.add(logger.color(line));
          }

          meta.setLore(colored);
        } else {
          meta.setLore(new ArrayList<>());
        }

        for (ItemFlag flag : ItemFlag.values()) {
          meta.removeItemFlags(flag);
        }

        stack.setItemMeta(meta);

        List<String> actions = new ArrayList<>();
        String singleAction = itemSec.getString("action");
        if (singleAction != null && !singleAction.isEmpty()) {
          actions.add(singleAction);
        }

        List<String> actionList = itemSec.getStringList("actions");
        if (actionList != null && !actionList.isEmpty()) {
          actions.addAll(actionList);
        }

        InventoryButton button = new InventoryButton()
            .creator(player -> {
              ItemStack clone = stack.clone();
              if (player != null) {
                ItemMeta cloneMeta = clone.getItemMeta();
                if (cloneMeta != null) {
                  if (cloneMeta.hasDisplayName()) {
                    cloneMeta.setDisplayName(template.applyPlaceholders(player, cloneMeta.getDisplayName()));
                  }

                  if (cloneMeta.hasLore()) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : cloneMeta.getLore()) {
                      newLore.add(template.applyPlaceholders(player, line));
                    }

                    cloneMeta.setLore(newLore);
                  }
                  clone.setItemMeta(cloneMeta);
                }
              }
              return clone;
            })
            .consumer(event -> {
              if (!((event.getWhoClicked()) instanceof Player)) {
                return;
              }

              handleActions((Player) event.getWhoClicked(), actions);
            });

        template.addButton(slot, button);
      });

      menus.put(menuKey, template);
    });
  }

  private void handleActions(Player player, List<String> actions) {
    if (actions == null || actions.isEmpty()) {
      return;
    }

    for (String action : actions) {
      if (action == null || action.isEmpty()) {
        continue;
      }

      if (action.startsWith("command:")) {
        String cmd = action.substring("command:".length());
        player.performCommand(cmd);
      } else {
        if (action.startsWith("message:")) {
          logger.send(player, action.substring("message:".length()));
        } else {
          if (action.startsWith("menu:")) {
            String menu = action.substring("menu:".length());
            openMenu(player, menu);
          } else {
            if (action.equalsIgnoreCase("close")) {
              player.closeInventory();
            }
          }
        }
      }
    }
  }

  public void openMenu(Player player, String menuName) {
    InventoryGUI menu = menus.get(menuName);
    if (menu == null || player == null) {
      return;
    }

    InventoryGUI gui = new InventoryGUI() {
      @Override
      protected Inventory createInventory() {
        return Bukkit.createInventory(this, menu.getInventory().getSize(), menu.getInventory().getTitle());
      }
    };

    menu.getButtonMap().forEach(gui::addButton);
    gui.decorate(player);

    player.openInventory(gui.getInventory());
  }

  public void reloadMenus() {
    configManager.reloadConfig("menus.yml");
    unregisterMenuCommands();
    listenerManager.disableMenuListener();

    for (InventoryGUI gui : new ArrayList<>(menus.values())) {
      coreManager.getCachedPlayers().forEach(player -> {
        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null &&
            player.getOpenInventory().getTopInventory().getHolder() == gui) {
          player.closeInventory();
        }
      });
      gui.clear();
    }

    menus.clear();
    menuCommands.clear();
    menuPermissions.clear();
    userCooldowns.clear();

    loadMenus();

    menus.keySet().forEach(menuKey -> {
      String permission = menuPermissions.get(menuKey);
      if (permission == null) {
        permission = "core.menu." + menuKey.toLowerCase();
      }

      registerMenuCommand(menuKey, permission);
    });

    listenerManager.enableMenuListener();
    logger.info("&a✔ &9Registered &e" + menus.size() + " &9menus.");
  }

  private void setupCommandMap() {
    try {
      Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      commandMap = (CommandMap) field.get(plugin.getServer());
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to access CommandMap!", exception);
    }
  }

  private void registerMenuCommand(String menuKey, String permission) {
    if (commandMap == null) {
      return;
    }

    String commandName = menuCommands.get(menuKey);
    if (commandName == null || commandName.isEmpty()) {
      return;
    }

    BukkitCommandWrapper cmd = new BukkitCommandWrapper(commandName, (sender, command, label, args) -> {
      if (!(sender instanceof Player)) {
        logger.send(sender, INGAME_ONLY);
        return true;
      }

      Player player = (Player) sender;
      String perm = menuPermissions.get(menuKey);
      if (perm == null) {
        perm = "core.menu." + menuKey.toLowerCase();
      }

      if (!player.hasPermission(perm)) {
        logger.send(player, NO_PERM, commandName, permission);
        return true;
      }

      UUID playerId = player.getUniqueId();
      long now = System.currentTimeMillis();
      if (userCooldowns.containsKey(playerId)) {
        long lastUsedTime = userCooldowns.get(playerId);
        long timeSinceLastUse = now - lastUsedTime;

        if (timeSinceLastUse < GUI_COOLDOWN_DURATION_MS) {
          long remainingTimeMs = GUI_COOLDOWN_DURATION_MS - timeSinceLastUse;
          long timeLeftSeconds = (long) Math.ceil(remainingTimeMs / 1000.0);

          logger.sendActionBar(player, ANTI_SPAM_COMMANDS, String.valueOf(timeLeftSeconds));
          return true;
        }
      }

      userCooldowns.put(playerId, now);
      openMenu(player, menuKey);
      return true;
    }, null);

    commandMap.register(plugin.getDescription().getName(), cmd);
  }

  private void unregisterMenuCommands() {
    if (commandMap == null) {
      return;
    }

    try {
      Field field = commandMap.getClass().getDeclaredField("knownCommands");
      field.setAccessible(true);
      Map<String, Command> knownCommands = (Map<String, Command>) field.get(commandMap);

      for (String cmdName : new ArrayList<>(menuCommands.values())) {
        Command cmd = knownCommands.remove(cmdName.toLowerCase());
        if (cmd != null) {
          cmd.unregister(commandMap);
        }
        knownCommands.remove(plugin.getDescription().getName().toLowerCase() + ":" + cmdName.toLowerCase());
      }
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to unregister menu commands!", exception);
    }
  }
}
