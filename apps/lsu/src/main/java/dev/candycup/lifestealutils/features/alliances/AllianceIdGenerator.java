package dev.candycup.lifestealutils.features.alliances;

import java.security.SecureRandom;

public final class AllianceIdGenerator {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CLIENT_ID_LENGTH = 16;

    private AllianceIdGenerator() {
    }

    public static String newClientId() {
        StringBuilder out = new StringBuilder(CLIENT_ID_LENGTH);
        for (int i = 0; i < CLIENT_ID_LENGTH; i++) {
            out.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return out.toString();
    }

    public static String newListId() {
        return "list_" + newClientId();
    }
}
