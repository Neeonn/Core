package io.github.divinerealms.core.utilities;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class Constants {
  public static final long NEWBIE_THRESHOLD_HOURS = 2;
  public static final Duration CLIENT_BLOCKER_EXEMPT_DURATION = Duration.ofMinutes(30);
  public static final String PROXY_CHECK_API_URL = "https://proxycheck.io/v2/";
  public static final long PROXY_CHECK_COOLDOWN_MS = 30 * 1000; // 30 seconds cooldown
  public static final long GUI_COOLDOWN_DURATION_MS = TimeUnit.SECONDS.toMillis(5);
  public static final String PATH_PLAYER_MESSAGES = "player_messages.custom_";
}