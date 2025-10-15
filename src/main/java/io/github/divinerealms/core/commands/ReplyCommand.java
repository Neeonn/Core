package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {
  private final ChannelManager channelManager;
  private final Logger logger;

  private static final String PERM_USE = "core.command.msg";

  public ReplyCommand(CoreManager coreManager) {
    this.channelManager = coreManager.getChannelManager();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) { logger.send(sender, Lang.INGAME_ONLY.replace(null)); return true; }
    Player pmSender = (Player) sender;
    if (!pmSender.hasPermission(PERM_USE)) { logger.send(pmSender, Lang.NO_PERM.replace(new String[]{PERM_USE, label})); return true; }
    if (args.length < 1) { logger.send(pmSender, Lang.USAGE.replace(new String[]{label + " <message>"})); return true; }

    String message = String.join(" ", args);
    channelManager.reply(pmSender, message);
    return true;
  }
}
