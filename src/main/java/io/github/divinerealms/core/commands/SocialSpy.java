package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.github.divinerealms.core.utilities.Permissions.PERM_COMMAND_SPY;

public class SocialSpy implements CommandExecutor {
  private final ChannelManager channelManager;
  private final Logger logger;

  public SocialSpy(CoreManager coreManager) {
    this.channelManager = coreManager.getChannelManager();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) { logger.send(sender, Lang.INGAME_ONLY.replace(null)); return true; }
    Player player = (Player) sender;
    if (!player.hasPermission(PERM_COMMAND_SPY)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_COMMAND_SPY, label + " "})); return true; }

    boolean toggled = channelManager.toggleSocialSpy(player);
    logger.send(player, Lang.CHANNEL_SPY_TOGGLED.replace(new String[]{toggled ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
    return true;
  }
}
