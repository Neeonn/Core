package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.PrivateMessagesManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.divinerealms.core.utilities.Permissions.PERM_COMMAND_MSG;

public class PrivateMessageCommand implements CommandExecutor {
  private final PrivateMessagesManager privateMessagesManager;
  private final Logger logger;

  public PrivateMessageCommand(CoreManager coreManager) {
    this.privateMessagesManager = coreManager.getPrivateMessagesManager();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) { logger.send(sender, Lang.INGAME_ONLY.replace(null)); return true; }
    Player pmSender = (Player) sender;
    if (!pmSender.hasPermission(PERM_COMMAND_MSG)) { logger.send(pmSender, Lang.NO_PERM.replace(new String[]{PERM_COMMAND_MSG, label})); return true; }
    if (args.length < 2) { logger.send(pmSender, Lang.USAGE.replace(new String[]{label + " <player> <message>"})); return true; }

    String targetName = args[0];
    Player recipient = Bukkit.getPlayer(targetName);
    if (recipient == null || !recipient.isOnline()) { logger.send(pmSender, Lang.PLAYER_NOT_FOUND.replace(new String[]{targetName})); return true; }
    if (recipient.equals(pmSender)) { logger.send(pmSender, Lang.PRIVATE_MESSAGES_SELF.replace(null)); return true; }

    String message = Stream.of(args).skip(1).collect(Collectors.joining(" "));
    privateMessagesManager.sendPrivateMessage(pmSender, recipient, message);
    return true;
  }
}
