package dev.candycup.lifestealutils.gaia.curiositas;

import dev.candycup.lifestealutils.gaia.GaiaApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * compatibility wrapper for curiositas Gaia APIs.
 */
public final class CuriositasBaltopSnapshotClient {
   private CuriositasBaltopSnapshotClient() {
   }

   public enum SnapshotRange {
      RANGE_24_HOURS("24h", "lifestealutils.baltop.delta.range.24h"),
      RANGE_7_DAYS("7d", "lifestealutils.baltop.delta.range.7d");

      private final String queryValue;
      private final String translationKey;

      SnapshotRange(String queryValue, String translationKey) {
         this.queryValue = queryValue;
         this.translationKey = translationKey;
      }

      public String queryValue() {
         return queryValue;
      }

      public String translationKey() {
         return translationKey;
      }
   }

   public record SnapshotEntry(String username, long currentAmount, Long pastAmount) {
   }

   public record SnapshotResponse(SnapshotRange range, List<SnapshotEntry> entries) {
   }

   public static CompletableFuture<SnapshotResponse> fetchSnapshot(SnapshotRange range) {
      return GaiaApiClient.getInstance().curiositas().fetchSnapshot(range);
   }
}
