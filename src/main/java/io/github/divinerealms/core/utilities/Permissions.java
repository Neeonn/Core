package io.github.divinerealms.core.utilities;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Permissions {
  public static final String PERM_MAIN = "core.";
  public static final String PERM_ADMIN_MAIN = PERM_MAIN + "admin";
  public static final String PERM_ADMIN_RELOAD = PERM_ADMIN_MAIN + ".reload";
  public static final String PERM_ADMIN_PROXY_CHECK = PERM_ADMIN_MAIN + ".proxy-check";
  public static final String PERM_ADMIN_SILENT_JOIN_QUIT = PERM_MAIN + ".silent-joinquit";

  public static final String PERM_CHAT_COLOR = PERM_MAIN + "chat.color";
  public static final String PERM_BYPASS_DISABLED_CHANNEL = PERM_MAIN + ".bypass.disabled-channel";

  public static final String PERM_RESULT_MAIN = PERM_MAIN + "result";

  public static final String PERM_CHANNEL_MAIN = PERM_MAIN + "channel";
  public static final String PERM_CHANNEL_TOGGLE = PERM_CHANNEL_MAIN + ".toggle";

  public static final String PERM_CLIENT_BLOCKER_MAIN = PERM_MAIN + "client-blocker";
  public static final String PERM_CLIENT_BLOCKER_TOGGLE = PERM_CLIENT_BLOCKER_MAIN + ".toggle";
  public static final String PERM_CLIENT_BLOCKER_EXEMPT = PERM_CLIENT_BLOCKER_MAIN + ".exempt";
  public static final String PERM_CLIENT_BLOCKER_CHECK = PERM_CLIENT_BLOCKER_MAIN + ".check";
  public static final String PERM_CLIENT_BLOCKER_BYPASS = PERM_CLIENT_BLOCKER_MAIN + ".bypass";
  public static final String PERM_CLIENT_BLOCKER_NOTIFY = PERM_CLIENT_BLOCKER_MAIN + ".notify";

  public static final String PERM_PLAYTIME_MAIN = PERM_MAIN + "playtime";
  public static final String PERM_PLAYTIME_OTHER = PERM_PLAYTIME_MAIN + ".other";
  public static final String PERM_PLAYTIME_TOP = PERM_PLAYTIME_MAIN + ".top";

  public static final String PERM_COMMAND_MAIN = PERM_MAIN + "command";
  public static final String PERM_COMMAND_MSG = PERM_COMMAND_MAIN + ".msg";
  public static final String PERM_COMMAND_SPY = PERM_COMMAND_MAIN + ".spy";
  public static final String PERM_COMMAND_TOGGLE = PERM_COMMAND_MAIN + ".toggle";
  public static final String PERM_COMMAND_TOGGLE_OTHER = PERM_COMMAND_TOGGLE + ".other";

  public static final String PERM_ROSTERS_MAIN = PERM_MAIN + "rosters";
  public static final String PERM_ROSTERS_CREATE = PERM_ROSTERS_MAIN + ".create";
  public static final String PERM_ROSTERS_DELETE = PERM_ROSTERS_MAIN + ".delete";
  public static final String PERM_ROSTERS_SET = PERM_ROSTERS_MAIN + ".set";
  public static final String PERM_ROSTERS_ADD = PERM_ROSTERS_MAIN + ".add";
  public static final String PERM_ROSTERS_REMOVE = PERM_ROSTERS_MAIN + ".remove";
  public static final String PERM_ROSTERS_NOTIFY = PERM_ROSTERS_MAIN + ".notify";
}
