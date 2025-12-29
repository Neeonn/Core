package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.PlaytimeManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Permissions.*;

public class PlaytimeCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Logger logger;
  private final PlaytimeManager playtimeManager;

  public PlaytimeCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.playtimeManager = coreManager.getPlaytimeManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_PLAYTIME_MAIN)) {
      logger.send(sender, NO_PERM, PERM_PLAYTIME_MAIN, label);
      return true;
    }

    if (args.length == 0) {
      if (!(sender instanceof Player)) {
        logger.send(sender, INGAME_ONLY);
        return true;
      }

      Player player = (Player) sender;
      long ticks = player.getStatistic(Statistic.PLAY_ONE_TICK);
      String formatted = playtimeManager.formatPlaytime(ticks);
      logger.send(sender, PLAYTIME_SELF, formatted);
      return true;
    }

    String sub = args[0].toLowerCase();
    if (sub.equalsIgnoreCase("?") || sub.equalsIgnoreCase("help")) {
      logger.send(sender, PLAYTIME_HELP);
      return true;
    } else {
      if (sub.equalsIgnoreCase("top")) {
        if (!sender.hasPermission(PERM_PLAYTIME_TOP)) {
          logger.send(sender, NO_PERM, PERM_PLAYTIME_TOP, label + " " + sub);
          return true;
        }

        int page = 1, limit = 10;

        if (args.length >= 2) {
          try {
            page = Math.max(1, Integer.parseInt(args[1]));
          } catch (NumberFormatException ignored) {
          }
        }
        if (args.length >= 3) {
          try {
            limit = Math.max(1, Integer.parseInt(args[2]));
          } catch (NumberFormatException ignored) {
          }
        }

        showTop(sender, page, limit);
        return true;
      }
    }

    if (!sender.hasPermission(PERM_PLAYTIME_OTHER)) {
      logger.send(sender, NO_PERM, PERM_PLAYTIME_OTHER, label + " " + sub);
      return true;
    }

    //noinspection deprecation
    OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
    if (!target.hasPlayedBefore()) {
      logger.send(sender, PLAYER_NOT_FOUND, args[0]);
      return true;
    }

    long ticks;
    if (target.isOnline()) {
      ticks = ((Player) target).getStatistic(Statistic.PLAY_ONE_TICK);
    } else {
      ticks = playtimeManager.getPlaytime(target.getUniqueId());
    }

    String formatted = playtimeManager.formatPlaytime(ticks);
    logger.send(sender, PLAYTIME_OTHER, target.getName(), formatted);
    return true;
  }

  private void showTop(CommandSender sender, int page, int limit) {
    List<Map.Entry<UUID, Long>> topList = playtimeManager.getTopPlaytimes(0);
    int totalPages = (int) Math.ceil((double) topList.size() / limit);
    if (totalPages == 0) {
      totalPages = 1;
    }

    if (page > totalPages) {
      page = totalPages;
    }

    int startIndex = (page - 1) * limit;
    int endIndex = Math.min(startIndex + limit, topList.size());

    UUID senderUUID = (sender instanceof Player)
                      ? ((Player) sender).getUniqueId()
                      : null;

    logger.send(sender, PLAYTIME_TOP_HEADER.replace(
        String.valueOf(limit), String.valueOf(page), String.valueOf(totalPages)));

    int rank = startIndex + 1;
    for (int i = startIndex; i < endIndex; i++) {
      Map.Entry<UUID, Long> entry = topList.get(i);
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
      String name = (offlinePlayer != null && offlinePlayer.getName() != null)
                    ? offlinePlayer.getName()
                    : entry.getKey().toString();
      String message = PLAYTIME_TOP_ENTRY.replace(
          String.valueOf(rank), name, playtimeManager.formatPlaytime(entry.getValue()));
      if (senderUUID != null && senderUUID.equals(entry.getKey())) {
        message = message + "&a <--";
      }

      logger.send(sender, message);
      rank++;
    }

    if (senderUUID != null) {
      int senderRank = 1;
      for (Map.Entry<UUID, Long> entry : topList) {
        if (senderUUID.equals(entry.getKey())) {
          break;
        }
        senderRank++;
      }
      if (senderRank < startIndex + 1 || senderRank > endIndex) {
        long ticks = playtimeManager.getPlaytime(senderUUID);
        logger.send(sender, "&7 . . .");
        logger.send(sender, PLAYTIME_TOP_ENTRY.replace(
            String.valueOf(senderRank), sender.getName(), playtimeManager.formatPlaytime(ticks)));
      }
    }

    logger.send(sender, PLAYTIME_TOP_FOOTER);
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_PLAYTIME_MAIN)) {
      return Collections.emptyList();
    }

    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission(PERM_PLAYTIME_OTHER)) {
      coreManager.getCachedPlayers().forEach(player -> completions.add(player.getName()));
      completions.addAll(Arrays.asList("top", "?", "help"));
    } else {
      if (args.length == 2 && args[0].equalsIgnoreCase("top") && sender.hasPermission(PERM_PLAYTIME_TOP)) {
        completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
      } else {
        if (args.length == 3 && args[0].equalsIgnoreCase("top") && sender.hasPermission(PERM_PLAYTIME_TOP)) {
          completions.addAll(Arrays.asList("5", "10", "15", "20"));
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