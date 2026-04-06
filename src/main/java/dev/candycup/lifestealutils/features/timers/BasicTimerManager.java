package dev.candycup.lifestealutils.features.timers;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.hud.HudAnchor;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import dev.candycup.lifestealutils.ui.HudElementEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class BasicTimerManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/timers");
   private static final int AUTO_HIDE_SCAN_INTERVAL_TICKS = 5;

   private final Map<String, BasicTimerDefinition> definitions = new LinkedHashMap<>();
   private final Map<String, TimerState> states = new LinkedHashMap<>();
   private final Map<String, HudElementDefinition> hudDefinitions = new LinkedHashMap<>();
   private final Set<String> watchedNbtIds = new LinkedHashSet<>();
   private final Set<String> presentNbtIds = new HashSet<>();

   private int ticksUntilNextAutoHideScan = 0;
   private boolean autoHideCacheValid = false;

   public BasicTimerManager(List<BasicTimerDefinition> definitions) {
      configure(definitions);

      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });

      LifestealUtilsEvents.CLIENT_TICK.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onClientTick(event);
      });
   }

   private void configure(List<BasicTimerDefinition> definitions) {
      this.definitions.clear();
      this.states.clear();
      this.hudDefinitions.clear();
      this.watchedNbtIds.clear();
      this.presentNbtIds.clear();
      this.ticksUntilNextAutoHideScan = 0;
      this.autoHideCacheValid = false;

      float baseY = 0.15F;
      float stepY = 0.035F;
      int index = 0;

      for (BasicTimerDefinition definition : definitions) {
         String slug = slugify(definition.name());
         String id = ensureUniqueId(slug);
         this.definitions.put(id, definition);
         this.states.put(id, new TimerState(0));
         Config.ensureBasicTimerKnown(id);

         HudElementDefinition hudDefinition = new HudElementDefinition(
                 Identifier.fromNamespaceAndPath("lifestealutils", id + "_timer"),
                 definition.name(),
                 () -> textFor(id, definition),
                 HudPosition.clamp(0.5F, baseY + (stepY * index), HudAnchor.CENTER)
         );
         this.hudDefinitions.put(id, hudDefinition);

         String nbtId = definition.nbtId();
         if (nbtId != null && !nbtId.isBlank()) {
            this.watchedNbtIds.add(nbtId);
         }

         index++;
      }

      LOGGER.info("[lsu-timers] configured {} basic timers", this.definitions.size());
   }

   public List<HudElementDefinition> getHudDefinitions() {
      return new ArrayList<>(hudDefinitions.values());
   }

   public List<TimerEntry> getTimerEntries() {
      return definitions.entrySet().stream()
              .map(e -> new TimerEntry(e.getKey(), e.getValue()))
              .collect(Collectors.toList());
   }

   public boolean isEnabled() {
      // enabled if any timer is enabled
      return definitions.keySet().stream()
              .anyMatch(Config::isBasicTimerEnabled);
   }

   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String message = event.getMessage().getString();
      if (message.isBlank()) {
         return;
      }

      for (Map.Entry<String, BasicTimerDefinition> entry : definitions.entrySet()) {
         BasicTimerDefinition definition = entry.getValue();
         if (definition.chatTrigger() != null && message.contains(definition.chatTrigger())) {
            if (!Config.isBasicTimerEnabled(entry.getKey())) {
               continue;
            }
            start(entry.getKey(), definition.durationSeconds());
            LOGGER.debug("[lsu-timers] started timer '{}' from chat trigger", definition.name());
         }
      }
   }

   public void onClientTick(ClientTickEvent event) {
      for (TimerState state : states.values()) {
         if (state.remainingTicks > 0) {
            state.remainingTicks--;
         }
      }

      refreshAutoHideCache(event.getClient());
   }

   private void start(String id, int durationSeconds) {
      TimerState state = states.get(id);
      if (state == null) {
         return;
      }
      state.remainingTicks = Math.max(durationSeconds * 20, 0);
   }

   private String textFor(String id, BasicTimerDefinition definition) {
      if (!Config.isBasicTimerEnabled(id)) return "";

      Minecraft minecraft = Minecraft.getInstance();
      boolean inEditor = minecraft.screen instanceof HudElementEditor;
      if (Config.isTimerAutoHide() && !inEditor) {
         String nbtId = definition.nbtId();
         if (nbtId != null && !nbtId.isBlank()) {
            if (!autoHideCacheValid) {
               refreshAutoHideCache(minecraft);
            }
            if (!presentNbtIds.contains(nbtId)) {
               return "";
            }
         }
      }

      TimerState state = states.get(id);
      int remainingTicks = state != null ? state.remainingTicks : 0;

      String value;
      if (remainingTicks > 0) {
         int remainingSeconds = (remainingTicks + 19) / 20;
         value = formatDuration(remainingSeconds);
      } else {
         value = definition.passiveState();
      }

      String format = Config.getBasicTimerFormat(id, definition.defaultFormat());
      if (format == null || format.isBlank()) {
         format = "{{timer}}";
      }

      if (format.contains("{{timer}}")) {
         return format.replace("{{timer}}", value);
      }

      return format + " " + value;
   }

   private void refreshAutoHideCache(Minecraft minecraft) {
      if (!Config.isTimerAutoHide()) {
         presentNbtIds.clear();
         autoHideCacheValid = false;
         ticksUntilNextAutoHideScan = 0;
         return;
      }

      if (minecraft == null || minecraft.player == null) {
         presentNbtIds.clear();
         autoHideCacheValid = false;
         ticksUntilNextAutoHideScan = 0;
         return;
      }

      if (watchedNbtIds.isEmpty()) {
         presentNbtIds.clear();
         autoHideCacheValid = true;
         return;
      }

      if (ticksUntilNextAutoHideScan > 0) {
         ticksUntilNextAutoHideScan--;
         return;
      }

      scanInventoryForWatchedNbtIds(minecraft);
      autoHideCacheValid = true;
      ticksUntilNextAutoHideScan = AUTO_HIDE_SCAN_INTERVAL_TICKS - 1;
   }

   private void scanInventoryForWatchedNbtIds(Minecraft minecraft) {
      presentNbtIds.clear();

      Inventory inventory = minecraft.player.getInventory();
      DynamicOps<Tag> ops = minecraft.player.registryAccess().createSerializationContext(NbtOps.INSTANCE);

      for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
         ItemStack stack = inventory.getItem(slot);
         if (stack.isEmpty()) continue;

         Tag tag = encodeStack(stack, ops);
         if (!(tag instanceof CompoundTag nbt)) continue;

         CompoundTag custom = nbt.getCompound("minecraft:custom_data").orElse(null);
         if (custom == null) continue;

         CompoundTag pbv = custom.getCompound("PublicBukkitValues").orElse(null);
         if (pbv == null) continue;

         for (String key : watchedNbtIds) {
            if (pbv.contains(key)) {
               presentNbtIds.add(key);
            }
         }

         if (presentNbtIds.size() >= watchedNbtIds.size()) {
            return;
         }
      }
   }

   public record TimerEntry(String id, BasicTimerDefinition definition) {
   }

   private static String formatDuration(int seconds) {
      int remaining = Math.max(seconds, 0);
      int hours = remaining / 3600;
      remaining -= hours * 3600;
      int minutes = remaining / 60;
      int secs = remaining % 60;

      StringBuilder builder = new StringBuilder();
      if (hours > 0) {
         builder.append(hours).append("h ");
      }
      if (hours > 0 || minutes > 0) {
         builder.append(minutes).append("m ");
      }
      builder.append(secs).append("s");
      return builder.toString().trim();
   }

   private String slugify(String name) {
      String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
      slug = slug.replaceAll("_+", "_");
      slug = slug.replaceAll("^_+|_+$", "");
      return slug.isBlank() ? "timer" : slug;
   }

   private String ensureUniqueId(String base) {
      String candidate = base;
      int counter = 1;
      while (definitions.containsKey(candidate)) {
         candidate = base + "_" + counter;
         counter++;
      }
      return candidate;
   }

   private static CompoundTag encodeStack(ItemStack stack, DynamicOps<Tag> ops) {
      DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ops, stack.getComponentsPatch());
      Tag nbtElement = result.getOrThrow();
      return (CompoundTag) nbtElement;
   }

   private static final class TimerState {
      int remainingTicks;

      TimerState(int remainingTicks) {
         this.remainingTicks = remainingTicks;
      }
   }

}
