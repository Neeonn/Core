package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientBlockerCommand implements CommandExecutor, TabCompleter {
  private final Logger logger;
  private final ClientBlocker clientBlocker;
  private final LuckPerms luckPerms;

  private static final Duration EXEMPT_DURATION = Duration.ofMinutes(30);
  private static final String PERM_TOGGLE = "core.client-blocker.toggle";
  private static final String PERM_EXEMPT = "core.client-blocker.exempt";
  private static final String PERM_CHECK = "core.client-blocker.check";

  public ClientBlockerCommand(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.clientBlocker = coreManager.getClientBlocker();
    this.luckPerms = coreManager.getLuckPerms();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      logger.send(sender, Lang.CLIENT_BLOCKER_TOGGLE.replace(new String[]{clientBlocker.isEnabled() ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "toggle":
        if (!sender.hasPermission(PERM_TOGGLE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_TOGGLE, label})); return true; }
        boolean enabled = clientBlocker.toggle();
        logger.send(ClientBlocker.NOTIFY_PERMISSION, Lang.CLIENT_BLOCKER_TOGGLE.replace(new String[]{enabled ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
        return true;

      case "exempt":
        if (!sender.hasPermission(PERM_EXEMPT)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_EXEMPT, label})); return true; }
        if (args.length < 2) { logger.send(sender, Lang.CLIENT_BLOCKER_USAGE.replace(null)); return true; }

        String targetName = args[1];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (!offlineTarget.hasPlayedBefore()) { logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{targetName})); return true; }

        luckPerms.getUserManager().modifyUser(offlineTarget.getUniqueId(), user -> {
          PermissionNode existing = user.getNodes().stream().filter(PermissionNode.class::isInstance).map(PermissionNode.class::cast)
              .filter(permissionNode -> permissionNode.getPermission().equalsIgnoreCase(ClientBlocker.BYPASS_PERMISSION))
              .findFirst().orElse(null);

          if (existing != null) {
            user.data().remove(existing);
            logger.send(ClientBlocker.NOTIFY_PERMISSION, Lang.CLIENT_BLOCKER_EXEMPT.replace(new String[]{Lang.OFF.replace(null), targetName}));
          } else {
            user.data().add(PermissionNode.builder(ClientBlocker.BYPASS_PERMISSION).value(true).expiry(EXEMPT_DURATION).build());
            logger.send(ClientBlocker.NOTIFY_PERMISSION, Lang.CLIENT_BLOCKER_EXEMPT.replace(new String[]{Lang.ON.replace(null), targetName}));
          }
        });

        return true;

      case "check":
        if (!sender.hasPermission(PERM_CHECK)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CHECK, label})); return true; }
        if (args.length < 2) { logger.send(sender, Lang.CLIENT_BLOCKER_USAGE.replace(null)); return true; }

        String checkName = args[1];
        Player target = Bukkit.getPlayer(checkName);
        if (target == null) { logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{checkName})); return true; }

        String brand = clientBlocker.getBrand(target);
        if (brand == null) brand = "&c???";
        boolean blocked = clientBlocker.shouldKick(target);
        String exempt = target.hasPermission(ClientBlocker.BYPASS_PERMISSION) ? Lang.ON.replace(null) : Lang.OFF.replace(null);

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
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.addAll(Arrays.asList("toggle", "check", "exempt", "help", "?"));
    } else if (args.length == 2) {
      if (args[0].equalsIgnoreCase("check")) {
        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
      } else if (args[0].equalsIgnoreCase("exempt")) {
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
