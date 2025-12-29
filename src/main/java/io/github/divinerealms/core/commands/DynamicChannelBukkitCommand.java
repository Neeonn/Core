package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.github.divinerealms.core.configs.Lang.CHANNEL_NOT_FOUND;
import static io.github.divinerealms.core.configs.Lang.CHANNEL_NO_PERM;

public class DynamicChannelBukkitCommand extends Command {
  private final String channel;
  private final ChannelManager channelManager;
  private final Logger logger;

  public DynamicChannelBukkitCommand(ChannelInfo info, ChannelManager channelManager, Logger logger) {
    super(info.name);
    this.channel = info.name;
    this.channelManager = channelManager;
    this.logger = logger;

    this.description = "Chat command for channel " + info.name;
    this.usageMessage = "/" + info.name + " [message]";

    if (info.permission != null && !info.permission.isEmpty()) {
      this.setPermission(info.permission);
    }
    if (info.aliases != null && !info.aliases.isEmpty()) {
      this.setAliases(info.aliases);
    }
  }

  @Override
  public boolean execute(CommandSender sender, String label, String[] args) {
    String message = args.length > 0
                     ? String.join(" ", args)
                     : null;

    if (sender instanceof Player) {
      Player player = (Player) sender;

      if (channelManager.getChannels(player.getUniqueId()).contains(channel.toLowerCase())) {
        if (channelManager.getChannels().containsKey(channel)) {
          channelManager.sendMessage(sender, channel, message);
          return true;
        }
      }
    }

    if (getPermission() != null && !getPermission().isEmpty() && !sender.hasPermission(getPermission())) {
      logger.send(sender, CHANNEL_NO_PERM, getPermission(), channel);
      return true;
    }

    if (!channelManager.getChannels().containsKey(channel)) {
      logger.send(sender, CHANNEL_NOT_FOUND, channel);
      return true;
    }

    channelManager.sendMessage(sender, channel, message);
    return true;
  }
}