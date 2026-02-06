package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.CustomEnchantUtilities;
import dev.candycup.lifestealutils.event.events.ClientAttackEvent;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.event.events.DamageConfirmedEvent;
import dev.candycup.lifestealutils.event.events.PlayerDamagedEvent;
import dev.candycup.lifestealutils.event.events.ServerChangeEvent;
import dev.candycup.lifestealutils.event.listener.CombatEventListener;
import dev.candycup.lifestealutils.event.listener.ServerEventListener;
import dev.candycup.lifestealutils.event.listener.TickEventListener;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
 * 2. server responds with damage dealt to that entity within 500ms -> increment chain
 * 3. player receives damage -> reset chain to 0
 */
public final class UnbrokenChainTracker implements CombatEventListener, TickEventListener, ServerEventListener {
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
   private int chainCount = 0;
   private long lastConfirmedHitTimeMs = 0L;

   private final HudElementDefinition hudDefinition;

   public UnbrokenChainTracker() {
      Config.ensureChainCounterFormat(DEFAULT_FORMAT);

      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_counter"),
              "Unbroken Chain Counter",
              this::getDisplayText,
              HudPosition.clamp(0.5F, 0.25F)
      );

      LOGGER.info("[lsu-chain] unbroken chain tracker initialized");
   }

   public HudElementDefinition getHudDefinition() {
      return hudDefinition;
   }

   @Override
   public boolean isEnabled() {
      return Config.isChainCounterEnabled();
   }

   @Override
   public void onClientAttack(ClientAttackEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) return;
      if (!CustomEnchantUtilities.hasCustomEnchant(
              client.player.getMainHandItem(), "enchants:unbroken_chain")) {
         return;
      }

      long now = System.currentTimeMillis();
      pendingHits.put(event.getTargetId(), now);
      LOGGER.debug("[lsu-chain] pending hit registered for entity {}", event.getTargetId());
   }

   @Override
   public void onDamageConfirmed(DamageConfirmedEvent event) {
      Long hitTime = pendingHits.remove(event.getEntityId());
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

   @Override
   public void onPlayerDamaged(PlayerDamagedEvent event) {
      if (chainCount > 0) {
         LOGGER.debug("[lsu-chain] chain reset from {} (player damaged)", chainCount);
         chainCount = 0;
      }
      lastConfirmedHitTimeMs = 0L;
      // also clear any pending hits since chain is broken
      pendingHits.clear();
   }

   @Override
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

   @Override
   public void onServerChange(ServerChangeEvent event) {
      if (event.isDisconnected()) {
         reset();
      }
   }

   public int getChainCount() {
      return chainCount;
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
   public void reset() {
      chainCount = 0;
      lastConfirmedHitTimeMs = 0L;
      pendingHits.clear();
      LOGGER.debug("[lsu-chain] tracker reset");
   }
}
