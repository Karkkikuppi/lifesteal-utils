package dev.candycup.lifestealutils.event;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class LifestealUtilsEvents {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/events");

   public static final Event<PacketEvent> PACKET_RECEIVED = EventFactory.createArrayBacked(PacketEvent.class, listeners -> (packet, callbackInfo) -> {
      for (PacketEvent listener : listeners) {
         try {
            listener.onPacketReceived(packet, callbackInfo);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] PACKET_RECEIVED listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<TitleScreenEvent> TITLE_SCREEN_INIT = EventFactory.createArrayBacked(TitleScreenEvent.class, listeners -> titleScreen -> {
      for (TitleScreenEvent listener : listeners) {
         try {
            listener.onTitleScreenInit(titleScreen);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] TITLE_SCREEN_INIT listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ClientAttackEventListener> CLIENT_ATTACK = EventFactory.createArrayBacked(ClientAttackEventListener.class, listeners -> event -> {
      for (ClientAttackEventListener listener : listeners) {
         try {
            listener.onClientAttack(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] CLIENT_ATTACK listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ChatMessageReceivedEventListener> CHAT_MESSAGE_RECEIVED = EventFactory.createArrayBacked(ChatMessageReceivedEventListener.class, listeners -> event -> {
      for (ChatMessageReceivedEventListener listener : listeners) {
         try {
            listener.onChatMessageReceived(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] CHAT_MESSAGE_RECEIVED listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ChatMessageSentEventListener> CHAT_MESSAGE_SENT = EventFactory.createArrayBacked(ChatMessageSentEventListener.class, listeners -> event -> {
      for (ChatMessageSentEventListener listener : listeners) {
         try {
            listener.onChatMessageSent(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] CHAT_MESSAGE_SENT listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ClientTickEventListener> CLIENT_TICK = EventFactory.createArrayBacked(ClientTickEventListener.class, listeners -> event -> {
      for (ClientTickEventListener listener : listeners) {
         try {
            listener.onClientTick(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] CLIENT_TICK listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ServerChangeEventListener> SERVER_CHANGE = EventFactory.createArrayBacked(ServerChangeEventListener.class, listeners -> event -> {
      for (ServerChangeEventListener listener : listeners) {
         try {
            listener.onServerChange(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] SERVER_CHANGE listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ShardSwapEventListener> SHARD_SWAP = EventFactory.createArrayBacked(ShardSwapEventListener.class, listeners -> event -> {
      for (ShardSwapEventListener listener : listeners) {
         try {
            listener.onShardSwap(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] SHARD_SWAP listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ItemRenderEventListener> ITEM_RENDER = EventFactory.createArrayBacked(ItemRenderEventListener.class, listeners -> event -> {
      for (ItemRenderEventListener listener : listeners) {
         try {
            listener.onItemRender(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] ITEM_RENDER listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<PlayerNameRenderEventListener> PLAYER_NAME_RENDER = EventFactory.createArrayBacked(PlayerNameRenderEventListener.class, listeners -> event -> {
      for (PlayerNameRenderEventListener listener : listeners) {
         try {
            listener.onPlayerNameRender(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] PLAYER_NAME_RENDER listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<SplashTextRequestEventListener> SPLASH_TEXT_REQUEST = EventFactory.createArrayBacked(SplashTextRequestEventListener.class, listeners -> event -> {
      for (SplashTextRequestEventListener listener : listeners) {
         try {
            listener.onSplashTextRequest(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] SPLASH_TEXT_REQUEST listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<GatewayConnectedEventListener> GATEWAY_CONNECTED = EventFactory.createArrayBacked(GatewayConnectedEventListener.class, listeners -> event -> {
      for (GatewayConnectedEventListener listener : listeners) {
         try {
            listener.onGatewayConnected(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] GATEWAY_CONNECTED listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<GatewayDisconnectedEventListener> GATEWAY_DISCONNECTED = EventFactory.createArrayBacked(GatewayDisconnectedEventListener.class, listeners -> event -> {
      for (GatewayDisconnectedEventListener listener : listeners) {
         try {
            listener.onGatewayDisconnected(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] GATEWAY_DISCONNECTED listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<GatewayErrorEventListener> GATEWAY_ERROR = EventFactory.createArrayBacked(GatewayErrorEventListener.class, listeners -> event -> {
      for (GatewayErrorEventListener listener : listeners) {
         try {
            listener.onGatewayError(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] GATEWAY_ERROR listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<GatewayMessageEventListener> GATEWAY_MESSAGE = EventFactory.createArrayBacked(GatewayMessageEventListener.class, listeners -> event -> {
      for (GatewayMessageEventListener listener : listeners) {
         try {
            listener.onGatewayMessage(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] GATEWAY_MESSAGE listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<CommandEvent> COMMAND_SENT = EventFactory.createArrayBacked(CommandEvent.class, listeners -> command -> {
      for (CommandEvent listener : listeners) {
         try {
            listener.onCommandSent(command);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] COMMAND_SENT listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   public static final Event<ContainerContentSetEventListener> CONTAINER_CONTENT_SET = EventFactory.createArrayBacked(ContainerContentSetEventListener.class, listeners -> event -> {
      for (ContainerContentSetEventListener listener : listeners) {
         try {
            listener.onContainerContentSet(event);
         } catch (Exception e) {
            LOGGER.error("[lsu-events] CONTAINER_CONTENT_SET listener {} threw an exception", listener.getClass().getName(), e);
         }
      }
   });

   private LifestealUtilsEvents() {
   }

   @FunctionalInterface
   public interface CommandEvent {
      void onCommandSent(String command);
   }

   @FunctionalInterface
   public interface PacketEvent {
      void onPacketReceived(Packet<?> packet, CallbackInfo callbackInfo);
   }

   @FunctionalInterface
   public interface TitleScreenEvent {
      void onTitleScreenInit(TitleScreen titleScreen);
   }

   @FunctionalInterface
   public interface ClientAttackEventListener {
      void onClientAttack(ClientAttackEvent event);
   }

   @FunctionalInterface
   public interface ChatMessageReceivedEventListener {
      void onChatMessageReceived(ChatMessageReceivedEvent event);
   }

   @FunctionalInterface
   public interface ChatMessageSentEventListener {
      void onChatMessageSent(ChatMessageSentEvent event);
   }

   @FunctionalInterface
   public interface ClientTickEventListener {
      void onClientTick(ClientTickEvent event);
   }

   @FunctionalInterface
   public interface ServerChangeEventListener {
      void onServerChange(ServerChangeEvent event);
   }

   @FunctionalInterface
   public interface ShardSwapEventListener {
      void onShardSwap(LifestealShardSwapEvent event);
   }

   @FunctionalInterface
   public interface ItemRenderEventListener {
      void onItemRender(ItemRenderEvent event);
   }

   @FunctionalInterface
   public interface PlayerNameRenderEventListener {
      void onPlayerNameRender(PlayerNameRenderEvent event);
   }

   @FunctionalInterface
   public interface SplashTextRequestEventListener {
      void onSplashTextRequest(SplashTextRequestEvent event);
   }

   @FunctionalInterface
   public interface GatewayConnectedEventListener {
      void onGatewayConnected(GatewayConnectedEvent event);
   }

   @FunctionalInterface
   public interface GatewayDisconnectedEventListener {
      void onGatewayDisconnected(GatewayDisconnectedEvent event);
   }

   @FunctionalInterface
   public interface GatewayErrorEventListener {
      void onGatewayError(GatewayErrorEvent event);
   }

   @FunctionalInterface
   public interface GatewayMessageEventListener {
      void onGatewayMessage(GatewayMessageEvent event);
   }

   @FunctionalInterface
   public interface ContainerContentSetEventListener {
      void onContainerContentSet(ContainerContentSetEvent event);
   }

   public static class ContainerContentSetEvent extends LSUEvent {
      private final AbstractContainerMenu menu;
      private final String screenTitle;

      public ContainerContentSetEvent(AbstractContainerMenu menu, String screenTitle) {
         this.menu = menu;
         this.screenTitle = screenTitle;
      }

      public AbstractContainerMenu getMenu() {
         return menu;
      }

      /**
       * plain-text title of the currently open screen, or empty string if none
       */
      public String getScreenTitle() {
         return screenTitle;
      }

      @Override
      public boolean isCancellable() {
         return false;
      }
   }

   public record GatewayConnectedEvent(String username, String uuid) {
   }

   public record GatewayDisconnectedEvent(String reason, boolean willReconnect) {
   }

   public record GatewayErrorEvent(String errorCode, String errorMessage, Throwable cause) {
      public GatewayErrorEvent(String errorCode, String errorMessage) {
         this(errorCode, errorMessage, null);
      }
   }

   public record GatewayMessageEvent(String type, JsonObject data) {
   }

   public static class ClientAttackEvent extends LSUEvent {
      private final Entity target;
      private final long timestamp;

      public ClientAttackEvent(Entity target, long timestamp) {
         this.target = target;
         this.timestamp = timestamp;
      }

      public Entity getTarget() {
         return target;
      }

      public int getTargetId() {
         return target != null ? target.getId() : -1;
      }

      public long getTimestamp() {
         return timestamp;
      }

      @Override
      public boolean isCancellable() {
         return true;
      }
   }

   public static class ClientTickEvent extends LSUEvent {
      private final Minecraft client;

      public ClientTickEvent(Minecraft client) {
         this.client = client;
      }

      public Minecraft getClient() {
         return client;
      }

      @Override
      public boolean isCancellable() {
         return false;
      }
   }

   public static class ChatMessageSentEvent extends LSUEvent {
      private final String message;

      public ChatMessageSentEvent(String message) {
         this.message = message;
      }

      public String getMessage() {
         return message;
      }

      @Override
      public boolean isCancellable() {
         return true;
      }
   }

   public static class ChatMessageReceivedEvent extends LSUEvent {
      private final Component message;
      private Component modifiedMessage;

      public ChatMessageReceivedEvent(Component message) {
         this.message = message;
         this.modifiedMessage = message;
      }

      public Component getMessage() {
         return message;
      }

      public Component getModifiedMessage() {
         return modifiedMessage;
      }

      public void setModifiedMessage(Component modifiedMessage) {
         this.modifiedMessage = modifiedMessage != null ? modifiedMessage : message;
      }

      @Override
      public boolean isCancellable() {
         return true;
      }
   }

   public static class ItemRenderEvent extends LSUEvent {
      private final ItemStack itemStack;
      private final PoseStack poseStack;
      private final boolean isRare;

      public ItemRenderEvent(ItemStack itemStack, PoseStack poseStack, boolean isRare) {
         this.itemStack = itemStack;
         this.poseStack = poseStack;
         this.isRare = isRare;
      }

      public ItemStack getItemStack() {
         return itemStack;
      }

      public PoseStack getPoseStack() {
         return poseStack;
      }

      public boolean isRare() {
         return isRare;
      }

      @Override
      public boolean isCancellable() {
         return true;
      }
   }

   public static class SplashTextRequestEvent extends LSUEvent {
      private String splashText;

      public String getSplashText() {
         return splashText;
      }

      public void setSplashText(String splashText) {
         this.splashText = splashText;
      }
   }

   public static class LifestealShardSwapEvent extends LSUEvent {
      @Getter
      private final String shardName;
      @Getter
      private final String fromShard;

      public LifestealShardSwapEvent(String shardName, String fromShard) {
         this.shardName = shardName;
         this.fromShard = fromShard;
      }

      @Override
      public boolean isCancellable() {
         return false;
      }
   }

   public static class ServerChangeEvent extends LSUEvent {
      private final Type type;
      private final String serverAddress;

      public enum Type {
         CONNECTED,
         DISCONNECTED
      }

      public ServerChangeEvent(Type type, String serverAddress) {
         this.type = type;
         this.serverAddress = serverAddress;
      }

      public Type getType() {
         return type;
      }

      public String getServerAddress() {
         return serverAddress;
      }

      public boolean isConnected() {
         return type == Type.CONNECTED;
      }

      public boolean isDisconnected() {
         return type == Type.DISCONNECTED;
      }

      @Override
      public boolean isCancellable() {
         return false;
      }
   }

   public static class PlayerNameRenderEvent extends LSUEvent {
      public enum RenderContext {
         NAMETAG,
         TABLIST
      }

      private final String playerName;
      private final RenderContext renderContext;
      private final Component originalDisplayName;
      private Component modifiedDisplayName;

      public PlayerNameRenderEvent(String playerName, RenderContext renderContext, Component originalDisplayName) {
         this.playerName = playerName;
         this.renderContext = renderContext;
         this.originalDisplayName = originalDisplayName;
         this.modifiedDisplayName = originalDisplayName;
      }

      public String getPlayerName() {
         return playerName;
      }

      public RenderContext getRenderContext() {
         return renderContext;
      }

      public Component getOriginalDisplayName() {
         return originalDisplayName;
      }

      public Component getModifiedDisplayName() {
         return modifiedDisplayName;
      }

      public void setModifiedDisplayName(Component displayName) {
         this.modifiedDisplayName = displayName != null ? displayName : originalDisplayName;
      }
   }
}
