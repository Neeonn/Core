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

  private static final String PERM_MAIN = "core.admin";
  private static final String PERM_RELOAD = PERM_MAIN + ".reload";

  public CoreCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_MAIN, label})); return true; }
    if (args.length == 0) { logger.send(sender, Lang.HELP.replace(null)); return true; }

    String sub = args[0].toLowerCase();
    if (sub.equalsIgnoreCase("reload")) {
      if (!sender.hasPermission(PERM_RELOAD)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_RELOAD, label + " " + sub})); return true; }

      coreManager.reload();
      logger.send(sender, Lang.ADMIN_RELOAD.replace(null));
      return true;
    }

    logger.send(sender, Lang.UNKNOWN_COMMAND.replace(null));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();

    if (args.length == 1) completions.add("reload");

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
