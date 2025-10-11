package io.github.divinerealms.core.utilities;

import java.util.List;

public class ChannelInfo {
  public final String name;
  public final String permission;
  public final String discordId;
  public final ChannelFormats formats;
  public final boolean broadcast;
  public final List<String> aliases;

  public ChannelInfo(String name, String permission, String discordId, ChannelFormats formats, boolean broadcast, List<String> aliases) {
    this.name = name;
    this.permission = permission;
    this.discordId = discordId;
    this.formats = formats;
    this.broadcast = broadcast;
    this.aliases = aliases;
  }
}
