package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.WeightNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.divinerealms.core.utilities.Constants.ROSTER_WEIGHT;

public class RostersCommand implements CommandExecutor, TabCompleter {
  private final LuckPerms luckPerms;
  private final Logger logger;

  private static final String PERM_MAIN = "core.rosters";
  private static final String PERM_CREATE = PERM_MAIN + ".create";
  private static final String PERM_DELETE = PERM_MAIN + ".delete";
  private static final String PERM_SET = PERM_MAIN + ".set";
  private static final String PERM_ADD = PERM_MAIN + ".add";
  private static final String PERM_REMOVE = PERM_MAIN + ".remove";
  private static final String PERM_NOTIFY = PERM_MAIN + ".notify";

  public RostersCommand(CoreManager coreManager) {
    this.luckPerms = coreManager.getLuckPerms();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_MAIN, label})); return true; }
    if (args.length == 0) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "switch":
        if (!sender.hasPermission(PERM_CREATE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CREATE, label + " " + sub})); return true; }
        if (args.length != 1) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }
        if (ROSTER_WEIGHT == 200) ROSTER_WEIGHT = 300;
        else if (ROSTER_WEIGHT == 300) ROSTER_WEIGHT = 200;

        logger.send(sender, "{prefix}Promenjen ROSTER_WEIGHT na " + ROSTER_WEIGHT);
        return true;

      case "create":
        if (!sender.hasPermission(PERM_CREATE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_CREATE, label + " " + sub})); return true; }
        String teamType = "";
        if (args.length == 4) teamType = args[3];
        else if (args.length < 3) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

        String teamName = args[1].toUpperCase(), teamTag = "%luckperms_prefix%[" + args[2] + "%luckperms_prefix%] ";
        if (luckPerms.getGroupManager().getGroup(teamName) != null) { logger.send(sender, Lang.ROSTERS_EXISTS.replace(new String[]{teamName})); return true; }
        int weight;

        MetaNode teamPrefix = MetaNode.builder("minecraftprefix", teamTag).build();
        if (teamType.equalsIgnoreCase("b")) {
          teamPrefix = MetaNode.builder("minecraftprefixb", teamTag).build();
          weight = 199;
        } else if (teamType.equalsIgnoreCase("rep")) weight = 300;
        else weight = ROSTER_WEIGHT;

        MetaNode finalTeamPrefix = teamPrefix;
        luckPerms.getGroupManager().createAndLoadGroup(teamName).thenApplyAsync(group -> {
          group.data().add(MetaNode.builder("displayname", teamTag).build());
          group.data().add(WeightNode.builder(weight).build());
          group.data().add(finalTeamPrefix);
          return group;
        }).thenCompose(luckPerms.getGroupManager()::saveGroup);

        logger.send(PERM_NOTIFY, Lang.ROSTERS_CREATE.replace(new String[]{teamName, teamTag}));
        return true;

      case "delete":
        if (!sender.hasPermission(PERM_DELETE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_DELETE, label + " " + sub})); return true; }
        if (args.length < 2) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

        String deleteName = args[1].toUpperCase();
        Group deleteGroup = luckPerms.getGroupManager().getGroup(deleteName);
        if (deleteGroup == null) { logger.send(sender, Lang.ROSTERS_NOT_FOUND.replace(new String[]{deleteName})); return true; }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
          if (!offlinePlayer.hasPlayedBefore()) continue;
          luckPerms.getUserManager().modifyUser(offlinePlayer.getUniqueId(), user ->
              user.data().remove(Node.builder("group." + deleteName.toLowerCase()).build())
          );
        }

        luckPerms.getGroupManager().deleteGroup(deleteGroup);
        logger.send(PERM_NOTIFY, Lang.ROSTERS_DELETE.replace(new String[]{deleteName}));
        return true;

      case "add":
        if (!sender.hasPermission(PERM_ADD)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_ADD, label + " " + sub})); return true; }
        if (args.length < 3) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

        String addTeam = args[1].toUpperCase(), playerName = args[2];
        OfflinePlayer addTarget = Bukkit.getOfflinePlayer(playerName);
        if (!addTarget.hasPlayedBefore()) { logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{playerName})); return true; }

        luckPerms.getUserManager().modifyUser(addTarget.getUniqueId(), user -> {
          for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
            if (group.getWeight().orElse(0) == ROSTER_WEIGHT) {
              user.data().remove(Node.builder("group." + group.getName().toLowerCase()).build());
            }
          }
          user.data().add(Node.builder("group." + addTeam.toLowerCase()).build());
        });

        logger.send(PERM_NOTIFY, Lang.ROSTERS_ADD.replace(new String[]{playerName, addTeam}));
        return true;

      case "remove":
        if (!sender.hasPermission(PERM_REMOVE)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_REMOVE, label + " " + sub})); return true; }
        if (args.length < 2) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

        String removeName = args[1];
        OfflinePlayer removeTarget = Bukkit.getOfflinePlayer(removeName);
        if (!removeTarget.hasPlayedBefore()) { logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{removeName})); return true; }

        luckPerms.getUserManager().modifyUser(removeTarget.getUniqueId(), user -> {
          for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
            if (group.getWeight().orElse(0) == ROSTER_WEIGHT) {
              user.data().remove(Node.builder("group." + group.getName().toLowerCase()).build());
            }
          }
        });

        logger.send(PERM_NOTIFY, Lang.ROSTERS_REMOVE.replace(new String[]{removeName}));
        return true;

      case "set":
        if (!sender.hasPermission(PERM_SET)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_SET, label + " " + sub})); return true; }
        if (args.length < 3) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

        String setTeam = args[1].toUpperCase(), type = args[2].toLowerCase(), value = args.length > 3 ? args[3] : null;
        Group setGroup = luckPerms.getGroupManager().getGroup(setTeam);
        if (setGroup == null) { logger.send(sender, Lang.ROSTERS_NOT_FOUND.replace(new String[]{setTeam})); return true; }
        if (value == null) { logger.send(sender, Lang.ROSTERS_USAGE.replace(null)); return true; }

        switch (type) {
          case "name":
            setGroup.data().add(MetaNode.builder("name", value).build());
            break;
          case "tag":
            setGroup.data().add(MetaNode.builder("displayname", value).build());
            setGroup.data().add(MetaNode.builder("minecraftprefix", "%luckperms_prefix%[" + value + "%luckperms_prefix%] ").build());
            break;
          case "default":
            logger.send(sender, Lang.ROSTERS_INVALID_TYPE.replace(null));
            return true;
        }

        luckPerms.getGroupManager().saveGroup(setGroup);
        logger.send(PERM_NOTIFY, Lang.ROSTERS_SET.replace(new String[]{type, value, setTeam}));
        return true;

      case "help":
      case "?":
      default:
        logger.send(sender, Lang.ROSTERS_USAGE.replace(null));
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_MAIN)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.addAll(Arrays.asList("switch", "create", "delete", "add", "remove", "set", "help"));
    } else if (args.length == 2) {
      if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("set")) {
        completions.addAll(luckPerms.getGroupManager().getLoadedGroups().stream()
            .filter(g -> g.getWeight().orElse(0) == ROSTER_WEIGHT)
            .map(g -> g.getName().toUpperCase())
            .collect(Collectors.toList())
        );
      } else if (args[0].equalsIgnoreCase("remove") && sender.hasPermission(PERM_REMOVE)) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
          if (offlinePlayer.hasPlayedBefore()) completions.add(offlinePlayer.getName());
        }
      }
    } else if (args.length == 3) {
      if (args[0].equalsIgnoreCase("add") && sender.hasPermission(PERM_ADD)) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
          if (offlinePlayer.hasPlayedBefore()) completions.add(offlinePlayer.getName());
        }
      } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission(PERM_SET)) {
        completions.addAll(Arrays.asList("name", "tag"));
      }
    } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) completions.addAll(Arrays.asList("b", "rep"));

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
