package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamChannelCommand implements CommandExecutor {
  private final CoreManager coreManager;
  private final Logger logger;
  private final ChannelManager channelManager;

  public TeamChannelCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) { logger.info(Lang.INGAME_ONLY.replace(null)); return true; }

    Player player = (Player) sender;
    String team = coreManager.getTeam(player);
    if (team == null) { logger.send(player, Lang.CHANNEL_NOT_IN_TEAM.replace(null)); return true; }

    String message = args.length > 0 ? String.join(" ", args) : null;
    channelManager.sendMessage(sender, team.toLowerCase(), message);
    return true;
  }
}
