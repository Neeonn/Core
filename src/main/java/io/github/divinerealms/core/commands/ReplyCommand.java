package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.PrivateMessagesManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Permissions.PERM_COMMAND_MSG;

public class ReplyCommand implements CommandExecutor {
  private final PrivateMessagesManager privateMessagesManager;
  private final Logger logger;

  public ReplyCommand(CoreManager coreManager) {
    this.privateMessagesManager = coreManager.getPrivateMessagesManager();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      logger.send(sender, INGAME_ONLY);
      return true;
    }

    Player pmSender = (Player) sender;
    if (!pmSender.hasPermission(PERM_COMMAND_MSG)) {
      logger.send(pmSender, NO_PERM, PERM_COMMAND_MSG, label);
      return true;
    }

    if (args.length < 1) {
      logger.send(pmSender, USAGE, label + " <message>");
      return true;
    }

    String message = String.join(" ", args);
    privateMessagesManager.reply(pmSender, message);
    return true;
  }
}
