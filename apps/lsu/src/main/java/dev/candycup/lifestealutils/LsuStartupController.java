package dev.candycup.lifestealutils;

import dev.candycup.lifestealutils.api.PersistentKnowledgeController;
import dev.candycup.lifestealutils.config.ConfigContainerRegistry;
import dev.candycup.lifestealutils.config.ConfigDescriptorRegistry;
import dev.candycup.lifestealutils.gaia.GaiaConsentController;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.persistence.MigrationController;
import dev.candycup.lifestealutils.persistence.PersistenceVersionStamp;
import dev.candycup.lifestealutils.persistence.PersistentDiskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates LSU's two-phase startup.
 */
public final class LsuStartupController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/startup");

   /** Guard against re-running the data phase. */
   private static final AtomicBoolean dataPhaseRan = new AtomicBoolean(false);

   private LsuStartupController() {
   }

   /**
    * Runs the always-on bootstrap phase and then runs data bootstrap.
    * <p>
    * Called once from {@link LifestealUtils#onInitializeClient()}.
    */
   public static void runBootstrap() {
      LOGGER.info("LSU bootstrap phase starting");

      // Configura init - the configuration library is independent of the
      // per-account file layout and always runs first so feature flags
      // and stored prefs are available everywhere downstream.
      ConfigContainerRegistry.initializeGeneratedIndex();
      ConfigDescriptorRegistry.registerDefaultProviders();
      ConfigUtils.load();

      // Sanity-check incompatible mods.
      FeatureFlagController.assertNoIncompatibleModsDetected();

      // Fetch the Gaia consent notice text
      GaiaConsentController.initialize();

      // HUD framework + everything that doesn't touch user-segmented disk.
      HudElementManager.init();
      LifestealUtils.registerListeners();
      LifestealUtils.registerStatelessFeatures();
      LifestealUtils.registerHudElements();
      LifestealUtils.registerKeybinds();
      LifestealUtils.registerCommands();

      // Legacy-alliance migration is now automatic and silent. It only runs
      // for root alliances when Gaia is disabled and unpublished entries exist.
      MigrationController.runLegacyAllianceMigrationIfNeeded();

      PersistentDiskManager.markUserStorageReady();
      writeFreshInstallStampIfNeeded();
      runDataBootstrap();
   }

   /**
    * Runs the data phase: persistent-knowledge, alliances, Gaia, and
    * everything else that touches {@code lifestealutils/{uuid}/}.
    * Idempotent.
    */
   public static void runDataBootstrap() {
      if (!dataPhaseRan.compareAndSet(false, true)) {
         return;
      }
      if (!PersistentDiskManager.isUserStorageReady()) {
         LOGGER.error("runDataBootstrap called before user storage is ready; aborting");
         dataPhaseRan.set(false);
         return;
      }

      LOGGER.info("LSU data phase starting");

      // Loads knowledge.json
      PersistentKnowledgeController.initialize();

      // Constructs alliance services, gateway client, alliance name
      // decorator, prestige listener, etc. - every feature that does
      // disk IO or wires onto the gateway client lives here.
      LifestealUtils.registerDataFeatures();

      // Finally try Gaia authentication if the user has consented. This
      // touches lifestealutils/<uuid>/gaia/authentication/, so it must
      // run after the migration gate.
      LifestealUtils.initializeGaiaIfAuthorized();
   }

   /**
    * For fresh installs (no legacy data and no prior stamp), write the
    * initial version stamp so future schema-migration logic has a
    * baseline to reason about. No-op if the stamp already exists at the
    * latest version.
    */
   private static void writeFreshInstallStampIfNeeded() {
      Path userDir = PersistentDiskManager.getCurrentUserDir();
      int existing = PersistenceVersionStamp.getCurrentVersion(userDir);
      if (existing >= PersistenceVersionStamp.LATEST_VERSION) {
         return;
      }
      PersistenceVersionStamp.appendMigration(
              userDir,
              PersistenceVersionStamp.LATEST_VERSION,
              existing == 0
                      ? "fresh install: initial per-account stamp"
                      : "upgrade stamp from version " + existing
      );
   }
}
