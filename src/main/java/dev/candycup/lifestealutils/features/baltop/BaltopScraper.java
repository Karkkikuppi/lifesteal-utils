package dev.candycup.lifestealutils.features.baltop;

import com.mojang.authlib.properties.PropertyMap;
import dev.candycup.lifestealutils.ui.BaltopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes the server's /baltop GUI to extract player balance data.
 * Reads directly from the player's container menu instead of intercepting packets.
 */
public class BaltopScraper {
   private static final Logger LOGGER = LoggerFactory.getLogger("LifestealUtils/BaltopScraper");

   private static final Pattern NAME_PATTERN = Pattern.compile("#(\\d+)\\s+(.+)");
   private static final Pattern BALANCE_PATTERN = Pattern.compile("^.\\s*([\\d,]+(?:\\.\\d+)?)\\s*Coins?$");
   private static final String NEXT_PAGE_ITEM_NAME = "Next Page";
   private static final int MAX_PAGES = 50;

   /**
    * ticks to wait after clicking before reading new content
    */
   private static final int WAIT_AFTER_CLICK_TICKS = 10;
   /**
    * ticks to wait for content to change after click
    */
   private static final int MAX_WAIT_FOR_CHANGE_TICKS = 100; // 5 seconds

   private static BaltopScraper instance;

   private enum State {
      IDLE,
      WAITING_FOR_GUI,
      READING_PAGE,
      WAITING_AFTER_CLICK,
      FINISHED
   }

   private State state = State.IDLE;
   private int ticksWaited = 0;
   private int pagesScraped = 0;
   private final List<BaltopEntry> scrapedEntries = new ArrayList<>();
   private Consumer<String> errorCallback;
   private Screen screenToRestore;
   private BaltopScreen activeScreen;

   /**
    * hash of the last processed container contents to detect changes
    */
   private int lastContentHash = 0;

   private BaltopScraper() {
   }

   public static BaltopScraper getInstance() {
      if (instance == null) {
         instance = new BaltopScraper();
      }
      return instance;
   }

   public boolean isScraping() {
      return state != State.IDLE && state != State.FINISHED;
   }

   public List<BaltopEntry> getScrapedEntries() {
      return Collections.unmodifiableList(scrapedEntries);
   }

   public boolean isLoading() {
      return isScraping();
   }

   /**
    * Returns the active BaltopScreen being used during scraping.
    */
   public BaltopScreen getActiveScreen() {
      return activeScreen;
   }

   /**
    * Starts the baltop scraping process.
    * Opens the BaltopScreen immediately so the user sees our loading UI.
    */
   public void startScraping(Screen currentScreen, Consumer<String> onError) {
      if (isScraping()) {
         LOGGER.warn("Already scraping baltop, ignoring duplicate request");
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.getConnection() == null) {
         onError.accept("Not connected to a server");
         return;
      }

      LOGGER.info("Starting baltop scrape");

      state = State.WAITING_FOR_GUI;
      ticksWaited = 0;
      pagesScraped = 0;
      scrapedEntries.clear();
      errorCallback = onError;
      screenToRestore = currentScreen;
      lastContentHash = 0;

      // open our BaltopScreen immediately to show loading state
      activeScreen = new BaltopScreen(screenToRestore, this);
      client.setScreen(activeScreen);

      // send the /baltop command (the server will open a container)
      client.player.connection.sendCommand("baltop");
   }

   /**
    * Cancels the current scraping operation.
    */
   public void cancelScraping(String reason) {
      if (state == State.IDLE) return;

      LOGGER.warn("Scraping cancelled: {}", reason);

      state = State.IDLE;

      if (errorCallback != null) {
         errorCallback.accept(reason);
      }

      // close container
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         client.player.closeContainer();
      }

      // notify the active screen of failure
      if (activeScreen != null) {
         activeScreen.onLoadingFailed(reason);
      }

