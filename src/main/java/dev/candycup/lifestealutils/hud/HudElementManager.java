package dev.candycup.lifestealutils.hud;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudElementManager {
   private static final Map<Identifier, HudElementDefinition> DEFINITIONS = new LinkedHashMap<>();
   private static final Map<Identifier, HudPosition> POSITIONS = new LinkedHashMap<>();

   private HudElementManager() {
   }

   public static void init() {
      POSITIONS.clear();
      POSITIONS.putAll(HudLayoutStorage.load());
   }

   public static void register(HudElementDefinition definition) {
      DEFINITIONS.put(definition.id(), definition);
      POSITIONS.putIfAbsent(definition.id(), definition.defaultPosition());
   }

   public static Collection<HudElementDefinition> definitions() {
      return DEFINITIONS.values();
   }

   public static HudPosition positionFor(Identifier id) {
      HudPosition position = POSITIONS.get(id);
      if (position != null) {
         return position;
      }
      HudElementDefinition definition = DEFINITIONS.get(id);
      if (definition != null) {
         position = definition.defaultPosition();
         POSITIONS.put(id, position);
         return position;
      }
      return HudPosition.clamp(0F, 0F);
   }

   /**
    * Checks if a position is registered for the given element id.
    *
    * @param id the element identifier
    * @return true if a position exists, false otherwise
    */
   public static boolean hasPosition(Identifier id) {
      return POSITIONS.containsKey(id);
   }

   /**
    * Registers a position for an element that doesn't have a full definition.
    * This is used for standalone elements like the POI indicator.
    *
    * @param id       the element identifier
    * @param position the position to register
    */
   public static void registerPosition(Identifier id, HudPosition position) {
      POSITIONS.putIfAbsent(id, position);
   }

   public static void updatePositionFromPixels(Identifier id, float pixelX, float pixelY, int guiWidth, int guiHeight, int textWidth, int textHeight) {
      float availableWidth = Math.max(guiWidth - textWidth, 1);
      float availableHeight = Math.max(guiHeight - textHeight, 1);
      float normalizedX = pixelX / availableWidth;
      float normalizedY = pixelY / availableHeight;
      POSITIONS.put(id, HudPosition.clamp(normalizedX, normalizedY));
   }

   public static void saveLayout() {
      HudLayoutStorage.save(POSITIONS);
   }

   public static List<RenderedHudElement> renderables(Font font, int guiWidth, int guiHeight) {
      List<RenderedHudElement> renderables = new ArrayList<>();
      for (HudElementDefinition definition : DEFINITIONS.values()) {
         renderables.add(renderable(definition, font, guiWidth, guiHeight));
      }
      return renderables;
   }

   public static RenderedHudElement renderable(HudElementDefinition definition, Font font, int guiWidth, int guiHeight) {
      Component component = MessagingUtils.miniMessage(definition.miniMessageSupplier().get());
      int textWidth = font.width(component);
      int textHeight = font.lineHeight;
      HudPosition position = positionFor(definition.id());
      int x = pixelCoordinate(position.x(), guiWidth, textWidth);
      int y = pixelCoordinate(position.y(), guiHeight, textHeight);
      return new RenderedHudElement(definition, component, x, y, textWidth, textHeight);
   }

   private static int pixelCoordinate(float normalized, int guiSize, int elementSize) {
      int available = Math.max(guiSize - elementSize, 0);
      float clamped = Mth.clamp(normalized, 0F, 1F);
      return Mth.floor(clamped * available);
   }

   public record RenderedHudElement(
           HudElementDefinition definition,
           Component component,
           int x,
           int y,
           int textWidth,
           int textHeight
   ) {
   }
}
