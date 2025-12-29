package io.github.divinerealms.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigTabCompleter implements TabCompleter {
  private final List<String> rules;

  public ConfigTabCompleter(List<String> rules) {
    this.rules = rules == null
                 ? Collections.emptyList()
                 : rules;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (rules.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();
    for (String rule : rules) {
      if (rule == null || rule.isEmpty()) {
        continue;
      }

      String trimmed = rule.trim();
      if (trimmed.equalsIgnoreCase("@online")) {
        Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
        continue;
      }

      result.add(trimmed);
    }

    if (args != null && args.length > 0) {
      String last = args[args.length - 1].toLowerCase();
      List<String> filtered = new ArrayList<>();
      for (String string : result) {
        if (string.toLowerCase().startsWith(last)) {
          filtered.add(string);
        }
      }
      return filtered;
    }

    return result;
  }
}
