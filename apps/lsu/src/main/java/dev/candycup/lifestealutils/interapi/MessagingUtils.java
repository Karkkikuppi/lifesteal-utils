package dev.candycup.lifestealutils.interapi;

import dev.candycup.lifestealutils.LifestealUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.IllegalFormatException;
import java.util.WeakHashMap;

public class MessagingUtils {
   private static final int MINI_CACHE_SIZE = 512;
   private static final String CHAT_PREFIX = "<red><bold>LSU</bold></red> <gray>|</gray> <white>";
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
   private static final MinecraftClientAudiences AUDIENCES = MinecraftClientAudiences.of();

   private static final Map<String, Component> MINI_TO_NATIVE_CACHE = Collections.synchronizedMap(
           new LinkedHashMap<String, Component>(MINI_CACHE_SIZE, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
                 return size() > MINI_CACHE_SIZE;
              }
           }
   );
   private static final Map<String, String> ESCAPE_TAG_CACHE = Collections.synchronizedMap(
           new LinkedHashMap<String, String>(MINI_CACHE_SIZE, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                 return size() > MINI_CACHE_SIZE;
              }
           }
   );
   private static final Map<Component, net.kyori.adventure.text.Component> NATIVE_TO_ADVENTURE = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<net.kyori.adventure.text.Component, String> ADVENTURE_TO_MINI = Collections.synchronizedMap(new WeakHashMap<>());

   public static void showMessage(Component message, int color) {
      try {
         GuiMessageTag tag = new GuiMessageTag(
                 color,
                 GuiMessageTag.Icon.CHAT_MODIFIED,
                 translated("lsu.chat.modified_tag"),
                 "Lifesteal Utils"
         );

         //? if >1.21.11 {
         /*Minecraft.getInstance().gui.getChat().addPlayerMessage(
            message,
            new MessageSignature(new byte[256]),
            tag
         );
         *///?} else {
         Minecraft.getInstance().gui.getChat().addMessage(
            message,
            new MessageSignature(new byte[256]),
            tag
         );
         //?}
      } catch (Exception e) {

      }
   }

   public static void showMiniMessage(String miniMessage) {
      showMiniMessage(miniMessage, 0xFFFFFF);
   }

   public static void showMiniMessage(String miniMessage, int color) {
      showMessage(
              miniMessage(miniMessage),
              color
      );
   }

   public static void showTranslated(String translationKey, MessageArgument... arguments) {
      showTranslated(translationKey, 0xe4625c, arguments);
   }

   public static void showTranslated(String translationKey, int color, MessageArgument... arguments) {
      showMiniMessage(CHAT_PREFIX + translatedMiniMessage(translationKey, arguments), color);
   }

   public static Component translated(String translationKey, MessageArgument... arguments) {
      return miniMessage(translatedMiniMessage(translationKey, arguments));
   }

   public static MessageArgument arg(Object value) {
      return arg(value, false);
   }

   public static MessageArgument arg(Object value, boolean sanitize) {
      return new MessageArgument(value == null ? "" : String.valueOf(value), sanitize);
   }

   private static String translatedMiniMessage(String translationKey, MessageArgument... arguments) {
      String template = Language.getInstance().getOrDefault(translationKey);
      if (template == null || template.equals(translationKey)) {
         template = translationKey;
      }
      if (arguments == null || arguments.length == 0) {
         return template;
      }
      Object[] values = new Object[arguments.length];
      for (int i = 0; i < arguments.length; i++) {
         MessageArgument argument = arguments[i];
         if (argument == null) {
            values[i] = "";
            continue;
         }
         values[i] = argument.sanitize() ? escapeMiniMessageTags(argument.value()) : argument.value();
      }
      try {
         return String.format(Locale.ROOT, template, values);
      } catch (IllegalFormatException ignored) {
         return template;
      }
   }

   public static Component miniMessage(String miniMessage) {
      if (miniMessage == null) {
         return Component.empty();
      }
      Component cached = MINI_TO_NATIVE_CACHE.get(miniMessage);
      if (cached != null) {
         return cached;
      }
      Component parsed = AUDIENCES.asNative(MINI_MESSAGE.deserialize(miniMessage));
      MINI_TO_NATIVE_CACHE.put(miniMessage, parsed);
      return parsed;
   }

   public static Component miniMessage(net.kyori.adventure.text.Component component) {
      return AUDIENCES.asNative(component);
   }

   public static net.kyori.adventure.text.Component asMiniMessage(Component component) {
      if (component == null) {
         return net.kyori.adventure.text.Component.empty();
      }
      net.kyori.adventure.text.Component cached = NATIVE_TO_ADVENTURE.get(component);
      if (cached != null) {
         return cached;
      }
      net.kyori.adventure.text.Component converted = AUDIENCES.asAdventure(component);
      NATIVE_TO_ADVENTURE.put(component, converted);
      return converted;
   }

   public static String serializeMiniMessage(Component component) {
      net.kyori.adventure.text.Component adventure = asMiniMessage(component);
      String cached = ADVENTURE_TO_MINI.get(adventure);
      if (cached != null) {
         return cached;
      }
      String serialized = MINI_MESSAGE.serialize(adventure);
      ADVENTURE_TO_MINI.put(adventure, serialized);
      return serialized;
   }

   public static String escapeMiniMessageTags(String raw) {
      if (raw == null || raw.isEmpty()) {
         return "";
      }
      String cached = ESCAPE_TAG_CACHE.get(raw);
      if (cached != null) {
         return cached;
      }
      String escaped = MINI_MESSAGE.escapeTags(raw);
      ESCAPE_TAG_CACHE.put(raw, escaped);
      return escaped;
   }

   /**
    * as support for components in the splash screen was added in 1.21.11,
    * older versions need a safe string version of minimessage (which removes formatting)
    *
    * @param miniMessage the minimessage formatted string
    * @return the safe string for the current version
    */
   public static String miniMessageToSplashSafe(String miniMessage) {
      //? if > 1.21.10 {
      return miniMessage;
      //? } else {
      /*return miniMessage(miniMessage).getString();
       *///? }
   }

   public record MessageArgument(String value, boolean sanitize) {
   }
}

