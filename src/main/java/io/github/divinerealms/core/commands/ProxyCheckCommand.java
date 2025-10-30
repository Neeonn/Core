package io.github.divinerealms.core.commands;

import io.github.divinerealms.core.config.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class ProxyCheckCommand implements CommandExecutor, TabCompleter {
  private final CoreManager coreManager;
  private final Plugin plugin;
  private final Logger logger;

  private static final String PERM_PROXY_CHECK = "core.admin.proxy-check";
  private static final String API_URL = "https://proxycheck.io/v2/";
  private static final long COOLDOWN_MS = 30 * 1000; // 30 seconds cooldown

  private final Map<String, Long> lastUse = new HashMap<>();

  public ProxyCheckCommand(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.plugin = coreManager.getPlugin();
    this.logger = coreManager.getLogger();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERM_PROXY_CHECK)) { logger.send(sender, Lang.NO_PERM.replace(new String[]{PERM_PROXY_CHECK, label})); return true; }
    if (args.length != 1) { logger.send(sender, Lang.USAGE.replace(new String[]{label + " <player|IP>"})); return true; }

    String cooldownKey = (sender instanceof Player) ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
    long now = System.currentTimeMillis();
    long last = lastUse.getOrDefault(cooldownKey, 0L);
    if (now - last < COOLDOWN_MS) {
      double seconds = (double) (COOLDOWN_MS - (now - last)) / 1000;
      logger.send(sender, Lang.PROXY_CHECK_COOLDOWN.replace(new String[]{String.format("%.0f", seconds)}));
      return true;
    }
    lastUse.put(cooldownKey, now);

    String input = args[0], targetIP, displayName;
    Player target = Bukkit.getPlayerExact(input);
    if (target != null) {
      targetIP = target.getAddress().getAddress().getHostAddress();
      displayName = target.getDisplayName();
    } else {
      displayName = input;
      if (isValidIPv4(input)) {
        targetIP = input;
      } else {
        logger.send(sender, Lang.PLAYER_NOT_FOUND.replace(new String[]{input}));
        return true;
      }
    }

    logger.send(sender, Lang.PROXY_CHECK_CHECKING.replace(new String[]{targetIP}));

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        JSONObject root = getJsonObject(targetIP);

        String status = safeJsonGet(root, "status", "unknown");
        JSONObject ipData = (JSONObject) root.get(targetIP);

        if (ipData == null) { Bukkit.getScheduler().runTask(plugin, () -> logger.send(sender, Lang.PROXY_CHECK_NO_DATA.replace(new String[]{targetIP}))); return; }

        String proxy = safeJsonGet(ipData, "proxy", "no");
        String asn = safeJsonGet(ipData, "asn", "N/A");
        String range = safeJsonGet(ipData, "range", "N/A");
        String provider = safeJsonGet(ipData, "provider", "N/A");
        String country = safeJsonGet(ipData, "country", "N/A");
        String region = safeJsonGet(ipData, "region", "N/A");
        String regionCode = safeJsonGet(ipData, "region_code", "N/A");
        String city = safeJsonGet(ipData, "city", "N/A");
        String type = safeJsonGet(ipData, "type", "N/A");

        String finalMsg = Lang.PROXY_CHECK_STATUS.replace(new String[]{
            displayName, targetIP, status,
            (proxy.equalsIgnoreCase("yes")) ? "&cYes" : "&aNo",
            asn, range, provider, country, region, regionCode, city, type
        });

        Bukkit.getScheduler().runTask(plugin, () -> logger.send(sender, finalMsg));
      } catch (IOException | ParseException | NullPointerException exception) {
        Bukkit.getScheduler().runTask(plugin, () -> logger.send(sender, Lang.PROXY_CHECK_ERROR.replace(new String[]{exception.getMessage()})));
        plugin.getLogger().log(Level.SEVERE, "Error checking proxy", exception);
      }
    });

    return true;
  }

  private static String safeJsonGet(JSONObject obj, String key, String def) {
    if (obj == null) return def;
    Object val = obj.get(key);
    return val != null ? val.toString() : def;
  }

  private static boolean isValidIPv4(String ip) {
    return ip.matches("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
  }

  private static JSONObject getJsonObject(String targetIP) throws IOException, ParseException, NullPointerException {
    URL url = new URL(API_URL + targetIP + "?vpn=1&asn=1");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) response.append(line);
      reader.close();
      JSONParser parser = new JSONParser();
      return (JSONObject) parser.parse(response.toString());
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERM_PROXY_CHECK)) return Collections.emptyList();

    List<String> completions = new ArrayList<>();

    if (args.length == 1) coreManager.getCachedPlayers().forEach(player -> completions.add(player.getName()));

    if (!completions.isEmpty()) {
      String lastWord = args[args.length - 1].toLowerCase();
      completions.removeIf(s -> !s.toLowerCase().startsWith(lastWord));
      completions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    return completions;
  }
}
