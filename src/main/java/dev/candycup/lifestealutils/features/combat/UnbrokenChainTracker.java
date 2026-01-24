package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.Config;
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
   private static final int MAX_CHAIN = 10; // max tracked chain count
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

   /**
    * Creates a new UnbrokenChainTracker instance and prepares its runtime state.
    *
    * Ensures chain-counter configuration and format defaults exist, constructs the HUD
    * element used to display the current chain and bonus, and initializes internal state.
    */
   public UnbrokenChainTracker() {
      Config.ensureChainCounterKnown();
      Config.ensureChainCounterFormat(DEFAULT_FORMAT);

      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_counter"),
              "Unbroken Chain Counter",
              this::getDisplayText,
              HudPosition.clamp(0.5F, 0.25F)
      );

      LOGGER.info("[lsu-chain] unbroken chain tracker initialized");
   }

   /**
    * Provides the HUD element definition used to display the unbroken hit chain counter.
    *
    * @return the HudElementDefinition used to render the chain counter on the HUD
    */
   public HudElementDefinition getHudDefinition() {
      return hudDefinition;
   }

   /**
    * Determines whether the unbroken chain tracker is enabled by configuration.
    *
    * @return `true` if the chain counter feature is enabled in config, `false` otherwise.
    */
   @Override
   public boolean isEnabled() {
      return Config.isChainCounterEnabled();
   }

   /**
    * Registers a timestamped pending hit for the attack's target when the local player is
    * holding the Unbroken Chain enchantment, so the hit can be confirmed by the server later.
    *
    * @param event the client-side attack event whose target entity ID will be tracked
    */
   @Override
   public void onClientAttack(ClientAttackEvent event) {
      // only track if player has unbroken chain enchant
      net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
      if (client.player == null) return;
      if (!dev.candycup.lifestealutils.CustomEnchantUtilities.hasCustomEnchant(
              client.player.getMainHandItem(), "enchants:unbroken_chain")) {
         return;
      }
      
      long now = System.currentTimeMillis();
      pendingHits.put(event.getTargetId(), now);
      LOGGER.debug("[lsu-chain] pending hit registered for entity {}", event.getTargetId());
   }

   /**
    * Processes a damage confirmation by validating a previously recorded pending hit and advancing the unbroken hit chain.
    *
    * <p>If a pending hit for the event's entity ID exists and the confirmation arrives within {@code HIT_CONFIRMATION_TIMEOUT_MS},
    * increments the chain count (capped at {@code MAX_CHAIN}) and updates the timestamp of the last confirmed hit; otherwise
    * leaves the chain state unchanged.</p>
    *
    * @param event the damage confirmation event whose entity ID is used to resolve a pending hit
    */
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

   /**
    * Resets the unbroken hit chain when the player takes damage.
    *
    * If a chain is active, sets the chain count to zero, clears any pending hit confirmations,
    * and resets the last confirmed hit timestamp.
    */
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

   /**
    * Performs per-tick maintenance for the unbroken hit chain: resets the chain after inactivity
    * and removes pending hit entries that have expired.
    *
    * If no confirmed hit has occurred within INACTIVE_RESET_MS, this clears the chain count,
    * resets the last confirmed hit timestamp, and clears all pending hits. Independently, any
    * pending hit older than HIT_CONFIRMATION_TIMEOUT_MS is removed each tick.
    *
    * @param event the client tick event triggering this maintenance pass
    */
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

   /**
    * Resets the tracker's state when the client disconnects from the server.
    *
    * @param event the server connection change event; if {@code event.isDisconnected()} is true the tracker is reset
    */
   @Override
   public void onServerChange(ServerChangeEvent event) {
      if (event.isDisconnected()) {
         reset();
      }
   }

   /**
    * Gets the current unbroken hit chain length.
    *
    * @return the current chain count; 0 if no chain is active
    */
   public int getChainCount() {
      return chainCount;
   }

   /**
    * Calculate the current damage bonus percentage based on the unbroken hit chain.
    *
    * @return {@code 0} if the chain count is less than {@code BONUS_START_CHAIN}; otherwise the bonus percentage computed as {@code (chainCount - BONUS_START_OFFSET) * BONUS_PER_HIT}, capped at {@code MAX_CHAIN * BONUS_PER_HIT}.
    */
   public int getBonusPercent() {
      if (chainCount < BONUS_START_CHAIN) {
         return 0;
      }
      int bonusHits = chainCount - BONUS_START_OFFSET;
      return Math.min(bonusHits * BONUS_PER_HIT, MAX_CHAIN * BONUS_PER_HIT);
   }

   /**
    * Builds the HUD text for the unbroken hit chain, substituting the current count and bonus.
    *
    * <p>If the chain count is zero, returns an empty string. The format is retrieved from config and
    * falls back to {@code DEFAULT_FORMAT} when missing or blank; placeholders {@code {{count}}} and
    * {@code {{bonus}}} are replaced with the current values.
    *
    * @return an empty string if the chain count is zero, otherwise the formatted display string with the current count and bonus
    */
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
    * Reset the tracker's unbroken-hit state, intended for world or server changes.
    *
    * Clears all pending hit records, sets the chain count to zero, and resets the timestamp
    * of the last confirmed hit to zero.
    */
   public void reset() {
      chainCount = 0;
      lastConfirmedHitTimeMs = 0L;
      pendingHits.clear();
      LOGGER.debug("[lsu-chain] tracker reset");
   }
}