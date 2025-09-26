package io.github.divinerealms.core.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum Config {
  CHANNELS_ENABLED("channels.enabled", true),
  CHANNELS_DEFAULT("channels.default_channel", "global"),

  CLIENT_BLOCKER_ENABLED("client_blocker.enabled", true),
  CLIENT_BLOCKER_MODE("client_blocker.mode", "WHITELIST"),
  CLIENT_BLOCKER_LIST("client_blocker.list", Arrays.asList("vanilla", "optifine"));

  public static FileConfiguration CONFIG;
  private final String path;
  private final Object def;

  Config(String path, Object def) {
    this.path = path;
    this.def = def;
  }

  public static void setFile(FileConfiguration config) {
    CONFIG = config;
  }

  public static FileConfiguration getConfig() {
    return CONFIG;
  }

  public Object getDefault() {
    return def;
  }

  @SuppressWarnings("unchecked")
  public <T> T getValue(Class<T> type) {
    Object value = CONFIG.get(path, def);

    if (type == List.class && value instanceof List) {
      return (T) ((List<?>) value).stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    }

    return (T) value;
  }
}
