package io.github.divinerealms.core.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.divinerealms.core.main.CoreManager;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class PlaytimeManager {
  private final File statsFolder;
  private final Map<UUID, Long> playtimeCache = new HashMap<>();

  public PlaytimeManager(CoreManager coreManager) {
    this.statsFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "stats");
    Bukkit.getScheduler().runTaskTimerAsynchronously(coreManager.getPlugin(), this::reloadCache, 0L, 12000L);
  }

  public void reloadCache() {
    if (!statsFolder.exists()) return;
    Map<UUID, Long> temp = new HashMap<>();

    File[] files = statsFolder.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null) return;

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(reader).getAsJsonObject();
        if (!json.has("stat.playOneMinute")) continue;
        long ticks = json.get("stat.playOneMinute").getAsLong();

        UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
        temp.put(uuid, ticks);
      } catch (Exception ignored) {}
    }

    synchronized (playtimeCache) {
      playtimeCache.clear();
      playtimeCache.putAll(temp);
    }
  }

  public long getPlaytime(UUID uuid) {
    synchronized (playtimeCache) {
      return playtimeCache.getOrDefault(uuid, 0L);
    }
  }

  public List<Map.Entry<UUID, Long>> getTopPlaytimes(int limit) {
    List<Map.Entry<UUID, Long>> sorted;
    synchronized (playtimeCache) {
      sorted = new ArrayList<>(playtimeCache.entrySet());
    }
    sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
    if (limit > 0 && sorted.size() > limit) return sorted.subList(0, limit);
    return sorted;
  }

  public String formatPlaytime(long ticks) {
    long totalSeconds = ticks / 20;

    long years = totalSeconds / (365 * 24 * 3600);
    totalSeconds %= 365 * 24 * 3600;
    long months = totalSeconds / (30 * 24 * 3600);
    totalSeconds %= 30 * 24 * 3600;
    long days = totalSeconds / (24 * 3600);
    totalSeconds %= 24 * 3600;
    long hours = totalSeconds / 3600;
    totalSeconds %= 3600;
    long minutes = totalSeconds / 60;
    long seconds = totalSeconds % 60;

    if (years > 0) return years + "y " + months + "mo " + days + "d";
    if (months > 0) return months + "mo " + days + "d " + hours + "h";
    if (days > 0) return days + "d " + hours + "h " + minutes + "m";
    if (hours > 0) return hours + "h " + minutes + "m";
    if (minutes > 0) return minutes + "m " + seconds + "s";
    return seconds + "s";
  }
}