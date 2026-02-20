package dev.candycup.lifestealutils.integrations.tiertagger;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.uku3lig.ukulib.utils.PlayerArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mixin(PlayerArgumentType.class)
public class UkuLibFairplayMixin {
   @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
   public <S> void listSuggestions(
           CommandContext<S> context,
           SuggestionsBuilder builder,
           CallbackInfoReturnable<CompletableFuture<Suggestions>> cir
   ) {
      cir.setReturnValue(
              Objects.requireNonNull(Minecraft.getInstance().getConnection())
                      .getOnlinePlayers()
                      .stream()
                      .filter((playerInfo) -> SharedSuggestionProvider.matchesSubStr(
                              builder.getRemaining().toLowerCase(Locale.ROOT),
                              //? if > 1.21.8 {
                              playerInfo.getProfile().name().toLowerCase(Locale.ROOT)
                              //?} else {
                              /*playerInfo.getProfile().getName().toLowerCase(Locale.ROOT)
                               *///?}
                      ))
                      //? if > 1.21.8 {
                      .map(playerInfo -> playerInfo.getProfile().name())
                      //?} else {
                      /*.map(playerInfo -> playerInfo.getProfile().getName())
                       *///?}
                      .filter(name -> Pattern.compile("^[a-zA-Z0-9_]{3,16}$").matcher(name).matches())
                      .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                         Collections.sort(list);
                         SuggestionsBuilder suggestionsBuilder = builder.createOffset(builder.getStart());
                         list.forEach(suggestionsBuilder::suggest);
                         return suggestionsBuilder.buildFuture();
                      }))
      );
   }
}
