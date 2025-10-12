package io.github.divinerealms.core.listeners;

import io.github.divinerealms.core.config.Config;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.managers.PlayerSettingsManager;
import io.github.divinerealms.core.managers.PlaytimeManager;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.TimeUnit;

public class PlayerEvents implements Listener {
  private final CoreManager coreManager;
  private final Logger logger;
  private final ChannelManager channelManager;
  private final ClientBlocker clientBlocker;
  private final PlayerSettingsManager playerSettingsManager;
  private final LuckPerms luckPerms;
  private final PlaytimeManager playtimeManager;

  private static final String PERM_SILENT_JOIN_QUIT = "core.silent-joinquit";
  private static final String PATH_PLAYER_MESSAGES = "player_messages.custom_";

  public PlayerEvents(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
    this.clientBlocker = coreManager.getClientBlocker();
    this.playerSettingsManager = coreManager.getPlayerSettingsManager();
    this.luckPerms = coreManager.getLuckPerms();
    this.playtimeManager = coreManager.getPlaytimeManager();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    playerSettingsManager.loadPlayer(player);

    long playtime = playtimeManager.getPlaytime(player.getUniqueId());
    long hoursPlayed = playtime / 20 / 3600;

    if (!player.hasPlayedBefore() || hoursPlayed < 5) {
      luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
        boolean hasNewbie = user.getNodes().stream().anyMatch(node -> node.getKey().equalsIgnoreCase("group.newbie"));
        if (!hasNewbie) {
          Node node = Node.builder("group.newbie").expiry(30, TimeUnit.DAYS).build();
          user.data().add(node);
          logger.info("&fNew player &b" + player.getDisplayName() + "&f got assigned a newbie rank.");
        }
      });
    }

    if (!Config.CONFIG.getBoolean(PATH_PLAYER_MESSAGES + "join.enabled", false)) return;
    event.setJoinMessage(null);
    if (player.hasPermission(PERM_SILENT_JOIN_QUIT)) return;

    boolean isDiscordSRV = coreManager.isDiscordSRV();

    String mcMsg = Config.CONFIG.getString(PATH_PLAYER_MESSAGES + "join.minecraft", ChatColor.YELLOW + player.getName() + " has joined the server");
    String dcMsg = isDiscordSRV ? Config.CONFIG.getString(PATH_PLAYER_MESSAGES + "join.discord", ":green_square: " + player.getName()) : null;

    if (coreManager.isPlaceholderAPI()) {
      mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
      if (mcMsg.contains("%")) mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
      if (isDiscordSRV) {
        dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
        if (dcMsg.contains("%")) dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
      }
    }

    String finalMcMsg = mcMsg;
    String finalDcMsg = isDiscordSRV ? ChatColor.translateAlternateColorCodes('&', dcMsg) : null;

    Bukkit.getScheduler().runTaskLaterAsynchronously(coreManager.getPlugin(), () -> {
      if (clientBlocker.shouldKick(player)) { clientBlocker.removePlayer(player); return; }

      logger.broadcast(finalMcMsg);
      if (isDiscordSRV) {
        ChannelInfo info = channelManager.getChannels().get(channelManager.getDefaultChannel());
        channelManager.sendToDiscord(info, finalDcMsg);
      }
    }, 10L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    playerSettingsManager.savePlayer(player);
    playerSettingsManager.removePlayer(player);

    if (!Config.CONFIG.getBoolean(PATH_PLAYER_MESSAGES + "quit.enabled", false)) return;

    event.setQuitMessage(null);

    if (player.hasPermission(PERM_SILENT_JOIN_QUIT)) return;

    boolean isDiscordSRV = coreManager.isDiscordSRV();

    String mcMsg = Config.CONFIG.getString(PATH_PLAYER_MESSAGES + "quit.minecraft", ChatColor.YELLOW + player.getName() + " left the server");
    String dcMsg = isDiscordSRV ? Config.CONFIG.getString(PATH_PLAYER_MESSAGES + "quit.discord", ":red_square: " + player.getName()) : null;

    if (coreManager.isPlaceholderAPI()) {
      mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
      if (mcMsg.contains("%")) mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
      if (isDiscordSRV) {
        dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
        if (dcMsg.contains("%")) dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
      }
    }

    String finalMcMsg = mcMsg;
    String finalDcMsg = isDiscordSRV ? ChatColor.translateAlternateColorCodes('&', dcMsg) : null;

    Bukkit.getScheduler().runTaskAsynchronously(coreManager.getPlugin(), () -> {
      if (!clientBlocker.shouldKick(player)) {
        logger.broadcast(finalMcMsg);
        if (isDiscordSRV) {
          ChannelInfo info = channelManager.getChannels().get(channelManager.getDefaultChannel());
          channelManager.sendToDiscord(info, finalDcMsg);
        }
      }
    });
  }
}
