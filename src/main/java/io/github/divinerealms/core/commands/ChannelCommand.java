package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
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

  private static final String PERM_MAIN = "core.channel";
  private static final String PERM_TOGGLE = PERM_MAIN + ".toggle";

  public ChannelCommand(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) { logger.send(sender, Lang.CHANNEL_HELP.replace(null)); return true; }

    String sub = args[0].toLowerCase();
    if (sub.equalsIgnoreCase("toggle")) {
      if (args.length < 2) { logger.send(sender, Lang.CHANNEL_HELP.replace(null)); return true; }
      if (!sender.hasPermission(PERM_TOGGLE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_TOGGLE, label + " " + sub})); return true; }

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
    if (!sender.hasPermission(PERM_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission(PERM_MAIN)) {
      completions.add("toggle");
    } else if (args.length == 2 && sender.hasPermission(PERM_TOGGLE)) {
      if (args[0].equalsIgnoreCase("toggle")) completions.addAll(channelManager.getChannels().keySet());
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}