package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.RostersManager;
import io.github.divinerealms.core.utilities.Logger;
import io.github.divinerealms.core.utilities.RosterInfo;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamChannelCommand implements CommandExecutor {
  private final ChannelManager channelManager;
  private final RostersManager rostersManager;
  private final Logger logger;
  private final String leagueType;

  public TeamChannelCommand(CoreManager coreManager, String leagueType) {
    this.channelManager = coreManager.getChannelManager();
    this.rostersManager = coreManager.getRostersManager();
    this.logger = coreManager.getLogger();
    this.leagueType = leagueType;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) { logger.info("&cConsole can't use team chat."); return true; }

    Player player = (Player) sender;

    RosterInfo roster = rostersManager.getPlayerRoster(player.getName(), leagueType);
    if (roster == null) { logger.send(player, Lang.CHANNEL_NOT_IN_TEAM.replace(null)); return true; }

    String team = roster.getName().toLowerCase();
    String message = args.length > 0 ? String.join(" ", args) : null;
    channelManager.setLastChannelUsed(player.getUniqueId(), team);

    if (message == null || message.isEmpty()) {
      String currentChannel = channelManager.getActiveChannel(player);
      boolean alreadyActive = team.equalsIgnoreCase(currentChannel);

      channelManager.setLastActiveChannel(player.getUniqueId(), alreadyActive ? channelManager.getDefaultChannel() : team);
      logger.send(player, Lang.CHANNEL_TOGGLE.replace(new String[]{roster.getName().toUpperCase(), alreadyActive ? Lang.OFF.replace(null) : Lang.ON.replace(null)}));
    } else channelManager.sendMessage(sender, team, message);
    return true;
  }
}
