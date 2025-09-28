package io.github.divinerealms.core.listeners;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.PlayerSettingsManager;
import io.github.divinerealms.core.utilities.Logger;
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

import java.util.UUID;

public class ChatChannelListener implements Listener {
  private final CoreManager coreManager;
  private final Server server;
  private final Logger logger;
  private final ChannelManager channelManager;
  private final LuckPerms luckPerms;
  private final PlayerSettingsManager playerSettingsManager;

  private static final String PERM_BYPASS = "core.bypass.disabled-channel";
  private static final String PERM_COLOR = "core.chat.color";

  public ChatChannelListener(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.server = coreManager.getPlugin().getServer();
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
    this.luckPerms = coreManager.getLuckPerms();
    this.playerSettingsManager = coreManager.getPlayerSettingsManager();
  }

  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    String activeChannel = channelManager.getActiveChannel(player);
    ChannelManager.ChannelInfo info = channelManager.getChannels().get(activeChannel);
    if (info == null) return;

    if (channelManager.isChannelDisabled(activeChannel) && !player.hasPermission(PERM_BYPASS)) {
      event.setCancelled(true);
      logger.send(player, Lang.CHANNEL_DISABLED.replace(new String[]{activeChannel}));
      return;
    }

    event.setCancelled(true);

    String message = event.getMessage();
    if (!player.hasPermission(PERM_COLOR)) message = ChatColor.stripColor(logger.color(message));

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
      logger.sendActionBar(player, Lang.ANTI_SPAM.replace(null));
      return;
    }

    if (channelManager.isMentionsEnabled()) {
      Sound mentionSound;
      try {
        mentionSound = Sound.valueOf(channelManager.getMentionSound().toUpperCase());
      } catch (IllegalArgumentException ignored) {
        mentionSound = Sound.ORB_PICKUP;
      }

      for (Player target : Bukkit.getOnlinePlayers()) {
        String targetName = target.getName();
        boolean isMentioned = message.startsWith(targetName) || message.contains("@" + targetName);

        if (!isMentioned) continue;

        User user = luckPerms.getUserManager().getUser(target.getUniqueId());
        String prefixColor = user != null ? user.getCachedData().getMetaData().getPrefix()
            : (channelManager.getMentionColor() != null && !channelManager.getMentionColor().isEmpty()
            ? channelManager.getMentionColor() : "&e");

        message = message.replaceAll("(?<!\\S)@?" + targetName + "(?!\\S)", logger.color(prefixColor) + "@" + targetName + ChatColor.RESET);

        logger.sendActionBar(target, Lang.MENTION.replace(new String[]{player.getName()}));
        if (playerSettingsManager.isMentionSoundEnabled(player)) {
          target.playSound(target.getLocation(), mentionSound, 1.0F, 1.0F);
        }
      }
    }

    String formattedMessage = channelManager.formatChat(player, info.formats.minecraftChat, message, true);
    if (info.broadcast || info.permission == null || info.permission.isEmpty()) {
      server.broadcastMessage(logger.color(formattedMessage));
    } else {
      server.broadcast(logger.color(formattedMessage), info.permission);
    }

    if (coreManager.isDiscordSRV() && info.formats.minecraftToDiscord != null && !info.formats.minecraftToDiscord.isEmpty()) {
      channelManager.sendToDiscord(info, channelManager.formatChat(player, info.formats.minecraftToDiscord, event.getMessage(), false));
    }
  }
}
