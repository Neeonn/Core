package io.github.divinerealms.core.listeners;

import io.github.divinerealms.core.config.Config;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.managers.PlayerSettingsManager;
import io.github.divinerealms.core.utilities.Logger;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEvents implements Listener {
  private final CoreManager coreManager;
  private final Logger logger;
  private final ChannelManager channelManager;
  private final ClientBlocker clientBlocker;
  private final PlayerSettingsManager playerSettingsManager;

  public PlayerEvents(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
    this.clientBlocker = coreManager.getClientBlocker();
    this.playerSettingsManager = coreManager.getPlayerSettingsManager();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    playerSettingsManager.loadPlayer(player);
    if (!Config.CONFIG.getBoolean("player_messages.custom_join.enabled", false)) return;
    event.setJoinMessage(null);
    if (player.hasPermission("core.silent-joinquit")) return;

    Bukkit.getScheduler().runTaskAsynchronously(coreManager.getPlugin(), () -> {
      boolean isDiscordSRV = coreManager.isDiscordSRV();

      String mcMsg = Config.CONFIG.getString("player_messages.custom_join.minecraft", ChatColor.YELLOW + player.getName() + " has joined the server");
      String dcMsg = isDiscordSRV ? Config.CONFIG.getString("player_messages.custom_join.discord", ":green_square: " + player.getName()) : null;

      if (coreManager.isPlaceholderAPI()) {
        mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
        if (isDiscordSRV) dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
      }

      String finalMcMsg = mcMsg;
      String finalDcMsg = isDiscordSRV ? ChatColor.translateAlternateColorCodes('&', dcMsg) : null;

      Bukkit.getScheduler().runTaskLater(coreManager.getPlugin(), () -> {
        if (clientBlocker.shouldKick(player)) {
          clientBlocker.removePlayer(player);
          return;
        }

        logger.broadcast(finalMcMsg);
        if (isDiscordSRV) {
          ChannelManager.ChannelInfo info = channelManager.getChannels().get(channelManager.getDefaultChannel());
          Bukkit.getScheduler().runTaskAsynchronously(coreManager.getPlugin(), () -> channelManager.sendToDiscord(info, finalDcMsg));
        }
      }, 10L);
    });
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    playerSettingsManager.savePlayer(player);
    playerSettingsManager.removePlayer(player);

    if (!Config.CONFIG.getBoolean("player_messages.custom_quit.enabled", false)) return;

    event.setQuitMessage(null);

    if (player.hasPermission("core.silent-joinquit")) return;

    Bukkit.getScheduler().runTaskAsynchronously(coreManager.getPlugin(), () -> {
      boolean isDiscordSRV = coreManager.isDiscordSRV();

      String mcMsg = Config.CONFIG.getString("player_messages.custom_quit.minecraft", ChatColor.YELLOW + player.getName() + " left the server");
      String dcMsg = isDiscordSRV ? Config.CONFIG.getString("player_messages.custom_quit.discord", ":red_square: " + player.getName()) : null;

      if (coreManager.isPlaceholderAPI()) {
        mcMsg = PlaceholderAPI.setPlaceholders(player, mcMsg);
        if (isDiscordSRV) dcMsg = PlaceholderAPI.setPlaceholders(player, dcMsg);
      }

      String finalMcMsg = mcMsg;
      String finalDcMsg = isDiscordSRV ? ChatColor.translateAlternateColorCodes('&', dcMsg) : null;

      Bukkit.getScheduler().runTaskLater(coreManager.getPlugin(), () -> {
        if (!clientBlocker.shouldKick(player)) {
          logger.broadcast(finalMcMsg);
          if (isDiscordSRV) {
            ChannelManager.ChannelInfo info = channelManager.getChannels().get(channelManager.getDefaultChannel());
            Bukkit.getScheduler().runTaskAsynchronously(coreManager.getPlugin(), () -> channelManager.sendToDiscord(info, finalDcMsg));
          }
        }
      }, 2L);
    });
  }
}
