package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.configs.Config;
import io.github.divinerealms.core.configs.Lang;
import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateMessagesManager {
  private final CoreManager coreManager;
  private final ChannelManager channelManager;
  private final Logger logger;

  @Getter private boolean enabled;

  private final Map<UUID, UUID> lastConvoPartner = new ConcurrentHashMap<>();

  public PrivateMessagesManager(CoreManager coreManager) {
    this.coreManager = coreManager;
    this.channelManager = coreManager.getChannelManager();
    this.logger = coreManager.getLogger();
    this.enabled = Config.PRIVATE_MESSAGES_ENABLED.getValue(Boolean.class);

    if (!this.enabled) return;
    this.clearState();
    logger.info("&a✔ &9Private Messages module loaded successfully.");
  }

  public void clearState() {
    lastConvoPartner.clear();
  }

  public void sendPrivateMessage(Player sender, Player recipient, String message) {
    String senderName = coreManager.getChat().getPlayerPrefix(sender) + sender.getName();
    String recipientName = coreManager.getChat().getPlayerPrefix(recipient) + recipient.getName();

    logger.send(sender, Config.PRIVATE_MESSAGES_SENDER_FORMAT.getString(new String[]{recipientName, message}));
    logger.send(recipient, Config.PRIVATE_MESSAGES_RECIPIENT_FORMAT.getString(new String[]{senderName, message}));

    lastConvoPartner.put(sender.getUniqueId(), recipient.getUniqueId());
    lastConvoPartner.put(recipient.getUniqueId(), sender.getUniqueId());

    channelManager.getSocialSpy().forEach(spyUUID -> {
      if (spyUUID.equals(sender.getUniqueId()) || spyUUID.equals(recipient.getUniqueId())) return;

      Player spy = Bukkit.getPlayer(spyUUID);
      if (spy == null) return;

      logger.send(spy, Config.PRIVATE_MESSAGES_SPY_FORMAT.getString(new String[]{Lang.CHANNEL_SPY_FORMAT.replace(null), senderName, recipientName, message}));
    });
  }

  public void reply(Player sender, String message) {
    UUID lastPartnerUUID = lastConvoPartner.get(sender.getUniqueId());
    if (lastPartnerUUID == null) { logger.send(sender, Lang.PRIVATE_MESSAGES_NO_REPLY_TARGET.replace(null)); return; }

    Player recipient = Bukkit.getPlayer(lastPartnerUUID);
    if (recipient == null || !recipient.isOnline()) {
      logger.send(sender, Lang.PRIVATE_MESSAGES_NO_REPLY_TARGET.replace(null));
      lastConvoPartner.remove(sender.getUniqueId());
      return;
    }

    sendPrivateMessage(sender, recipient, message);
  }
}
