package io.github.divinerealms.core.listeners;

import fr.xephi.authme.events.LoginEvent;
import io.github.divinerealms.core.config.Config;
import io.github.divinerealms.core.config.PlayerData;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.managers.PlayerDataManager;
import io.github.divinerealms.core.managers.PlaytimeManager;
import io.github.divinerealms.core.utilities.AuthMeHook;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerEvents implements Listener {
  private final CoreManager coreManager;
  private final Plugin plugin;
  private final Logger logger;
  private final ChannelManager channelManager;
  private final ClientBlocker clientBlocker;
  private final LuckPerms luckPerms;
  private final PlaytimeManager playtimeManager;
  private final PlayerDataManager dataManager;
  private final BukkitScheduler scheduler;

  private static final long NEWBIE_THRESHOLD_HOURS = 2;
  private static final String PERM_SILENT_JOIN_QUIT = "core.silent-joinquit";
  private static final String PATH_PLAYER_MESSAGES = "player_messages.custom_";

  public PlayerEvents(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.plugin = coreManager.getPlugin();
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
    this.clientBlocker = coreManager.getClientBlocker();
    this.luckPerms = coreManager.getLuckPerms();
    this.playtimeManager = coreManager.getPlaytimeManager();
    this.dataManager = coreManager.getDataManager();
    this.scheduler = coreManager.getPlugin().getServer().getScheduler();
  }

  @EventHandler
  public void onLogin(LoginEvent event) {
    Player player = event.getPlayer();
    final UUID playerId = player.getUniqueId();

    if (clientBlocker.shouldKick(player)) {
      clientBlocker.removePlayer(player);
      return;
    }

    scheduler.runTaskAsynchronously(coreManager.getPlugin(), () -> {
      Player asyncPlayer = plugin.getServer().getPlayer(playerId);
      if (asyncPlayer == null || !asyncPlayer.isOnline()) return;

      PlayerData playerData = dataManager.get(asyncPlayer);
      dataManager.addDefaults(playerData);
      coreManager.preloadSettings(asyncPlayer, playerData);

      long playtime = playtimeManager.getPlaytime(playerId);
      long hoursPlayed = playtime / 20 / 3600;

      luckPerms.getUserManager().modifyUser(playerId, user -> {
        Node newbieNode = user.getNodes().stream().filter(node -> node.getKey().equalsIgnoreCase("group.newbie")).findFirst().orElse(null);

        boolean removed = false, added = false;
        if (hoursPlayed >= NEWBIE_THRESHOLD_HOURS) {
          if (newbieNode != null) {
            user.data().remove(newbieNode);
            removed = true;
          }
        } else if (newbieNode == null) {
          Node node = Node.builder("group.newbie").expiry(5, TimeUnit.DAYS).build();
          user.data().add(node);
          added = true;
        }

        if (removed || added) {
          logger.info(removed
              ? "&fPlayer &b" + player.getDisplayName() + "&f reached &c" + NEWBIE_THRESHOLD_HOURS + "&f hours and had the newbie rank automatically removed."
              : "&fNew/low-playtime player &b" + player.getDisplayName() + "&f got assigned the newbie rank.");
        }
      });
    });

    if (!Config.CONFIG.getBoolean(PATH_PLAYER_MESSAGES + "join.enabled", false)) return;
    if (player.hasPermission(PERM_SILENT_JOIN_QUIT)) return;
    boolean isDiscordSRV = coreManager.isDiscordSRV();

    String mcMsg = Config.CONFIG.getString(PATH_PLAYER_MESSAGES + "join.minecraft", ChatColor.YELLOW + player.getName() + " has joined the server");
    String dcMsg = Config.CONFIG.getString(PATH_PLAYER_MESSAGES + "join.discord", ":green_square: " + player.getName());

    if (coreManager.isPlaceholderAPI()) {
      mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
      if (mcMsg.contains("%")) mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
      if (isDiscordSRV) {
        dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
        if (dcMsg.contains("%")) dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
      }
    }

    String finalMcMsg = mcMsg;
    String finalDcMsg = ChatColor.translateAlternateColorCodes('&', dcMsg);

    logger.broadcast(finalMcMsg);
    if (isDiscordSRV) {
      ChannelInfo info = channelManager.getChannels().get(channelManager.getDefaultChannel());
      channelManager.sendToDiscord(info, ChatColor.translateAlternateColorCodes('&', finalDcMsg));
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (Config.CONFIG.getBoolean(PATH_PLAYER_MESSAGES + "join.enabled", false)) event.setJoinMessage(null);
    coreManager.getCachedPlayers().add(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (Config.CONFIG.getBoolean(PATH_PLAYER_MESSAGES + "quit.enabled", false)) event.setQuitMessage(null);

    coreManager.getCachedPlayers().remove(player);
    scheduler.runTaskAsynchronously(plugin, () -> dataManager.unload(player));
    coreManager.getPlayerSettings().remove(playerId);

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

    if (!Config.CONFIG.getBoolean(PATH_PLAYER_MESSAGES + "quit.enabled", false)) return;
    if (player.hasPermission(PERM_SILENT_JOIN_QUIT)) return;
    if (!AuthMeHook.isAuthenticated(player)) return;

    if (!clientBlocker.shouldKick(player)) {
        logger.broadcast(finalMcMsg);
        if (isDiscordSRV) {
          ChannelInfo info = channelManager.getChannels().get(channelManager.getDefaultChannel());
          channelManager.sendToDiscord(info, finalDcMsg);
        }
      }
  }
}
