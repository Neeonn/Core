package io.github.divinerealms.core.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import io.github.divinerealms.core.configs.Config;
import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import io.github.divinerealms.core.utilities.Timer;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.awt.*;

public class ResultManager {
  private final Logger logger;
  private final LuckPerms luckPerms;
  private final Plugin plugin;

  @Getter @Setter private int defaultHalfDuration = 10 * 60;
  @Getter @Setter private int defaultMatchDuration = defaultHalfDuration * 2;
  @Getter private int extraTime = 0;

  @Getter private String prefix = Lang.RESULT_PREFIX_HOST.replace(null);
  @Getter private String home, away;
  @Getter private int homeScore;
  @Getter private int awayScore;

  public enum Half { NOT_STARTED, FIRST, SECOND }
  @Getter private Half currentHalf = Half.NOT_STARTED;

  private Timer matchTimer;
  private int currentHalfExtraTime = 0;
  private boolean paused = false;
  private boolean shouldSendToDiscord = false;

  public ResultManager(CoreManager coreManager) {
    this.logger = coreManager.getLogger();
    this.luckPerms = coreManager.getLuckPerms();
    this.plugin = coreManager.getPlugin();
  }

  public void startMatch(CommandSender sender) {
    if (home == null || away == null) { logger.send(sender, Lang.RESULT_TEAMS_UNKNOWN.replace(null)); return; }
    if (matchTimer != null && matchTimer.isRunning()) { logger.send(sender, Lang.RESULT_MATCH_RUNNING.replace(null)); return; }

    if (currentHalf == Half.NOT_STARTED) {
      currentHalf = Half.FIRST;
      startHalf(Half.FIRST);
    } else if (currentHalf == Half.FIRST && paused) {
      currentHalf = Half.SECOND;
      paused = false;
      startHalf(Half.SECOND);
    } else logger.send(sender, Lang.RESULT_MATCH_FINISHED.replace(null));
  }

  private void startHalf(Half half) {
    matchTimer = new Timer(plugin, () -> {
      String msgMC = half == Half.FIRST
          ? Config.RESULT_FORMATS_MINECRAFT_START.getString(new String[]{prefix, home, away})
          : Config.RESULT_FORMATS_MINECRAFT_SECOND_HALF.getString(new String[]{prefix, home, away});

      String msgDC = half == Half.FIRST
          ? Config.RESULT_FORMATS_DISCORD_START.getString(new String[]{formatTime(matchTimer.getSecondsElapsed()), prefix, home, away})
          : Config.RESULT_FORMATS_DISCORD_SECOND_HALF.getString(new String[]{formatTime(matchTimer.getSecondsElapsed()), home, String.valueOf(homeScore), String.valueOf(awayScore), away});

      broadcastBoth(msgMC, msgDC);
    }, timer -> updateHalfMessage());

    if (half == Half.SECOND) matchTimer.setSecondsElapsed(defaultHalfDuration);

    matchTimer.start();
    paused = false;
  }

  private void endHalf() {
    if (currentHalf == Half.FIRST) {
      String msgMC = Config.RESULT_FORMATS_MINECRAFT_HALFTIME.getString(new String[]{prefix, home, String.valueOf(homeScore), String.valueOf(awayScore), away});
      String msgDC = Config.RESULT_FORMATS_DISCORD_HALFTIME.getString(new String[]{formatTime(matchTimer.getSecondsElapsed()), home, String.valueOf(homeScore), String.valueOf(awayScore), away});

      broadcastBoth(msgMC, msgDC);
      paused = true;
      currentHalfExtraTime = 0;
    } else if (currentHalf == Half.SECOND) endMatch();

    if (matchTimer != null) {
      matchTimer.cancel();
      matchTimer = null;
    }
  }

  public void stopHalf(CommandSender sender) {
    if (matchTimer != null) {
      matchTimer.cancel();
      logger.send(sender, Lang.RESULT_HALF_STOPPED.replace(null));
      endHalf();
      currentHalfExtraTime = 0;
    } else {
      logger.send(sender, Lang.RESULT_HALF_NONE.replace(null));
    }
  }

  public void stopMatch() {
    if (matchTimer != null) matchTimer.cancel();
    endMatch();
  }

  private void endMatch() {
    String msgMC = Config.RESULT_FORMATS_MINECRAFT_END.getString(new String[]{prefix, home, String.valueOf(homeScore), String.valueOf(awayScore), away});
    String msgDC = Config.RESULT_FORMATS_DISCORD_END.getString(new String[]{formatTime(matchTimer.getSecondsElapsed()), home, String.valueOf(homeScore), String.valueOf(awayScore), away});

    broadcastBoth(msgMC, msgDC);
    resetMatch();
  }

  public void setTeams(CommandSender sender, String home, String away) {
    this.home = resolveGroupName(home.toUpperCase());
    this.away = resolveGroupName(away.toUpperCase());

    logger.send(sender, Lang.RESULT_TEAMS_SET.replace(new String[]{this.home, this.away}));
  }

