package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.commands.BukkitCommandWrapper;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.ActionHandler;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static io.github.divinerealms.core.configs.Lang.*;

public class CommandManager {
  @Getter
  private final Map<String, String> customCommands = new HashMap<>();
  private final Map<String, BukkitCommandWrapper> registeredWrappers = new ConcurrentHashMap<>();
  private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

  private final CoreManager coreManager;
  private final ConfigManager configManager;
  private final Logger logger;
  private final Plugin plugin;
  private final ActionHandler actionHandler;

  private CommandMap commandMap;

  public CommandManager(CoreManager coreManager) {
    this.coreManager = coreManager;
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
    registeredWrappers.clear();

    var config = configManager.getConfig("commands.yml");
    if (config == null) {
      return;
    }

    var commandSection = config.getConfigurationSection("commands");
    if (commandSection == null) {
      return;
    }

    Set<String> presentCommands = new HashSet<>();

    commandSection.getKeys(false).forEach(commandKey -> {
      ConfigurationSection cmdSec = commandSection.getConfigurationSection(commandKey);
      if (cmdSec == null) {
        return;
      }

      String commandName = commandKey.toLowerCase();
      presentCommands.add(commandName);

      String permission = cmdSec.getString("permission", "");
      String description = cmdSec.getString("description", "");
      String usage = cmdSec.getString("usage", "/" + commandName);
      List<String> aliases = cmdSec.getStringList("aliases");
      List<String> actions = new ArrayList<>();
      List<String> actionList = cmdSec.getStringList("actions");
      if (actionList != null && !actionList.isEmpty()) {
        actions.addAll(actionList);
      }

      List<String> tabRules = cmdSec.getStringList("tab");
      String cooldownRaw = cmdSec.getString("cooldown", "");

      registerOrUpdateCustomCommand(commandName, permission, description, usage, aliases, tabRules, cooldownRaw,
          actions);
      customCommands.put(commandName, permission);
    });

    Set<String> toRemove = new HashSet<>(registeredWrappers.keySet());
    toRemove.removeAll(presentCommands);
    for (String removed : toRemove) {
      BukkitCommandWrapper wrapper = registeredWrappers.remove(removed);
      if (wrapper != null) {
        try {
          Field known = commandMap.getClass().getDeclaredField("knownCommands");
          known.setAccessible(true);
          Map<String, Command> knownMap = (Map<String, Command>) known.get(commandMap);
          knownMap.remove(removed);
          knownMap.remove(plugin.getName().toLowerCase() + ":" + removed);
        } catch (Exception exception) {
          plugin.getLogger().log(Level.WARNING,
              "Failed to fully remove command " + removed + " from CommandMap: " + exception.getMessage());
        }
      }
    }
  }

  private void registerOrUpdateCustomCommand(String commandName, String permission, String description,
                                             String usage, List<String> aliases, List<String> tabRules,
                                             String cooldownRaw, List<String> actions) {
    if (commandMap == null) {
      return;
    }

    long cooldownMillis = parseCooldownToMillis(cooldownRaw);

    CommandExecutor executor = (sender, command, label, args) -> {
      if (!(sender instanceof Player)) {
        logger.send(sender, INGAME_ONLY);
        return true;
      }

      Player player = (Player) sender;
      Player target = player;

      if (args.length > 0) {
        Player possibleTarget = plugin.getServer().getPlayerExact(args[0]);
        if (possibleTarget != null) {
          target = possibleTarget;
        }
      }

      if (!permission.isEmpty() && !player.hasPermission(permission)) {
        logger.send(player, NO_PERM, permission, label);
        return true;
      }

      if (cooldownMillis > 0) {
        Map<UUID, Long> map = cooldowns.computeIfAbsent(commandName, k -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        Long expires = map.get(player.getUniqueId());
        if (expires != null && expires > now) {
          long remaining = (expires - now) / 1000;
          logger.sendActionBar(player, ANTI_SPAM_COMMANDS, String.valueOf(remaining));
          return true;
        }

        map.put(player.getUniqueId(), now + cooldownMillis);
      }

      if (actions != null && !actions.isEmpty()) {
        actionHandler.handleActions(player, actions, args, target);
      } else {
        logger.send(player, "&cNo actions defined for this command!");
      }
      return true;
    };

    BukkitCommandWrapper wrapper = registeredWrappers.get(commandName);
    if (wrapper != null) {
      wrapper.setExecutor(executor);
      wrapper.setDescription(description);
      wrapper.setUsage(usage);
      if (aliases != null) {
        wrapper.setAliases(aliases);
      }

      wrapper.setTabCompleter(new ConfigTabCompleter(tabRules));
      return;
    }

    if (aliases == null) {
      aliases = new ArrayList<>();
    }

    BukkitCommandWrapper cmd = new BukkitCommandWrapper(commandName, executor, aliases);
    cmd.setDescription(description);
    cmd.setUsage(usage);
    cmd.setTabCompleter(new ConfigTabCompleter(tabRules));

    try {
      commandMap.register(plugin.getName().toLowerCase(), cmd);
      registeredWrappers.put(commandName, cmd);

      cmd.setAliases(aliases);
      coreManager.getRegisteredCommands().add(commandName.toLowerCase());
      aliases.forEach(alias -> coreManager.getRegisteredCommands().add(alias.toLowerCase()));
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to register custom command: " + commandName, exception);
    }
  }

  private long parseCooldownToMillis(String raw) {
    if (raw == null || raw.isEmpty()) {
      return 0;
    }

    raw = raw.trim().toLowerCase();
    try {
      if (raw.endsWith("ms")) {
        return Long.parseLong(raw.substring(0, raw.length() - 2).trim());
      }

      long singleLetter = Long.parseLong(raw.substring(0, raw.length() - 1).trim());
      if (raw.endsWith("s")) {
        return singleLetter * 1000;
      }

      if (raw.endsWith("m")) {
        return singleLetter * 60_000;
      }

      return Long.parseLong(raw) * 1000;
    } catch (NumberFormatException exception) {
      return 0;
    }
  }

  public void reloadCommands() {
    configManager.reloadConfig("commands.yml");
    loadCustomCommands();
    logger.info("&a✔ &9Reloaded &e" + customCommands.size() + " &9custom commands.");
  }
}
