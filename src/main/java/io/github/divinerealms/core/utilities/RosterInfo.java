package io.github.divinerealms.core.utilities;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class RosterInfo {
  private String name;
  private String longName;
  private String tag;
  private String league;
  private String manager;
  private Set<String> members;
  private String discordChannelId;

  public RosterInfo(String name, String tag, String league) {
    this.name = name.toUpperCase();
    this.longName = name;
    this.tag = tag;
    this.league = league.toLowerCase();
    this.members = new HashSet<>();
    this.discordChannelId = "";
  }

  public String getFormattedTag() {
    return "%luckperms_prefix%[" + tag + "%luckperms_prefix%] &r";
  }

  public boolean hasMember(String playerName) {
    return members.stream().anyMatch(member -> member.equalsIgnoreCase(playerName));
  }

  public boolean isManager(String playerName) {
    return manager != null && manager.equalsIgnoreCase(playerName);
  }

  public void addMember(String playerName) {
    members.add(playerName);
  }

  public void removeMember(String playerName) {
    if (isManager(playerName)) {
      manager = null;
    }

    members.remove(playerName);
  }

  public void setManager(String playerName) {
    if (playerName != null && !hasMember(playerName)) {
      members.add(playerName);
    }

    this.manager = playerName;
  }

  public int getMemberCount() {
    return members.size();
  }
}