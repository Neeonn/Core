package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ResultManager;
import io.github.divinerealms.core.utilities.Logger;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResultCommand implements CommandExecutor, TabCompleter {
  private final ResultManager resultManager;
  private final LuckPerms luckPerms;
  private final Logger logger;

  private static final String PERM_MAIN = "core.result";

  public ResultCommand(CoreManager coreManager) {
    this.resultManager = coreManager.getResultManager();
    this.luckPerms = coreManager.getLuckPerms();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_MAIN, label})); return true; }
    if (args.length == 0) { logger.send(sender, Lang.RESULT_HELP.replace(null)); return true; }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "start": resultManager.startMatch(sender); break;
      case "stop": resultManager.stopMatch(); break;
      case "teams": if (args.length >= 3) resultManager.setTeams(sender, args[1], args[2]); break;
      case "prefix": if (args.length >= 2) resultManager.setPrefix(sender, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))); break;
      case "time":
        if (args.length >= 2) {
          try {
            resultManager.setTime(sender, resultManager.parseTime(args[1]));
          } catch (NumberFormatException exception) {
            logger.send(sender, Lang.RESULT_MATCH_INVALID_TIME.replace(null));
            logger.info("Error: " + exception.getMessage());
          }
        }
        break;
      case "add": if (args.length >= 3) resultManager.addScore(sender, args[1], args[2], args.length >= 4 ? args[3] : null); break;
      case "remove": if (args.length >= 2) resultManager.removeScore(sender, args[1]); break;
      case "extratime":
      case "extend": if (args.length >= 2) resultManager.addExtraTime(sender, args[1]); break;
      case "stophalf": resultManager.stopHalf(sender); break;
      case "status":
      default: logger.send(sender, resultManager.getMatchStatus()); break;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();
    String sub = args.length > 0 ? args[0].toLowerCase() : "";

    if (args.length == 1) {
      completions.addAll(Arrays.asList(
          "start", "stop", "teams", "prefix", "time", "add", "remove",
          "extratime", "extend", "stophalf", "status"
      ));
    } else if (args.length == 2) {
      switch (sub) {
        case "teams":
          completions.addAll(luckPerms.getGroupManager().getLoadedGroups().stream()
              .filter(g -> g.getWeight().orElse(0) == 200)
              .map(g -> g.getName().toUpperCase())
              .collect(Collectors.toList())
          );
          break;
        case "add":
        case "remove":
          completions.addAll(Arrays.asList("home", "away"));
          break;
        case "time":
        case "extratime":
        case "extend":
          completions.addAll(Arrays.asList("10s", "30s", "1m", "5m", "10m"));
          break;
        case "prefix":
          break;
      }
    } else if (args.length == 3) {
      if ("add".equals(sub)) {
        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
      } else if ("teams".equals(sub)) {
        completions.addAll(luckPerms.getGroupManager().getLoadedGroups().stream()
            .filter(g -> g.getWeight().orElse(0) == 200)
            .map(g -> g.getName().toUpperCase())
            .collect(Collectors.toList())
        );
      }
    } else if (args.length == 4) {
      if ("add".equals(sub)) Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}