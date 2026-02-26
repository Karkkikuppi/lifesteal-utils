package dev.candycup.lifestealutils.features.messages;

import java.util.regex.Pattern;

/**
 * shared regex patterns for chat message processing.
 */
public final class MessagePatterns {
   public static final Pattern PRIVATE_MESSAGE_PATTERN = Pattern.compile(
           "^\\(MSG\\s+(From|To)\\s+([^)]+)\\)\\s+(.*)$",
           Pattern.CASE_INSENSITIVE
   );

   private MessagePatterns() {
   }
}
