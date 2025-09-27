package io.github.divinerealms.core.main;

import github.scarsz.discordsrv.DiscordSRV;
import io.github.divinerealms.core.listeners.ChatChannelListener;
import io.github.divinerealms.core.listeners.ClientBlockerListener;
import io.github.divinerealms.core.listeners.DiscordMessageListener;
import io.github.divinerealms.core.listeners.PlayerEvents;
import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

@Getter
public class ListenerManager {
  private final CoreManager coreManager;
  private final Plugin plugin;
  private final Server server;

  private ClientBlockerListener clientBlockerListener;
  private DiscordMessageListener discordMessageListener;

  public ListenerManager(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.plugin = coreManager.getPlugin();
    this.server = coreManager.getPlugin().getServer();
  }

  public void registerAll() {
    unregisterAll();

    server.getPluginManager().registerEvents(new ChatChannelListener(coreManager), plugin);
    server.getPluginManager().registerEvents(new PlayerEvents(coreManager), plugin);

    if (coreManager.isDiscordSRV() && discordMessageListener == null) {
      DiscordSRV.api.subscribe(discordMessageListener = new DiscordMessageListener(coreManager));
      coreManager.getLogger().info("&a✔ &dDiscordSRV &9found and &eDiscord Integration &9enabled.");
    }

    if (coreManager.getClientBlocker().isEnabled()) enableClientBlocker();
  }

  public void unregisterAll() {
    HandlerList.unregisterAll(plugin);

    if (discordMessageListener != null) {
      DiscordSRV.api.unsubscribe(discordMessageListener);
      discordMessageListener = null;
    }

    disableClientBlocker();
  }

  public void enableClientBlocker() {
    if (clientBlockerListener != null) return;

    clientBlockerListener = new ClientBlockerListener(coreManager);
    server.getPluginManager().registerEvents(clientBlockerListener, plugin);

    server.getMessenger().registerIncomingPluginChannel(plugin, "MC|Brand", clientBlockerListener);
    server.getMessenger().registerOutgoingPluginChannel(plugin, "MC|Brand");

    coreManager.getLogger().info("&a✔ &eClientBlocker &aenabled&9 and plugin channels registered.");
  }

  public void disableClientBlocker() {
    if (clientBlockerListener == null) return;

    HandlerList.unregisterAll(clientBlockerListener);

    server.getMessenger().unregisterIncomingPluginChannel(plugin, "MC|Brand", clientBlockerListener);
    server.getMessenger().unregisterOutgoingPluginChannel(plugin, "MC|Brand");

    clientBlockerListener = null;
    coreManager.getLogger().info("&a✔ &eClientBlocker &cdisabled&9 and plugin channels unregistered.");
  }
}