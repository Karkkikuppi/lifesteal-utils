package dev.candycup.lifestealutils.features.alliances;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import dev.candycup.lifestealutils.gaia.modules.alliances.AlliancesModule;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;

public final class AllianceCommandController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliance-command");
   private static String lastProvidedListNameThisSession = "";

   private AllianceCommandController() {
   }

   public static CompletableFuture<Suggestions> suggestAllianceNames(String remaining, SuggestionsBuilder builder) {
      String needle = remaining == null ? "" : remaining.trim().toLowerCase();
      for (AllianceModels.AllianceRecord alliance : AllianceService.listAll()) {
         String name = alliance.data == null ? "" : alliance.data.name;
         if (name == null || name.isBlank()) {
            continue;
         }
         if (needle.isBlank() || name.toLowerCase().contains(needle)) {
            builder.suggest(name);
         }
      }
      return builder.buildFuture();
   }

   public static CompletableFuture<Suggestions> suggestListNames(String allianceName, String remaining, SuggestionsBuilder builder) {
      AllianceModels.AllianceRecord alliance = AllianceService.findByName(allianceName);
      if (alliance == null || alliance.data == null || alliance.data.lists == null) {
         return builder.buildFuture();
      }
      String needle = remaining == null ? "" : remaining.trim().toLowerCase();
      for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
         if (list.name == null || list.name.isBlank()) {
            continue;
         }
         if (needle.isBlank() || list.name.toLowerCase().contains(needle)) {
            builder.suggest(list.name);
         }
      }
      return builder.buildFuture();
   }

   public static CompletableFuture<Suggestions> suggestAllianceAndListTargets(String remaining, SuggestionsBuilder builder) {
      String needle = remaining == null ? "" : remaining.trim().toLowerCase(Locale.ROOT);
      for (AllianceModels.AllianceRecord alliance : AllianceService.listAll()) {
         if (alliance == null || alliance.data == null || alliance.data.name == null || alliance.data.name.isBlank()) {
            continue;
         }
         String allianceName = alliance.data.name.trim();
         if (alliance.data.lists == null) {
            continue;
         }
         for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            if (list == null || list.name == null || list.name.isBlank()) {
               continue;
            }
            String combined = allianceName + "/" + list.name.trim();
            if (needle.isBlank() || combined.toLowerCase(Locale.ROOT).contains(needle)) {
               builder.suggest(combined);
            }
         }
      }
      return builder.buildFuture();
   }

   public static CompletableFuture<Suggestions> suggestOnlinePlayers(String remaining, SuggestionsBuilder builder) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null || minecraft.getConnection() == null) {
         return builder.buildFuture();
      }
      String needle = remaining == null ? "" : remaining.trim().toLowerCase(Locale.ROOT);
      for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
         if (playerInfo == null) {
            continue;
         }
         String name;
         //? if >1.21.8 {
         name = playerInfo.getProfile().name();
         //?} else {
         /*name = playerInfo.getProfile().getName();
          *///?}
         if (name == null || name.isBlank()) {
            continue;
         }
         if (needle.isBlank() || name.toLowerCase(Locale.ROOT).contains(needle)) {
            builder.suggest(name);
         }
      }
      return builder.buildFuture();
   }

   public static int createAlliance(String name) {
      if (name == null || name.isBlank()) {
         MessagingUtils.showMiniMessage("<red>Alliance name cannot be empty.</red>");
         return 0;
      }
      String trimmed = name.trim();
      if (trimmed.length() > 32) {
         MessagingUtils.showMiniMessage("<red>Alliance name must be 32 characters or fewer.</red>");
         return 0;
      }
      AllianceModels.AllianceRecord existing = AllianceService.findByName(trimmed);
      if (existing != null) {
         MessagingUtils.showMiniMessage("<red>An alliance named <white>" + escape(trimmed) + "</white> already exists.</red>");
         return 0;
      }
      AllianceModels.AllianceRecord record = AllianceService.createLocal(trimmed);
      AllianceSyncManager.publishOrUpdateAsync(record);
      MessagingUtils.showMiniMessage("<green>Created alliance <white>" + escape(record.data.name) + "</white>.</green>");
      return 1;
   }

   public static int addMemberToAlliance(String usernameOrUuid, String allianceName, String listNameOrNull) {
      AllianceModels.AllianceRecord alliance = AllianceService.findByName(allianceName);
      if (alliance == null) {
         MessagingUtils.showMiniMessage("<red>No alliance matches <white>" + escape(allianceName) + "</white>.</red>");
         return 0;
      }
      if (!alliance.canEdit) {
         MessagingUtils.showMiniMessage("<red>You cannot add users to alliances you don't control! Create your own alliance if you wish to execute this command.</red>");
         return 0;
      }

      String listToUse = listNameOrNull;
      if (listToUse != null && !listToUse.isBlank()) {
         lastProvidedListNameThisSession = listToUse;
      } else {
         listToUse = lastProvidedListNameThisSession;
      }

      AllianceModels.AlliancePlayerList list = AllianceService.resolveList(alliance, listToUse);
      if (list == null) {
         MessagingUtils.showMiniMessage("<red>No previous list selected this session and no list provided.</red>");
         return 0;
      }

      String uuid = AllianceProfileCacheManager.resolveUuidFromInput(usernameOrUuid);
      if (uuid == null) {
         MessagingUtils.showMiniMessage("<red>Unable to resolve player <white>" + escape(usernameOrUuid) + "</white>.</red>");
         return 0;
      }

      boolean added = AllianceService.addMember(alliance, list.id, uuid);
      if (!added) {
         MessagingUtils.showMiniMessage("<yellow><white>" + escape(usernameOrUuid) + "</white> is already in <white>" + escape(alliance.data.name) + "</white>.</yellow>");
         return 0;
      }

      AllianceProfileCacheManager.cache(usernameOrUuid, uuid);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      MessagingUtils.showMiniMessage("<green>Added <white>" + escape(usernameOrUuid) + "</white> to <white>" + escape(alliance.data.name) + "</white>.</green>");
      return 1;
   }

   public static int addMemberToAllianceParsed(String usernameOrUuid, String allianceAndMaybeList) {
      ParsedAddTarget parsed = parseAddTarget(allianceAndMaybeList);
      if (parsed == null) {
         MessagingUtils.showMiniMessage("<red>Could not resolve alliance. Use <white>/lsu alliances list</white> and try again.</red>");
         return 0;
      }
      return addMemberToAlliance(usernameOrUuid, parsed.allianceName(), parsed.listName());
   }

   public static int removeMemberFromAlliance(String usernameOrUuid, String allianceName) {
      AllianceModels.AllianceRecord alliance = AllianceService.findByName(allianceName);
      if (alliance == null) {
         MessagingUtils.showMiniMessage("<red>No alliance matches <white>" + escape(allianceName) + "</white>.</red>");
         return 0;
      }
      if (!alliance.canEdit) {
         MessagingUtils.showMiniMessage("<red>You cannot remove users from alliances you don't control!</red>");
         return 0;
      }

      String uuid = AllianceProfileCacheManager.resolveUuidFromInput(usernameOrUuid);
      if (uuid == null) {
         MessagingUtils.showMiniMessage("<red>Unable to resolve player <white>" + escape(usernameOrUuid) + "</white>.</red>");
         return 0;
      }

      boolean removed = AllianceService.removeMember(alliance, uuid);
      if (!removed) {
         MessagingUtils.showMiniMessage("<red>Couldn't remove <white>" + escape(usernameOrUuid) + "</white> from <white>" + escape(alliance.data.name) + "</white>.</red>");
         return 0;
      }

      AllianceSyncManager.publishOrUpdateAsync(alliance);
      MessagingUtils.showMiniMessage("<green>Removed <white>" + escape(usernameOrUuid) + "</white> from <white>" + escape(alliance.data.name) + "</white>.</green>");
      return 1;
   }

   public static int removeMemberFromAllianceParsed(String usernameOrUuid, String allianceAndMaybeList) {
      ParsedAddTarget parsed = parseAddTarget(allianceAndMaybeList);
      if (parsed == null) {
         MessagingUtils.showMiniMessage("<red>Could not resolve alliance. Use <white>/lsu alliances list</white> and try again.</red>");
         return 0;
      }
      return removeMemberFromAlliance(usernameOrUuid, parsed.allianceName());
   }

   public static CompletableFuture<Suggestions> suggestSubscribedAllianceNames(String remaining, SuggestionsBuilder builder) {
      String needle = remaining == null ? "" : remaining.trim().toLowerCase(Locale.ROOT);
      for (AllianceModels.AllianceRecord alliance : AllianceService.listAll()) {
         if (!"remote".equals(alliance.source)) {
            continue;
         }
         String name = alliance.data == null ? "" : alliance.data.name;
         if (name == null || name.isBlank()) {
            continue;
         }
         if (needle.isBlank() || name.toLowerCase(Locale.ROOT).contains(needle)) {
            builder.suggest(name);
         }
      }
      return builder.buildFuture();
   }

   public static int subscribeToAlliance(String id) {
      if (id == null || id.isBlank()) {
         MessagingUtils.showMiniMessage("<red>Enter an invite code first.</red>");
         return 0;
      }
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         MessagingUtils.showMiniMessage("<red>Gaia is disabled. Run /lsu consent-gaia to enable.</red>");
         return 0;
      }
      AlliancesModule.SubscriptionResult result;
      try {
         result = GaiaApiClient.getInstance().alliances().subscribeWithDetails(id.trim());
      } catch (Exception e) {
         LOGGER.error("Failed to subscribe to alliance '{}'", id, e);
         MessagingUtils.showMiniMessage("<red>Failed to contact alliance service.</red>");
         return 0;
      }
      if (!result.success()) {
         String msg = result.errorMessage() != null ? escape(result.errorMessage()) : "Subscribe failed. Please try again.";
         MessagingUtils.showMiniMessage("<red>" + msg + "</red>");
         return 0;
      }
      AllianceSyncManager.syncSubscriptionsNow();
      AllianceService.reloadFromDisk();
      MessagingUtils.showMiniMessage("<green>Subscribed to alliance.</green>");
      return 1;
   }

   public static int unsubscribeFromAlliance(String nameOrId) {
      if (nameOrId == null || nameOrId.isBlank()) {
         MessagingUtils.showMiniMessage("<red>Provide an alliance name or ID.</red>");
         return 0;
      }
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         MessagingUtils.showMiniMessage("<red>Gaia is disabled. Run /lsu consent-gaia to enable.</red>");
         return 0;
      }
      String trimmed = nameOrId.trim();
      AllianceModels.AllianceRecord alliance = AllianceService.findByName(trimmed);
      String serverId = alliance != null ? alliance.serverId : trimmed;
      if (serverId == null || serverId.isBlank()) {
         MessagingUtils.showMiniMessage("<red>Could not resolve server ID for <white>" + escape(trimmed) + "</white>.</red>");
         return 0;
      }
      boolean success;
      try {
         success = GaiaApiClient.getInstance().alliances().unsubscribe(serverId);
      } catch (Exception e) {
         LOGGER.error("Failed to unsubscribe from alliance '{}'", nameOrId, e);
         MessagingUtils.showMiniMessage("<red>Failed to contact alliance service.</red>");
         return 0;
      }
      if (!success) {
         MessagingUtils.showMiniMessage("<red>Unsubscribe failed. Please try again.</red>");
         return 0;
      }
      AllianceSyncManager.syncSubscriptionsNow();
      AllianceService.reloadFromDisk();
      MessagingUtils.showMiniMessage("<green>Unsubscribed from alliance.</green>");
      return 1;
   }

   private static String escape(String input) {
      if (input == null) {
         return "";
      }
      return input.replace("<", "").replace(">", "");
   }

   private static ParsedAddTarget parseAddTarget(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }
      String input = raw.trim();

      int slashIndex = input.indexOf('/');
      if (slashIndex > 0 && slashIndex < input.length() - 1) {
         String allianceName = input.substring(0, slashIndex).trim();
         String listName = input.substring(slashIndex + 1).trim();
         if (!allianceName.isBlank()) {
            return new ParsedAddTarget(allianceName, listName.isBlank() ? null : listName);
         }
      }

      if (input.startsWith("\"") && input.length() > 1) {
         int closingQuote = input.indexOf('"', 1);
         if (closingQuote > 1) {
            String allianceName = input.substring(1, closingQuote).trim();
            String rest = input.substring(closingQuote + 1).trim();
            String listName = rest.isBlank() ? null : rest;
            if (!allianceName.isBlank()) {
               return new ParsedAddTarget(allianceName, listName);
            }
         }
      }

      ArrayList<String> candidateNames = new ArrayList<>();
      for (AllianceModels.AllianceRecord alliance : AllianceService.listAll()) {
         String name = alliance == null || alliance.data == null ? null : alliance.data.name;
         if (name != null && !name.isBlank()) {
            candidateNames.add(name.trim());
         }
      }
      candidateNames.sort((a, b) -> Integer.compare(b.length(), a.length()));

      String loweredInput = input.toLowerCase(Locale.ROOT);
      for (String candidate : candidateNames) {
         String lowered = candidate.toLowerCase(Locale.ROOT);
         if (Objects.equals(loweredInput, lowered)) {
            return new ParsedAddTarget(candidate, null);
         }
         if (loweredInput.startsWith(lowered + " ")) {
            String remainder = input.substring(candidate.length()).trim();
            return new ParsedAddTarget(candidate, remainder.isBlank() ? null : remainder);
         }
      }

      return new ParsedAddTarget(input, null);
   }

   private record ParsedAddTarget(String allianceName, String listName) {
   }
}
