package io.github.divinerealms.core.main;

import io.github.divinerealms.core.commands.*;
import io.github.divinerealms.core.configs.Config;
import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.configs.PlayerData;
import io.github.divinerealms.core.managers.*;
import io.github.divinerealms.core.utilities.*;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class CoreManager {
  private final Plugin plugin;
  private final BukkitScheduler scheduler;

  private final Logger logger;
  private final ConfigManager configManager;
  private final ChannelManager channelManager;
  private final ClientBlocker clientBlocker;
  private final ListenerManager listenerManager;
  private final ResultManager resultManager;
  private final PlayerDataManager dataManager;
  private final PlaytimeManager playtimeManager;
  private final GUIManager guiManager;
  private final BookManager bookManager;
  private final ActionHandler actionHandler;
  private final CommandManager commandManager;
  private final PrivateMessagesManager privateMessagesManager;
  private final RostersManager rostersManager;

  private final Set<String> registeredCommands = new HashSet<>();
  private final Set<Player> cachedPlayers = ConcurrentHashMap.newKeySet();
  private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();

  private Chat chat;
  private LuckPerms luckPerms;
  private boolean authMe;
  private boolean discordSRV;
  private boolean placeholderAPI;

  @Setter private boolean enabling = false, disabling = false;

  public CoreManager(Plugin plugin) throws IllegalStateException {
    this.plugin = plugin;
    this.scheduler = plugin.getServer().getScheduler();

    this.configManager = new ConfigManager(plugin, "");
    this.logger = new Logger(this);
    this.sendBanner();

    this.initializeConfigs();
    this.setupConfig();
    this.setupMessages();
    this.setupDependencies();

    this.channelManager = new ChannelManager(this);
    this.channelManager.clearAllState();
    this.listenerManager = new ListenerManager(this);
    this.clientBlocker = new ClientBlocker();
    this.rostersManager = new RostersManager(this);
    this.resultManager = new ResultManager(this);
    this.resultManager.preloadTeamMedia();
    this.dataManager = new PlayerDataManager(this);
    this.playtimeManager = new PlaytimeManager(this);
    this.guiManager = new GUIManager(this);
    this.bookManager = new BookManager(this);
    this.actionHandler = new ActionHandler(this);
    this.commandManager = new CommandManager(this);
    this.privateMessagesManager = new PrivateMessagesManager(this);

    if (placeholderAPI) new Placeholders(this).register();

    this.reload();
  }

  public void reload() {
    initializeCachedPlayers();
    configManager.reloadAllConfigs();
    setupConfig();
    setupMessages();
    channelManager.reloadAll();
    registerCommands();
    commandManager.reloadCommands();
    getListenerManager().registerAll();
    guiManager.reloadMenus();
    bookManager.reloadBooks();
    rostersManager.reloadRosters();
    List<UUID> onlinePlayers = cachedPlayers.stream().map(Player::getUniqueId).collect(Collectors.toList());
    scheduler.runTaskAsynchronously(plugin, () -> onlinePlayers.forEach(uuid -> {
      Player asyncPlayer = plugin.getServer().getPlayer(uuid);
      if (asyncPlayer == null || !asyncPlayer.isOnline()) return;

      PlayerData playerData = dataManager.get(asyncPlayer);
      if (playerData != null) preloadSettings(asyncPlayer, playerData);

      resultManager.preloadTeamMedia();
    }));
    initializeRosterSubscriptions();
  }

  private void initializeCachedPlayers() {
    Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
    cachedPlayers.clear();
    cachedPlayers.addAll(onlinePlayers);
  }

  public void registerCommands() {
    unregisterCommands();

    try {
      Field field = plugin.getServer().getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      CommandMap commandMap = (CommandMap) field.get(plugin.getServer());

      registerCommand(commandMap, "core", new BukkitCommandWrapper("core", new CoreCommand(this), null));
      registerCommand(commandMap, "channel", new BukkitCommandWrapper("channel", new ChannelCommand(this), Collections.singletonList("ch")));
      registerCommand(commandMap, "clientblocker", new BukkitCommandWrapper("clientblocker", new ClientBlockerCommand(this), Collections.singletonList("cb")));
      registerCommand(commandMap, "result", new BukkitCommandWrapper("result", new ResultCommand(this), Collections.singletonList("rs")));
      registerCommand(commandMap, "togglemention", new BukkitCommandWrapper("togglemention", new ToggleCommand(this), Collections.singletonList("tgm")));
      registerCommand(commandMap, "playtime", new BukkitCommandWrapper("playtime", new PlaytimeCommand(this), Collections.singletonList("ptm")));
      registerCommand(commandMap, "rosters", new BukkitCommandWrapper("rosters", new RostersCommand(this), Collections.singletonList("rt")));
      registerCommand(commandMap, "proxycheck", new BukkitCommandWrapper("proxycheck", new ProxyCheckCommand(this), Collections.singletonList("proxy")));
      setupDynamicCommands(commandMap);

      if (privateMessagesManager.isEnabled()) {
        registerCommand(commandMap, "msg", new BukkitCommandWrapper("msg", new PrivateMessageCommand(this), List.of("pm", "whisper", "w")));
        registerCommand(commandMap, "reply", new BukkitCommandWrapper("reply", new ReplyCommand(this), Collections.singletonList("r")));
      }

      channelManager.getChannels().values().forEach(info -> {
        if (info.name.equalsIgnoreCase("global")) return;
        if (info.name.equalsIgnoreCase("roster_template")) return;

        DynamicChannelBukkitCommand dynamicCommand = new DynamicChannelBukkitCommand(info, channelManager, logger);
        registerCommand(commandMap, dynamicCommand.getName(), dynamicCommand);
      });

      logger.info("&a✔ &9Registered &e" + registeredCommands.size() + " &9commands.");
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to register commands", exception);
    }
  }

  private void setupDynamicCommands(CommandMap commandMap) {
    FileConfiguration config = configManager.getConfig("config.yml");

    for (String league : rostersManager.getAvailableLeagues()) {
      String commandPath = "rosters.league_settings." + league + ".command_name";
      String aliasesPath = "rosters.league_settings." + league + ".command_aliases";

      String commandName = config.getString(commandPath);
      List<String> commandAliases = config.getStringList(aliasesPath);
      if (commandName == null || commandName.isEmpty()) { logger.info("No command name defined for league: " + league); continue; }

      TeamChannelCommand executor = new TeamChannelCommand(this, league);
      registerCommand(commandMap, commandName, new BukkitCommandWrapper(commandName, executor, commandAliases != null ? commandAliases : Collections.emptyList()));
    }
  }

  private void registerCommand(CommandMap commandMap, String name, Command command) {
    commandMap.register(plugin.getName().toLowerCase(), command);
    registeredCommands.add(name.toLowerCase());

    if (command.getAliases() != null || !command.getAliases().isEmpty()) registeredCommands.addAll(command.getAliases());

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
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Failed to unregister commands", exception);
    }
  }

  private void initializeConfigs() {
    configManager.createNewFile("config.yml", "Core Plugin Configuration");
    configManager.createNewFile("messages.yml", "Core Plugin Messages");
    configManager.createNewFile("menus.yml", "Core Plugin Menus Configuration");
    configManager.createNewFile("commands.yml", "Core Plugin Custom Commands Configuration");
    configManager.createNewFile("books.yml", "Core Plugin Custom Books Configuration");
  }

  private void setupConfig() {
    FileConfiguration file = configManager.getConfig("config.yml");
    Config.setFile(file);

    for (Config value : Config.values()) {
      setDefaultIfMissing(file, value.getPath(), value.getDefault());
    }

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

  public PlayerSettings getPlayerSettings(Player player) {
    return playerSettings.get(player.getUniqueId());
  }

  public void preloadSettings(Player player, PlayerData playerData) {
    PlayerSettings settings = getPlayerSettings(player);
    if (settings == null) {
      settings = new PlayerSettings();
      playerSettings.put(player.getUniqueId(), settings);
    }

    if (playerData.has("mention_sound.enabled")) settings.setMentionSoundEnabled((Boolean) playerData.get("mention_sound.enabled"));
    if (playerData.has("mention_sound.sound")) settings.setMentionSound(Sound.valueOf((String) playerData.get("mention_sound.sound")));
  }

  private void setupDependencies() throws IllegalStateException {
    RegisteredServiceProvider<LuckPerms> rsp = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
    this.luckPerms = rsp == null ? null : rsp.getProvider();
    if (luckPerms == null) throw new IllegalStateException("LuckPerms not found!");

    this.discordSRV = plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null;
    if (!discordSRV) logger.info("&cDiscordSRV not found! Discord Integration disabled.");

    this.placeholderAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    if (!placeholderAPI) logger.info("&cPlaceholderAPI not found! Placeholders features disabled.");

    this.authMe = plugin.getServer().getPluginManager().getPlugin("AuthMe") != null;
    if (!authMe) logger.info("&cAuthMe not found! We won't check if the player is logged in.");
    else AuthMeHook.initializeHook();

    RegisteredServiceProvider<Chat> chatRsp = plugin.getServer().getServicesManager().getRegistration(Chat.class);
    this.chat = chatRsp == null ? null : chatRsp.getProvider();
    if (chat == null) throw new IllegalStateException("Vault not found!");

    logger.info("&a✔ &9Hooked into &dLuckPerms&9, &dAuthMe &9and &dVault &9successfully!");
  }

  @SuppressWarnings("deprecation")
  private void initializeRosterSubscriptions() {
    Map<String, RosterInfo> allRosters = rostersManager.getRosters();
    if (allRosters == null || allRosters.isEmpty()) { logger.info("No active rosters found to initialize subscription."); return; }

    for (RosterInfo roster : allRosters.values()) {
      String channel = roster.getName().toLowerCase();
      for (String member : roster.getMembers()) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(member);
        if (!target.isOnline()) continue;
        channelManager.subscribe(target.getUniqueId(), channel);
        channelManager.setLastActiveChannel(target.getUniqueId(), channelManager.getDefaultChannel());
      }
    }
  }

  private void setDefaultIfMissing(FileConfiguration file, String path, Object value) {
    if (!file.isSet(path)) file.set(path, value);
  }

  public void saveAll() {
    rostersManager.saveRosters();
    configManager.saveAll();
    dataManager.saveAll();
  }

  private void sendBanner() {
    String[] banner = new String[] {"┏┓┏┓┳┓┏┓" + "&8 -+------------------------------------+-", "┃ ┃┃┣┫┣ " + "&7  Created by &b" + plugin.getDescription().getAuthors().stream().map(String::valueOf).collect(Collectors.joining(", ")) + "&7, version &f" + plugin.getDescription().getVersion(), "┗┛┗┛┛┗┗┛" + "&8 -+------------------------------------+-"};

    for (String line : banner) {
      plugin.getServer().getConsoleSender().sendMessage(logger.getConsolePrefix() + logger.color(line));
    }
  }
}