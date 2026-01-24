package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * base interface for all Lifesteal Utils event listeners.
 * implement specific listener interfaces (CombatEventListener, ChatEventListener, etc.)
 * and override the event handler methods you need.
 */
public interface LifestealEventListener {

    /**
     * Specifies the execution priority of this listener.
     *
     * @return the EventPriority that determines listener execution order; higher priority listeners run before lower priority ones
     */
    default EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
 * Indicates whether this listener is active and should receive events.
 *
 * @return true if the listener is enabled and should receive events, false otherwise.
 */
    boolean isEnabled();

    /**
     * Dispatches a generic LSUEvent to the matching typed handler method implemented by this listener.
     *
     * <p>This is an internal dispatch method; do not override it. Instead implement the specific
     * typed listener interfaces (for example, CombatEventListener, ChatEventListener) and their
     * handler methods to receive events.</p>
     *
     * @param event the LSUEvent to dispatch to the appropriate typed handler
     */
    default void handleEvent(LSUEvent event) {
        // dispatch to specific handler based on event type
        if (this instanceof CombatEventListener combatListener) {
            if (event instanceof dev.candycup.lifestealutils.event.events.ClientAttackEvent e) {
                combatListener.onClientAttack(e);
            } else if (event instanceof dev.candycup.lifestealutils.event.events.DamageConfirmedEvent e) {
                combatListener.onDamageConfirmed(e);
            } else if (event instanceof dev.candycup.lifestealutils.event.events.PlayerDamagedEvent e) {
                combatListener.onPlayerDamaged(e);
            }
        }

        if (this instanceof ChatEventListener chatListener) {
            if (event instanceof dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent e) {
                chatListener.onChatMessageReceived(e);
            } else if (event instanceof dev.candycup.lifestealutils.event.events.ChatMessageSentEvent e) {
                chatListener.onChatMessageSent(e);
            }
        }

        if (this instanceof TickEventListener tickListener) {
            if (event instanceof dev.candycup.lifestealutils.event.events.ClientTickEvent e) {
                tickListener.onClientTick(e);
            }
        }

        if (this instanceof ServerEventListener serverListener) {
            if (event instanceof dev.candycup.lifestealutils.event.events.ServerChangeEvent e) {
                serverListener.onServerChange(e);
            }
        }

        if (this instanceof RenderEventListener renderListener) {
            if (event instanceof dev.candycup.lifestealutils.event.events.ItemRenderEvent e) {
                renderListener.onItemRender(e);
            } else if (event instanceof dev.candycup.lifestealutils.event.events.PlayerNameRenderEvent e) {
                renderListener.onPlayerNameRender(e);
            }
        }

        if (this instanceof UIEventListener uiListener) {
            if (event instanceof dev.candycup.lifestealutils.event.events.TitleScreenInitEvent e) {
                uiListener.onTitleScreenInit(e);
            } else if (event instanceof dev.candycup.lifestealutils.event.events.SplashTextRequestEvent e) {
                uiListener.onSplashTextRequest(e);
            }
        }
    }
}