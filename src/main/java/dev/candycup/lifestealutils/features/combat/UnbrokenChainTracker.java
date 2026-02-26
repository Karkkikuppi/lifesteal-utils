package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.CustomEnchantParsingUtilities;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientAttackEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.hud.HudAnchor;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * tracks unbroken hit chains without receiving damage.
 * <p>
 * mechanic: each consecutive hit without taking damage grants +5% bonus damage,
 * capping at 50%. bonus only applies starting with the 3rd hit.
 * the chain resets if you fail to hit anyone for 5 seconds.
 * <p>
 * tracking flow:
 * 1. client swings at an entity -> record pending hit with entity id + timestamp
 * 2. incoming damage packet confirms damage dealt to that entity within 500ms -> increment chain
 * 3. player receives damage -> reset chain to 0
 */
public final class UnbrokenChainTracker {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/chain");

   public static final String CONFIG_ID = "unbroken_chain";
   public static final String DEFAULT_FORMAT = "<gray>Chain:</gray> <gold>{{count}}</gold> <gray>(+{{bonus}}% dmg)</gray>";
   private static final long HIT_CONFIRMATION_TIMEOUT_MS = 500;
   private static final int MAX_CHAIN = 12; // max tracked chain count (allows 50% bonus)
   private static final int BONUS_START_CHAIN = 3;
   private static final int BONUS_START_OFFSET = 2;
   private static final int BONUS_PER_HIT = 5;
   private static final long INACTIVE_RESET_MS = 5_000;

   // pending hits awaiting server confirmation: entity id -> timestamp
   private final Map<Integer, Long> pendingHits = new ConcurrentHashMap<>();

   // current chain count
   @Getter
   private int chainCount = 0;
   private long lastConfirmedHitTimeMs = 0L;

   @Getter
   private final HudElementDefinition hudDefinition;

   public UnbrokenChainTracker() {
      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_counter"),
              "Unbroken Chain Counter",
              this::getDisplayText,
              HudPosition.clamp(0.5F, 0.25F, HudAnchor.CENTER)
      );

      LifestealUtilsEvents.CLIENT_ATTACK.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onClientAttack(event);
      });
      LifestealUtilsEvents.PACKET_RECEIVED.register((packet, callbackInfo) -> {
         if (!isEnabled()) {
            return;
         }
         if (!(packet instanceof ClientboundDamageEventPacket damagePacket)) {
            return;
         }
         onIncomingDamagePacket(damagePacket);
      });
      LifestealUtilsEvents.CLIENT_TICK.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onClientTick(event);
      });
      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });

      LOGGER.info("[lsu-chain] unbroken chain tracker initialized");
   }

   public boolean isEnabled() {
      return Config.isChainCounterEnabled();
   }

   public void onClientAttack(ClientAttackEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) return;
      if (!LifestealAPI.hasSpecificCustomEnchant(
              client.player.getMainHandItem(),
              "enchants:unbroken_chain")
      ) {
         return;
      }

      long now = System.currentTimeMillis();
      pendingHits.put(event.getTargetId(), now);
      LOGGER.debug("[lsu-chain] pending hit registered for entity {}", event.getTargetId());
   }

   private void onIncomingDamagePacket(ClientboundDamageEventPacket packet) {
      if (!LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return;
      }

      int entityId = packet.entityId();
      if (entityId == client.player.getId()) {
         onPlayerDamaged();
         return;
      }

      Entity entity = client.level != null ? client.level.getEntity(entityId) : null;
      if (!(entity instanceof Player)) {
         return;
      }

      Long hitTime = pendingHits.remove(entityId);
      if (hitTime == null) {
         return;
      }

      long elapsed = System.currentTimeMillis() - hitTime;
      if (elapsed > HIT_CONFIRMATION_TIMEOUT_MS) {
         LOGGER.debug("[lsu-chain] hit confirmation too slow ({}ms > {}ms)", elapsed, HIT_CONFIRMATION_TIMEOUT_MS);
         return;
      }

      chainCount = Math.min(chainCount + 1, MAX_CHAIN);
      lastConfirmedHitTimeMs = System.currentTimeMillis();
      LOGGER.debug("[lsu-chain] chain incremented to {}", chainCount);
   }

   private void onPlayerDamaged() {
      if (chainCount > 0) {
         LOGGER.debug("[lsu-chain] chain reset from {} (player damaged)", chainCount);
         chainCount = 0;
      }
      lastConfirmedHitTimeMs = 0L;
      // also clear any pending hits since chain is broken
      pendingHits.clear();
   }

   public void onClientTick(ClientTickEvent event) {
      long now = System.currentTimeMillis();
      if (chainCount > 0 && lastConfirmedHitTimeMs > 0L && now - lastConfirmedHitTimeMs > INACTIVE_RESET_MS) {
         LOGGER.debug("[lsu-chain] chain reset from {} (inactive for {}ms)", chainCount, INACTIVE_RESET_MS);
         chainCount = 0;
         lastConfirmedHitTimeMs = 0L;
         pendingHits.clear();
      }
      pendingHits.entrySet().removeIf(entry ->
              now - entry.getValue() > HIT_CONFIRMATION_TIMEOUT_MS
      );
   }

   public void onServerChange(ServerChangeEvent event) {
      if (event.isDisconnected()) {
         reset();
      }
   }

   public int getBonusPercent() {
      if (chainCount < BONUS_START_CHAIN) {
         return 0;
      }
      int bonusHits = chainCount - BONUS_START_OFFSET;
      return Math.min(bonusHits * BONUS_PER_HIT, MAX_CHAIN * BONUS_PER_HIT);
   }

   private String getDisplayText() {
      int count = chainCount;
      int bonus = getBonusPercent();

      // don't show if chain is 0
      if (count == 0) {
         return "";
      }

      String format = Config.getChainCounterFormat();
      if (format == null || format.isBlank()) {
         format = DEFAULT_FORMAT;
      }

      return format
              .replace("{{count}}", String.valueOf(count))
              .replace("{{bonus}}", String.valueOf(bonus));
   }

   /**
    * resets all state - useful for world/server changes.
    */
   public void reset() {
      chainCount = 0;
      lastConfirmedHitTimeMs = 0L;
      pendingHits.clear();
      LOGGER.debug("[lsu-chain] tracker reset");
   }
}
