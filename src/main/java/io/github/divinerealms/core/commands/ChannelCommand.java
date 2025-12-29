package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Permissions.*;

public class ChannelCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Logger logger;
  private final ChannelManager channelManager;

  public ChannelCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      logger.send(sender, CHANNEL_HELP);
      return true;
    }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "toggle":
      case "t":
        if (args.length < 2) {
          logger.send(sender, CHANNEL_HELP);
          return true;
        }

        if (!sender.hasPermission(PERM_CHANNEL_TOGGLE)) {
          logger.send(sender, NO_PERM, PERM_CHANNEL_TOGGLE, label + " " + sub);
          return true;
        }

        boolean disabled = channelManager.toggleChannel(args[1]);
        String toggledStatus = disabled
                               ? OFF.toString()
                               : ON.toString();

        logger.broadcast(CHANNEL_DISABLED_BROADCAST,
            args[1].toUpperCase(), toggledStatus, sender.getName()
        );
        break;

      case "list":
      case "l":
        if (!sender.hasPermission(PERM_CHANNEL_LIST)) {
          logger.send(sender, NO_PERM, PERM_CHANNEL_LIST, label + " " + sub);
          return true;
        }

        if (args.length != 1) {
          logger.send(sender, CHANNEL_HELP);
          return true;
        }

        Set<String> channelNames = channelManager.getChannels().keySet();
        List<String> sortedChannels = channelNames.stream().map(String::toUpperCase).sorted().collect(
            Collectors.toList());

        logger.send(sender, CHANNEL_LIST, String.join("&7, &e", sortedChannels));
        break;

      case "info":
      case "i":
        if (!sender.hasPermission(PERM_CHANNEL_INFO)) {
          logger.send(sender, NO_PERM, PERM_CHANNEL_INFO, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, CHANNEL_HELP);
          return true;
        }

        String channelName = args[1];
        ChannelInfo info = channelManager.getChannels().get(channelName.toLowerCase());
        if (info == null) {
          logger.send(sender, CHANNEL_NOT_FOUND, channelName);
          return true;
        }

        String disabledStatus = channelManager.isChannelDisabled(channelName)
                                ? OFF.toString()
                                : ON.toString();

        String hasPermission = info.permission != null && !info.permission.isEmpty()
                               ? (sender.hasPermission("core.channel." + info.permission)
                                  ? "&a" + info.permission
                                  : "&c" + info.permission + NO_PERM_SHORT)
                               : NO.toString();

        String isBroadcast = info.broadcast
                             ? YES.toString()
                             : NO.toString();

        String definedDiscordId = info.discordId != null && !info.discordId.isEmpty()
                                  ? info.discordId
                                  : UNDEFINED.toString();

        Set<UUID> subscribers = channelManager.getSubscribers(channelName);
        String subscribersList = subscribers.stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).filter(
            Objects::nonNull).collect(Collectors.joining("&7, &f"));
        if (subscribersList.isEmpty()) {
          subscribersList = "&7---";
        }

        logger.send(sender, CHANNEL_INFO,
            channelName.toUpperCase(), disabledStatus,
            hasPermission, isBroadcast, definedDiscordId,
            subscribersList
        );
        break;

      case "switch":
      case "join":
      case "j":
      case "leave":
      case "s":
        if (!(sender instanceof Player)) {
          logger.send(sender, INGAME_ONLY);
          return true;
        }

        if (!sender.hasPermission(PERM_CHANNEL_SWITCH)) {
          logger.send(sender, NO_PERM, PERM_CHANNEL_SWITCH, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, CHANNEL_HELP);
          return true;
        }

        Player switchingPlayer = (Player) sender;
        String channelToSwitchTo = args[1];
        if (!channelManager.getChannels().containsKey(channelToSwitchTo.toLowerCase())) {
          logger.send(sender, CHANNEL_NOT_FOUND, channelToSwitchTo);
          return true;
        }

        if (channelManager.isChannelDisabled(channelToSwitchTo)) {
          logger.send(sender, CHANNEL_DISABLED, channelToSwitchTo.toUpperCase());
          return true;
        }

        if (!channelManager.getChannels(switchingPlayer.getUniqueId()).contains(channelToSwitchTo.toLowerCase())) {
          logger.send(sender, CHANNEL_NOT_SUBSCRIBED, channelToSwitchTo.toUpperCase());
          return true;
        }

        channelManager.setLastChannelUsed(switchingPlayer.getUniqueId(), channelToSwitchTo.toLowerCase());
        String currentChannel = channelManager.getActiveChannel(switchingPlayer);
        boolean alreadyActive = channelToSwitchTo.equalsIgnoreCase(currentChannel);

        channelManager.setLastActiveChannel(switchingPlayer.getUniqueId(), alreadyActive
                                                                           ? channelManager.getDefaultChannel()
                                                                           : channelToSwitchTo.toLowerCase());

        logger.send(switchingPlayer, CHANNEL_TOGGLE, channelToSwitchTo.toUpperCase(), alreadyActive
                                                                                      ? OFF.toString()
                                                                                      : ON.toString());
        break;

      case "status":
      case "subs":
        if (!sender.hasPermission(PERM_CHANNEL_INFO)) {
          logger.send(sender, NO_PERM, PERM_CHANNEL_INFO, label + " " + sub);
          return true;
        }

        if (args.length < 2) {
          logger.send(sender, CHANNEL_HELP);
          return true;
        }

        Player statusPlayer = Bukkit.getPlayer(args[1]);
        if (statusPlayer == null || !statusPlayer.isOnline()) {
          logger.send(sender, PLAYER_NOT_FOUND, args[1]);
          return true;
        }

        Set<String> subscribedChannels = channelManager.getChannels(statusPlayer.getUniqueId());
        String activeChannel = channelManager.getActiveChannel(statusPlayer);
        String subsList = subscribedChannels.stream()
            .map(channel -> channel.equalsIgnoreCase(activeChannel)
                            ? "&a&l" + channel.toUpperCase() + "&f"
                            : "&e" + channel.toUpperCase() + "&f")
            .collect(Collectors.joining("&7, "));
        if (subsList.isEmpty()) {
          subsList = "&7---";
        }

        String rosterList = coreManager.getRostersManager().getPlayerRosters(statusPlayer.getName()).values().stream()
            .map(roster -> "&b" + roster.getName() + " &7[" + roster.getLeague() + "]&f")
            .collect(Collectors.joining("&7, "));
        if (rosterList.isEmpty()) {
          rosterList = "&7---";
        }

        logger.send(sender, String.join(System.lineSeparator(),
            "{prefix}&aStatus kanala za &b" + statusPlayer.getDisplayName() + "&a:",
            "&aRosters: &r" + rosterList,
            "&aSubscribed channels: &r" + subsList,
            "&aAktivni kanal: &r" + activeChannel));
        break;

      case "spy":
        if (!(sender instanceof Player)) {
          logger.send(sender, INGAME_ONLY);
          return true;
        }

        if (!sender.hasPermission(PERM_CHANNEL_SPY)) {
          logger.send(sender, NO_PERM, PERM_CHANNEL_SPY, label + " " + sub);
          return true;
        }

        if (args.length != 1) {
          logger.send(sender, CHANNEL_HELP);
          return true;
        }

        Player spyingPlayer = (Player) sender;
        boolean spyingStatus = channelManager.toggleSocialSpy(spyingPlayer);

        String spyStatus = spyingStatus
                           ? ON.toString()
                           : OFF.toString();
        logger.send(spyingPlayer, CHANNEL_SPY_TOGGLED, spyStatus);
        break;

      default:
        logger.send(sender, CHANNEL_HELP);
        break;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_CHANNEL_MAIN)) {
      return Collections.emptyList();
    }

    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission(PERM_CHANNEL_MAIN)) {
      completions.addAll(List.of("toggle", "list", "info", "switch", "join", "leave", "spy", "status", "subs"));
    } else {
      if (args.length == 2 && sender.hasPermission(PERM_CHANNEL_MAIN) &&
          (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("subs"))) {
        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
      } else {
        if (args.length == 2 && sender.hasPermission(PERM_CHANNEL_MAIN)) {
          completions.addAll(channelManager.getChannels().keySet());
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