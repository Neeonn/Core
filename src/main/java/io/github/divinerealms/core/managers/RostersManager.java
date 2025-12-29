package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import io.github.divinerealms.core.utilities.RosterInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.github.divinerealms.core.configs.Lang.*;

public class RostersManager {
  private final ConfigManager configManager;
  private final ChannelManager channelManager;
  private final Logger logger;

  @Getter
  private final Map<String, RosterInfo> rosters = new ConcurrentHashMap<>();
  private final Map<String, String> playerToRoster = new ConcurrentHashMap<>();
  @Getter
  private String activeLeague = "main";
  @Getter
  @Setter
  private List<String> availableLeagues;

  public RostersManager(CoreManager coreManager) {
    this.configManager = coreManager.getConfigManager();
    this.channelManager = coreManager.getChannelManager();
    this.logger = coreManager.getLogger();

    configManager.createNewFile("rosters.yml", "Core Rosters Configuration");
    loadRosters();
  }

  public void loadRosters() {
    rosters.clear();
    playerToRoster.clear();

    FileConfiguration config = configManager.getConfig("config.yml");
    FileConfiguration rtConfig = configManager.getConfig("rosters.yml");

    activeLeague = config.getString("rosters.active_league", "main");
    List<String> loadedLeagues = config.getStringList("rosters.available_leagues");
    availableLeagues = loadedLeagues.isEmpty()
                       ? new ArrayList<>(List.of("main", "juniors", "nationals"))
                       : loadedLeagues;

    ConfigurationSection rostersSection = rtConfig.getConfigurationSection("rosters");
    if (rostersSection == null) {
      logger.info("&cNo rosters found in rosters.yml");
      return;
    }

    for (String rosterName : rostersSection.getKeys(false)) {
      String path = "rosters." + rosterName;

      String longName = rtConfig.getString(path + ".longName", rosterName);
      String tag = rtConfig.getString(path + ".tag", "???");
      String league = rtConfig.getString(path + ".league", "main");
      String discordId = rtConfig.getString(path + ".discordChannelId", "");

      RosterInfo roster = new RosterInfo(rosterName, tag, league);
      roster.setLongName(longName);
      roster.setDiscordChannelId(discordId);

      String managerName = rtConfig.getString(path + ".manager");
      if (managerName != null && !managerName.isEmpty()) {
        roster.setManager(managerName);
      }

      List<String> members = rtConfig.getStringList(path + ".members");
      for (String playerName : members) {
        roster.addMember(playerName);
        String lookupKey = playerName + ":" + league.toLowerCase();
        playerToRoster.put(lookupKey, rosterName.toUpperCase());
      }

      rosters.put(rosterName.toUpperCase(), roster);
      if (channelManager != null) {
        channelManager.createRosterChannel(rosterName, discordId);
      }
    }

    logger.info(
        "&a✔ &9Loaded &e" + rosters.size() + " &9rosters with &e" + playerToRoster.size() + " &9total members.");
  }

  public void saveRosters() {
    FileConfiguration config = configManager.getConfig("config.yml");
    FileConfiguration rtConfig = configManager.getConfig("rosters.yml");
    config.set("rosters.active_league", activeLeague);
    config.set("rosters.available_leagues", availableLeagues);
    rtConfig.set("rosters", null);

    for (Map.Entry<String, RosterInfo> entry : rosters.entrySet()) {
      String rosterName = entry.getKey();
      RosterInfo roster = entry.getValue();
      String path = "rosters." + rosterName;

      rtConfig.set(path + ".longName", roster.getLongName());
      rtConfig.set(path + ".tag", roster.getTag());
      rtConfig.set(path + ".league", roster.getLeague());
      rtConfig.set(path + ".manager", roster.getManager());
      rtConfig.set(path + ".discordChannelId", roster.getDiscordChannelId());

      List<String> members = new ArrayList<>(roster.getMembers());
      rtConfig.set(path + ".members", members);
    }

    configManager.saveConfig("rosters.yml");
  }

  public void reloadRosters() {
    configManager.reloadConfig("rosters.yml");
    loadRosters();
  }

