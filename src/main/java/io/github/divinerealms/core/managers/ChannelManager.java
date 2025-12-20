package io.github.divinerealms.core.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import io.github.divinerealms.core.configs.Config;
import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.ChannelFormats;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.divinerealms.core.utilities.Permissions.PERM_BYPASS_DISABLED_CHANNEL;
import static io.github.divinerealms.core.utilities.Permissions.PERM_CHAT_COLOR;

public class ChannelManager {
  private final CoreManager coreManager;
  private final Logger logger;

  private final Map<UUID, Set<String>> playerChannels = new HashMap<>();
  @Getter private final Set<String> disabledChannels = new HashSet<>();
  @Getter private final Map<String, List<String>> discordIdToMinecraft = new HashMap<>();

  @Getter private final Map<String, ChannelInfo> channels = new HashMap<>();
  @Getter @Setter private String defaultChannel = "global";

  private final Map<UUID, String> lastActiveChannel = new ConcurrentHashMap<>();
  private final Map<UUID, String> lastChannelUsed = new ConcurrentHashMap<>();

  @Getter private boolean mentionsEnabled;
  @Getter private String mentionColor;
  @Getter private String mentionSound;
  @Getter private int mcMaxMessages;
  @Getter private long mcCooldownMs;

  @Getter private final ConcurrentHashMap<UUID, Integer> mcMessageCount = new ConcurrentHashMap<>();
  @Getter private final ConcurrentHashMap<UUID, Long> mcLastMessageTime = new ConcurrentHashMap<>();
  @Getter private final Map<String, String> discordLastMessage = new ConcurrentHashMap<>();

  @Getter private final Set<UUID> socialSpy = ConcurrentHashMap.newKeySet();

  public ChannelManager(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
  }

  public void reloadAll() {
    loadChannels();
  }

  public void clearAllState() {
    playerChannels.clear();
    disabledChannels.clear();
    discordIdToMinecraft.clear();
    lastActiveChannel.clear();
    lastChannelUsed.clear();
    mcMessageCount.clear();
    mcLastMessageTime.clear();
    discordLastMessage.clear();
    socialSpy.clear();
  }

  public Set<String> getChannels(UUID uuid) {
    return playerChannels.getOrDefault(uuid, new HashSet<>());
  }

  public void setLastActiveChannel(UUID uuid, String channelName) {
    lastActiveChannel.put(uuid, channelName);
  }

  public String getActiveChannel(Player player) {
    UUID uuid = player.getUniqueId();

    if (lastActiveChannel.containsKey(uuid)) {
      String channel = lastActiveChannel.get(uuid);
      if (channels.containsKey(channel)) return channel;
    }

    return getChannels(uuid).stream().findFirst().orElse(defaultChannel);
  }

  public void setLastChannelUsed(UUID uuid, String channelName) {
    lastChannelUsed.put(uuid, channelName);
  }

  public String getLastChannelUsed(Player player) {
    UUID uuid = player.getUniqueId();

    if (lastChannelUsed.containsKey(uuid)) {
      String channel = lastChannelUsed.get(uuid);
      if (channels.containsKey(channel)) return channel;
    }

    return getChannels(uuid).stream().findFirst().orElse(defaultChannel);
  }

  public boolean switchChannel(UUID uuid, String channel) {
    Set<String> channels = playerChannels.computeIfAbsent(uuid, k -> new HashSet<>());

    if (channels.contains(channel)) {
      channels.remove(channel);
      if (channels.isEmpty()) playerChannels.remove(uuid);

      if (lastActiveChannel.containsKey(uuid) && lastActiveChannel.get(uuid).equalsIgnoreCase(channel))
        setLastActiveChannel(uuid, defaultChannel);

      return false;
    } else {
      channels.add(channel);
      setLastActiveChannel(uuid, channel);
      return true;
    }
  }

  public void subscribe(UUID uuid, String channel) {
    playerChannels.computeIfAbsent(uuid, k -> new HashSet<>()).add(channel);
  }

  public void unsubscribe(UUID uuid, String channel) {
    Set<String> channels = playerChannels.get(uuid);
    if (channels != null) {
      channels.remove(channel);
      if (channels.isEmpty()) playerChannels.remove(uuid);
    }
  }

  public Set<UUID> getSubscribers(String channel) {
    String lowerChannel = channel.toLowerCase();
    Set<UUID> subscribers = new HashSet<>();
    for (Map.Entry<UUID, Set<String>> entry : playerChannels.entrySet()) if (entry.getValue().contains(lowerChannel)) subscribers.add(entry.getKey());
    return subscribers;
  }

  public boolean toggleChannel(String channel) {
    if (disabledChannels.contains(channel)) {
      disabledChannels.remove(channel);
      return false;
    } else {
      disabledChannels.add(channel);
      return true;
    }
  }

