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
import java.util.Collections;
import java.util.List;

public class ToggleCommand implements CommandExecutor, TabCompleter {
  private final Logger logger;
  private final PlayerSettingsManager playerSettingsManager;

  private final static String PERM_MAIN = "core.toggle";
  private final static String PERM_OTHER = PERM_MAIN + ".other";

  public ToggleCommand(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.playerSettingsManager = coreManager.getPlayerSettingsManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_MAIN, label})); return true; }
    if (!(sender instanceof Player)) { logger.send(sender, Lang.INGAME_ONLY.replace(null)); return true; }

    Player player = (Player) sender;
    if (args.length == 0) {
      boolean updated = playerSettingsManager.toggleMentionSound(player);
      logger.send(player, Lang.MENTION_TOGGLED.replace(new String[]{updated ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
      return true;
    }

    if (!player.hasPermission(PERM_OTHER)) { logger.send(player, Lang.NO_PERM.replace(new String[]{PERM_OTHER, label + " <player>"})); return true; }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) { logger.send(player, Lang.PLAYER_NOT_FOUND.replace(new String[]{args[0]})); return true; }

    boolean updated = playerSettingsManager.toggleMentionSound(target);
    logger.send(player, Lang.MENTION_TOGGLED.replace(new String[]{updated ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
    logger.send(target, Lang.MENTION_TOGGLED.replace(new String[]{updated ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission(PERM_OTHER)) {
      Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
