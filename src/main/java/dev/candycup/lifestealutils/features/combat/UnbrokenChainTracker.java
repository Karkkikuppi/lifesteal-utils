package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * tracks unbroken hit chains without receiving damage.
 * <p>
 * mechanic: each consecutive hit without taking damage grants +5% bonus damage,
 * capping at 50%. bonus only applies after 2 consecutive hits.
 * <p>
 * tracking flow:
 * 1. client swings at an entity -> record pending hit with entity id + timestamp
 * 2. server responds with damage dealt to that entity within 500ms -> increment chain
 * 3. player receives damage -> reset chain to 0
 */
public final class UnbrokenChainTracker {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/chain");

   public static final String CONFIG_ID = "unbroken_chain";
   public static final String DEFAULT_FORMAT = "<gray>Chain:</gray> <gold>{{count}}</gold> <gray>(+{{bonus}}% dmg)</gray>";
   private static final long HIT_CONFIRMATION_TIMEOUT_MS = 500;
   private static final int MAX_CHAIN = 10; // 10 hits = 50% cap
   private static final int MIN_CHAIN_FOR_BONUS = 2;
   private static final int BONUS_PER_HIT = 5;

   // pending hits awaiting server confirmation: entity id -> timestamp
   private static final Map<Integer, Long> pendingHits = new ConcurrentHashMap<>();

   // current chain count
   private static int chainCount = 0;

   private static HudElementDefinition hudDefinition;

   private UnbrokenChainTracker() {
   }

   public static void init() {
      Config.ensureChainCounterKnown();
      Config.ensureChainCounterFormat(DEFAULT_FORMAT);

      hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_counter"),
              "Unbroken Chain Counter",
              UnbrokenChainTracker::getDisplayText,
              HudPosition.clamp(0.5F, 0.25F)
      );

      LOGGER.info("[lsu-chain] unbroken chain tracker initialized");
   }

   public static HudElementDefinition hudDefinition() {
      return hudDefinition;
   }

   /**
    * called when the client player attacks/swings at an entity.
    * records the entity id and timestamp for later confirmation.
    */
   public static void onClientAttack(int entityId) {
      if (!Config.isChainCounterEnabled()) {
         return;
      }
      long now = System.currentTimeMillis();
      pendingHits.put(entityId, now);
      LOGGER.debug("[lsu-chain] pending hit registered for entity {}", entityId);
   }

   /**
    * called when the server confirms damage dealt to an entity.
    * if we have a pending hit for this entity within the timeout, increment chain.
    */
   public static void onServerDamageConfirmed(int entityId) {
      if (!Config.isChainCounterEnabled()) {
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
      LOGGER.debug("[lsu-chain] chain incremented to {}", chainCount);
   }

   /**
    * called when the local player receives damage.
    * resets the chain to 0.
    */
   public static void onPlayerDamaged() {
      if (chainCount > 0) {
         LOGGER.debug("[lsu-chain] chain reset from {} (player damaged)", chainCount);
         chainCount = 0;
      }
      // also clear any pending hits since chain is broken
      pendingHits.clear();
   }

   /**
    * called each tick to clean up stale pending hits.
    */
   public static void tick() {
      if (!Config.isChainCounterEnabled()) {
         return;
      }
      long now = System.currentTimeMillis();
      pendingHits.entrySet().removeIf(entry ->
              now - entry.getValue() > HIT_CONFIRMATION_TIMEOUT_MS
      );
   }

   public static int getChainCount() {
      return chainCount;
   }

   public static int getBonusPercent() {
      if (chainCount < MIN_CHAIN_FOR_BONUS) {
         return 0;
      }
      return Math.min(chainCount * BONUS_PER_HIT, MAX_CHAIN * BONUS_PER_HIT);
   }

   private static String getDisplayText() {
      if (!Config.isChainCounterEnabled()) {
         return "";
      }

      int count = chainCount;
      int bonus = getBonusPercent();

      // don't show if chain is 0
      if (count == 0) {
         return "";
      }

      String format = Config.getChainCounterFormat(DEFAULT_FORMAT);
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
   public static void reset() {
      chainCount = 0;
      pendingHits.clear();
      LOGGER.debug("[lsu-chain] tracker reset");
   }
}
