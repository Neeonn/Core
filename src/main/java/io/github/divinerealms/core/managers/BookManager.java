package io.github.divinerealms.core.managers;

import io.github.divinerealms.core.main.CoreManager;
import io.github.divinerealms.core.utilities.Logger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.github.divinerealms.core.configs.Lang.BOOK_NOT_FOUND;

public class BookManager {
  private final ConfigManager configManager;
  private final Logger logger;
  private final Plugin plugin;
  @Getter
  private final Map<String, ItemStack> books = new HashMap<>();
  private Method AS_NMS_COPY_METHOD;
  private Constructor<?> PACKET_CONSTRUCTOR;
  private Method SEND_PACKET_METHOD;
  private boolean reflectionInitialized = false;

  public BookManager(CoreManager coreManager) {
    this.configManager = coreManager.getConfigManager();
    this.logger = coreManager.getLogger();
    this.plugin = coreManager.getPlugin();

    this.initializeReflection();
    this.loadBooks();
  }

  private void initializeReflection() {
    String version = getServerVersion();

    Class<?> CRAFT_BOOK_CLASS = getNMSClass("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
    AS_NMS_COPY_METHOD = getMethod(CRAFT_BOOK_CLASS, "asNMSCopy", ItemStack.class);

    Class<?> PACKET_CLASS_CUSTOM_PAYLOAD = getNMSClass(
        "net.minecraft.server." + version + ".PacketPlayOutCustomPayload");
    Class<?> PACKET_CLASS_GENERIC = getNMSClass("net.minecraft.server." + version + ".Packet");
    Class<?> packetDataSerializerClass = getNMSClass("net.minecraft.server." + version + ".PacketDataSerializer");

    PACKET_CONSTRUCTOR = getConstructor(PACKET_CLASS_CUSTOM_PAYLOAD, String.class, packetDataSerializerClass);

    Class<?> playerConnectionClass = getNMSClass("net.minecraft.server." + version + ".PlayerConnection");

    Method packetMethod = getMethod(playerConnectionClass, "sendPacket", PACKET_CLASS_GENERIC);
    if (packetMethod == null) {
      packetMethod = getMethod(playerConnectionClass, "a", PACKET_CLASS_GENERIC);
    }
    
    SEND_PACKET_METHOD = packetMethod;

    if (CRAFT_BOOK_CLASS != null && AS_NMS_COPY_METHOD != null &&
        PACKET_CLASS_CUSTOM_PAYLOAD != null && PACKET_CLASS_GENERIC != null &&
        packetDataSerializerClass != null && PACKET_CONSTRUCTOR != null &&
        playerConnectionClass != null && SEND_PACKET_METHOD != null) {
      reflectionInitialized = true;
      plugin.getLogger().info("BookManager reflection successfully initialized for version " + version);
    } else {
      plugin.getLogger().severe("BookManager reflection setup FAILED! Virtual books will not work.");
      plugin.getLogger().severe("Check the console logs above for specific class or method failures.");
    }
  }

  private Class<?> getNMSClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      plugin.getLogger().log(Level.SEVERE, "Could not find NMS class: " + name + " (FAILED)", e);
      return null;
    }
  }

  private Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
    if (clazz == null) {
      return null;
    }
    
    try {
      return clazz.getConstructor(parameterTypes);
    } catch (NoSuchMethodException e) {
      plugin.getLogger().log(Level.SEVERE,
          "Could not find constructor in class " + clazz.getName() + " with required parameters.", e);
      return null;
    }
  }

  private String getServerVersion() {
    return plugin.getServer().getClass().getPackage().getName().split("\\.")[3];
  }

  private Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
    if (clazz == null) {
      return null;
    }
    
    try {
      return clazz.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException e) {
      try {
        Method method = clazz.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
      } catch (NoSuchMethodException ex) {
        return null;
      }
    }
  }

  public void loadBooks() {
    books.clear();
    var config = configManager.getConfig("books.yml");
    if (config == null) {
      return;
    }

    var booksSection = config.getConfigurationSection("books");
    if (booksSection == null) {
      return;
    }

    booksSection.getKeys(false).forEach(bookKey -> {
      ConfigurationSection bookSec = booksSection.getConfigurationSection(bookKey);
      if (bookSec == null) {
        return;
      }

      String title = bookSec.getString("title", bookKey);
      String author = bookSec.getString("author", plugin.getName());
      List<String> rawPages = bookSec.getStringList("pages");
      if (rawPages.isEmpty()) {
        plugin.getLogger().log(Level.WARNING, "&cBook \"" + bookKey + "\" has no pages defined. Skipping");
        return;
      }

      ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
      BookMeta meta = (BookMeta) book.getItemMeta();
      if (meta == null) {
        return;
      }

      meta.setTitle(logger.color(title));
      meta.setAuthor(logger.color(author));

      List<String> processedPages = rawPages.stream().map(logger::color).collect(Collectors.toList());
      meta.setPages(processedPages);
      book.setItemMeta(meta);

      books.put(bookKey.toLowerCase(), book);
    });

    logger.info("&a✔ &9Loaded &e" + books.size() + " &9custom books.");
  }

  public void reloadBooks() {
    configManager.reloadConfig("books.yml");
    loadBooks();
  }

  public void openBook(Player player, String bookId) {
    ItemStack book = books.get(bookId.toLowerCase());
    if (book == null) {
      logger.send(player, BOOK_NOT_FOUND, bookId);
      return;
    }
    
    if (!reflectionInitialized) {
      logger.send(player,
          "&cCould not open book. Server reflection failed to initialize. Check console for NMS errors.");
      return;
    }

    plugin.getServer().getScheduler().runTask(plugin, () -> {
      try {
        ItemStack heldItem = player.getItemInHand();
        player.setItemInHand(book);

        Object nmsItemStack = AS_NMS_COPY_METHOD.invoke(null, book);
        Class<?> itemStackClass = getNMSClass("net.minecraft.server." + getServerVersion() + ".ItemStack");

        Class<?> byteBufClass = Class.forName("io.netty.buffer.Unpooled");

        Method bufferMethod = getMethod(byteBufClass, "buffer", int.class);
        if (bufferMethod == null) {
          throw new NoSuchMethodException("Could not find io.netty.buffer.Unpooled#buffer(int).");
        }

        Object byteBuf = bufferMethod.invoke(null, 256);

        Class<?> packetDataSerializerClass = getNMSClass(
            "net.minecraft.server." + getServerVersion() + ".PacketDataSerializer");
        if (packetDataSerializerClass == null) {
          throw new ClassNotFoundException("PacketDataSerializer not found.");
        }

        Constructor<?> dataSerializerConstructor = getConstructor(packetDataSerializerClass,
            Class.forName("io.netty.buffer.ByteBuf"));
        if (dataSerializerConstructor == null) {
          throw new NoSuchMethodException("PacketDataSerializer constructor not found.");
        }

        Object dataSerializer = dataSerializerConstructor.newInstance(byteBuf);

        Method writeItemStackMethod = getMethod(packetDataSerializerClass, "a", itemStackClass);
        if (writeItemStackMethod == null) {
          throw new NoSuchMethodException("Method 'a(ItemStack)' not found in PacketDataSerializer.");
        }

        writeItemStackMethod.invoke(dataSerializer, nmsItemStack);

        Object packet = PACKET_CONSTRUCTOR.newInstance("MC|BOpen", dataSerializer);

        Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
        Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

        SEND_PACKET_METHOD.invoke(playerConnection, packet);
        player.setItemInHand(heldItem);
      } catch (Exception exception) {
        plugin.getLogger().log(Level.SEVERE,
            "Failed to open virtual book for " + player.getName() + " using NMS reflection!", exception);
        player.setItemInHand(player.getInventory().getItemInHand());
      }
    });
  }
}
