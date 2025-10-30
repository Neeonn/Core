package io.github.divinerealms.core.listeners;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ClientBlocker;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class ClientBlockerListener implements Listener, PluginMessageListener {
  private final Logger logger;
  private final ClientBlocker clientBlocker;

  public ClientBlockerListener(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.clientBlocker = coreManager.getClientBlocker();
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] msg) {
    if (player == null || !clientBlocker.isEnabled()) return;

    String brand = msg.length > 1 ? new String(msg, StandardCharsets.UTF_8).substring(1) : "";
    String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "[UNKNOWN]";

    clientBlocker.setPlayerBrand(player, brand);
    if (clientBlocker.shouldKick(player)) {
      player.kickPlayer(Lang.CLIENT_BLOCKER_KICK.replace(new String[]{brand}));
      logger.send(ClientBlocker.NOTIFY_PERMISSION, Lang.CLIENT_BLOCKER_NOTIFY.replace(new String[]{player.getName(), ip, brand}));
      return;
    }

    logger.send(ClientBlocker.NOTIFY_PERMISSION, Lang.CLIENT_BLOCKER_BYPASS_NOTIFY.replace(new String[]{player.getName(), ip, brand}));
  }
}
