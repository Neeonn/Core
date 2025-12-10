package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ResultManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.divinerealms.core.utilities.Permissions.PERM_RESULT_MAIN;

public class ResultCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final ResultManager resultManager;
  private final Logger logger;

  public ResultCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.resultManager = coreManager.getResultManager();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_RESULT_MAIN)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_RESULT_MAIN, label})); return true; }
    if (args.length == 0) { logger.send(sender, Lang.RESULT_HELP.replace(null)); return true; }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "start":
        if (resultManager.isMatchRunning()) { logger.send(sender, Lang.RESULT_MATCH_RUNNING.replace(null)); return true; }
        if (resultManager.getWarp() == null) {
          resultManager.setWarp(resultManager.getHome());
          logger.send(sender, Lang.RESULT_WARP_MISSING.replace(null));
          return true;
        }

        resultManager.startMatch(sender);
        break;

      case "stop":
        if (!resultManager.isMatchRunning()) { logger.send(sender, Lang.RESULT_STATUS_NONE.replace(null)); return true; }
        resultManager.stopMatch();
        break;

      case "teams":
        if (args.length == 3) resultManager.setTeams(sender, args[1], args[2]);
        else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "prefix":
        if (args.length >= 2) resultManager.setPrefix(sender, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "setwarp":
      case "sw":
        if (args.length == 2) { resultManager.setWarp(args[1]); logger.send(sender, Lang.RESULT_WARP_SET.replace(new String[]{args[1].toUpperCase()})); }
        else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "time":
        if (args.length == 2) {
          try {
            resultManager.setTime(sender, resultManager.parseTime(args[1]));
          } catch (NumberFormatException exception) {
            logger.send(sender, Lang.RESULT_MATCH_INVALID_TIME.replace(null));
            logger.info("Error: " + exception.getMessage());
          }
        } else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "add":
        if (!resultManager.isMatchRunning()) { logger.send(sender, Lang.RESULT_STATUS_NONE.replace(null)); return true; }
        if (args.length == 3 || args.length == 4) resultManager.addScore(sender, args[1], args[2], args.length == 4 ? args[3] : null);
        else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "remove":
      case "rm":
        if (!resultManager.isMatchRunning()) { logger.send(sender, Lang.RESULT_STATUS_NONE.replace(null)); return true; }
        if (args.length == 2) resultManager.removeScore(sender, args[1]);
        else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "extratime":
      case "extend":
      case "et":
        if (!resultManager.isMatchRunning()) { logger.send(sender, Lang.RESULT_STATUS_NONE.replace(null)); return true; }
        if (args.length == 2) resultManager.addExtraTime(sender, args[1]);
        else logger.send(sender, Lang.RESULT_HELP.replace(null));
        break;

      case "stophalf":
      case "sh":
      case "pause":
        if (!resultManager.isMatchRunning()) { logger.send(sender, Lang.RESULT_STATUS_NONE.replace(null)); return true; }
        resultManager.stopHalf(sender);
        break;

      case "status":
      default:
        logger.send(sender, resultManager.getMatchStatus());
        break;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_RESULT_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();
    String sub = args.length > 0 ? args[0].toLowerCase() : "";

    if (args.length == 1) {
      completions.addAll(Arrays.asList(
          "start", "stop", "teams", "prefix", "time", "add", "remove", "rm", "setwarp",
          "sw", "extratime", "extend", "et", "stophalf", "sh", "pause", "status"
      ));
    } else if (args.length == 2) {
      switch (sub) {
        case "teams":
          completions.addAll(coreManager.getRostersManager().getAllRosterNames());
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
      }
    } else if (args.length == 3) {
      if ("add".equals(sub)) coreManager.getCachedPlayers().forEach(player -> completions.add(player.getName()));
      else if ("teams".equals(sub)) completions.addAll(coreManager.getRostersManager().getAllRosterNames());
    } else if (args.length == 4) {
      if ("add".equals(sub)) coreManager.getCachedPlayers().forEach(player -> completions.add(player.getName()));
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}