  public boolean toggleSocialSpy(Player player) {
    if (socialSpy.contains(player.getUniqueId())) {
      socialSpy.remove(player.getUniqueId());
      return false;
    } else {
      socialSpy.add(player.getUniqueId());
      return true;
    }
  }

  public boolean isChannelDisabled(String channel) {
    return disabledChannels.contains(channel);
  }

  public void loadChannels() {
    channels.clear();
    discordIdToMinecraft.clear();

    if (!Config.CHANNELS_ENABLED.getValue(Boolean.class)) return;

    defaultChannel = Config.CHANNELS_DEFAULT.getValue(String.class);
    mentionsEnabled = Config.CONFIG.getBoolean("channels.mentions.enabled", true);
    mentionColor = Config.CONFIG.getString("channels.mentions.format", "&e");
    mentionSound = Config.CONFIG.getString("channels.mentions.sound", "LEVEL_UP");
    mcMaxMessages = Config.CONFIG.getInt("channels.anti-spam.max-messages", 5);
    mcCooldownMs = Config.CONFIG.getLong("channels.anti-spam.cooldown", 2500);

    if (Config.CONFIG.isConfigurationSection("channels.list")) {
      for (String key : Config.CONFIG.getConfigurationSection("channels.list").getKeys(false)) {
        if (key.equalsIgnoreCase("roster_template")) continue;
        String path = "channels.list." + key;

        ChannelFormats formats = new ChannelFormats(
            Config.CONFIG.getString(path + ".formats.minecraft_chat", "%player%: %message%"),
            Config.CONFIG.getString(path + ".formats.discord_to_minecraft", "%player%: %message%"),
            Config.CONFIG.getString(path + ".formats.minecraft_to_discord", "%player%: %message%")
        );

        String discordId = Config.CONFIG.getString(path + ".discord_id", "");
        ChannelInfo info = new ChannelInfo(
            key,
            Config.CONFIG.getString(path + ".permission", ""),
            discordId,
            formats,
            Config.CONFIG.getBoolean(path + ".broadcast", false),
            Config.CONFIG.getStringList(path + ".aliases")
        );

        channels.put(key, info);
        if (discordId != null && !discordId.isEmpty()) discordIdToMinecraft.computeIfAbsent(discordId, k -> new ArrayList<>()).add(key);
      }
    }

    for (UUID uuid : new HashSet<>(playerChannels.keySet())) {
      Set<String> subs = playerChannels.get(uuid);
      subs.removeIf(channel -> !channels.containsKey(channel));
      if (subs.isEmpty()) subs.add(defaultChannel);
    }

    logger.info("&a✔ &9Loaded &e" + channels.size() + " &9chat channels from config.");
  }

  public void sendMessage(CommandSender sender, String channel, String message) {
    ChannelInfo info = channels.get(channel);
    if (info == null) { logger.send(sender, Lang.CHANNEL_NOT_FOUND.replace(new String[]{channel})); return; }

    Player player = sender instanceof Player ? (Player) sender : null;

    if (message == null || message.isEmpty()) {
      if (player == null) { logger.send(sender, Lang.INGAME_ONLY.replace(null)); return; }

      boolean nowOn = switchChannel(player.getUniqueId(), channel);
      logger.send(player, Lang.CHANNEL_TOGGLE.replace(new String[]{channel.toUpperCase(), nowOn ? Lang.ON.replace(null) : Lang.OFF.replace(null)}));
      return;
    }

    if (player == null) {
      String formatted = info.formats.minecraftChat
          .replace("%player_name", "&cConsole").replace("%essentials_nickname%", "&cConsole")
          .replace("%message%", message).replaceAll("%[^%]+%", "").trim();
      logger.send(info.permission, formatted);
      if (coreManager.isDiscordSRV() && info.formats.minecraftToDiscord != null && !info.formats.minecraftToDiscord.isEmpty())
        sendToDiscord(info, "Console » " + message);
      return;
    }

    if (isChannelDisabled(channel) && !player.hasPermission(PERM_BYPASS_DISABLED_CHANNEL)) { logger.send(player, Lang.CHANNEL_DISABLED.replace(new String[]{channel})); return; }

    String formattedMessage = formatChat(player, info.formats.minecraftChat, message, true);
    boolean isSenderSubscribed = getChannels(player.getUniqueId()).contains(channel);
    boolean isBroadcast = info.broadcast;
    boolean hasPermission = info.permission != null && !info.permission.isEmpty() && player.hasPermission(info.permission);

    socialSpy.forEach(spyUUID -> {
      if (spyUUID.equals(player.getUniqueId())) return;
      if (isBroadcast) return;

      Player spy = Bukkit.getPlayer(spyUUID);
      if (spy == null) return;
      if (getSubscribers(channel).contains(spyUUID)) return;

      String senderName = coreManager.getChat().getPlayerPrefix(player) + player.getDisplayName();
      logger.send(spy, Lang.CHANNEL_SPY_FORMAT.replace(new String[]{channel.toUpperCase(), senderName, message}));
    });

    if (!isBroadcast && !isSenderSubscribed && !hasPermission) { logger.send(player, Lang.CHANNEL_NOT_SUBSCRIBED.replace(new String[]{channel.toUpperCase()})); return; }

    if (isBroadcast) {
      logger.broadcast(formattedMessage);
    } else if (isSenderSubscribed) {
      Set<UUID> subscribers = getSubscribers(channel);
      subscribers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> logger.send(p, formattedMessage));
      if (!subscribers.contains(player.getUniqueId())) logger.send(player, formattedMessage);
      logger.info(formattedMessage);
    } else logger.send(info.permission, formattedMessage);

