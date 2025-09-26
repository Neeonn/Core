package io.github.divinerealms.core;

import io.github.divinerealms.core.main.CoreManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class Core extends JavaPlugin {
  @Getter private CoreManager coreManager;

  @Override
  public void onEnable() {
    try {
      this.coreManager = new CoreManager(this);
      coreManager.getLogger().info("&aSuccessfully enabled.");
    } catch (Exception exception) {
      getLogger().severe("Failed to initialize Core: " + exception.getMessage());
      exception.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (coreManager != null) {
      coreManager.saveAll();
      coreManager.unregisterCommands();
      coreManager.getListenerManager().unregisterAll();
      coreManager.getPlayerSettingsManager().saveAll();
      coreManager.getLogger().info("&cSuccessfully disabled.");
    }
  }
}