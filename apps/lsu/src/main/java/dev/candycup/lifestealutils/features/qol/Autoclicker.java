package dev.candycup.lifestealutils.features.qol;

import dev.candycup.lifestealutils.ConfigUtils;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.config.configurables.ConfigurableFloat;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.candycup.lifestealutils.mixin.MouseHandlerInvoker;
import dev.candycup.configura.serial.SerialEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class Autoclicker {
    public static final float MIN_CPS = 0.01f;
    public static final float MAX_CPS = 10.0f;

    @SerialEntry(comment = "Autoclicker clicks per second")
    @ConfigurableFloat(location = "qol.autoclicker.cps", min = MIN_CPS, max = MAX_CPS)
    private static float autoclickerCps = 10.0f;

    private static boolean inputErrorShown;
    private static volatile boolean enabled;
    private static Thread clickThread;

    public boolean toggle() {
        return setEnabled(!enabled);
    }

    public boolean setEnabled(boolean enabled) {
        if (enabled && !LifestealAPI.isOnLifestealNetwork()) {
            Autoclicker.enabled = false;
            stopClickThread();
            MessagingUtils.showTranslated("lsu.autoclicker.lifesteal_only");
            return false;
        }

        Autoclicker.enabled = enabled;
        if (enabled) {
            startClickThread();
        } else {
            stopClickThread();
        }
        showStatus(enabled);
        return enabled;
    }

    public void setCps(float cps) {
        setAutoclickerCps(cps);
        MessagingUtils.showTranslated("lsu.autoclicker.cps_set", MessagingUtils.arg(formatCps(getAutoclickerCps())));
    }

    public static void setAutoclickerCps(float cps) {
        autoclickerCps = clampCps(cps);
        ConfigUtils.HANDLER.save();
    }

    public static float getAutoclickerCps() {
        return autoclickerCps;
    }

    private static synchronized void startClickThread() {
        if (clickThread != null && clickThread.isAlive()) {
            return;
        }

        clickThread = new Thread(Autoclicker::clickLoop, "LSU Autoclicker");
        clickThread.setDaemon(true);
        clickThread.start();
    }

    private static synchronized void stopClickThread() {
        Thread thread = clickThread;
        clickThread = null;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static void clickLoop() {
        while (enabled) {
            long startedAt = System.nanoTime();

            Minecraft client = Minecraft.getInstance();
            if (shouldClick(client)) {
                client.execute(() -> click(client));
            }

            sleepRemainingInterval(startedAt);
        }
    }

    private static boolean shouldClick(Minecraft client) {
        return enabled
                && client.player != null
                && client.screen == null
                && LifestealAPI.isOnLifestealNetwork();
    }

    private static void click(Minecraft client) {
        if (!shouldClick(client)) {
            return;
        }

        try {
            long window = client.getWindow().handle();
            MouseButtonInfo button = new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0);
            MouseHandlerInvoker mouseHandler = (MouseHandlerInvoker) client.mouseHandler;

            mouseHandler.lifestealutils$onButton(window, button, GLFW.GLFW_PRESS);
            mouseHandler.lifestealutils$onButton(window, button, GLFW.GLFW_RELEASE);
        } catch (RuntimeException exception) {
            enabled = false;
            stopClickThread();
            if (!inputErrorShown) {
                inputErrorShown = true;
                MessagingUtils.showTranslated("lsu.autoclicker.input_unavailable");
            }
        }
    }

    private static void sleepRemainingInterval(long startedAt) {
        long remainingNanos = clickIntervalNanos(getAutoclickerCps()) - (System.nanoTime() - startedAt);
        if (remainingNanos <= 0L) {
            return;
        }

        try {
            Thread.sleep(remainingNanos / 1_000_000L, (int) (remainingNanos % 1_000_000L));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static long clickIntervalNanos(float cps) {
        return Math.max(1L, Math.round(1_000_000_000.0 / clampCps(cps)));
    }

    private static float clampCps(float cps) {
        return Math.max(MIN_CPS, Math.min(MAX_CPS, cps));
    }

    private static String formatCps(float cps) {
        return String.format(Locale.ROOT, "%.2f", cps);
    }

    private static void showStatus(boolean enabled) {
        if (enabled) {
            MessagingUtils.showTranslated("lsu.autoclicker.enabled", MessagingUtils.arg(formatCps(getAutoclickerCps())));
        } else {
            MessagingUtils.showTranslated("lsu.autoclicker.disabled");
        }
    }
}