    if (info.formats.minecraftToDiscord != null && !info.formats.minecraftToDiscord.isEmpty()) {
      sendToDiscord(info, formatChat(player, info.formats.minecraftToDiscord, message, false));
    }
  }

  public String formatChat(Player player, String format, String message, boolean colorMessage) {
    String formatted = format.replace("%message%", "{MESSAGE}");

    if (player != null && coreManager.isPlaceholderAPI()) {
      formatted = PlaceholderAPI.setPlaceholders(player, formatted);
      if (formatted.contains("%")) formatted = PlaceholderAPI.setPlaceholders(player, formatted);
    }

    formatted = logger.color(formatted);

    String msgPart = colorMessage && player != null && player.hasPermission(PERM_CHAT_COLOR) ? logger.color(message) : message;
    return formatted.replace("{MESSAGE}", msgPart);
  }

  public void sendToDiscord(ChannelInfo channelInfo, String message) {
    if (DiscordSRV.getPlugin() == null) return;
    ChannelInfo defaultInfo = channels.get(defaultChannel);
    String discordId = (channelInfo.discordId == null || channelInfo.discordId.isEmpty())
        ? (channelInfo.broadcast ? defaultInfo.discordId : "")
        : channelInfo.discordId;

    if (discordId == null || discordId.isEmpty()) return;
    TextChannel discordChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordId);
    if (discordChannel == null) return;

    TextChannel consoleChannel = DiscordSRV.getPlugin().getConsoleChannel();
    if (consoleChannel != null && consoleChannel.getId().equals(discordId)) return;

    String lastMessage = discordLastMessage.get(discordId);
    if (lastMessage != null && lastMessage.equalsIgnoreCase(message)) return;

    discordChannel.sendMessage(ChatColor.stripColor(message)).queue();
    discordLastMessage.put(discordId, message);
  }

  public void createRosterChannel(String rosterChannelName, String discordId) {
    String uniqueChannelName = rosterChannelName.toLowerCase();
    if (channels.containsKey(uniqueChannelName)) return;

    String templatePath = "channels.list.roster_template";
    if (!Config.CONFIG.isConfigurationSection(templatePath)) {
      logger.info("Missing \"roster_template\" configuration section in config.yml");
      return;
    }

    ChannelFormats formats = new ChannelFormats(
        Config.CONFIG.getString(templatePath + ".formats.minecraft_chat", "%player%: %message%"),
        Config.CONFIG.getString(templatePath + ".formats.discord_to_minecraft", "%player%: %message%"),
        Config.CONFIG.getString(templatePath + ".formats.minecraft_to_discord", "%player%: %message%")
    );

    ChannelInfo info = new ChannelInfo(uniqueChannelName, null, discordId, formats, false, Collections.emptyList());
    channels.put(uniqueChannelName, info);
    if (discordId != null && !discordId.isEmpty()) discordIdToMinecraft.computeIfAbsent(discordId, k -> new ArrayList<>()).add(uniqueChannelName);
  }

  public void handlePlayerSubscriptions(Player player) {
    if (coreManager.getRostersManager() == null) return;

    UUID uuid = player.getUniqueId();
    String playerName = player.getName();

    coreManager.getRostersManager().getPlayerRosters(playerName).values().forEach(roster -> {
      String rosterChannelName = roster.getName().toLowerCase();
      if (channels.containsKey(rosterChannelName)) subscribe(uuid, rosterChannelName);
    });

    String defaultChannel = getDefaultChannel();

    channels.values().forEach(channel -> {
      String channelName = channel.name;
      if (channelName.equalsIgnoreCase(defaultChannel)) subscribe(uuid, channelName);
      if (channel.broadcast) subscribe(uuid, channelName);
      if (channel.permission != null && !channel.permission.isEmpty() && player.hasPermission(channel.permission)) subscribe(uuid, channelName);
    });

    setLastActiveChannel(uuid, defaultChannel);
  }
}
