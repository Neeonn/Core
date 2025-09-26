package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoreCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Logger logger;

  public CoreCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      logger.send(sender, Lang.HELP.replace(null));
      return true;
    }

    if (args[0].equalsIgnoreCase("reload")) {
      if (!sender.hasPermission("core.admin.reload")) {
        logger.send(sender, Lang.NO_PERM.replace(new String[]{"core.admin.reload", "core reload"}));
        return true;
      }

      coreManager.reload();
      logger.send(sender, Lang.ADMIN_RELOAD.replace(null));
      return true;
    }

    logger.send(sender, Lang.UNKNOWN_COMMAND.replace(null));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> completions = new ArrayList<>();
      completions.add("reload");
      String typed = args[0].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(typed));
      Collections.sort(completions);
      return completions;
    }
    return Collections.emptyList();
  }
}