  public boolean createRoster(String name, String tag, String league) {
    String upperName = name.toUpperCase();
    if (rosters.containsKey(upperName)) {
      return false;
    }

    RosterInfo roster = new RosterInfo(name, tag, league);
    rosters.put(upperName, roster);
    if (channelManager != null) {
      channelManager.createRosterChannel(name, roster.getDiscordChannelId());
    }

    saveRosters();

    return true;
  }

  public boolean deleteRoster(String name) {
    String upperName = name.toUpperCase();
    RosterInfo roster = rosters.remove(upperName);
    if (roster == null) {
      return false;
    }

    String league = roster.getLeague();
    for (String playerName : roster.getMembers()) {
      String lookupKey = playerName + ":" + league.toLowerCase();
      playerToRoster.remove(lookupKey);
    }

    saveRosters();
    return true;
  }

  public RosterInfo getRoster(String name) {
    return rosters.get(name.toUpperCase());
  }

  public RosterInfo getPlayerRoster(String playerName, String league) {
    String lookupKey = playerName + ":" + league.toLowerCase();
    String rosterName = playerToRoster.get(lookupKey);
    return rosterName != null
           ? rosters.get(rosterName)
           : null;
  }

  public RosterInfo getPlayerRoster(Player player) {
    for (RosterInfo roster : rosters.values()) {
      if (roster.getMembers().contains(player.getName())) {
        return roster;
      }

      if (player.getName().equalsIgnoreCase(roster.getManager())) {
        return roster;
      }
    }
    return null;
  }

  public Map<String, RosterInfo> getPlayerRosters(String playerName) {
    Map<String, RosterInfo> playerRosters = new HashMap<>();
    for (RosterInfo roster : rosters.values()) {
      if (roster.hasMember(playerName)) {
        playerRosters.put(roster.getLeague(), roster);
      }
    }
    return playerRosters;
  }

  @SuppressWarnings("deprecation")
  public boolean addPlayerToRoster(String rosterName, String playerName) {
    RosterInfo roster = getRoster(rosterName);
    if (roster == null) {
      return false;
    }

    String league = roster.getLeague();

    removePlayerFromRoster(playerName, league);

    roster.addMember(playerName);
    String lookupKey = playerName + ":" + league.toLowerCase();
    playerToRoster.put(lookupKey, rosterName.toUpperCase());
    saveRosters();

    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
    if (target != null && target.isOnline()) {
      String channelName = roster.getName().toLowerCase();
      channelManager.subscribe(target.getUniqueId(), channelName);
      channelManager.setLastActiveChannel(target.getUniqueId(), channelManager.getDefaultChannel());
    }

    return true;
  }

  @SuppressWarnings("deprecation")
  public boolean removePlayerFromRoster(String playerName, String league) {
    String lookupKey = playerName + ":" + league.toLowerCase();
    String currentRoster = playerToRoster.remove(lookupKey);
    if (currentRoster == null) {
      return false;
    }

    RosterInfo roster = rosters.get(currentRoster);
    if (roster != null) {
      roster.removeMember(playerName);
      saveRosters();
    }

    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
    if (target != null && target.isOnline() && roster != null) {
      String channelName = roster.getName().toLowerCase();
      channelManager.unsubscribe(target.getUniqueId(), channelName);
      logger.send(target.getPlayer(), CHANNEL_TOGGLE, channelName, OFF.toString());
    }

    return true;
  }

  public boolean setManager(String playerName, String league) {
    String lookupKey = playerName + ":" + league.toLowerCase();
    String rosterName = playerToRoster.get(lookupKey);
    if (rosterName == null) {
      return false;
    }

    RosterInfo roster = rosters.get(rosterName);
    if (roster != null) {
      roster.setManager(playerName);
      saveRosters();
      return true;
    }

    return false;
  }

