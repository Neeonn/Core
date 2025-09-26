package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.PlayerSettingsManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ToggleCommand implements CommandExecutor, TabCompleter {
  private final Logger logger;
  private final PlayerSettingsManager playerSettingsManager;

  public ToggleCommand(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.playerSettingsManager = coreManager.getPlayerSettingsManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      logger.send(sender, "&cConsole can't use this command.");
      return true;
    }

    Player player = (Player) sender;
    if (args.length == 0) {
      boolean updated = playerSettingsManager.toggleMentionSound(player);
      logger.send(player, Lang.MENTION_TOGGLED.replace(new String[]{updated ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
      return true;
    }

    if (!player.hasPermission("core.toggle.other")) {
      logger.send(player, Lang.NO_PERM.replace(new String[]{"core.toggle.other", label + " <player>"}));
      return true;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      logger.send(player, Lang.PLAYER_NOT_FOUND.replace(new String[]{args[0]}));
      return true;
    }

    boolean updated = playerSettingsManager.toggleMentionSound(target);
    logger.send(player, Lang.MENTION_TOGGLED.replace(new String[]{updated ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
    logger.send(target, Lang.MENTION_TOGGLED.replace(new String[]{updated ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission("core.toggle.other")) {
      Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
    }

    return completions;
  }
}
