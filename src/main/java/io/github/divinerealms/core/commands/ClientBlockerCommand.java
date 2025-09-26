package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClientBlockerCommand implements CommandExecutor, TabCompleter {
  private final Logger logger;
  private final ClientBlocker clientBlocker;

  public ClientBlockerCommand(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.clientBlocker = coreManager.getClientBlocker();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      String status = clientBlocker.isEnabled() ? Lang.ON.replace(null) : Lang.OFF.replace(null);
      logger.send(sender, Lang.CLIENT_BLOCKER_TOGGLE.replace(new String[]{status}) + Lang.CLIENT_BLOCKER_USAGE.replace(null));
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "toggle":
        boolean disabled = clientBlocker.toggle();
        String status = disabled ? Lang.ON.replace(null) : Lang.OFF.replace(null);
        logger.send(sender, Lang.CLIENT_BLOCKER_TOGGLE.replace(new String[]{status}));
        return true;

      case "check":
        if (args.length < 2) {
          logger.send(sender, Lang.CLIENT_BLOCKER_USAGE.replace(null));
          return true;
        }
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
          logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{targetName}));
          return true;
        }

        String brand = clientBlocker.getBrand(target);
        if (brand == null) brand = "&8&o[UNKNOWN]";
        boolean blocked = clientBlocker.shouldKick(target);
        logger.send(sender, Lang.CLIENT_BLOCKER_CHECK_RESULT.replace(new String[]{target.getName(), (blocked ? "&c" : "&a") + brand}));
        return true;

      default:
        logger.send(sender, Lang.UNKNOWN_COMMAND.replace(null));
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.addAll(Arrays.asList("toggle", "check"));
    } else if (args.length == 2) {
      if (args[0].equalsIgnoreCase("check")) {
        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
      }
    }

    // Filter by what the user has already typed
    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      Collections.sort(completions);
    }

    return completions;
  }
}
