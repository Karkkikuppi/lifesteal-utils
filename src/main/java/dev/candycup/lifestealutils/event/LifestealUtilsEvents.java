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
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class LifestealUtilsEvents {
   public static final Event<PacketEvent> PACKET_RECEIVED = EventFactory.createArrayBacked(PacketEvent.class, listeners -> (packet, callbackInfo) -> {
      for (PacketEvent listener : listeners) {
         listener.onPacketReceived(packet, callbackInfo);
      }
   });

   public static final Event<TitleScreenEvent> TITLE_SCREEN_INIT = EventFactory.createArrayBacked(TitleScreenEvent.class, listeners -> (titleScreen) -> {
      for (TitleScreenEvent listener : listeners) {
         listener.onTitleScreenInit(titleScreen);
      }
   });

   public static final Event<ClientAttackEventListener> CLIENT_ATTACK = EventFactory.createArrayBacked(ClientAttackEventListener.class, listeners -> event -> {
      for (ClientAttackEventListener listener : listeners) {
         listener.onClientAttack(event);
      }
   });

   public static final Event<ChatMessageReceivedEventListener> CHAT_MESSAGE_RECEIVED = EventFactory.createArrayBacked(ChatMessageReceivedEventListener.class, listeners -> event -> {
      for (ChatMessageReceivedEventListener listener : listeners) {
         listener.onChatMessageReceived(event);
      }
   });

   public static final Event<ChatMessageSentEventListener> CHAT_MESSAGE_SENT = EventFactory.createArrayBacked(ChatMessageSentEventListener.class, listeners -> event -> {
      for (ChatMessageSentEventListener listener : listeners) {
         listener.onChatMessageSent(event);
      }
   });

   public static final Event<ClientTickEventListener> CLIENT_TICK = EventFactory.createArrayBacked(ClientTickEventListener.class, listeners -> event -> {
      for (ClientTickEventListener listener : listeners) {
         listener.onClientTick(event);
      }
   });

   public static final Event<ServerChangeEventListener> SERVER_CHANGE = EventFactory.createArrayBacked(ServerChangeEventListener.class, listeners -> event -> {
      for (ServerChangeEventListener listener : listeners) {
         listener.onServerChange(event);
      }
   });

   public static final Event<ShardSwapEventListener> SHARD_SWAP = EventFactory.createArrayBacked(ShardSwapEventListener.class, listeners -> event -> {
      for (ShardSwapEventListener listener : listeners) {
         listener.onShardSwap(event);
      }
   });

   public static final Event<ItemRenderEventListener> ITEM_RENDER = EventFactory.createArrayBacked(ItemRenderEventListener.class, listeners -> event -> {
      for (ItemRenderEventListener listener : listeners) {
         listener.onItemRender(event);
      }
   });

   public static final Event<PlayerNameRenderEventListener> PLAYER_NAME_RENDER = EventFactory.createArrayBacked(PlayerNameRenderEventListener.class, listeners -> event -> {
      for (PlayerNameRenderEventListener listener : listeners) {
         listener.onPlayerNameRender(event);
      }
   });

   public static final Event<SplashTextRequestEventListener> SPLASH_TEXT_REQUEST = EventFactory.createArrayBacked(SplashTextRequestEventListener.class, listeners -> event -> {
      for (SplashTextRequestEventListener listener : listeners) {
         listener.onSplashTextRequest(event);
      }
   });

   public static final Event<GatewayConnectedEventListener> GATEWAY_CONNECTED = EventFactory.createArrayBacked(GatewayConnectedEventListener.class, listeners -> event -> {
      for (GatewayConnectedEventListener listener : listeners) {
         listener.onGatewayConnected(event);
      }
   });

   public static final Event<GatewayDisconnectedEventListener> GATEWAY_DISCONNECTED = EventFactory.createArrayBacked(GatewayDisconnectedEventListener.class, listeners -> event -> {
      for (GatewayDisconnectedEventListener listener : listeners) {
         listener.onGatewayDisconnected(event);
      }
   });

   public static final Event<GatewayErrorEventListener> GATEWAY_ERROR = EventFactory.createArrayBacked(GatewayErrorEventListener.class, listeners -> event -> {
      for (GatewayErrorEventListener listener : listeners) {
         listener.onGatewayError(event);
      }
   });

   public static final Event<GatewayMessageEventListener> GATEWAY_MESSAGE = EventFactory.createArrayBacked(GatewayMessageEventListener.class, listeners -> event -> {
      for (GatewayMessageEventListener listener : listeners) {
         listener.onGatewayMessage(event);
      }
   });

   public static final Event<CommandEvent> COMMAND_SENT = EventFactory.createArrayBacked(CommandEvent.class, listeners -> command -> {
      for (CommandEvent listener : listeners) {
         listener.onCommandSent(command);
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
