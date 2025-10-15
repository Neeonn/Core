package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrivateMessageCommand implements CommandExecutor {
  private final ChannelManager channelManager;
  private final Logger logger;

  private static final String PERM_USE = "core.command.msg";

  public PrivateMessageCommand(CoreManager coreManager) {
    this.channelManager = coreManager.getChannelManager();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player pmSender = (Player) sender;
    if (!pmSender.hasPermission(PERM_USE)) { logger.send(pmSender, Lang.NO_PERM.replace(new String[]{PERM_USE, label})); return true; }
    if (args.length < 2) { logger.send(pmSender, Lang.USAGE.replace(new String[]{label + " <player> <message>"})); return true; }

    String targetName = args[0];
    Player recipient = Bukkit.getPlayer(targetName);
    if (recipient == null || !recipient.isOnline()) { logger.send(pmSender, Lang.PLAYER_NOT_FOUND.replace(new String[]{targetName})); return true; }
    if (recipient.equals(pmSender)) { logger.send(pmSender, Lang.PRIVATE_MESSAGES_SELF.replace(null)); return true; }

    String message = Stream.of(args).skip(1).collect(Collectors.joining(" "));
    channelManager.sendPrivateMessage(pmSender, recipient, message);
    return true;
  }
}
