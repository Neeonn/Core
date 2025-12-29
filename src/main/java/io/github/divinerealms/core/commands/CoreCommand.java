package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Permissions.PERM_ADMIN_MAIN;
import static io.github.divinerealms.core.utilities.Permissions.PERM_ADMIN_RELOAD;

public class CoreCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Logger logger;

  public CoreCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_ADMIN_MAIN)) {
      logger.send(sender, NO_PERM, PERM_ADMIN_MAIN, label);
      return true;
    }

    if (args.length == 0) {
      logger.send(sender, HELP);
      return true;
    }

    String sub = args[0].toLowerCase();
    if (sub.equalsIgnoreCase("reload")) {
      if (args.length == 1) {
        logger.send(sender, HELP);
        return true;
      }

      if (!sender.hasPermission(PERM_ADMIN_RELOAD)) {
        logger.send(sender, NO_PERM, PERM_ADMIN_RELOAD, label + " " + sub);
        return true;
      }

      switch (args[1].toLowerCase()) {
        case "menus":
          coreManager.getGuiManager().reloadMenus();
          logger.send(sender, ADMIN_RELOAD, "menus");
          return true;

        case "configs":
          coreManager.getConfigManager().reloadAllConfigs();
          logger.send(sender, ADMIN_RELOAD, "configs");
          return true;

        case "commands":
          coreManager.getCommandManager().reloadCommands();
          logger.send(sender, ADMIN_RELOAD, "custom commands");
          return true;

        case "books":
          coreManager.getBookManager().reloadBooks();
          logger.send(sender, ADMIN_RELOAD, "books");
          return true;

        case "channels":
          coreManager.getChannelManager().reloadAll();
          logger.send(sender, ADMIN_RELOAD, "channels");
          return true;

        case "rosters":
          coreManager.getRostersManager().reloadRosters();
          logger.send(sender, ADMIN_RELOAD, "rosters");
          return true;

        case "all":
          coreManager.reload();
          logger.send(sender, ADMIN_RELOAD, "everything");
          return true;

        default:
          logger.send(sender, USAGE, label + " " + sub + " <menus|configs|all>");
          return true;
      }
    }

    logger.send(sender, UNKNOWN_COMMAND);
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_ADMIN_MAIN)) {
      return Collections.emptyList();
    }

    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.add("reload");
    }
    if (args.length == 2) {
      completions.addAll(Arrays.asList("menus", "configs", "commands", "books", "channels", "all"));
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
