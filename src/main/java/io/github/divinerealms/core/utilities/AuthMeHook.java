package io.github.divinerealms.core.utilities;

import fr.xephi.authme.api.v3.AuthMeApi;
import lombok.Getter;
import org.bukkit.entity.Player;

public class AuthMeHook {
  @Getter
  private static AuthMeApi authMeApi;

  public static void initializeHook() {
    authMeApi = AuthMeApi.getInstance();
  }

  public static boolean notAuthenticated(Player player) {
    if (authMeApi != null) {
      return !authMeApi.isAuthenticated(player);
    }
    return true;
  }
}
