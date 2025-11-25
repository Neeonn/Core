package io.github.divinerealms.core.listeners;

import fr.xephi.authme.events.LoginEvent;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.managers.PlaytimeManager;
import io.github.divinerealms.core.utilities.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.github.divinerealms.core.utilities.Constants.NEWBIE_THRESHOLD_HOURS;

public class PlayerEvents implements Listener {
  private final CoreManager coreManager;
  private final Plugin plugin;
  private final Logger logger;
  private final ClientBlocker clientBlocker;
  private final LuckPerms luckPerms;
  private final PlaytimeManager playtimeManager;
  private final BukkitScheduler scheduler;

  public PlayerEvents(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.plugin = coreManager.getPlugin();
    this.logger = coreManager.getLogger();
    this.clientBlocker = coreManager.getClientBlocker();
    this.luckPerms = coreManager.getLuckPerms();
    this.playtimeManager = coreManager.getPlaytimeManager();
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
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    coreManager.getCachedPlayers().add(event.getPlayer());
  }
}
