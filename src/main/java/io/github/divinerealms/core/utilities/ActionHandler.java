package io.github.divinerealms.core.utilities;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.BookManager;
import io.github.divinerealms.core.managers.GUIManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionHandler {
  private final CoreManager coreManager;
  private final Server server;
  private final Logger logger;
  private final GUIManager guiManager;
  private final BookManager bookManager;

  public ActionHandler(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.server = coreManager.getPlugin().getServer();
    this.logger = coreManager.getLogger();
    this.guiManager = coreManager.getGuiManager();
    this.bookManager = coreManager.getBookManager();
  }

  public void handleActions(Player executor, List<String> actions, String[] args, Player target) {
    if (actions == null || actions.isEmpty()) return;

    for (String action : actions) {
      if (action == null || action.isEmpty()) continue;

      String processed = replaceArgs(action.trim(), args);
      if (coreManager.isPlaceholderAPI()) processed = PlaceholderAPI.setPlaceholders(target, processed);
      String lower = processed.toLowerCase();

      if (lower.startsWith("perm:")) {
        String perm = processed.substring("perm:".length()).trim();
        if (!executor.hasPermission(perm)) return;
        continue;
      }

      String condition = processed.substring("if:".length()).trim();
      if (lower.startsWith("if:") && !checkCondition(condition)) continue;

      runAction(executor, processed);
    }
  }

  private void runAction(Player player, String action) {
    if (action == null || action.isEmpty()) return;

    String lower = action.toLowerCase();

    if (lower.startsWith("command:")) {
      String cmd = action.substring("command:".length()).trim();
      player.performCommand(cmd);
      return;
    }

    if (lower.startsWith("console:")) {
      String cmd = action.substring("console:".length()).trim();
      server.dispatchCommand(server.getConsoleSender(), cmd);
      return;
    }

    if (lower.startsWith("message:")) {
      String msg = action.substring("message:".length()).trim();
      logger.send(player, msg);
      return;
    }

    if (lower.startsWith("menu:")) {
      String menu = action.substring("menu:".length()).trim();
      guiManager.openMenu(player, menu);
      return;
    }

    if (lower.startsWith("book:")) {
      String book = action.substring("book:".length()).trim();
      bookManager.openBook(player, book);
      return;
    }

    if (lower.equalsIgnoreCase("close")) {
      player.closeInventory();
      return;
    }

    if (lower.equalsIgnoreCase("stop")) return;

    player.performCommand(action);
  }

  private String replaceArgs(String input, String[] args) {
    if (input == null || input.isEmpty()) return input;

    String result = input;
    String joined = args != null && args.length > 0 ? String.join(" ", args) : "";

    result = result.replace("%args%", joined);
    result = result.replace("%raw_args%", joined);

    Pattern pattern = Pattern.compile("%arg(\\d+)%");
    Matcher matcher = pattern.matcher(result);

    StringBuilder stringBuilder = new StringBuilder();
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1)) - 1;
      String replacement = (args != null && index < args.length) ? args[index] : "";
      matcher.appendReplacement(stringBuilder, replacement);
    }
    matcher.appendTail(stringBuilder);

    return stringBuilder.toString();
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkCondition(String condition) {
    if (condition == null || condition.isEmpty()) return false;

    String normalized = condition.replace(":", " ").replaceAll("\\s+", " ").trim();

    String[] ops = {"!contains", "contains", ">=", "<=", "!=", "=", ">", "<"};
    String used = null;

    for (String op : ops) {
      String pattern = " " + op + " ";
      if (normalized.toLowerCase().contains(pattern)) {
        used = op;
        break;
      }
    }

    if (used == null) return false;

    String[] parts = normalized.split("\\s" + Pattern.quote(used) + "\\s", 2);
    if (parts.length < 2) return false;

    String left = parts[0].trim();
    String right = parts[1].trim();

    if (right.equalsIgnoreCase("null")) right = "";
    if (left.equalsIgnoreCase("null")) left = "";

    switch (used) {
      case "=": return left.equals(right);
      case "!=": return !left.equals(right);

      case "contains": return left.contains(right);
      case "!contains": return !left.contains(right);

      case ">": return parseDouble(left) > parseDouble(right);
      case "<": return parseDouble(left) < parseDouble(right);
      case ">=": return parseDouble(left) >= parseDouble(right);
      case "<=": return parseDouble(left) <= parseDouble(right);

      default: return false;
    }
  }

  private double parseDouble(String input) {
    if (input == null || input.isEmpty()) return 0;
    try { return Double.parseDouble(input); }
    catch (Exception ignored) { return 0; }
  }
}
