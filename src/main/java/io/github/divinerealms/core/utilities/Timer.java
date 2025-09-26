package io.github.divinerealms.core.utilities;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

@Getter
public class Timer {
  private final Plugin plugin;
  private final Consumer<Timer> everySecond;
  private final Runnable beforeTimer;

  @Setter private int secondsElapsed;
  @Setter private boolean isRunning = false;

  private BukkitRunnable task;

  public Timer(Plugin plugin, Runnable beforeTimer, Consumer<Timer> everySecond) {
    this.plugin = plugin;
    this.beforeTimer = beforeTimer;
    this.everySecond = everySecond;
  }

  public void start() {
    if (isRunning) return;
    isRunning = true;

    if (beforeTimer != null) beforeTimer.run();

    task = new BukkitRunnable() {
      @Override
      public void run() {
        secondsElapsed++;
        if (everySecond != null) everySecond.accept(Timer.this);
      }
    };

    task.runTaskTimer(plugin, 20L, 20L);
  }

  public void cancel() {
    if (task != null) task.cancel();
    isRunning = false;
  }

  public String getFormattedTime() {
    int min = secondsElapsed / 60;
    int sec = secondsElapsed % 60;
    return String.format("%02d:%02d", min, sec);
  }
}
