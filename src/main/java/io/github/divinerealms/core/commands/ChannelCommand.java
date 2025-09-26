package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelCommand implements CommandExecutor, TabCompleter {
  private final Logger logger;
  private final ChannelManager channelManager;

  public ChannelCommand(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      logger.send(sender, Lang.CHANNEL_HELP.replace(null));
      return true;
    }

    if (args[0].equalsIgnoreCase("toggle")) {
      if (args.length < 2) {
        logger.send(sender, Lang.CHANNEL_HELP.replace(null));
        return true;
      }

      if (!sender.hasPermission("core.channel.toggle")) {
        logger.send(sender, Lang.NO_PERM.replace(new String[]{"core.channel.toggle", "channel toggle"}));
        return true;
      }

      boolean disabled = channelManager.toggleChannel(args[1]);
      String status = disabled ? Lang.OFF.replace(null) : Lang.ON.replace(null);
      logger.broadcast(Lang.CHANNEL_DISABLED_BROADCAST.replace(new String[]{args[1].toUpperCase(), status, sender.getName()}));
      return true;
    }

    logger.send(sender, Lang.CHANNEL_HELP.replace(null));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      // Suggest subcommands
      completions.add("toggle");
    } else if (args.length == 2) {
      // If the subcommand is toggle, suggest existing channel names
      if (args[0].equalsIgnoreCase("toggle")) {
        completions.addAll(channelManager.getChannels().keySet());
      }
    }

    // Filter completions to what the user has typed so far
    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      Collections.sort(completions);
    }

    return completions;
  }
}