  public void setPrefix(CommandSender sender, String prefix) {
    this.prefix = logger.color(prefix);
    logger.send(sender, Lang.RESULT_MATCH_PREFIX.replace(new String[]{this.prefix}));
  }

  public void setTime(CommandSender sender, int totalSeconds) {
    defaultHalfDuration = totalSeconds;
    if (matchTimer != null && matchTimer.isRunning()) updateHalfMessage();
    logger.send(sender, Lang.RESULT_MATCH_TIME.replace(new String[]{formatTime(totalSeconds)}));
  }

  public void addExtraTime(CommandSender sender, String value) {
    boolean subtract = false;
    String input = value;

    if (value.startsWith("-")) {
      subtract = true;
      input = value.substring(1);
    }

    try {
      int seconds = parseTime(input);
      if (subtract) seconds = -seconds;
      currentHalfExtraTime += seconds;
      logger.send(sender, Lang.RESULT_MATCH_EXTRA.replace(new String[]{(subtract ? "&c-" : "&a+") + Math.abs(seconds)}));
      updateHalfMessage();
    } catch (NumberFormatException exception) {
      logger.send(sender, Lang.RESULT_MATCH_INVALID_TIME.replace(null));
    }
  }

  public void addScore(CommandSender sender, String team, String scorer, String assist) {
    if ("home".equalsIgnoreCase(team)) homeScore++;
    else if ("away".equalsIgnoreCase(team)) awayScore++;
    else {
      logger.send(sender, Lang.RESULT_TEAMS_INVALID.replace(null));
      return;
    }

    String teamName = "home".equalsIgnoreCase(team) ? home : away;
    String time = formatTime(matchTimer.getSecondsElapsed());
    int goalMinute = matchTimer != null ? matchTimer.getSecondsElapsed() / 60 + 1 : 0;

    String msgMC;
    if (assist != null && !assist.isEmpty()) {
      msgMC = Config.RESULT_FORMATS_MINECRAFT_GOAL_ASSIST.getString(new String[]{scorer, teamName, String.valueOf(goalMinute), assist});
    } else {
      msgMC = Config.RESULT_FORMATS_MINECRAFT_GOAL_ADD.getString(new String[]{scorer, teamName, String.valueOf(goalMinute)});
    }

    broadcastMinecraft(msgMC);
    updateHalfMessage();

    if (!Config.RESULT_ENABLED.getValue(Boolean.class) || DiscordSRV.getPlugin() == null) return;
    if (!shouldSendToDiscord) return;

    String discordID = Config.RESULT_DISCORD_ID.getString(null);
    if (discordID.isEmpty()) return;

    TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordID);
    if (channel == null) return;

