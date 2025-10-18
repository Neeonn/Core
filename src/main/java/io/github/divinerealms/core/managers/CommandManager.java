package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.commands.BukkitCommandWrapper;
import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.ActionHandler;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CommandManager {
  @Getter private final Map<String, String> customCommands = new HashMap<>();

  private final ConfigManager configManager;
  private final Logger logger;
  private final Plugin plugin;
  private final ActionHandler actionHandler;

  private CommandMap commandMap;

  public CommandManager(CoreManager coreManager) {
    this.configManager = coreManager.getConfigManager();
    this.logger = coreManager.getLogger();
    this.plugin = coreManager.getPlugin();
    this.actionHandler = coreManager.getActionHandler();

    setupCommandMap();
    loadCustomCommands();

    logger.info("&a✔ &9Registered &e" + customCommands.size() + " &9custom commands.");
  }

  private void setupCommandMap() {
    try {
      Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      commandMap = (CommandMap) field.get(plugin.getServer());
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to access CommandMap for custom commands!", exception);
    }
  }

  private void loadCustomCommands() {
    customCommands.clear();
    var config = configManager.getConfig("commands.yml");
    if (config == null) return;

    var commandSection = config.getConfigurationSection("commands");
    if (commandSection == null) return;

    commandSection.getKeys(false).forEach(commandKey -> {
      ConfigurationSection cmdSec = commandSection.getConfigurationSection(commandKey);
      if (cmdSec == null) return;

      String commandName = commandKey.toLowerCase();
      String permission = cmdSec.getString("permission", "");
      List<String> actions = new ArrayList<>();
      String singleAction = cmdSec.getString("action");
      if (singleAction != null && !singleAction.isEmpty()) actions.add(singleAction);
      List<String> actionList = cmdSec.getStringList("actions");
      if (actionList != null && !actionList.isEmpty()) actions.addAll(actionList);

      registerCustomCommand(commandName, permission, actions);
      customCommands.put(commandName, permission);
    });
  }

  private void registerCustomCommand(String commandName, String permission, List<String> actions) {
    if (commandMap == null) return;

    BukkitCommandWrapper cmd = new BukkitCommandWrapper(commandName, (sender, command, label, args) -> {
      if (!(sender instanceof Player)) { logger.send(sender, Lang.INGAME_ONLY.replace(null)); return true; }

      Player player = (Player) sender;
      if (!permission.isEmpty() && !player.hasPermission(permission)) {
        logger.send(player, Lang.NO_PERM.replace(new String[]{permission, label}));
        return true;
      }

      actionHandler.handleActions(player, actions);
      return true;
    }, null);

    commandMap.register(plugin.getDescription().getName(), cmd);
  }

  public void reloadCommands() {
    configManager.reloadConfig("commands.yml");
    unregisterCustomCommands();
    loadCustomCommands();
    logger.info("&a✔ &9Reloaded &e" + customCommands.size() + " &9custom commands.");
  }

  private void unregisterCustomCommands() {
    if (commandMap == null) return;

    try {
      Field field = commandMap.getClass().getDeclaredField("knownCommands");
      field.setAccessible(true);
      Map<String, Command> knownCommands = (Map<String, Command>) field.get(commandMap);

      for (String cmdName : new ArrayList<>(customCommands.keySet())) {
        Command cmd = knownCommands.remove(cmdName.toLowerCase());
        if (cmd != null) cmd.unregister(commandMap);
        knownCommands.remove(plugin.getDescription().getName().toLowerCase() + ":" + cmdName.toLowerCase());
      }
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to unregister custom commands!", exception);
    }
  }
}
