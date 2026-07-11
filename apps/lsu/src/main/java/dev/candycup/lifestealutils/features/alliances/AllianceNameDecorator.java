package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.IncludeInAccordion;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.util.List;

public final class AllianceNameDecorator {
    @Getter
    @Setter
    @SerialEntry(comment = "Whether to enable alliance features such as colored name tags.")
    @ConfigurableBoolean(location = "alliances.general.enablealliances")
    private static boolean enableAlliances = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Whether to render alliance prefixes in nametags")
    @ConfigurableBoolean(location = "alliances.general.allianceprefixenabled")
    private static boolean allianceNamePrefixEnabled = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Whether to apply list-specific alliance name colors to player names")
    @ConfigurableBoolean(location = "alliances.general.alliancenamecolorenabled")
    private static boolean allianceNameColorEnabled = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Allow alliance members to bypass ghosted chat filtering")
    @ConfigurableBoolean(location = "alliances.general.allowalliancebypassghostedchat")
    private static boolean allowAllianceBypassGhostedChat = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Also shows the alliance tag next to your own name in the first person")
    @ConfigurableBoolean(location = "alliances.general.showalliancetagonself")
    @IncludeInAccordion("general")
    private static boolean showAllianceTagOnSelf = false;

    public AllianceNameDecorator() {
        LifestealUtilsEvents.PLAYER_NAME_RENDER.register(this::onPlayerNameRender);
    }

    private void onPlayerNameRender(PlayerNameRenderEvent event) {
        if (!enableAlliances || !allianceNamePrefixEnabled) {
            return;
        }
        String playerName = event.getPlayerName();
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        if (!showAllianceTagOnSelf) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && playerName.equals(client.player.getName().getString())) {
                return;
            }
        }
        AllianceProfileCacheManager.initialize();
        String playerUuid = AllianceProfileCacheManager.getCachedUuidByName(playerName);
        if (playerUuid == null) {
            AllianceProfileCacheManager.queueUuidLookupForName(playerName);
            return;
        }

        DecorData decor = findDecorForUuid(playerUuid);
        if (decor == null) {
            return;
        }

        MutableComponent coloredName = allianceNameColorEnabled
                ? colorOnlyPlayerName(event.getModifiedDisplayName(), playerName, decor.nameColor)
                : event.getModifiedDisplayName().copy();
        Component prefix = parsePrefix(decor.prefixText, decor.prefixFallback, decor.prefixColor);
        Component separator = Component.literal(" | ").withStyle(style -> style.withColor(0xAAAAAA));
        event.setModifiedDisplayName(Component.empty().append(prefix).append(separator).append(coloredName));
    }

    private DecorData findDecorForUuid(String playerUuid) {
        String normalizedPlayerUuid = AllianceProfileCacheManager.normalizeUuid(playerUuid);
        if (normalizedPlayerUuid == null) {
            return null;
        }

        List<AllianceModels.AllianceRecord> ordered = AllianceService.listAll();
        for (AllianceModels.AllianceRecord alliance : ordered) {
            if (alliance == null || alliance.data == null || alliance.data.lists == null) {
                continue;
            }
            for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
                if (list == null || list.members == null) {
                    continue;
                }
                for (AllianceModels.AllianceMember member : list.members) {
                    String memberUuid = AllianceProfileCacheManager.normalizeUuid(member == null ? null : member.uuid);
                    if (memberUuid == null || !memberUuid.equalsIgnoreCase(normalizedPlayerUuid)) {
                        continue;
                    }
                    String fallback = alliance.data.name == null || alliance.data.name.isBlank() ? "Alliance" : alliance.data.name;
                    return new DecorData(list.prefix == null ? "" : list.prefix, fallback, safeColor(list.prefixColor), safeColor(list.nameColor));
                }
            }
        }
        return null;
    }

    private Component parsePrefix(String prefix, String fallbackName, int color) {
        if (prefix == null || prefix.isBlank()) {
            return Component.literal("[" + (fallbackName == null ? "Alliance" : fallbackName) + "]")
                    .withStyle(style -> style.withColor(safeColor(color)));
        }
        try {
            return MessagingUtils.miniMessage(prefix).copy().withStyle(style -> style.withColor(safeColor(color)));
        } catch (Exception ignored) {
            return Component.literal(prefix).withStyle(style -> style.withColor(safeColor(color)));
        }
    }

    private static int safeColor(int color) {
        if (color < 0 || color > 0xFFFFFF) {
            return 0xFFFFFF;
        }
        return color;
    }

    private MutableComponent colorOnlyPlayerName(Component source, String playerName, int color) {
        if (source == null) {
            return Component.empty();
        }
        MutableComponent root = source.copy();
        String needle = playerName == null ? "" : playerName.trim();
        if (needle.isBlank()) {
            return root.withStyle(style -> style.withColor(safeColor(color)));
        }
        if (!applyColorToMatchingLiteral(root, needle, safeColor(color))) {
            return root.withStyle(style -> style.withColor(safeColor(color)));
        }
        return root;
    }

    private boolean applyColorToMatchingLiteral(MutableComponent component, String needle, int color) {
        boolean changed = false;
        if (component.getContents() instanceof PlainTextContents.LiteralContents(String current)) {
            if (current != null && current.equalsIgnoreCase(needle)) {
                component.withStyle(style -> style.withColor(color));
                changed = true;
            }
        }
        List<Component> siblings = component.getSiblings();
        for (Component sibling : siblings) {
            if (!(sibling instanceof MutableComponent mutableSibling)) {
                continue;
            }
            changed |= applyColorToMatchingLiteral(mutableSibling, needle, color);
        }
        return changed;
    }

    private record DecorData(String prefixText, String prefixFallback, int prefixColor, int nameColor) {
    }
}
