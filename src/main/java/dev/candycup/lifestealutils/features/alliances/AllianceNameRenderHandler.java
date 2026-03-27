package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;

public final class AllianceNameRenderHandler {
   public AllianceNameRenderHandler() {
      LifestealUtilsEvents.PLAYER_NAME_RENDER.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onPlayerNameRender(event);
      });
   }

   public boolean isEnabled() {
      return Config.isEnableAlliances();
   }

   public void onPlayerNameRender(PlayerNameRenderEvent event) {
      if (!Alliances.isAlliedName(event.getPlayerName())) {
         return;
      }
      event.setModifiedDisplayName(Alliances.colorizeNameTag(event.getModifiedDisplayName()));
   }

   public static void refreshPrefixCandidatesNow() {
   }
}
