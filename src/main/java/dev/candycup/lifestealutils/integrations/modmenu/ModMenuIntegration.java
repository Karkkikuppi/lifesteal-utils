package dev.candycup.lifestealutils.integrations.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.candycup.lifestealutils.config.ConfigResolver;
import dev.candycup.lifestealutils.config.configurables.ConfigurableString;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import lombok.Getter;
import lombok.Setter;

public class ModMenuIntegration implements ModMenuApi {
   @Getter
   @Setter
   @SerialEntry(comment = "Customize the format of private messages (/msg, /r)")
   @ConfigurableString(location = "meow.messages.pmformat")
   private static String meowPmFormat = "<light_purple><bold>{{direction}}</bold> {{sender}}</light_purple> <white>➡ {{message}}</white>";

   @Override
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return parent -> ConfigResolver.resolve().generateScreen(parent);
   }
}