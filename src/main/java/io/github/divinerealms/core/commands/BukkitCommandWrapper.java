package io.github.divinerealms.core.commands;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class BukkitCommandWrapper extends Command {
  @Getter private final CommandExecutor executor;
  @Setter private TabCompleter tabCompleter;

  public BukkitCommandWrapper(String name, CommandExecutor executor, List<String> aliases) {
    super(name);
    this.executor = executor;
    if (aliases != null && !aliases.isEmpty()) this.setAliases(aliases);
    this.setDescription("Command " + name);
    this.setUsage("/" + name);
    if (executor instanceof TabCompleter) this.tabCompleter = (TabCompleter) executor;
  }

  @Override
  public boolean execute(CommandSender sender, String label, String[] args) {
    return executor.onCommand(sender, this, label, args);
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
    if (tabCompleter != null) {
      try {
        List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
        return completions != null ? completions : Collections.emptyList();
      } catch (Exception ex) {
        Bukkit.getLogger().log(Level.SEVERE, "Error on tab complete", ex);
        return Collections.emptyList();
      }
    }
    return super.tabComplete(sender, alias, args);
  }
}
