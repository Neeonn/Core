package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.PlayerData;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.PlayerDataManager;
import io.github.divinerealms.core.utilities.Logger;
import io.github.divinerealms.core.utilities.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.divinerealms.core.configs.Lang.*;
import static io.github.divinerealms.core.utilities.Permissions.PERM_COMMAND_TOGGLE;
import static io.github.divinerealms.core.utilities.Permissions.PERM_COMMAND_TOGGLE_OTHER;

public class ToggleCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Logger logger;
  private final PlayerDataManager dataManager;

  public ToggleCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.dataManager = coreManager.getDataManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_COMMAND_TOGGLE)) {
      logger.send(sender, NO_PERM, PERM_COMMAND_TOGGLE, label);
      return true;
    }

    if (!(sender instanceof Player)) {
      logger.send(sender, INGAME_ONLY);
      return true;
    }

    Player player = (Player) sender;
    PlayerData playerData = dataManager.get(player);
    PlayerSettings settings = coreManager.getPlayerSettings(player);
    if (args.length == 0) {
      settings.setMentionSoundEnabled(!settings.isMentionSoundEnabled());
      playerData.set("mention_sound.enabled", settings.isMentionSoundEnabled());
      logger.send(player, MENTION_TOGGLED, settings.isMentionSoundEnabled()
                                           ? ON.toString()
                                           : OFF.toString());
      return true;
    }

    if (!player.hasPermission(PERM_COMMAND_TOGGLE_OTHER)) {
      logger.send(player, NO_PERM, PERM_COMMAND_TOGGLE_OTHER, label + " <player>");
      return true;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      logger.send(player, PLAYER_NOT_FOUND, args[0]);
      return true;
    }

    playerData = dataManager.get(target);
    settings = coreManager.getPlayerSettings(target);

    settings.setMentionSoundEnabled(!settings.isMentionSoundEnabled());
    playerData.set("mention_sound.enabled", settings.isMentionSoundEnabled());
    logger.send(player, MENTION_TOGGLED, settings.isMentionSoundEnabled()
                                         ? ON.toString()
                                         : OFF.toString());
    logger.send(target, MENTION_TOGGLED, settings.isMentionSoundEnabled()
                                         ? ON.toString()
                                         : OFF.toString());

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_COMMAND_TOGGLE)) {
      return Collections.emptyList();
    }

    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission(PERM_COMMAND_TOGGLE_OTHER)) {
      coreManager.getCachedPlayers().forEach(player -> completions.add(player.getName()));
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
