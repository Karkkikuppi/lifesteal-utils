package dev.candycup.lifestealutils.features.ah;

public sealed interface AhParseResult permits AhParseResult.Valid, AhParseResult.Fallback {
   record Valid(AhView view) implements AhParseResult {
   }

   record Fallback(AhStabilityCode code, String detail) implements AhParseResult {
   }
}
