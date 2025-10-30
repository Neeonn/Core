package io.github.divinerealms.core.utilities;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Sound;

@Setter
@Getter
public class PlayerSettings {
  private boolean mentionSoundEnabled = true;
  private Sound mentionSound = Sound.ORB_PICKUP;
}