    String type = Config.RESULT_FORMATS_DISCORD_GOAL_TYPE.getString(null);
    if (type.equalsIgnoreCase("embed")) {
      String description = assist != null && !assist.isEmpty()
          ? Config.RESULT_FORMATS_DISCORD_GOAL_ASSIST_EMBED_DESCRIPTION.getString(new String[]{scorer, assist, home, String.valueOf(homeScore), String.valueOf(awayScore), away, time})
          : Config.RESULT_FORMATS_DISCORD_GOAL_ADD_EMBED_DESCRIPTION.getString(new String[]{scorer, home, String.valueOf(homeScore), String.valueOf(awayScore), away, time});

      EmbedBuilder embedBuilder = new EmbedBuilder()
          .setAuthor(
              ChatColor.stripColor(logger.color(Config.RESULT_FORMATS_DISCORD_GOAL_EMBED_TITLE.getString(new String[]{teamName}))),
              null,
              Config.RESULT_FORMATS_DISCORD_GOAL_EMBED_ICON_URL.getString(null)
          )
          .setDescription(ChatColor.stripColor(logger.color(description)))
          .setColor(Color.decode(Config.RESULT_FORMATS_DISCORD_GOAL_EMBED_COLOR.getString(null)));

      channel.sendMessageEmbeds(embedBuilder.build()).queue();
    } else {
      String message = assist != null && !assist.isEmpty()
          ? Config.RESULT_FORMATS_DISCORD_GOAL_ASSIST_REGULAR.getString(new String[]{time, scorer, teamName, assist, home, String.valueOf(homeScore), String.valueOf(awayScore), away})
          : Config.RESULT_FORMATS_DISCORD_GOAL_ADD_REGULAR.getString(new String[]{time, scorer, teamName, home, String.valueOf(homeScore), String.valueOf(awayScore), away});

      channel.sendMessage(ChatColor.stripColor(logger.color(message))).queue();
    }
  }

  public void removeScore(CommandSender sender, String team) {
    String teamName;

    switch (team.toLowerCase()) {
      case "home":
        if (homeScore <= 0) { logger.send(sender, Lang.RESULT_SCORE_INVALID.replace(null)); return; }
        homeScore--;
        teamName = home;
        break;

      case "away":
        if (awayScore <= 0) { logger.send(sender, Lang.RESULT_SCORE_INVALID.replace(null)); return; }
        awayScore--;
        teamName = away;
        break;

      default:
        logger.send(sender, Lang.RESULT_SCORE_INVALID.replace(null));
        return;
    }

    String msgMC = Config.RESULT_FORMATS_MINECRAFT_GOAL_REMOVE.getString(new String[]{prefix, teamName});
    String msgDC = Config.RESULT_FORMATS_DISCORD_GOAL_REMOVE.getString(new String[]{formatTime(matchTimer.getSecondsElapsed()), teamName});

    broadcastBoth(msgMC, msgDC);
    updateHalfMessage();
    logger.send(sender, Lang.RESULT_SCORE_UPDATED.replace(new String[]{team}));
  }

  public void updateHalfMessage() {
    if (matchTimer == null) return;

    int displayTime = matchTimer.getSecondsElapsed();
    String msgMC = Config.RESULT_FORMATS_MINECRAFT_UPDATE.getString(
        new String[]{
            prefix, home, String.valueOf(homeScore), String.valueOf(awayScore), away,
            formatColoredTime(displayTime),
            currentHalf == Half.FIRST ? " 1HT" : " 2HT",
            currentHalfExtraTime > 0 ? "&c (ET: " + formatTime(currentHalfExtraTime) + ")" : ""
        });

    logger.broadcastBar(msgMC);
  }

  public void broadcastMinecraft(String message) {
    logger.broadcast(message);
    logger.broadcastBar(message);
  }

  public void broadcastDiscord(String message) {
    if (shouldSendToDiscord) sendToDiscord(message);
  }

  public void broadcastBoth(String minecraftMsg, String discordMsg) {
    broadcastMinecraft(minecraftMsg);
    broadcastDiscord(discordMsg);
  }

  public String getMatchStatus() {
    if (currentHalf == Half.NOT_STARTED) return Lang.RESULT_STATUS_NONE.replace(null);

    return Lang.RESULT_STATUS.replace(
        new String[]{
            prefix, home, String.valueOf(homeScore), String.valueOf(awayScore), away,
            formatColoredTime(matchTimer.getSecondsElapsed()),
            currentHalf == Half.FIRST ? " 1HT" : " 2HT",
            paused ? "&c Pauziran" : "",
            currentHalfExtraTime > 0 ? "&c (ET: " + formatTime(currentHalfExtraTime) + ")" : "",
    });
  }

  private String resolveGroupName(String input) {
    Group group = luckPerms.getGroupManager().getGroup(input.toLowerCase());
    if (group != null) {
      shouldSendToDiscord = true;
      return group.getDisplayName() != null ? group.getDisplayName() : group.getName().toUpperCase();
    }

    shouldSendToDiscord = false;
    return input;
  }

  public void resetMatch() {
    home = null;
    away = null;
    homeScore = 0;
    awayScore = 0;
    matchTimer = null;
    currentHalf = Half.NOT_STARTED;
    paused = false;
    prefix = "&bEvent";
    currentHalfExtraTime = 0;
  }

  public int parseTime(String input) throws NumberFormatException {
    if (input == null || input.isEmpty()) throw new NumberFormatException("Time string is empty");

    String normalized = input.toLowerCase().replaceAll("\\s+", "");
    int totalSeconds = 0;
    StringBuilder number = new StringBuilder();

    for (int i = 0; i < normalized.length(); i++) {
      char c = normalized.charAt(i);

      if (Character.isDigit(c)) {
        number.append(c);
        continue;
      }

      if (number.length() == 0) throw new NumberFormatException("Unit without number at position " + i);
      int value = Integer.parseInt(number.toString());

      switch (c) {
        case 'm':
          if (i + 2 < normalized.length() && normalized.startsWith("min", i)) {
            totalSeconds += value * 60;
            i += 2;
          } else totalSeconds += value * 60;
          break;

        case 's':
          totalSeconds += value;
          break;

        default: throw new NumberFormatException("Invalid character '" + c + "' at position " + i);
      }

      number.setLength(0);
    }

    if (number.length() > 0) totalSeconds += Integer.parseInt(number.toString());
    return totalSeconds;
  }


  public String formatTime(int totalSeconds) {
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }

  public String formatColoredTime(int secondsElapsed) {
    int warningThreshold = (int) (defaultHalfDuration * 0.9);

    int adjustedSeconds = secondsElapsed;
    if (currentHalf == Half.SECOND) {
      adjustedSeconds -= defaultHalfDuration;
      if (adjustedSeconds < 0) adjustedSeconds = 0;
    }

    String time = formatTime(secondsElapsed);
    if (adjustedSeconds < warningThreshold) return "&a" + time;
    else if (adjustedSeconds < defaultHalfDuration) return "&e" + time;
    else return "&c" + time;
  }

  public void sendToDiscord(String message) {
    if (!Config.RESULT_ENABLED.getValue(Boolean.class) || DiscordSRV.getPlugin() == null) return;

    String discordID = Config.RESULT_DISCORD_ID.getString(null);
    if (discordID.isEmpty()) return;

    TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordID);
    if (channel == null) return;

    channel.sendMessage(ChatColor.stripColor(logger.color(message))).queue();
  }
}