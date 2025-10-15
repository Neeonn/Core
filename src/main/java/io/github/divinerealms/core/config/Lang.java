package io.github.divinerealms.core.config;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public enum Lang {
  PREFIX("prefix", "&b&lCore&8&l» &9"),
  HELP("help", "{prefix}&cNepoznata komanda. Dostupno: &6/&ecore reload"),
  NO_PERM("no-perm", "{prefix}&cNemate dozvolu (&4{0}&c) za komandu &6/&e{1}&c!"),
  UNKNOWN_COMMAND("unknown-command", "{prefix}&cNepoznata komanda."),
  PLAYER_NOT_FOUND("player-not-found", "{prefix}&cIgrač {0} nije pronađen."),
  ANTI_SPAM("anti-spam", "&cŠalješ previše poruka, iskuliraj malo."),
  INGAME_ONLY("ingame-only", "{prefix}&cOva komanda se može koristiti samo u igri."),
  USAGE("usage", "{prefix}Usage: /{0}"),

  CHANNEL_HELP("channels.help", String.join(System.lineSeparator(),
      "{prefix}&cNepoznata komanda. Dostupne komande u Core pluginu:",
      "&6/&echannel toggle &2<&akanal&2>&7: &f&oOmogućite/onemogućite određeni čet kanal.",
      "&6/&echannel team&6|&et&7: &f&oKomanda za ligaški tim čet."
  )),
  CHANNEL_NOT_FOUND("channels.not-found", "&cChannel '&e{0}&c' not found."),
  CHANNEL_NOT_IN_TEAM("channels.not-in-team", "{prefix}&cNiste ni u jednom timu!"),
  CHANNEL_NO_PERM("channels.insufficient-permission", "{prefix}&cNemate dozvolu (&4{0}&c) da pišete u '&e{1}&c' četu!"),
  CHANNEL_TOGGLE("channels.toggle", "{prefix}&e{0} čet je {1}&e!"),
  CHANNEL_DISABLED("channels.disabled", "{prefix}&cČet kanal '&e{0}&c' je isključen od strane admina."),
  CHANNEL_DISABLED_BROADCAST("channels.disabled-broadcast", "{prefix}&cČet kanal &e{0} &cje {1}&c od strane {2}!"),
  CHANNEL_REPLY("channels.reply", "&7 &o(reply -> {0})&r"),
  CHANNEL_SPY_PREFIX("channels.spy.prefix", "&c[SPY] [&e{0}&c] &r"),
  CHANNEL_SPY_TOGGLED("channels.spy.toggled", "{prefix}Social Spy je {0}&f!"),

  PRIVATE_MESSAGES_SENDER_FORMAT("private-messages.format.sender", "&6✉ &fVi &7→ &b{0}&7: {1}"),
  PRIVATE_MESSAGES_RECIPIENT_FORMAT("private-messages.format.recipient", "&6✉ &b{0} &7kaže: &f{1}"),
  PRIVATE_MESSAGES_SPY_FORMAT("private-messages.format.spy", "{0}&e({0} &7→ &e{1}): &f{2}"),
  PRIVATE_MESSAGES_SELF("private-messages.self", "&cNe možete poslati poruku samom sebi."),
  PRIVATE_MESSAGES_NO_REPLY_TARGET("private-messages.reply.no-target", ""),

  CLIENT_BLOCKER_USAGE("client-blocker.usage", String.join(System.lineSeparator(),
      "{prefix}Lista dostupnih &b/clientblocker &fkomandi:",
      "&b/clientblocker|cb toggle: &fIsključite/uključite clientblocker.",
      "&b/clientblocker|cb check <igrač>: &fProverite kojim clientom je igrač ušao.",
      "&b/clientblocker|cb exempt <igrač>: &fPrivremeno (30min) dozvolite igraču da uđe sa clientima.")),
  CLIENT_BLOCKER_TOGGLE("client-blocker.toggle", "{prefix}&eClient Blocker je {0}&e!"),
  CLIENT_BLOCKER_NOTIFY("client-blocker.notify", "&9[Client Blocker] &c{0} &8&o({1}) &cje pokušao ući sa &c{2}"),
  CLIENT_BLOCKER_BYPASS_NOTIFY("client-blocker.notify-bypass", "&9[Client Blocker] &c{0} &8&o({1}) &bje ušao sa &c{2}"),
  CLIENT_BLOCKER_KICK("client-blocker.kick", "&cZabranjeni clienti!"),
  CLIENT_BLOCKER_CHECK_RESULT("client-blocker.check-result", "{prefix}&b{0} &fkoristi &e{1} &fclient (Bypass: {2}&f)"),
  CLIENT_BLOCKER_EXEMPT("client-blocker.exempt", "{prefix}&fClient Blocker bypass je {0}&f za &b{1}&f!"),

  RESULT_HELP("result.help", String.join(System.lineSeparator(),
      "{prefix}Lista dostupnih &6/result &9komandi:",
      "&6/result|rs status: &fPokazuje status trenutne utakmice.",
      "&6/result|rs start: &fStartuje meč/nastavlja poluvreme.",
      "&6/result|rs pause/resume: &fPauzira/nastavlja štopericu.",
      "&6/result|rs stop: &fPrekida meč.",
      "&6/result|rs teams <home> <away>: &fPostavlja timove.",
      "&6/result|rs prefix: &fPostavlja prefix.",
      "&6/result|rs time: &fPodešava vreme trajanja.",
      "&6/result|rs add <home|away> <scorer> [assister]: &fDodaje gol timu.",
      "&6/result|rs remove <home|away>: &fUklanja gol timu.",
      "&6/result|rs stophalf: &fStopira poluvreme.",
      "&6/result|rs extratime: &fDodaje ET (primer: 20s, 1min, 1min20s, -50s)"
  )),
  RESULT_TEAMS_SET("result.teams.set", "{prefix}&aTimovi podešeni: &9{0} &avs &c{1}&a!"),
  RESULT_TEAMS_UNKNOWN("result.teams.unknown", "{prefix}&cTimovi nisu podešeni. Koristite: &6/result teams <home> <away>"),
  RESULT_TEAMS_INVALID("result.teams.invalid", "{prefix}&cTim nije validan! Koristite &ehome &cili &eaway&c."),
  RESULT_MATCH_RUNNING("result.match.running", "{prefix}&cMeč je već u toku!"),
  RESULT_MATCH_FINISHED("result.match.finished", "{prefix}&cMeč je već završen!"),
  RESULT_MATCH_PREFIX("result.match.prefix", "{prefix}&aMeč prefix podešen: {0}"),
  RESULT_MATCH_TIME("result.match.time", "{prefix}&aVreme podešeno na &e{0}&a!"),
  RESULT_MATCH_INVALID_TIME("result.match.invalid-time", "{prefix}&cVreme nije validno! Koristite: 1min20s, 30s ili -20s."),
  RESULT_MATCH_EXTRA("result.match.extra", "{prefix}&aDodato vreme podešeno: &e{0}"),
  RESULT_HALF_STOPPED("result.half.stopped", "{prefix}&cPoluvreme stopirano!"),
  RESULT_HALF_NONE("result.half.none", "{prefix}&cNema aktivnog poluvremena."),
  RESULT_SCORE_INVALID("result.score.invalid", "{prefix}&cNepostojeći tim ili je rezultat već 0."),
  RESULT_SCORE_UPDATED("result.score.update", "{prefix}&aRezultat osvežen za tim {0}&a."),
  RESULT_STATUS("result.status.message", String.join(System.lineSeparator(),
      "&e---------------------------------------------",
      "{prefix}&bStatus trenutne utakmice:",
      "&r &r",
      "&e[{0} Utakmica&e]",
      "&7Rezultat: &9{1} &f{2} &7- &f{3} &c{4}",
      "&7Vreme: &e{5}{6}{7}{8}",
      "&r &r",
      "&e---------------------------------------------"
  )),
  RESULT_STATUS_NONE("result.status.none", "{prefix}&cNema aktivnih mečeva..."),
  RESULT_PREFIX_HOST("result.prefix-host", "&b&lEvent Host"),

  PLAYTIME_SELF("playtime.self", "{prefix}Vaš playtime je: &e{0}"),
  PLAYTIME_OTHER("playtime.other", "{prefix}Playtime igrača &b{0} &fje: &e{1}"),
  PLAYTIME_HELP("playtime.help", String.join(System.lineSeparator(),
      "{prefix}Lista dostupnih &b/playtime&f komandi:",
      "&b/playtime: &fPokazuje Vaš playtime.",
      "&b/playtime <igrač>: &fPokazuje playtime navedenog igrača.",
      "&b/playtime top <strana> <limit>: &fPokazuje playtime top.")),
  PLAYTIME_TOP_HEADER("playtime.top.header", String.join(System.lineSeparator(),
      "&e---------------------------------------------",
      "{prefix}&eTop {0} Playtime &7(Strana {1}/{2})",
      "&r &r")),
  PLAYTIME_TOP_FOOTER("playtime.top.footer", String.join(System.lineSeparator(),
      "&r &r",
      "&9TIP: &f&oKoristite &b/playtime <igrač> &f&oda vidite tuđi playtime.",
      "&e---------------------------------------------")),
  PLAYTIME_TOP_ENTRY("playtime.top.entry", "&e#{0} &b{1} &7- &e{2}"),

  ROSTERS_USAGE("rosters.usage", String.join(System.lineSeparator(),
      "{prefix}Lista dostupnih &b/rosters&f komandi:",
      "&b/rosters|rt create <imeTima> <tagTima>: &fPravljenje novog tima.",
      "&b/rosters|rt delete <imeTima>: &fBrisanje tima.",
      "&b/rosters|rt set <imeTima> name|tag <nazivTima|tagTima>: &fPodešavanje tima.",
      "&b/rosters|rt add <imeTima> <imeIgrača>: &fDodavanje igrača u tim.",
      "&b/rosters|rt remove <imeIgrača>: &fBrisanje trenutnog tima igraču.")),
  ROSTERS_EXISTS("rosters.exists", "{prefix}&cTim &e\"{0}\" &cveć postoji!"),
  ROSTERS_NOT_FOUND("rosters.not-found", "{prefix}&cTim &e\"{0}\" &cnije pronađen!"),
  ROSTERS_CREATE("rosters.create", "{prefix}Tim &e\"{0}\"&f sa tagom {1}&f je uspešno napravljen!"),
  ROSTERS_DELETE("rosters.delete", "{prefix}Tim &e\"{0}\"&f je uspešno obrisan."),
  ROSTERS_SET("rosters.set", "{prefix}Podešen &e{0} &fna {1}&f za tim &e\"{2}\"&f!"),
  ROSTERS_INVALID_TYPE("rosters.invalid-type", "{prefix}&cKoristite &e\"name\"&c ili &e\"tag\"&c."),
  ROSTERS_ADD("rosters.add", "{prefix}Igrač &b{0} &fje dodat u tim &e\"{1}\"&f!"),
  ROSTERS_REMOVE("rosters.remove", "{prefix}Igrač &b{0} &fje izbačen iz svog tima!"),

  PROXY_CHECK_STATUS("proxy-check.status", String.join(System.lineSeparator(),
      "&e---------------------------------------------",
      "{prefix}ProxyCheck Result for &b{0} &7(&o{1}&7)",
      "&r &r",
      "&fStatus: &a{2}",
      "&fProxy: {3}",
      "&r &r",
      "&fASN: &7{4}",
      "&fRange: &7{5}",
      "&fProvider: &7{6}",
      "&fCountry: &7{7}",
      "&fRegion: &7{8} (&o{9}&7)",
      "&fCity: &7{10}",
      "&fType: &7{11}",
      "&e---------------------------------------------")),
  PROXY_CHECK_CHECKING("proxy-check.checking", "{prefix}Proveravamo IP &e\"{0}\" &fsa proxycheck.io..."),
  PROXY_CHECK_NO_DATA("proxy-check.no-data", "{prefix}&cNismo dobili informacije za IP &e\"{0}\"&c..."),
  PROXY_CHECK_ERROR("proxy-check.error", "{prefix}&cGreška prilikom provere: {0}"),
  PROXY_CHECK_COOLDOWN("proxy-check.cooldown", "{prefix}&cMorate sačekati još {0}s pre korišćenja."),

  MENTION_TOGGLED("toggle.mention", "{prefix}&fZvuk za mention u četu je {0}&f!"),

  ADMIN_RELOAD("admin.reload", "{prefix}&eCore reloaded! Osveženo: &e{0}"),

  MENTION("mention", "&6Pstt, &e{0} &6te je spomenuo u četu!"),

  ON("on", "&auključen"),
  OFF("off", "&cisključen");

  private static FileConfiguration LANG;
  private final String path;
  private final String def;

  Lang(String path, String start) {
    this.path = path;
    this.def = start;
  }

  public static void setFile(FileConfiguration config) {
    LANG = config;
  }

  public String getDefault() {
    return this.def;
  }

  public String replace(String[] args) {
    String value = ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, this.def));
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
