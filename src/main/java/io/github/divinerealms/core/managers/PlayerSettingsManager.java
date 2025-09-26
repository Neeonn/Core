package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.config.ConfigManager;
import io.github.divinerealms.core.main.CoreManager;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSettingsManager {
  private final ConfigManager configManager;

  @Getter private final Map<UUID, Boolean> mentionSound = new HashMap<>();

  public PlayerSettingsManager(CoreManager coreManager) {
    this.configManager = coreManager.getConfigManager();
  }

  // ===== Loading and saving =====
  public void loadPlayer(Player player) {
    FileConfiguration settingsConfig = configManager.getConfig("settings.yml");
    boolean enabled = settingsConfig.getBoolean(player.getUniqueId().toString() + ".mention_sound", true); // default true
    mentionSound.put(player.getUniqueId(), enabled);
  }

  public void savePlayer(Player player) {
    FileConfiguration settingsConfig = configManager.getConfig("settings.yml");
    settingsConfig.set(player.getUniqueId().toString() + ".mention_sound", mentionSound.getOrDefault(player.getUniqueId(), true));
    configManager.saveConfig("settings.yml");
  }

  public void saveAll() {
    FileConfiguration settingsConfig = configManager.getConfig("settings.yml");
    for (UUID uuid : mentionSound.keySet()) {
      settingsConfig.set(uuid.toString() + ".mention_sound", mentionSound.get(uuid));
    }
    configManager.saveConfig("settings.yml");
  }

  // ===== API =====
  public boolean isMentionSoundEnabled(Player player) {
    return mentionSound.getOrDefault(player.getUniqueId(), true);
  }

  public boolean toggleMentionSound(Player player) {
    boolean current = mentionSound.getOrDefault(player.getUniqueId(), true);
    boolean newValue = !current;
    mentionSound.put(player.getUniqueId(), newValue);
    return newValue;
  }

  public void removePlayer(Player player) {
    mentionSound.remove(player.getUniqueId());
  }
}
