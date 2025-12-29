package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.configs.PlayerData;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class PlayerDataManager {
  private final CoreManager coreManager;
  private final Plugin plugin;
  private final ConfigManager configManager;
  private final Logger logger;
  private final Map<String, PlayerData> playerCache = new ConcurrentHashMap<>();

  private final Map<String, String> uuidCache = new ConcurrentHashMap<>();

  private final Queue<String> dataQueue = new ConcurrentLinkedQueue<>();
  private final Set<String> dataQueueSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private volatile boolean saveScheduled = false;

  public PlayerDataManager(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.plugin = coreManager.getPlugin();
    this.configManager = coreManager.getConfigManager();
    this.logger = coreManager.getLogger();
  }

  public PlayerData get(Player player) {
    uuidCache.computeIfAbsent(player.getName(), name -> {
      String uuid = player.getUniqueId().toString();
      queueAdd(name);
      return uuid;
    });

    return playerCache.computeIfAbsent(player.getName(), name -> new PlayerData(name, configManager, this));
  }

  public PlayerData get(String playerName) {
    String uuid = uuidCache.get(playerName);
    if (uuid == null) {
      return null;
    }

    return playerCache.computeIfAbsent(playerName, name -> new PlayerData(name, configManager, this));
  }

  public void queueAdd(String playerName) {
    if (dataQueueSet.add(playerName)) {
      dataQueue.add(playerName);
      scheduleSave();
    }
  }

  public void unload(Player player) {
    queueAdd(player.getName());
    playerCache.remove(player.getName());
  }

  public void addDefaults(PlayerData playerData) {
    if (!playerData.has("mention_sound.enabled")) {
      playerData.set("mention_sound.enabled", false);
    }
  }

  public void saveQueue() {
    if (dataQueue.isEmpty()) {
      return;
    }

    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      int chunkSize = 20;
      int processed = 0;
      int totalSaved = 0;

      while (processed < chunkSize) {
        String playerName = dataQueue.poll();
        if (playerName == null) {
          break;
        }

        try {
          savePlayerData(playerName);
          totalSaved++;
        } catch (Exception exception) {
          plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerName, exception);
        }
        processed++;
      }

      logger.info("Auto saved " + totalSaved + " player data file(s) this batch.");

      if (!dataQueue.isEmpty()) {
        logger.info(dataQueue.size() + " player data file(s) remaining in queue, scheduling next batch...");
        scheduleSave();
      }
    });
  }

  public void saveAll() {
    playerCache.values().forEach(PlayerData::save);
    dataQueue.clear();
    logger.info("Saved all player data.");
  }

  public void savePlayerData(String playerName) {
    PlayerData data = playerCache.get(playerName);
    if (data != null) {
      data.save();
    }

    dataQueueSet.remove(playerName);
  }

  private void scheduleSave() {
    if (coreManager.isDisabling()) {
      return;
    }

    if (saveScheduled) {
      return;
    }

    saveScheduled = true;

    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
      saveQueue();
      saveScheduled = false;
    }, 6000L);
  }
}