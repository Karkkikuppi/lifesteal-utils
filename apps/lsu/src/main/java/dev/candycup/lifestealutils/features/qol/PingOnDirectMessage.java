package dev.candycup.lifestealutils.features.qol;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.regex.Matcher;

import static dev.candycup.lifestealutils.features.messages.MessageCustomizer.PRIVATE_MESSAGE_PATTERN;

public class PingOnDirectMessage {
    public static boolean tryHandle(String messageContents) {
        Matcher matcher = PRIVATE_MESSAGE_PATTERN.matcher(messageContents);

        if (!matcher.find()) {
            return false;
        }

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f
        ));
        return true;
    }
}
