package io.github.divinerealms.core.utilities;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.managers.ConfigManager;
import io.github.divinerealms.core.managers.RostersManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Placeholders extends PlaceholderExpansion {
  private final RostersManager rostersManager;
  private final ConfigManager configManager;
  private final ChannelManager channelManager;
  private final Plugin plugin;

  public Placeholders(CoreManager coreManager) {
    this.rostersManager = coreManager.getRostersManager();
    this.configManager = coreManager.getConfigManager();
    this.channelManager = coreManager.getChannelManager();
    this.plugin = coreManager.getPlugin();
  }

  @Override
  public @NotNull String getIdentifier() {
    return "core";
  }

  @Override
  public @NotNull String getAuthor() {
    return "neonsh";
  }

  @Override
  public @NotNull String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public String onRequest(OfflinePlayer player, @NotNull String params) {
    if (player == null) return "";

    String playerName = player.getName();
    if (playerName == null) return "";
    Player onlinePlayer = player.getPlayer();

    String activeLeague = rostersManager.getActiveLeague();
    RosterInfo activeRoster = rostersManager.getPlayerRoster(playerName, activeLeague);
    Map<String, RosterInfo> playerRosters = rostersManager.getPlayerRosters(playerName);
    FileConfiguration config = configManager.getConfig("config.yml");

    if (params.equalsIgnoreCase("channel_name")) {
      if (onlinePlayer != null) {
        String lastChannelName = channelManager.getLastChannelUsed(onlinePlayer);
        if (lastChannelName != null) return lastChannelName.toUpperCase();
      }
      return "";
    }

    if (params.toLowerCase().endsWith("_roster_name") || params.toLowerCase().endsWith("_roster_tag") || params.toLowerCase().endsWith("_roster_tag_formatted")) {
      String leaguePart = params.substring(0, params.indexOf('_'));
      String infoType = params.substring(params.indexOf('_') + 1);

      RosterInfo specificRoster = playerRosters.get(leaguePart.toLowerCase());
      if (specificRoster == null) return "";

      if (infoType.equalsIgnoreCase("roster_name")) return specificRoster.getName();
      if (infoType.equalsIgnoreCase("roster_tag")) return specificRoster.getTag();
      if (infoType.equalsIgnoreCase("roster_tag_formatted")) return specificRoster.getFormattedTag();
    }

    if (params.equalsIgnoreCase("roster_name")) return activeRoster != null ? activeRoster.getName() : "";
    if (params.equalsIgnoreCase("roster_tag")) return activeRoster != null ? activeRoster.getTag() : "";
    if (params.equalsIgnoreCase("roster_tag_formatted")) return activeRoster != null ? activeRoster.getFormattedTag() : "";
    if (params.equalsIgnoreCase("roster_league")) return activeRoster != null ? activeRoster.getLeague().toUpperCase() : "";
    if (params.equalsIgnoreCase("active_league")) return activeLeague;

    if (params.equalsIgnoreCase("all_tags")) {
      StringBuilder tags = new StringBuilder();
      for (String league : rostersManager.getAvailableLeagues()) {
        RosterInfo roster = playerRosters.get(league);
        if (roster != null) tags.append(roster.getFormattedTag()).append(" ");
      }
      return tags.toString().trim();
    }

    if (params.equalsIgnoreCase("roster_members")) return activeRoster != null ? String.valueOf(activeRoster.getMemberCount()) : "0";
    if (params.equalsIgnoreCase("roster_manager")) return activeRoster != null && activeRoster.getManager() != null ? activeRoster.getManager() : "";

    if (params.equalsIgnoreCase("is_manager")) {
      if (activeRoster == null) return "false";
      return String.valueOf(activeRoster.isManager(playerName));
    }

    if (params.equalsIgnoreCase("manager_suffix")) {
      String activeLeaguePath = "rosters.league_settings." + activeLeague + ".is_exclusive_display";
      boolean isActiveExclusive = config.getBoolean(activeLeaguePath, false);

      if (isActiveExclusive) {
        RosterInfo activeLeagueRoster = playerRosters.get(activeLeague);
        if (activeLeagueRoster != null) return config.getString("rosters.league_settings." + activeLeague + ".manager_suffix", "&a [M]");
      } else {
        for (String league : rostersManager.getAvailableLeagues()) {
          String exclusionPath = "rosters.league_settings." + league + ".is_excluded_from_display";
          boolean isExcluded = config.getBoolean(exclusionPath, false);

          if (isExcluded) continue;

          RosterInfo roster = playerRosters.get(league);
          if (roster != null && roster.isManager(playerName)) {
            String path = "rosters.league_settings." + league + ".manager_suffix";
            String defaultPath = "rosters.league_settings.default.manager_suffix";
            return config.getString(path, config.getString(defaultPath, "&a [M]"));
          }
        }
      }

      return "";
    }

    if (params.equalsIgnoreCase("has_roster")) return String.valueOf(activeRoster != null);
    if (params.toLowerCase().startsWith("has_") && params.toLowerCase().endsWith("_roster")) {
      String leagueName = params.substring(4, params.lastIndexOf('_')).toLowerCase();
      return String.valueOf(playerRosters.containsKey(leagueName));
    }

    if (params.equalsIgnoreCase("rosters_display")) {
      StringBuilder display = new StringBuilder();

      String activeLeaguePath = "rosters.league_settings." + activeLeague + ".is_exclusive_display";
      boolean isActiveExclusive = config.getBoolean(activeLeaguePath, false);

      if (isActiveExclusive) {
        RosterInfo activeLeagueRoster = playerRosters.get(activeLeague);
        if (activeLeagueRoster != null) display.append(activeLeagueRoster.getFormattedTag());
      } else {
        for (String league : rostersManager.getAvailableLeagues()) {
          String exclusionPath = "rosters.league_settings." + league + ".is_excluded_from_display";
          boolean isExcluded = config.getBoolean(exclusionPath, false);

          if (isExcluded) continue;

          RosterInfo roster = playerRosters.get(league);
          if (roster != null) display.append(roster.getFormattedTag());
        }
      }

      return display.toString().trim();
    }

    if (params.equalsIgnoreCase("rosters_display_naked")) {
      String activeLeaguePath = "rosters.league_settings." + activeLeague + ".is_exclusive_display";
      boolean isActiveExclusive = config.getBoolean(activeLeaguePath, false);

      if (isActiveExclusive) {
        RosterInfo activeLeagueRoster = playerRosters.get(activeLeague);
        if (activeLeagueRoster != null) return activeLeagueRoster.getTag() + " ";
      } else {
        for (String league : rostersManager.getAvailableLeagues()) {
          String exclusionPath = "rosters.league_settings." + league + ".is_excluded_from_display";
          boolean isExcluded = config.getBoolean(exclusionPath, false);

          if (isExcluded) continue;

          RosterInfo roster = playerRosters.get(league);
          if (roster != null) return roster.getTag() + " ";
        }
      }

      return "";
    }

    if (params.equalsIgnoreCase("roster_sort")) {
      String activeLeaguePath = "rosters.league_settings." + activeLeague + ".is_exclusive_display";
      boolean isActiveExclusive = config.getBoolean(activeLeaguePath, false);

      if (isActiveExclusive) {
        RosterInfo activeLeagueRoster = playerRosters.get(activeLeague);
        if (activeLeagueRoster != null) return "1_" + activeLeagueRoster.getName().toLowerCase();
      } else {
        int sortPriority = 1;
        for (String league : rostersManager.getAvailableLeagues()) {
          String exclusionPath = "rosters.league_settings." + league + ".is_excluded_from_display";
          boolean isExcluded = config.getBoolean(exclusionPath, false);

          if (isExcluded) continue;

          RosterInfo roster = playerRosters.get(league);
          if (roster != null) return sortPriority + "_" + roster.getName().toLowerCase();
          sortPriority++;
        }
      }

      return "9_free_agent";
    }

    if (params.equalsIgnoreCase("manager_sort")) {
      String activeLeaguePath = "rosters.league_settings." + activeLeague + ".is_exclusive_display";
      boolean isActiveExclusive = config.getBoolean(activeLeaguePath, false);

      for (String league : rostersManager.getAvailableLeagues()) {
        String exclusionPath = "rosters.league_settings." + league + ".is_excluded_from_display";
        boolean isExcluded = config.getBoolean(exclusionPath, false);

        if (isExcluded) continue;

        RosterInfo roster = playerRosters.get(league);
        if (roster != null && roster.isManager(playerName)) return "0";
      }

      if (isActiveExclusive) {
        RosterInfo activeLeagueRoster = playerRosters.get(activeLeague);
        if (activeLeagueRoster != null && activeLeagueRoster.isManager(playerName)) return "0";
      }

      return "1";
    }

    return "";
  }
}
