package io.github.divinerealms.core.main;

import io.github.divinerealms.core.Core;
import io.github.divinerealms.core.commands.*;
import io.github.divinerealms.core.config.Config;
import io.github.divinerealms.core.config.ConfigManager;
import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.managers.*;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class CoreManager {
  private final Core plugin;

  private final Logger logger;
  private final ConfigManager configManager;
  private final ChannelManager channelManager;
  private final ClientBlocker clientBlocker;
  private final ListenerManager listenerManager;
  private final ResultManager resultManager;
  private final PlayerSettingsManager playerSettingsManager;
  private final PlaytimeManager playtimeManager;

  private final Set<String> registeredCommands = new HashSet<>();

  private LuckPerms luckPerms;
  private boolean discordSRV;
  private boolean placeholderAPI;

  public CoreManager(Core plugin) throws IllegalStateException {
    this.plugin = plugin;

    this.configManager = new ConfigManager(plugin, "");
    this.logger = new Logger(plugin);
    this.sendBanner();

    this.setupConfig();
    this.setupMessages();
    this.setupDependencies();

    this.channelManager = new ChannelManager(this);
    this.channelManager.clearAllState();
    this.listenerManager = new ListenerManager(this);
    this.clientBlocker = new ClientBlocker();
    this.resultManager = new ResultManager(this);
    this.playerSettingsManager = new PlayerSettingsManager(this);
    this.playtimeManager = new PlaytimeManager(this);

    this.reload();
  }

  public void reload() {
    configManager.reloadAllConfigs();
    setupConfig();
    setupMessages();
    channelManager.reloadAll();
    registerCommands();
    getListenerManager().registerAll();
    resultManager.load();
  }

  public void registerCommands() {
    unregisterCommands();

    try {
      Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      CommandMap commandMap = (CommandMap) field.get(plugin.getServer());

      registerCommand(commandMap, "core", new BukkitCommandWrapper("core", new CoreCommand(this), null));
      registerCommand(commandMap, "channel", new BukkitCommandWrapper("channel", new ChannelCommand(this), null));
      registerCommand(commandMap, "clientblocker", new BukkitCommandWrapper("clientblocker", new ClientBlockerCommand(this), Collections.singletonList("cb")));
      registerCommand(commandMap, "team", new BukkitCommandWrapper("team", new TeamChannelCommand(this), Collections.singletonList("t")));
      registerCommand(commandMap, "result", new BukkitCommandWrapper("result", new ResultCommand(this), Collections.singletonList("rs")));
      registerCommand(commandMap, "togglemention", new BukkitCommandWrapper("togglemention", new ToggleCommand(this), null));
      registerCommand(commandMap, "playtime", new BukkitCommandWrapper("playtime", new PlaytimeCommand(this), null));

      for (ChannelManager.ChannelInfo info : channelManager.getChannels().values()) {
        if (info.name.equalsIgnoreCase("global")) continue;
        if (info.permission.startsWith("tab.group.")) continue;

        DynamicChannelBukkitCommand dynamicCommand = new DynamicChannelBukkitCommand(info, channelManager, logger);
        registerCommand(commandMap, dynamicCommand.getName(), dynamicCommand);
      }

      logger.info("&a✔ &9Registered &e" + registeredCommands.size() + " &9commands.");
    } catch (Exception exception) {
      logger.info("&cFailed to register commands: &4" + exception.getMessage());
      exception.printStackTrace();
    }
  }

  private void registerCommand(CommandMap commandMap, String name, Command command) {
    commandMap.register("core", command);
    registeredCommands.add(name);
    if (command.getAliases() != null) registeredCommands.addAll(command.getAliases());
    if (command instanceof BukkitCommandWrapper) {
      CommandExecutor executor = ((BukkitCommandWrapper) command).getExecutor();
      if (executor instanceof TabCompleter) ((BukkitCommandWrapper) command).setTabCompleter((TabCompleter) executor);
    }
  }

  public void unregisterCommands() {
    if (registeredCommands.isEmpty()) return;

    try {
      Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      CommandMap commandMap = (CommandMap) field.get(plugin.getServer());

      Field knownCommandField = SimpleCommandMap.class.getDeclaredField("knownCommands");
      knownCommandField.setAccessible(true);
      //noinspection unchecked
      Map<String, Command> knownCommands = (Map<String, Command>) knownCommandField.get(commandMap);

      for (String cmd : registeredCommands) {
        Command removed = knownCommands.remove(cmd.toLowerCase());
        knownCommands.remove("core:" + cmd.toLowerCase());

        if (removed != null) {
          for (String alias : removed.getAliases()) {
            knownCommands.remove(alias.toLowerCase());
            knownCommands.remove("core:" + alias.toLowerCase());
          }
        }
      }

      if (!registeredCommands.isEmpty()) {
        logger.info("&a✔ &9Unregistered &e" + registeredCommands.size() + " &9commands.");
        registeredCommands.clear();
      }
    } catch (Exception e) {
      logger.info("&cFailed to unregister commands: &4" + e.getMessage());
      e.printStackTrace();
    }
  }

  private void setupConfig() {
    FileConfiguration file = configManager.getConfig("config.yml");
    Config.setFile(file);

    for (Config value : Config.values()) {
      setDefaultIfMissing(file, value.getPath(), value.getDefault());
    }

    ensureSection(file, "channels.list");
    ensureSection(file, "channels.list.global");
    ensureSection(file, "channels.list.global.formats");
    ensureSection(file, "player_messages");
    ensureSection(file, "player_messages.custom_join");
    ensureSection(file, "player_messages.custom_quit");
    ensureSection(file, "result");
    ensureSection(file, "result.formats");

    setDefaultIfMissing(file, "channels.list.global.permission", "core.channel.global");
    setDefaultIfMissing(file, "channels.list.global.discord_id", "");
    setDefaultIfMissing(file, "channels.list.global.formats.minecraft_chat", "%luckperms_meta_rankPrefix%%essentials_nickname%%luckperms_meta_tag% &8» &r%message%");
    setDefaultIfMissing(file, "channels.list.global.formats.discord_to_minecraft", "&3&o[Discord] %name%%reply% » %message%");
    setDefaultIfMissing(file, "channels.list.global.formats.minecraft_to_discord", "%luckperms_meta_rankPrefix%%name%%luckperms_meta_tag% » %message%");
    setDefaultIfMissing(file, "player_messages.custom_join.enabled", false);
    setDefaultIfMissing(file, "player_messages.custom_join.minecraft", "&8[&a+&8] &r%luckperms_meta_rankprefix%%essentials_nickname%");
    setDefaultIfMissing(file, "player_messages.custom_join.discord", ":green_square: %luckperms_meta_rankprefix%%essentials_nickname%");
    setDefaultIfMissing(file, "player_messages.custom_quit.enabled", false);
    setDefaultIfMissing(file, "player_messages.custom_quit.minecraft", "&8[&c-&8] &r%luckperms_meta_rankprefix%%essentials_nickname%");
    setDefaultIfMissing(file, "player_messages.custom_quit.discord", ":red_square: %luckperms_meta_rankprefix%%essentials_nickname%");
    setDefaultIfMissing(file, "result.enabled", true);
    setDefaultIfMissing(file, "result.discord_id", "");
    setDefaultIfMissing(file, "result.formats.minecraft.start", "%prefix% &8| &aMeč započinje: &9%home% &fvs &c%away%");
    setDefaultIfMissing(file, "result.formats.minecraft.half", "%prefix% &8| &ePoluvreme! &9%home% &f%home_score% &7- &f%away_score% &c%away%");
    setDefaultIfMissing(file, "result.formats.minecraft.resume", "%prefix% &8| &aDrugo poluvreme započinje!");
    setDefaultIfMissing(file, "result.formats.minecraft.end", "%prefix% &8| &cMeč završen! &9%home% &f%home_score% &7- &f%away_score% &c%away%");
    setDefaultIfMissing(file, "result.formats.minecraft.update", "%prefix% &8| &9%home% &f%home_score% &7- &f%away_score% &c%away% &8| &e%time%");
    setDefaultIfMissing(file, "result.formats.discord.start", "**%home%** vs **%away%** meč započinje!");
    setDefaultIfMissing(file, "result.formats.discord.half", "Poluvreme: **%home%** %home_score% - %away_score% **%away%**");
    setDefaultIfMissing(file, "result.formats.discord.resume", "Drugo poluvreme započinje!");
    setDefaultIfMissing(file, "result.formats.discord.end", "Meč završen: **%home%** %home_score% - %away_score% **%away%**");
    setDefaultIfMissing(file, "result.formats.discord.goal", "**GOOOOOL!** **%scorer%** je dao gol za **%team%**.");

    file.options().copyDefaults(true);
    configManager.saveConfig("config.yml");
  }

  private void setupMessages() {
    FileConfiguration file = configManager.getConfig("messages.yml");
    Lang.setFile(file);

    for (Lang value : Lang.values()) {
      setDefaultIfMissing(file, value.getPath(), value.getDefault());
    }

    file.options().copyDefaults(true);
    configManager.saveConfig("messages.yml");
  }

  private void setupDependencies() throws IllegalStateException {
    RegisteredServiceProvider<LuckPerms> rsp = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
    this.luckPerms = rsp == null ? null : rsp.getProvider();
    if (luckPerms == null) throw new IllegalStateException("LuckPerms not found!");

    this.discordSRV = plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null;
    if (!discordSRV) logger.info("&cDiscordSRV not found! Discord Integration disabled.");

    this.placeholderAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    if (!placeholderAPI) logger.info("&cPlaceholderAPI not found! Placeholders features disabled.");

    logger.info("&a✔ &9Hooked into &dLuckPerms &9successfully!");
  }

  public String getTeam(Player player) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null) return null;

    return user.getNodes().stream()
        .filter(node -> node instanceof InheritanceNode)
        .map(node -> (InheritanceNode) node)
        .map(inheritanceNode -> luckPerms.getGroupManager().getGroup(inheritanceNode.getGroupName()))
        .filter(Objects::nonNull)
        .filter(group -> group.getWeight().orElse(0) == 200)
        .map(Group::getName)
        .map(String::toUpperCase)
        .findFirst().orElse(null);
  }

  private void ensureSection(FileConfiguration file, String path) {
    if (!file.isConfigurationSection(path)) file.createSection(path);
  }

  private void setDefaultIfMissing(FileConfiguration file, String path, Object value) {
    if (!file.isSet(path)) file.set(path, value);
  }

  public void saveAll() {
    configManager.saveAll();
  }

  private void sendBanner() {
    String[] banner = new String[] {"┏┓┏┓┳┓┏┓" + "&8 -+------------------------------------+-", "┃ ┃┃┣┫┣ " + "&7  Created by &b" + plugin.getDescription().getAuthors().stream().map(String::valueOf).collect(Collectors.joining(", ")) + "&7, version &f" + plugin.getDescription().getVersion(), "┗┛┗┛┛┗┗┛" + "&8 -+------------------------------------+-"};

    for (String line : banner) {
      plugin.getServer().getConsoleSender().sendMessage(logger.getConsolePrefix() + logger.color(line));
    }
  }
}