  @SuppressWarnings("deprecation")
  public List<String> getRosterInfo(String rosterName) {
    RosterInfo roster = getRoster(rosterName);
    if (roster == null) {
      return Collections.emptyList();
    }

    List<String> info = new ArrayList<>();
    info.add(ROSTERS_INFO_HEADER.replace(roster.getTag(), roster.getLeague(), String.valueOf(roster.getMemberCount()),
        roster.getLongName() + (roster.getManager() != null
                                ? ROSTERS_INFO_MANAGER_DISPLAY.replace(roster.getManager())
                                : "")));

    for (String playerName : roster.getMembers()) {
      OfflinePlayer member = Bukkit.getOfflinePlayer(playerName);
      String status = member.isOnline()
                      ? ONLINE.toString()
                      : OFFLINE.toString();
      info.add(ROSTERS_INFO_PLAYER_ENTRY.replace(status, member.isOnline()
              ? member.getPlayer().getDisplayName()
              : member.getName()));
    }

    info.add(System.lineSeparator());
    info.add(ROSTERS_INFO_FOOTER.replace(activeLeague));
    return info;
  }

  public List<String> getAllRosterNames() {
    return rosters.keySet().stream().sorted().collect(Collectors.toList());
  }

  public List<String> getRostersByLeague(String league) {
    return rosters.values().stream()
        .filter(rosterInfo -> rosterInfo.getLeague().equalsIgnoreCase(league))
        .map(RosterInfo::getName)
        .sorted()
        .collect(Collectors.toList());
  }

  public boolean rosterExists(String name) {
    return rosters.containsKey(name.toUpperCase());
  }

  public boolean updateTag(String rosterName, String newTag) {
    RosterInfo roster = getRoster(rosterName);
    if (roster == null) {
      return false;
    }

    roster.setTag(newTag);
    saveRosters();
    return true;
  }

  public boolean updateLongName(String rosterName, String newLongName) {
    RosterInfo roster = getRoster(rosterName);
    if (roster == null) {
      return false;
    }
    if (newLongName == null || newLongName.trim().isEmpty()) {
      return false;
    }

    roster.setLongName(newLongName.trim());
    saveRosters();
    return true;
  }

  public boolean updateLeague(String rosterName, String newLeague) {
    RosterInfo roster = getRoster(rosterName);
    if (roster == null) {
      return false;
    }

    String oldLeague = roster.getLeague();
    roster.setLeague(newLeague.toLowerCase());

    for (String playerName : roster.getMembers()) {
      String oldKey = playerName + ":" + oldLeague;
      String newKey = playerName + ":" + newLeague.toLowerCase();

      playerToRoster.remove(oldKey);
      playerToRoster.put(newKey, rosterName.toUpperCase());
    }

    saveRosters();
    return true;
  }

  public void setActiveLeague(String league) {
    this.activeLeague = league.toLowerCase();
    saveRosters();
  }

  public void addAvailableLeague(String league) {
    String lowerLeague = league.toLowerCase();
    if (!this.availableLeagues.contains(lowerLeague)) {
      this.availableLeagues.add(lowerLeague);
      saveRosters();
    }
  }

  public void removeAvailableLeague(String league) {
    String lowerLeague = league.toLowerCase();
    if (this.availableLeagues.remove(lowerLeague)) {
      saveRosters();
    }
  }

  public boolean renameLeague(String oldName, String newName) {
    String lowerOld = oldName.toLowerCase(), lowerNew = newName.toLowerCase();

    if (!availableLeagues.contains(lowerOld)) {
      return false;
    }

    if (availableLeagues.contains(lowerNew)) {
      return false;
    }

    for (RosterInfo roster : rosters.values()) {
      if (roster.getLeague().equalsIgnoreCase(lowerOld)) {
        roster.setLeague(lowerNew);

        for (String playerName : roster.getMembers()) {
          String oldKey = playerName + ":" + lowerOld, newKey = playerName + ":" + lowerNew;
          String rosterName = playerToRoster.remove(oldKey);
          if (rosterName != null) {
            playerToRoster.put(newKey, rosterName);
          }
        }
      }
    }

    availableLeagues.remove(lowerOld);
    availableLeagues.add(lowerNew);

    if (activeLeague.equalsIgnoreCase(lowerOld)) {
      activeLeague = lowerNew;
    }

    saveRosters();
    return true;
  }
}
