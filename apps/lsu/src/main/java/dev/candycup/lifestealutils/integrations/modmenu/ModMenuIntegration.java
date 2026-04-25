package dev.candycup.lifestealutils.integrations.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.candycup.lifestealutils.config.ConfigResolver;

public class ModMenuIntegration implements ModMenuApi {

   @Override
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return ConfigResolver::resolveScreen;
   }
}
