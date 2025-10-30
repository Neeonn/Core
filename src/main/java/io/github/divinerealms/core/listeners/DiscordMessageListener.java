package io.github.divinerealms.core.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.managers.ChannelManager;
import io.github.divinerealms.core.utilities.ChannelInfo;
import io.github.divinerealms.core.utilities.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DiscordMessageListener {
  private final CoreManager coreManager;
  private final Logger logger;
  private final ChannelManager channelManager;

  public DiscordMessageListener(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.logger = coreManager.getLogger();
    this.channelManager = coreManager.getChannelManager();
  }

  @SuppressWarnings("unused")
  @Subscribe
  public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
    if (!coreManager.isDiscordSRV()) return;
    TextChannel consoleChannel = DiscordSRV.getPlugin().getConsoleChannel();
    if (consoleChannel != null && event.getChannel().getId().equals(consoleChannel.getId())) return;
    if (event.getMessage().getAuthor().isBot()) return;

    List<String> minecraftChannels = channelManager.getDiscordIdToMinecraft().get(event.getChannel().getId());
    if (minecraftChannels == null || minecraftChannels.isEmpty()) return;

    ChannelInfo globalInfo = channelManager.getChannels().get(channelManager.getDefaultChannel());
    String displayName = event.getMember() != null ? event.getMember().getEffectiveName().trim() : event.getAuthor().getName().trim();
    String messageRaw = resolveDiscordMentions(event.getMessage());
    String replyName = getReplyName(event);

    if (messageRaw.trim().isEmpty() && !event.getMessage().getAttachments().isEmpty()) {
      messageRaw = event.getMessage().getAttachments().get(0).getUrl();
    }

    for (String minecraftChannel : minecraftChannels) {
      ChannelInfo info = channelManager.getChannels().get(minecraftChannel);
      if (info == null) continue;
      if (info.discordId.equals(globalInfo.discordId) && !minecraftChannel.equals(channelManager.getDefaultChannel())) continue;

      String formattedMessage = getFormatted(info, displayName, replyName, messageRaw);
      if (info.permission != null && !info.permission.isEmpty()) {
        logger.send(info.permission, formattedMessage);
      } else {
        logger.broadcast(formattedMessage);
      }
    }
  }

  private static @NotNull String getReplyName(DiscordGuildMessageReceivedEvent event) {
    Message referenced = event.getMessage().getReferencedMessage();
    if (referenced == null) return "";
    return referenced.getMember() != null
        ? Lang.CHANNEL_REPLY.replace(new String[]{referenced.getMember().getEffectiveName().trim()})
        : Lang.CHANNEL_REPLY.replace(new String[]{referenced.getAuthor().getName().trim()});
  }

  private static @NotNull String getFormatted(ChannelInfo info, String displayName, String replyName, String message) {
    String format = info.formats.discordToMinecraft;
    if (format == null || format.trim().isEmpty()) format = "%name%: %message%";

    return format.replace("%name%", displayName).replace("%reply%", replyName).replace("%message%", message).replaceAll("%[^%]+%", "").trim();
  }

  private static String resolveDiscordMentions(Message message) {
    String content = message.getContentRaw();

    for (User user : message.getMentionedUsers()) {
      String name = user.getName();
      content = content
          .replace("<@!" + user.getId() + ">", "@" + name)
          .replace("<@" + user.getId() + ">", "@" + name);
    }

    for (Role role : message.getMentionedRoles()) {
      String name = role.getName();
      content = content.replace("<@&" + role.getId() + ">", "@" + name);
    }

    for (GuildChannel channel : message.getMentionedChannels()) {
      String name = channel.getName();
      content = content.replace("<#" + channel.getId() + ">", "#" + name);
    }

    return content;
  }
}