package dev.candycup.lifestealutils.features.shop;

public sealed interface ShopParseResult permits ShopParseResult.Valid, ShopParseResult.Fallback {
   record Valid(ShopView view) implements ShopParseResult {
   }

   record Fallback(ShopStabilityCode code, String detail) implements ShopParseResult {
   }
}
