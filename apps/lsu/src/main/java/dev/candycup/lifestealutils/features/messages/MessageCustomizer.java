package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageCustomizer {
    public static final Pattern PRIVATE_MESSAGE_PATTERN = Pattern.compile("^\\(MSG\\s+(From|To)\\s+([^)]+)\\)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageCustomizer() {
    }

    public static boolean tryHandle(String messageContents) {
        Matcher matcher = PRIVATE_MESSAGE_PATTERN.matcher(messageContents);
        if (!matcher.find()) {
            return false;
        }

        String direction = capitalizeFirst(matcher.group(1));
        String sender = MINI_MESSAGE.escapeTags(matcher.group(2));
        String message = MINI_MESSAGE.escapeTags(matcher.group(3));

        String format = getFormat();

        String formatted = format
                .replace("{{direction}}", direction)
                .replace("{{sender}}", sender)
                .replace("{{message}}", message);

        MessagingUtils.showMiniMessage(formatted);
        return true;
    }

    private static String getFormat() {
        try {
            String format = PrivateMessageFormatter.getPmFormat();
            return format == null || format.isBlank() ? PrivateMessageFormatter.DEFAULT_FORMAT : format;
        } catch (Exception e) {
            return PrivateMessageFormatter.DEFAULT_FORMAT;
        }
    }

    private static String capitalizeFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
