package io.github.divinerealms.core.utilities;

public class ChannelFormats {
  public final String minecraftChat;
  public final String discordToMinecraft;
  public final String minecraftToDiscord;

  public ChannelFormats(String minecraftChat, String discordToMinecraft, String minecraftToDiscord) {
    this.minecraftChat = minecraftChat;
    this.discordToMinecraft = discordToMinecraft;
    this.minecraftToDiscord = minecraftToDiscord;
  }
}