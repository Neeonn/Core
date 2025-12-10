package io.github.divinerealms.core;

import io.github.divinerealms.core.main.CoreManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class Core extends JavaPlugin {
  private CoreManager coreManager;

  @Override
  public void onEnable() {
    try {
      this.coreManager = new CoreManager(this);
      coreManager.setEnabling(true);
      coreManager.getLogger().info("&aSuccessfully enabled.");
    } catch (Exception exception) {
      getLogger().log(Level.SEVERE, "Failed to initialize Core", exception);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (coreManager != null) {
      coreManager.setDisabling(true);
      coreManager.saveAll();
      coreManager.unregisterCommands();
      coreManager.getListenerManager().unregisterAll();
      coreManager.getLogger().info("&cSuccessfully disabled.");
    }
  }
}