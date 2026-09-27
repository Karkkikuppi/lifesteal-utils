package dev.candycup.lifestealutils.features;

/**
 * Lets the version-specific screen render hook query overlay ownership without knowing how
 * auction and shop screens are detected.
 */
public interface ContainerOverlayBackgroundState {
    default boolean lifestealutils$shouldReplaceAuctionBackground() {
        return false;
    }

    default boolean lifestealutils$shouldReplaceShopBackground() {
        return false;
    }

    default boolean lifestealutils$shouldReplaceContainerBackground() {
        return lifestealutils$shouldReplaceAuctionBackground()
                || lifestealutils$shouldReplaceShopBackground();
    }
}
