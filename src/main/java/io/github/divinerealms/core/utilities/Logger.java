package io.github.divinerealms.core.utilities;

import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Logger {
  private final CoreManager coreManager;
  private final Server server;
  private final ConsoleCommandSender consoleSender;
  @Getter private final String consolePrefix;

  public Logger(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.server = coreManager.getPlugin().getServer();
    this.consoleSender = this.server.getConsoleSender();
    this.consolePrefix = ChatColor.AQUA + "[" + coreManager.getPlugin().getDescription().getName() + "] " + ChatColor.BLUE;
  }

  public void info(String message) {
    message = message.replace("{prefix}", "");
    consoleSender.sendMessage(consolePrefix + color(message));
  }

  public void send(CommandSender sender, String message) {
    if (sender instanceof Player) {
      message = message.replace("{prefix}", Lang.PREFIX.replace(null));
      sender.sendMessage(color(message));
    } else {
      message = message.replace("{prefix}", "");
      consoleSender.sendMessage(consolePrefix + color(message));
    }
  }

  public void send(String permission, String message) {
    String formattedMc = message.replace("{prefix}", Lang.PREFIX.replace(null));
    String formattedConsole = message.replace("{prefix}", "");

    server.broadcast(color(formattedMc), permission);
    consoleSender.sendMessage(consolePrefix + color(formattedConsole));
  }

  public void broadcast(String message) {
    message = message.replace("{prefix}", Lang.PREFIX.replace(null));
    server.broadcastMessage(color(message));
  }

  public void sendActionBar(Player player, String message) {
    message = color(message.replace("{prefix}", Lang.PREFIX.replace(null)));
    IChatBaseComponent iChatBaseComponent = ChatSerializer.a("{\"text\": \"" + message + "\"}");
    PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(iChatBaseComponent, (byte)2);
    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
  }

  public void broadcastBar(String message) {
    message = color(message.replace("{prefix}", Lang.PREFIX.replace(null)));
    IChatBaseComponent iChatBaseComponent = ChatSerializer.a("{\"text\": \"" + message + "\"}");
    PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(iChatBaseComponent, (byte)2);

    for(Player player : coreManager.getCachedPlayers()) {
      ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
    }
  }

  public void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    title = color(title);
    subtitle = color(subtitle);
    CraftPlayer craftPlayer = (CraftPlayer) player;

    IChatBaseComponent titleJSON = ChatSerializer.a("{\"text\":\"" + title + "\"}");
    IChatBaseComponent subtitleJSON = ChatSerializer.a("{\"text\":\"" + subtitle + "\"}");

    PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, titleJSON);
    PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleJSON);
    PacketPlayOutTitle timesPacket = new PacketPlayOutTitle(fadeIn, stay, fadeOut);

    craftPlayer.getHandle().playerConnection.sendPacket(titlePacket);
    craftPlayer.getHandle().playerConnection.sendPacket(subtitlePacket);
    craftPlayer.getHandle().playerConnection.sendPacket(timesPacket);
  }

  public String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }
}