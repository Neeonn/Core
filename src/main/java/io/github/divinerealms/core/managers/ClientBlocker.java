package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.config.Config;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientBlocker {
  private final Map<String, String> playerBrands = new HashMap<>();
  private final List<String> allowedVanillaClients = Arrays.asList("vanilla", "optifine");

  @Getter private boolean enabled = Config.CLIENT_BLOCKER_ENABLED.getValue(Boolean.class);

  public static final String BYPASS_PERMISSION = "core.client-blocker.bypass";
  public static final String NOTIFY_PERMISSION = "core.client-blocker.notify";

  public boolean toggle() {
    if (enabled) {
      enabled = false;
      return false;
    } else {
      enabled = true;
      return true;
    }
  }

  public void setPlayerBrand(Player player, String brand) {
    String current = playerBrands.get(player.getName());
    if (current == null || !current.equalsIgnoreCase(brand)) playerBrands.put(player.getName(), brand);
  }

  public void removePlayer(Player player) {
    playerBrands.remove(player.getName());
  }

  public String getBrand(Player player) {
    return playerBrands.get(player.getName());
  }

  public boolean shouldKick(Player player) {
    if (!enabled) return false;
    if (player.hasPermission(BYPASS_PERMISSION)) return false;

    String brand = getBrand(player);
    if (brand == null) return false;

    String brandLower = brand.toLowerCase();
    for (String allowed : allowedVanillaClients) if (brandLower.contains(allowed)) return false;

    List<?> rawList = Config.CLIENT_BLOCKER_LIST.getValue(List.class);
    List<String> checks = rawList.stream().map(Object::toString).collect(Collectors.toList());
    String mode = Config.CLIENT_BLOCKER_MODE.getValue(String.class).toUpperCase();

    switch (mode) {
      case "BLACKLIST":
        for (String blocked : checks)
          if (brandLower.contains(blocked.toLowerCase())) {
            return true;
          }
        break;
      case "WHITELIST":
        for (String allowed : checks)
          if (brandLower.contains(allowed.toLowerCase())) {
            return false;
          }
        return true;
    }

    return false;
  }
}
