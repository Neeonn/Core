package io.github.divinerealms.core.utilities;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.GUIManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.List;

public class ActionHandler {
  private final CoreManager coreManager;
  private final Logger logger;
  private final GUIManager guiManager;

  public ActionHandler(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.guiManager = coreManager.getGuiManager();
  }

  public void handleActions(Player player, List<String> actions) {
    if (actions == null || actions.isEmpty()) return;

    for (String action : actions) {
      if (action == null || action.isEmpty()) continue;

      String trimmedAction = action.trim();

      if (trimmedAction.startsWith("command:")) {
        String cmd = trimmedAction.substring("command:".length()).trim();
        player.performCommand(cmd);
      } else if (trimmedAction.startsWith("message:")) {
        String msg = trimmedAction.substring("message:".length()).trim();

        if (coreManager.isPlaceholderAPI()) {
          msg = PlaceholderAPI.setPlaceholders(player, msg);
          if (msg.contains("%")) PlaceholderAPI.setPlaceholders(player, msg);
        }

        logger.send(player, msg);
      } else if (trimmedAction.startsWith("menu:")) {
        String menu = trimmedAction.substring("menu:".length()).trim();
        guiManager.openMenu(player, menu);
      } else if (trimmedAction.equalsIgnoreCase("close")) {
        player.closeInventory();
      }
    }
  }
}
