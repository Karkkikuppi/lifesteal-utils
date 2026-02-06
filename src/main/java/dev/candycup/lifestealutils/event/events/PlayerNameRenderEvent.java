package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.chat.Component;

/**
 * fired when a player's display name is rendered (in-world name tag or tab list).
 * <p>
 * performance note: this event fires frequently during rendering.
 * listeners should cache results where possible and avoid expensive operations.
 * <p>
 * features can modify the display name by setting a new value.
 */
public class PlayerNameRenderEvent extends LSUEvent {
   private final String playerName;
   private final Component originalDisplayName;
   private Component modifiedDisplayName;

   public PlayerNameRenderEvent(String playerName, Component originalDisplayName) {
      this.playerName = playerName;
      this.originalDisplayName = originalDisplayName;
      this.modifiedDisplayName = originalDisplayName;
   }

   /**
    * @return the plain username of the player
    */
   public String getPlayerName() {
      return playerName;
   }

   /**
    * @return the original unmodified display name
    */
   public Component getOriginalDisplayName() {
      return originalDisplayName;
   }

   /**
    * @return the current display name (may be modified by previous listeners)
    */
   public Component getModifiedDisplayName() {
      return modifiedDisplayName;
   }

   /**
    * set a modified display name. subsequent listeners will see this modified version.
    *
    * @param displayName the new display name
    */
   public void setModifiedDisplayName(Component displayName) {
      this.modifiedDisplayName = displayName != null ? displayName : originalDisplayName;
   }
}
