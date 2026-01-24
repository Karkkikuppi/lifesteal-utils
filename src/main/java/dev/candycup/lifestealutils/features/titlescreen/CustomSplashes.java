package dev.candycup.lifestealutils.features.titlescreen;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.SplashTextRequestEvent;
import dev.candycup.lifestealutils.event.listener.UIEventListener;

import java.util.ArrayList;

/**
 * provides custom splash texts for the title screen.
 */
public final class CustomSplashes implements UIEventListener {
    private static final ArrayList<String> SPLASH_TEXTS = new ArrayList<>() {{
        // TP trapper galore
        add("tpa for team");
        // Claim shield gimmick
        add("Your claim shield isn't up yet!");
        // Newbies repeatedly being confused about road/claim protection
        add("Why can't I break anything?");
        // Taking sides on the S2 drunken enchant controversy
        add("Nerf Drunken");
        add("Buff Drunken");
    }};

    /**
     * Indicates whether custom title-screen splash texts are enabled.
     *
     * @return `true` if custom splash texts are enabled, `false` otherwise.
     */
    @Override
    public boolean isEnabled() {
        return Config.getCustomSplashes();
    }

    /**
     * Specifies the priority at which this UI event listener runs.
     *
     * @return the listener's event priority, `EventPriority.NORMAL`
     */
    @Override
    public EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Selects a random custom splash text and applies it to the provided splash text request event.
     *
     * @param event the splash text request event to set the selected splash text on
     */
    @Override
    public void onSplashTextRequest(SplashTextRequestEvent event) {
        String splash = SPLASH_TEXTS.get((int) (Math.random() * SPLASH_TEXTS.size()));
        event.setSplashText(splash);
    }
}