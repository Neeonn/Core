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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    List<Message.Attachment> attachments = event.getMessage().getAttachments();
    BaseComponent[] attachmentComponent = null;
    if (!attachments.isEmpty()) {
      String attachmentText = attachments.size() == 1 ? logger.color("&9 &l[ATTACHMENT]&r ") : logger.color("&9 &l[" + attachments.size() + "x ATTACHMENTS] &r");
      TextComponent component = new TextComponent(attachmentText);
      component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachments.get(0).getUrl()));
      component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(logger.color("&eKliknite da vidite attachment(s)"))}));
      attachmentComponent = new BaseComponent[]{component};
    }

    if (messageRaw.trim().isEmpty() && attachmentComponent != null) messageRaw = "";

    for (String minecraftChannel : minecraftChannels) {
      ChannelInfo info = channelManager.getChannels().get(minecraftChannel);
      if (info == null) continue;
      if (info.discordId.equals(globalInfo.discordId) && !minecraftChannel.equals(channelManager.getDefaultChannel())) continue;

      String formattedMessage = getFormatted(info, displayName, replyName, messageRaw, minecraftChannel.toUpperCase());
      String[] parts = formattedMessage.split("\\{ATTACHMENTS}", 2);

      TextComponent prefixComponent = new TextComponent(logger.color(parts[0]));
      TextComponent suffixComponent = parts.length > 1 ? new TextComponent(logger.color(parts[1])) : new TextComponent("");

      if (attachmentComponent != null) prefixComponent.addExtra(attachmentComponent[0]);
      prefixComponent.addExtra(suffixComponent);

      if (info.permission != null && !info.permission.isEmpty()) {
        Set<UUID> subscribers = channelManager.getSubscribers(minecraftChannel);

        for (Player player : Bukkit.getOnlinePlayers()) {
          if (player.hasPermission(info.permission) || subscribers.contains(player.getUniqueId())) player.spigot().sendMessage(prefixComponent);
        }
      } else {
        for (Player player : Bukkit.getOnlinePlayers()) player.spigot().sendMessage(prefixComponent);
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

  private static @NotNull String getFormatted(ChannelInfo info, String displayName, String replyName, String message, String channelName) {
    String format = info.formats.discordToMinecraft;
    if (format == null || format.trim().isEmpty()) format = "%name%: %message%";

    return format
        .replace("%channelName%", channelName)
        .replace("%name%", displayName)
        .replace("%reply%", replyName)
        .replace("%message%", message)
        .replaceAll("%[^%]+%", "")
        .trim();
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