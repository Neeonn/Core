package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.RostersManager;
import io.github.divinerealms.core.utilities.Logger;
import io.github.divinerealms.core.utilities.RosterInfo;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Permissions.*;

public class RostersCommand implements CommandExecutor, TabCompleter {
  private final RostersManager rostersManager;
  private final Logger logger;

  public RostersCommand(CoreManager coreManager) {
    this.rostersManager = coreManager.getRostersManager();
    this.logger = coreManager.getLogger();
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_ROSTERS_MAIN)) {
      logger.send(sender, NO_PERM, PERM_ROSTERS_MAIN, label);
      return true;
    }

    String allAvailableLeagues = String.join(", ",
        rostersManager.getAvailableLeagues()), activeLeague = rostersManager.getActiveLeague();
    if (args.length == 0) {
      logger.send(sender, ROSTERS_HELP, allAvailableLeagues, activeLeague);
      return true;
    }

    String sub = args[0].toLowerCase(), rosterName, playerName, league, newName;
    switch (sub) {
      case "list":
        league = args.length > 1
                 ? args[1]
                 : null;

        if (league != null) {
          List<String> rosterNames = rostersManager.getRostersByLeague(league);
          if (rosterNames.isEmpty()) {
            logger.send(sender, ROSTERS_LIST_LEAGUE_EMPTY, StringUtils.capitalize(league));
            return true;
          }

          logger.send(sender, ROSTERS_LIST_HEADER, StringUtils.capitalize(league));
          rosterNames.forEach(name -> {
            RosterInfo rosterList = rostersManager.getRoster(name);
            if (rosterList == null) {
              return;
            }

            logger.send(sender, ROSTERS_LIST_ENTRY.replace(rosterList.getTag(), rosterList.getLongName(),
                String.valueOf(rosterList.getMemberCount()),
                rosterList.getManager() != null
                ? ROSTERS_INFO_MANAGER_DISPLAY.replace(rosterList.getManager())
                : ""));
          });

          logger.send(sender, System.lineSeparator());
        } else {
          List<String> leagueOrder = rostersManager.getAvailableLeagues();
          logger.send(sender, ROSTERS_LIST_HEADER, "List");

          boolean foundAnyRoster = false;

          for (String type : leagueOrder) {
            List<String> rosterNames = rostersManager.getRostersByLeague(type);
            if (!rosterNames.isEmpty()) {
              foundAnyRoster = true;

              logger.send(sender, ROSTERS_LIST_LEAGUE_ENTRY, StringUtils.capitalize(type));
              rosterNames.forEach(name -> {
                RosterInfo rosterList = rostersManager.getRoster(name);
                if (rosterList == null) {
                  return;
                }

                logger.send(sender, ROSTERS_LIST_ENTRY,
                    rosterList.getTag(), rosterList.getLongName(), String.valueOf(rosterList.getMemberCount()),
                    rosterList.getManager() != null
                    ? ROSTERS_INFO_MANAGER_DISPLAY.replace(rosterList.getManager())
                    : "");
              });
              logger.send(sender, System.lineSeparator());
            }
          }

          if (!foundAnyRoster) {
            logger.send(sender, ROSTERS_LIST_ALL_EMPTY);
          }
        }

        logger.send(sender, ROSTERS_INFO_FOOTER, rostersManager.getActiveLeague());
        return true;

      case "create":
        if (!sender.hasPermission(PERM_ROSTERS_CREATE)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_CREATE, label + " " + sub);
          return true;
        }

        if (args.length < 4) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        rosterName = args[1];
        String tag = args[2];
        league = args[3];
        if (!rostersManager.getAvailableLeagues().contains(league.toLowerCase())) {
          logger.send(sender, ROSTERS_LEAGUE_INVALID, allAvailableLeagues);
          return true;
        }

        if (rostersManager.rosterExists(rosterName)) {
          logger.send(sender, ROSTERS_EXISTS, rosterName);
          return true;
        }

        if (rostersManager.createRoster(rosterName, tag, league)) {
          logger.send(PERM_ROSTERS_NOTIFY,
              ROSTERS_CREATE, rosterName.toUpperCase(), tag, league.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_FAIL_GENERIC);
        return true;

      case "delete":
        if (!sender.hasPermission(PERM_ROSTERS_DELETE)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_DELETE, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        rosterName = args[1];
        if (!rostersManager.rosterExists(rosterName)) {
          logger.send(sender, ROSTERS_NOT_FOUND, rosterName);
          return true;
        }

        if (rostersManager.deleteRoster(rosterName)) {
          logger.send(PERM_ROSTERS_NOTIFY, ROSTERS_DELETE, rosterName.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_FAIL_GENERIC);
        return true;

      case "add":
        if (!sender.hasPermission(PERM_ROSTERS_ADD)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_ADD, label + " " + sub);
          return true;
        }

        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        rosterName = args[1];
        playerName = args[2];

        if (!rostersManager.rosterExists(rosterName)) {
          logger.send(sender, ROSTERS_NOT_FOUND, rosterName);
          return true;
        }

        OfflinePlayer addTarget = Bukkit.getOfflinePlayer(playerName);
        if (!addTarget.hasPlayedBefore()) {
          logger.send(sender, PLAYER_NOT_FOUND, playerName);
          return true;
        }

        if (rostersManager.addPlayerToRoster(rosterName, addTarget.getName())) {
          logger.send(PERM_ROSTERS_NOTIFY,
              ROSTERS_ADD, playerName, rosterName.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_FAIL_GENERIC);
        return true;

      case "remove":
        if (!sender.hasPermission(PERM_ROSTERS_REMOVE)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_REMOVE, label + " " + sub);
          return true;
        }
        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        playerName = args[1];
        league = args[2];
        OfflinePlayer removeTarget = Bukkit.getOfflinePlayer(playerName);
        if (!removeTarget.hasPlayedBefore()) {
          logger.send(sender, PLAYER_NOT_FOUND, playerName);
          return true;
        }

        if (rostersManager.removePlayerFromRoster(removeTarget.getName(), league)) {
          logger.send(PERM_ROSTERS_NOTIFY, ROSTERS_REMOVE, playerName, league.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_PLAYER_NOT_IN_ROSTER, playerName, league.toUpperCase());
        return true;

      case "view":
      case "info":
        if (args.length < 2) {
          if (!(sender instanceof Player)) {
            logger.send(sender, USAGE, label + " " + sub + " <roster>");
            return true;
          }

          Player player = (Player) sender;
          RosterInfo rosterInfo = rostersManager.getPlayerRoster(player);
          if (rosterInfo == null) {
            logger.send(player, ROSTERS_NOT_IN_ANY);
            return true;
          }

          rosterName = rosterInfo.getName();
        } else {
          rosterName = args[1];
        }

        List<String> info = rostersManager.getRosterInfo(rosterName);
        if (info.isEmpty()) {
          logger.send(sender, ROSTERS_NOT_FOUND, rosterName);
          return true;
        }

        info.forEach(line -> logger.send(sender, line));
        return true;

      case "manager":
      case "setmanager":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        playerName = args[1];
        league = args[2];
        OfflinePlayer managerTarget = Bukkit.getOfflinePlayer(playerName);
        if (!managerTarget.hasPlayedBefore()) {
          logger.send(sender, PLAYER_NOT_FOUND, playerName);
          return true;
        }

        RosterInfo rosterInfo = rostersManager.getPlayerRoster(managerTarget.getName(), league);
        if (rosterInfo == null) {
          logger.send(sender, ROSTERS_PLAYER_NOT_IN_ROSTER, playerName, league);
          return true;
        }

        if (rostersManager.setManager(managerTarget.getName(), league)) {
          logger.send(PERM_ROSTERS_NOTIFY, ROSTERS_MANAGER_SET.replace(
              playerName, rosterInfo.getTag() + " &f" + rosterInfo.getName()));
          return true;
        }

        logger.send(sender, ROSTERS_MANAGER_FAIL);
        return true;

      case "name":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        rosterName = args[1];
        newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (!rostersManager.rosterExists(rosterName)) {
          logger.send(sender, ROSTERS_NOT_FOUND, rosterName);
          return true;
        }

        if (rostersManager.updateLongName(rosterName, newName)) {
          logger.send(PERM_ROSTERS_NOTIFY,
              ROSTERS_SET, "name", newName, rosterName.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_FAIL_GENERIC);
        return true;

      case "tag":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        rosterName = args[1];
        String newTag = args[2];

        if (!rostersManager.rosterExists(rosterName)) {
          logger.send(sender, ROSTERS_NOT_FOUND, rosterName);
          return true;
        }

        if (rostersManager.updateTag(rosterName, newTag)) {
          logger.send(PERM_ROSTERS_NOTIFY,
              ROSTERS_SET, "tag", newTag, rosterName.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_FAIL_GENERIC);
        return true;

      case "league":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        rosterName = args[1];
        String newLeague = args[2];
        if (!rostersManager.getAvailableLeagues().contains(newLeague.toLowerCase())) {
          logger.send(sender, ROSTERS_LEAGUE_INVALID, allAvailableLeagues);
          return true;
        }

        if (!rostersManager.rosterExists(rosterName)) {
          logger.send(sender, ROSTERS_NOT_FOUND, rosterName);
          return true;
        }

        if (rostersManager.updateLeague(rosterName, newLeague)) {
          logger.send(PERM_ROSTERS_NOTIFY,
              ROSTERS_SET, "league", newLeague.toUpperCase(), rosterName.toUpperCase());
          return true;
        }

        logger.send(sender, ROSTERS_FAIL_GENERIC);
        return true;

      case "switch":
      case "setleague":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        String switchedLeague = args[1];
        if (!rostersManager.getAvailableLeagues().contains(switchedLeague.toLowerCase())) {
          logger.send(sender, ROSTERS_LEAGUE_INVALID, allAvailableLeagues);
          return true;
        }

        rostersManager.setActiveLeague(switchedLeague);
        logger.send(PERM_ROSTERS_NOTIFY,
            ROSTERS_LEAGUE_SWITCHED, switchedLeague.toUpperCase());
        return true;

      case "addleague":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        league = args[1];
        if (rostersManager.getAvailableLeagues().contains(league.toLowerCase())) {
          logger.send(sender, ROSTERS_LEAGUE_INVALID, allAvailableLeagues);
          return true;
        }

        rostersManager.addAvailableLeague(league);
        logger.send(PERM_ROSTERS_NOTIFY, ROSTERS_LEAGUE_ADDED, league.toUpperCase());
        return true;

      case "removeleague":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        league = args[1];
        if (!rostersManager.getAvailableLeagues().contains(league.toLowerCase())) {
          logger.send(sender, ROSTERS_LEAGUE_NOT_FOUND, league);
          return true;
        }

        if (!rostersManager.getRostersByLeague(league).isEmpty()) {
          logger.send(sender, ROSTERS_LEAGUE_NON_EMPTY_REMOVE, league.toUpperCase());
          return true;
        }

        rostersManager.removeAvailableLeague(league);
        logger.send(PERM_ROSTERS_NOTIFY, ROSTERS_LEAGUE_REMOVED, league.toUpperCase());
        return true;

      case "renameleague":
        if (!sender.hasPermission(PERM_ROSTERS_SET)) {
          logger.send(sender, NO_PERM, PERM_ROSTERS_SET, label + " " + sub);
          return true;
        }

        if (args.length < 3) {
          logger.send(sender, ROSTERS_USAGE);
          return true;
        }

        String oldName = args[1];
        newName = args[2];

        if (rostersManager.renameLeague(oldName, newName)) {
          logger.send(PERM_ROSTERS_NOTIFY,
              ROSTERS_LEAGUE_RENAMED, oldName.toUpperCase(), newName.toUpperCase());
        } else {
          logger.send(sender, ROSTERS_LEAGUE_RENAME_FAIL);
        }
        return true;

      case "help":
      case "?":
        logger.send(sender, ROSTERS_HELP, allAvailableLeagues, activeLeague);
        return true;

      default:
        logger.send(sender, ROSTERS_USAGE);
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_ROSTERS_MAIN)) {
      return Collections.emptyList();
    }

    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.addAll(Arrays.asList("list", "info", "create", "delete", "add",
          "remove", "setmanager", "manager", "name", "tag", "league", "switch",
          "addleague", "removeleague", "renameleague", "reload", "help"));
    } else {
      if (args.length == 2) {
        String sub = args[0].toLowerCase();
        switch (sub) {
          case "list":
          case "switch":
          case "setleague":
          case "removeleague":
          case "renameleague":
            completions.addAll(rostersManager.getAvailableLeagues());
            break;

          case "delete":
          case "info":
          case "view":
          case "tag":
          case "name":
          case "league":
          case "add":
            completions.addAll(rostersManager.getAllRosterNames());
            break;

          case "remove":
          case "manager":
          case "setmanager":
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            break;
        }
      } else {
        if (args.length == 3) {
          String sub = args[0].toLowerCase();
          switch (sub) {
            case "add":
              completions.addAll(Arrays.stream(Bukkit.getOfflinePlayers()).filter(OfflinePlayer::hasPlayedBefore).map(
                  OfflinePlayer::getName).collect(Collectors.toSet()));
              break;

            case "remove":
            case "manager":
            case "setmanager":
            case "league":
            case "renameleague":
              completions.addAll(rostersManager.getAvailableLeagues());
              break;
          }
        } else {
          if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(rostersManager.getAvailableLeagues());
          }
        }
      }
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