      cleanup();
   }

   /**
    * Called every client tick. This is where all the state machine logic lives.
    */
   public void tick() {
      if (state == State.IDLE || state == State.FINISHED) return;

      Minecraft client = Minecraft.getInstance();
      ticksWaited++;

      switch (state) {
         case WAITING_FOR_GUI -> handleWaitingForGui(client);
         case READING_PAGE -> handleReadingPage(client);
         case WAITING_AFTER_CLICK -> handleWaitingAfterClick(client);
         default -> {
         }
      }
   }

   private void handleWaitingForGui(Minecraft client) {
      // check if a container menu is available (may be opened invisibly)
      if (client.player != null && client.player.containerMenu != null
              && client.player.containerMenu != client.player.inventoryMenu) {
         // container is open, start reading
         LOGGER.info("Container menu opened with {} slots", client.player.containerMenu.slots.size());
         state = State.READING_PAGE;
         ticksWaited = 0;
         return;
      }

      // timeout check
      if (ticksWaited > MAX_WAIT_FOR_CHANGE_TICKS) {
         cancelScraping("Timed out waiting for baltop GUI to open");
      }
   }

   private void handleReadingPage(Minecraft client) {
      AbstractContainerMenu menu = client.player != null ? client.player.containerMenu : null;
      if (menu == null) {
         cancelScraping("Container closed unexpectedly");
         return;
      }

      // read items directly from the container menu slots
      int nextPageSlot = -1;
      int newEntries = 0;

      LOGGER.info("Reading container with {} slots", menu.slots.size());

      for (int i = 0; i < menu.slots.size(); i++) {
         Slot slot = menu.slots.get(i);
         ItemStack stack = slot.getItem();
         if (stack.isEmpty()) continue;

         String itemName = stack.getHoverName().getString();

         // check for next page button
         if (itemName.equals(NEXT_PAGE_ITEM_NAME)) {
            nextPageSlot = i;
            LOGGER.info("Found 'Next Page' button at slot {}", i);
            continue;
         }

         // check if it's a player head
         if (stack.is(Items.PLAYER_HEAD)) {
            BaltopEntry entry = parsePlayerHead(stack);
            if (entry != null) {
               // check for duplicate by position
               boolean isDuplicate = scrapedEntries.stream()
                       .anyMatch(e -> e.position() == entry.position());
               if (!isDuplicate) {
                  scrapedEntries.add(entry);
                  newEntries++;
               }
            }
         }
      }

      pagesScraped++;
      LOGGER.info("Scraped page {}: {} new entries, {} total, nextPageSlot={}",
              pagesScraped, newEntries, scrapedEntries.size(), nextPageSlot);

      // refresh the screen with new entries
      if (activeScreen != null) {
         activeScreen.refreshEntries();
      }

      // compute content hash for change detection
      lastContentHash = computeContentHash(menu);

      // check if we should continue to next page
      if (nextPageSlot >= 0 && pagesScraped < MAX_PAGES) {
         LOGGER.info("Clicking Next Page button at slot {}", nextPageSlot);
         clickSlot(client, menu, nextPageSlot);
         state = State.WAITING_AFTER_CLICK;
         ticksWaited = 0;
      } else {
         if (nextPageSlot < 0) {
            LOGGER.info("No 'Next Page' button found - this is the last page");
         } else {
            LOGGER.info("Reached max page limit ({})", MAX_PAGES);
         }
         finishScraping();
      }
   }

   private void handleWaitingAfterClick(Minecraft client) {
      // wait a minimum number of ticks
      if (ticksWaited < WAIT_AFTER_CLICK_TICKS) {
         return;
      }

      AbstractContainerMenu menu = client.player != null ? client.player.containerMenu : null;
      if (menu == null) {
         cancelScraping("Container closed unexpectedly");
         return;
      }

      // check if content has changed
      int currentHash = computeContentHash(menu);
      if (currentHash != lastContentHash) {
         LOGGER.info("Container content changed (hash {} -> {}), reading new page", lastContentHash, currentHash);
         state = State.READING_PAGE;
         ticksWaited = 0;
         return;
      }

      // timeout check
      if (ticksWaited > MAX_WAIT_FOR_CHANGE_TICKS) {
         cancelScraping("Timed out waiting for page to change after clicking");
      }
   }

   /**
    * Computes a hash of the container contents for change detection.
    */
   private int computeContentHash(AbstractContainerMenu menu) {
      int hash = 0;
      for (int i = 0; i < Math.min(54, menu.slots.size()); i++) {
         ItemStack stack = menu.slots.get(i).getItem();
         if (!stack.isEmpty()) {
            hash = 31 * hash + stack.getHoverName().getString().hashCode();
            hash = 31 * hash + stack.getCount();
         }
      }
      return hash;
   }

   /**
    * Clicks a slot using vanilla's click handling.
    */
   private void clickSlot(Minecraft client, AbstractContainerMenu menu, int slotIndex) {
      if (client.gameMode == null || client.player == null) {
         cancelScraping("Lost connection");
         return;
      }

      LOGGER.info("Clicking slot {} (containerId={})", slotIndex, menu.containerId);

      client.gameMode.handleInventoryMouseClick(
              menu.containerId,
              slotIndex,
              0, // left click
              ClickType.PICKUP,
              client.player
      );
   }

   /**
    * Parses a player head ItemStack to extract baltop entry data.
    */
   private BaltopEntry parsePlayerHead(ItemStack stack) {
      try {
         String name = stack.getHoverName().getString();

         Matcher nameMatcher = NAME_PATTERN.matcher(name);
         if (!nameMatcher.matches()) {
            return null;
         }

         int position = Integer.parseInt(nameMatcher.group(1));
         String username = nameMatcher.group(2);

         ItemLore lore = stack.get(DataComponents.LORE);
         if (lore == null || lore.lines().isEmpty()) {
            return null;
         }

         String loreLine = lore.lines().get(0).getString();
         Matcher balanceMatcher = BALANCE_PATTERN.matcher(loreLine);
         if (!balanceMatcher.matches()) {
            return null;
         }

         // remove any decimal portion from the balance
         String rawBalance = balanceMatcher.group(1);
         if (rawBalance.contains(".")) {
            rawBalance = rawBalance.substring(0, rawBalance.indexOf('.'));
         }
         String balance = "$" + rawBalance;

         ResolvableProfile profile = stack.get(DataComponents.PROFILE);
         if (profile == null) {
            // create a minimal profile if none was found
            //? if >1.21.8 {
            profile = ResolvableProfile.createUnresolved(username);
            //?} else {
            /*profile = new ResolvableProfile(Optional.of(username), Optional.empty(), new PropertyMap());
             *///?}
            LOGGER.warn("Could not extract profile for player {} - using fallback profile", username);
         }

         return new BaltopEntry(position, username, balance, profile);

      } catch (Exception e) {
         LOGGER.error("Failed to parse player head", e);
         return null;
      }
   }

   private void finishScraping() {
      LOGGER.info("Finished scraping baltop: {} entries across {} pages",
              scrapedEntries.size(), pagesScraped);

      state = State.FINISHED;

      Minecraft client = Minecraft.getInstance();
      BaltopScreen screen = activeScreen;

      // close the server container (this may trigger screen close via server packet)
      if (client.player != null) {
         client.player.closeContainer();
      }

      // ensure our screen stays open after container close and notify loading complete
      if (screen != null) {
         client.execute(() -> {
            client.setScreen(screen);
            screen.onLoadingComplete();
         });
      }

      errorCallback = null;
   }

   private void cleanup() {
      errorCallback = null;
      screenToRestore = null;
      activeScreen = null;
      lastContentHash = 0;
   }

   public record BaltopEntry(int position, String username, String balance, ResolvableProfile profile) {
   }
}
