package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.LSUEvent;
import dev.candycup.lifestealutils.event.events.*;

/**
 * base interface for all Lifesteal Utils event listeners.
 * implement specific listener interfaces (CombatEventListener, ChatEventListener, etc.)
 * and override the event handler methods you need.
 */
public interface LifestealEventListener {

   /**
    * @return the priority of this listener (higher priority executes first)
    */
   default EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   /**
    * @return true if this listener should receive events (checks config)
    */
   boolean isEnabled();

   /**
    * internal method to dispatch events to the appropriate handler.
    * do not override this method - implement specific listener interfaces instead.
    *
    * @param event the event to handle
    */
   default void handleEvent(LSUEvent event) {
      // dispatch to specific handler based on event type
      if (this instanceof CombatEventListener combatListener) {
         if (event instanceof ClientAttackEvent e) {
            combatListener.onClientAttack(e);
         } else if (event instanceof DamageConfirmedEvent e) {
            combatListener.onDamageConfirmed(e);
         } else if (event instanceof PlayerDamagedEvent e) {
            combatListener.onPlayerDamaged(e);
         }
      }

      if (this instanceof ChatEventListener chatListener) {
         if (event instanceof ChatMessageReceivedEvent e) {
            chatListener.onChatMessageReceived(e);
         } else if (event instanceof ChatMessageSentEvent e) {
            chatListener.onChatMessageSent(e);
         }
      }

      if (this instanceof TickEventListener tickListener) {
         if (event instanceof ClientTickEvent e) {
            tickListener.onClientTick(e);
         }
      }

      if (this instanceof ServerEventListener serverListener) {
         if (event instanceof ServerChangeEvent e) {
            serverListener.onServerChange(e);
         } else if (event instanceof LifestealShardSwapEvent e) {
            serverListener.onShardSwap(e);
         }
      }

      if (this instanceof RenderEventListener renderListener) {
         if (event instanceof ItemRenderEvent e) {
            renderListener.onItemRender(e);
         } else if (event instanceof PlayerNameRenderEvent e) {
            renderListener.onPlayerNameRender(e);
         }
      }

      if (this instanceof UIEventListener uiListener) {
         if (event instanceof TitleScreenInitEvent e) {
            uiListener.onTitleScreenInit(e);
         } else if (event instanceof SplashTextRequestEvent e) {
            uiListener.onSplashTextRequest(e);
         }
      }
   }
}
