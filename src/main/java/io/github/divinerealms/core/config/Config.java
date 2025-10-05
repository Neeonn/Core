package io.github.divinerealms.core.config;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum Config {
  CHANNELS_ENABLED("channels.enabled", true),
  CHANNELS_DEFAULT("channels.default_channel", "global"),
  CHANNELS_DEFAULT_CHANNEL_PERMISSION("channels.list.global.permission", ""),
  CHANNELS_DEFAULT_CHANNEL_DISCORD_ID("channels.list.global.discord_id", ""),
  CHANNELS_DEFAULT_CHANNEL_FORMATS_MINECRAFT("channels.list.global.formats.minecraft_chat", "&r<%player_displayname%&r> %message%"),
  CHANNELS_DEFAULT_CHANNEL_FORMATS_DISCORD_TO_MINECRAFT("channels.list.global.formats.discord_to_minecraft", "&3[Discord] &r<%player_displayname&r> %message%"),
  CHANNELS_DEFAULT_CHANNEL_FORMATS_MINECRAFT_TO_DISCORD("channels.list.global.formats.minecraft_to_discord", "**%player_name%**: %message%"),
  CHANNELS_ANTI_SPAM_MAX_MESSAGES("channels.anti_spam.max_messages", 5),
  CHANNELS_ANTI_SPAM_COOLDOWN("channels.anti_spam.cooldown", 2500L),

  CLIENT_BLOCKER_ENABLED("client_blocker.enabled", true),
  CLIENT_BLOCKER_MODE("client_blocker.mode", "WHITELIST"),
  CLIENT_BLOCKER_LIST("client_blocker.list", Arrays.asList("vanilla", "optifine")),

  PLAYER_MESSAGES_CUSTOM_JOIN_ENABLED("player_messages.custom_join.enabled", true),
  PLAYER_MESSAGES_CUSTOM_JOIN_FORMATS_MINECRAFT("player_messages.custom_join.minecraft", "&e%player_displayname%&e has joined the server!"),
  PLAYER_MESSAGES_CUSTOM_JOIN_FORMATS_DISCORD("player_messages.custom_join.discord", "**%player_name%** has joined the server!"),
  PLAYER_MESSAGES_CUSTOM_QUIT_FORMATS_MINECRAFT("player_messages.custom_quit.minecraft", "&e%player_displayname%&e has left the server!"),
  PLAYER_MESSAGES_CUSTOM_QUIT_FORMATS_DISCORD("player_messages.custom_quit.discord", "**%player_name%** has left the server!"),

  RESULT_ENABLED("result.enabled", true),
  RESULT_DISCORD_ID("result.discord_id", ""),
  RESULT_FORMATS_MINECRAFT_START("result.formats.minecraft.start", "{0} &8| &aMatch &9{1} &f- &c{2} &ais starting!"),
  RESULT_FORMATS_MINECRAFT_HALFTIME("result.formats.minecraft.half", "{0} &8| &aHalftime! &9{1} &f{2} - {3} &c{4}"),
  RESULT_FORMATS_MINECRAFT_SECOND_HALF("result.formats.minecraft.resume", "{0} &8| &aSecond Half Time starting!"),
  RESULT_FORMATS_MINECRAFT_END("result.formats.minecraft.end", "{0} &8| &cMatch ended! &9{1} &f{2} - {3} &c{4}"),
  RESULT_FORMATS_MINECRAFT_UPDATE("result.formats.minecraft.update", "{0} &8| &9{1} &f{2} - {3} &c{4} &8| &e{5}{6}"),
  RESULT_FORMATS_MINECRAFT_GOAL_ADD("result.formats.minecraft.goal.add", String.join(System.lineSeparator(),
      "&r &r",
      "&e   &lGOOOOOOOOOL!",
      "&b   {0} &rje postigao gol za {1} &rtim!",
      "&r &r"
  )),
  RESULT_FORMATS_MINECRAFT_GOAL_ASSIST("result.formats.minecraft.goal.assist", String.join(System.lineSeparator(),
      "&r &r",
      "&e   &lGOOOOOOOOOL!",
      "&b   {0} &rje postigao gol za {1} &rtim!",
      "&f   Asistent: &b{2}",
      "&r &r"
  )),
  RESULT_FORMATS_MINECRAFT_GOAL_REMOVE("result.formats.minecraft.remove_goal", "{0} &8| &cRemoved goal for team {1}"),
  RESULT_FORMATS_DISCORD_START("result.formats.discord.start", "`{0}` Starting **{1}**, match **{2} - {3}**!"),
  RESULT_FORMATS_DISCORD_HALFTIME("result.formats.discord.half", "`{0}` Halftime! **{1} {2} - {3} {4}**"),
  RESULT_FORMATS_DISCORD_SECOND_HALF("result.formats.discord.resume", "`{0}` Second Half Time starting! **{1} {2} - {3} {4}**"),
  RESULT_FORMATS_DISCORD_END("result.formats.discord.end", "`{0}` Match ended! **{1} {2} - {3} {4}**"),
  RESULT_FORMATS_DISCORD_GOAL_TYPE("result.formats.discord.goal.type", "regular"),
  RESULT_FORMATS_DISCORD_GOAL_EMBED_TITLE("result.formats.discord.goal.embed.title", "GOOOOOOL for {0}"),
  RESULT_FORMATS_DISCORD_GOAL_EMBED_ICON_URL("result.formats.discord.goal.embed.icon_url", ""),
  RESULT_FORMATS_DISCORD_GOAL_EMBED_COLOR("result.formats.discord.goal.embed.color", "#ffb80c"),
  RESULT_FORMATS_DISCORD_GOAL_ADD_REGULAR("result.formats.discord.goal.add.regular", String.join(System.lineSeparator(),
      "`{0}` **GOOOOOOL! {1}** scored for **{2}** team!",
      "`{0}` Result: **{3} {4} - {5} {6}**"
  )),
  RESULT_FORMATS_DISCORD_GOAL_ADD_EMBED_DESCRIPTION("result.formats.discord.goal.add.embed_description", String.join(System.lineSeparator(),
      "Scorer: **{0}**",
      "Result: **{1} {2} - {3} {4}**",
      "Time: `{5}`"
  )),
  RESULT_FORMATS_DISCORD_GOAL_ASSIST_REGULAR("result.formats.discord.goal.assist.regular", String.join(System.lineSeparator(),
      "`{0}` **GOOOOOOL! {1}** scored for **{2}** team! Assist: **{3}**",
      "`{0}` Result: **{4} {5} - {6} {7}**"
  )),
  RESULT_FORMATS_DISCORD_GOAL_ASSIST_EMBED_DESCRIPTION("result.formats.discord.goal.assist.embed_description", String.join(System.lineSeparator(),
      "Scorer: **{0}**",
      "Assist: **{1}**",
      "Result: **{2} {3} - {4} {5}**",
      "Time: `{6}`"
  )),
  RESULT_FORMATS_DISCORD_GOAL_REMOVE("result.formats.discord.goal.remove", "`{0}` __Removed__ goal for team **{1}**!");

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

  public <T> T getValue(Class<T> type) {
    Object value = CONFIG.get(path, def);

    if (type == List.class && value instanceof List) {
      return (T) ((List<?>) value).stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    }

    return (T) value;
  }

  public String getString(String[] args) {
    String value = ChatColor.translateAlternateColorCodes('&', CONFIG.getString(this.path, (String) this.def));
    if (args == null) {
      return value;
    } else if (args.length == 0) {
      return value;
    } else {
      for (int i = 0; i < args.length; ++i) {
        value = value.replace("{" + i + "}", args[i]);
      }

      value = ChatColor.translateAlternateColorCodes('&', value);
      return value;
    }
  }
}
