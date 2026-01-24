package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.listener.ChatEventListener;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * normalizes rank plus coloring by merging the colored plus into the rank's color.
 * example: "<bold><#FF7200>HEROIC</#FF7200></bold><green>+</green>" 
 *       -> "<bold><#FF7200>HEROIC+</#FF7200></bold>"
 */
public class RankPlusColorNormalizer implements ChatEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/rankplus");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Indicates whether normalization of the rank-plus color is enabled in configuration.
     *
     * @return `true` if normalization is enabled in configuration, `false` otherwise.
     */
    @Override
    public boolean isEnabled() {
        return Config.getRemoveUniquePlusColor();
    }

    /**
     * Specifies the listener's processing order for chat events.
     *
     * @return the listener's event priority, NORMAL.
     */
    @Override
    public EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Normalize the rank badge's plus color in the incoming chat message and update the event if a change was made.
     *
     * If the message's rank-plus coloring differs from the normalized form, replaces the event's modified message
     * with a component containing the normalized representation and logs a debug entry.
     *
     * @param event the chat message event whose modified message may be normalized
     */
    @Override
    public void onChatMessageReceived(ChatMessageReceivedEvent event) {
        Component original = event.getModifiedMessage();
        String serialized = MINI_MESSAGE.serialize(
            MinecraftClientAudiences.of().asAdventure(original)
        );

        String filtered = normalizePlusColor(serialized);
        
        if (!filtered.equals(serialized)) {
            Component modified = MessagingUtils.miniMessage(filtered);
            event.setModifiedMessage(modified);
            LOGGER.debug("[lsu-rankplus] normalized plus color");
        }
    }

    /**
     * Merge a colored trailing plus sign into the preceding rank's color in a serialized chat message.
     *
     * <p>If the message contains a bold, colored rank immediately followed by a separately colored plus
     * (for example a rank tag followed by a green "+"), the plus will be absorbed into the rank's
     * color so the plus shares the rank color. The method also normalizes surrounding bracket and
     * whitespace sequences.</p>
     *
     * @param message the serialized chat message to normalize; may be null or empty
     * @return the normalized message with the plus merged into the rank color, or the original value if the input was null or empty
     */
    private String normalizePlusColor(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        String pattern = "(<bold>\\s*<([#A-Za-z0-9_]+)>)([^<>]+)(</[A-Za-z0-9_#]+>\\s*</bold>)(\\s*)<[^>]*>\\+(?:</[^>]*>)?";
        String result = message.replaceAll(pattern, "$1$3+$4");
        
        // normalize whitespace
        result = result.replaceAll("(<dark_gray>\\]</dark_gray>)\\s+", "$1 ");
        result = result.replaceAll("\\]\\s+", "] ");
        result = result.replaceAll("[\\s\\u00A0]+", " ").trim();
        
        return result;
    }
}