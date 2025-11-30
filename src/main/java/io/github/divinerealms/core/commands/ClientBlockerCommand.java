package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.utilities.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.divinerealms.core.utilities.Constants.CLIENT_BLOCKER_EXEMPT_DURATION;
import static io.github.divinerealms.core.utilities.Permissions.*;

public class ClientBlockerCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Logger logger;
  private final ClientBlocker clientBlocker;
  private final LuckPerms luckPerms;

  public ClientBlockerCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.clientBlocker = coreManager.getClientBlocker();
    this.luckPerms = coreManager.getLuckPerms();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_CLIENT_BLOCKER_MAIN)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CLIENT_BLOCKER_MAIN, label})); return true; }
    if (args.length == 0) {
      logger.send(sender, Lang.CLIENT_BLOCKER_TOGGLE.replace(new String[]{clientBlocker.isEnabled() ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
      return true;
    }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "toggle":
        if (!sender.hasPermission(PERM_CLIENT_BLOCKER_TOGGLE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CLIENT_BLOCKER_TOGGLE, label + " " + sub})); return true; }
        boolean enabled = clientBlocker.toggle();
        logger.send(PERM_CLIENT_BLOCKER_NOTIFY, Lang.CLIENT_BLOCKER_TOGGLE.replace(new String[]{enabled ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
        return true;

      case "exempt":
        if (!sender.hasPermission(PERM_CLIENT_BLOCKER_EXEMPT)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CLIENT_BLOCKER_EXEMPT, label + " " + sub})); return true; }
        if (args.length < 2) { logger.send(sender, Lang.CLIENT_BLOCKER_USAGE.replace(null)); return true; }

        String targetName = args[1];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (!offlineTarget.hasPlayedBefore()) { logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{targetName})); return true; }

        luckPerms.getUserManager().modifyUser(offlineTarget.getUniqueId(), user -> {
          PermissionNode existing = user.getNodes().stream().filter(PermissionNode.class::isInstance).map(PermissionNode.class::cast)
              .filter(permissionNode -> permissionNode.getPermission().equalsIgnoreCase(PERM_CLIENT_BLOCKER_BYPASS))
              .findFirst().orElse(null);

          if (existing != null) {
            user.data().remove(existing);
            logger.send(PERM_CLIENT_BLOCKER_NOTIFY, Lang.CLIENT_BLOCKER_EXEMPT.replace(new String[]{Lang.OFF.replace(null), targetName}));
          } else {
            user.data().add(PermissionNode.builder(PERM_CLIENT_BLOCKER_BYPASS).value(true).expiry(CLIENT_BLOCKER_EXEMPT_DURATION).build());
            logger.send(PERM_CLIENT_BLOCKER_NOTIFY, Lang.CLIENT_BLOCKER_EXEMPT.replace(new String[]{Lang.ON.replace(null), targetName}));
          }
        });

        return true;

      case "check":
        if (!sender.hasPermission(PERM_CLIENT_BLOCKER_CHECK)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CLIENT_BLOCKER_CHECK, label + " " + sub})); return true; }
        if (args.length < 2) { logger.send(sender, Lang.CLIENT_BLOCKER_USAGE.replace(null)); return true; }

        String checkName = args[1];
        Player target = Bukkit.getPlayer(checkName);
        if (target == null) { logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{checkName})); return true; }

        String brand = clientBlocker.getBrand(target);
        if (brand == null) brand = "&c???";
        boolean blocked = clientBlocker.shouldKick(target);
        String exempt = target.hasPermission(PERM_CLIENT_BLOCKER_BYPASS) ? Lang.ON.replace(null) : Lang.OFF.replace(null);

        logger.send(sender, Lang.CLIENT_BLOCKER_CHECK_RESULT.replace(new String[]{target.getDisplayName(), (blocked ? "&c" : "&a") + brand, exempt}));
        return true;

      case "help":
      case "?":
      default:
        logger.send(sender, Lang.CLIENT_BLOCKER_USAGE.replace(null));
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_CLIENT_BLOCKER_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.addAll(Arrays.asList("toggle", "check", "exempt", "help", "?"));
    } else if (args.length == 2) {
      if (args[0].equalsIgnoreCase("check") && sender.hasPermission(PERM_CLIENT_BLOCKER_CHECK)) {
        coreManager.getCachedPlayers().forEach(player -> completions.add(player.getName()));
      } else if (args[0].equalsIgnoreCase("exempt") && sender.hasPermission(PERM_CLIENT_BLOCKER_EXEMPT)) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
          if (offlinePlayer.hasPlayedBefore()) completions.add(offlinePlayer.getName());
        }
      }
    }

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
