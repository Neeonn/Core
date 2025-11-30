package io.github.divinerealms.core.listeners;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.ResultManager;
import io.github.divinerealms.core.utilities.AuthMeHook;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import io.github.divinerealms.core.utilities.PlayerSettings;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;

import static io.github.divinerealms.core.utilities.Permissions.PERM_BYPASS_DISABLED_CHANNEL;
import static io.github.divinerealms.core.utilities.Permissions.PERM_CHAT_COLOR;

public class ChatChannelListener implements Listener {
  private final CoreManager coreManager;
  private final Plugin plugin;
  private final Server server;
  private final BukkitScheduler scheduler;
  private final Logger logger;
  private final ChannelManager channelManager;
  private final LuckPerms luckPerms;
  private final ResultManager resultManager;

  public ChatChannelListener(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.plugin = coreManager.getPlugin();
    this.server = plugin.getServer();
    this.scheduler = server.getScheduler();
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
    this.luckPerms = coreManager.getLuckPerms();
    this.resultManager = coreManager.getResultManager();
  }

  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    String activeChannel = channelManager.getActiveChannel(player);
    ChannelInfo info = channelManager.getChannels().get(activeChannel);
    if (info == null) return;

    if (!AuthMeHook.isAuthenticated(player)) {
      event.setCancelled(true);
      return;
    }

    if (channelManager.isChannelDisabled(activeChannel) && !player.hasPermission(PERM_BYPASS_DISABLED_CHANNEL)) {
      event.setCancelled(true);
      logger.send(player, Lang.CHANNEL_DISABLED.replace(new String[]{activeChannel}));
      return;
    }

    boolean isBroadcast = info.broadcast || info.permission == null || info.permission.isEmpty();
    if (!isBroadcast && !player.hasPermission(info.permission)) {
      String defaultChannel = channelManager.getDefaultChannel();
      if (defaultChannel == null) return;

      channelManager.setLastActiveChannel(player.getUniqueId(), defaultChannel);
      logger.send(player, Lang.CHANNEL_RESET_PERMISSION.replace(new String[]{activeChannel.toUpperCase(), defaultChannel.toUpperCase()}));
      event.setCancelled(true);
      return;
    }

    event.setCancelled(true);

    String message = event.getMessage();
    if (!player.hasPermission(PERM_CHAT_COLOR)) message = ChatColor.stripColor(logger.color(message));
    final String initialMessage = message;

    UUID uuid = player.getUniqueId();
    long now = System.currentTimeMillis();

    channelManager.getMcLastMessageTime().putIfAbsent(uuid, now);
    if (now - channelManager.getMcLastMessageTime().get(uuid) > channelManager.getMcCooldownMs()) {
      channelManager.getMcMessageCount().put(uuid, 0);
      channelManager.getMcLastMessageTime().put(uuid, now);
    }

    channelManager.getMcMessageCount().put(uuid, channelManager.getMcMessageCount().getOrDefault(uuid, 0) + 1);

    if (channelManager.getMcMessageCount().get(uuid) > channelManager.getMcMaxMessages()) {
      event.setCancelled(true);
      logger.sendActionBar(player, Lang.ANTI_SPAM_MESSAGES.replace(null));
      return;
    }

    scheduler.runTask(plugin, () -> {
      String messageToSend = initialMessage;

      boolean fcMatchesEnabled = PlaceholderAPI.setPlaceholders(null, "%fc_enabled%").equals("YES");
      if (channelManager.isMentionsEnabled() && fcMatchesEnabled) {
        Sound mentionSound;
        try {
          mentionSound = Sound.valueOf(channelManager.getMentionSound().toUpperCase());
        } catch (IllegalArgumentException ignored) {
          mentionSound = Sound.ORB_PICKUP;
        }

        for (Player target : coreManager.getCachedPlayers()) {
          String targetName = target.getName();
          boolean isMentioned = false;

          if (messageToSend.contains("@" + targetName)) isMentioned = true;
          else if (messageToSend.startsWith(targetName)) {
            int nameEndIndex = targetName.length();
            if (messageToSend.length() == nameEndIndex || !Character.isLetterOrDigit(messageToSend.charAt(nameEndIndex))) isMentioned = true;
          }

          if (!isMentioned) continue;
          if (!activeChannel.equals(channelManager.getActiveChannel(target))) continue;

          User user = luckPerms.getUserManager().getUser(target.getUniqueId());
          String prefixColor = user != null ? user.getCachedData().getMetaData().getPrefix()
              : (channelManager.getMentionColor() != null && !channelManager.getMentionColor().isEmpty()
              ? channelManager.getMentionColor() : "&7");
          final String coloredName = logger.color(prefixColor) + "@" + targetName + ChatColor.RESET;

          if (messageToSend.contains("@" + targetName)) messageToSend = messageToSend.replace("@" + targetName, coloredName);
          else if (messageToSend.startsWith(targetName)) messageToSend = coloredName + messageToSend.substring(targetName.length());

          logger.sendActionBar(target, Lang.MENTION.replace(new String[]{player.getName()}));

          PlayerSettings settings = coreManager.getPlayerSettings(target);
          if (settings != null && settings.isMentionSoundEnabled()) {
            target.playSound(target.getLocation(), mentionSound, 1.0F, 1.0F);
          }
        }
      }

      final String formattedMessage = activeChannel.equals("host") ?
          channelManager.formatChat(player, info.formats.minecraftChat.replace("{prefix-host}", resultManager.getPrefix()), messageToSend, true) :
          channelManager.formatChat(player, info.formats.minecraftChat, messageToSend, true);

      channelManager.getSocialSpy().stream()
          .filter(playerId -> !playerId.equals(player.getUniqueId()) && !isBroadcast)
          .map(Bukkit::getPlayer)
          .filter(spy -> spy != null && !spy.hasPermission(info.permission))
          .forEach(spy -> logger.send(spy, Lang.CHANNEL_SPY_PREFIX.replace(new String[]{activeChannel.toUpperCase()}) + formattedMessage));

      if (isBroadcast) server.broadcastMessage(logger.color(formattedMessage));
      else server.broadcast(logger.color(formattedMessage), info.permission);

      if (coreManager.isDiscordSRV() && info.formats.minecraftToDiscord != null && !info.formats.minecraftToDiscord.isEmpty()) {
        channelManager.sendToDiscord(info, channelManager.formatChat(player,
            info.formats.minecraftToDiscord.replace("{prefix-host}", resultManager.getPrefix()), initialMessage, false));
      }
    });
  }
}
