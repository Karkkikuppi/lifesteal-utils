package dev.candycup.lifestealutils.gaia.collectivum;

import dev.candycup.lifestealutils.gaia.GaiaApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * compatibility wrapper for collectivum Gaia APIs.
 */
public final class CollectivumBaltopClient {
   private CollectivumBaltopClient() {
   }

   public record BaltopEntryPayload(String username, long amount) {
   }

   public static CompletableFuture<Boolean> submitBaltopEntries(List<BaltopEntryPayload> entries) {
      return GaiaApiClient.getInstance().collectivum().submitBaltopEntries(entries);
   }
}
