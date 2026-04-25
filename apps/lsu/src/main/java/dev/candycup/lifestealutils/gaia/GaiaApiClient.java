package dev.candycup.lifestealutils.gaia;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.gaia.modules.collectivum.CollectivumModule;
import dev.candycup.lifestealutils.gaia.modules.curiositas.CuriositasModule;
import dev.candycup.lifestealutils.gaia.modules.gateway.GatewayModule;
import dev.candycup.lifestealutils.gaia.modules.imperium.AuthModule;
import dev.candycup.lifestealutils.gaia.modules.alliances.AlliancesModule;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class GaiaApiClient {
   public static final String GAIA_ROOT = resolveApiRoot();
   public static final String GAIA_GATEWAY_ROOT = resolveGatewayRoot();

   private static final GaiaApiClient INSTANCE = new GaiaApiClient();

   private final CollectivumModule collectivumModule;
   private final CuriositasModule curiositasModule;
   private final AuthModule authModule;
   private final GatewayModule gatewayModule;
   private final AlliancesModule alliancesModule;

   private GaiaApiClient() {
      this.collectivumModule = new CollectivumModule(this);
      this.curiositasModule = new CuriositasModule(this);
      this.authModule = new AuthModule(this);
      this.gatewayModule = new GatewayModule(this);
      this.alliancesModule = new AlliancesModule(this);
   }

   public static GaiaApiClient getInstance() {
      return INSTANCE;
   }

   public CollectivumModule collectivum() {
      return collectivumModule;
   }

   public CuriositasModule curiositas() {
      return curiositasModule;
   }

   public AuthModule auth() {
      return authModule;
   }

   public GatewayModule gateway() {
      return gatewayModule;
   }

   public AlliancesModule alliances() {
      return alliancesModule;
   }

   public NetworkUtilsController.HttpResult getWithAuth(String path, Duration timeout, long rateLimitMs) {
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return NetworkUtilsController.HttpResult.failure("Gaia is disabled");
      }
      String token = resolveCurrentToken();
      if (token == null || token.isBlank()) {
         return NetworkUtilsController.HttpResult.failure("auth token is null or blank");
      }
      return NetworkUtilsController.getWithAuth(toHttpUrl(path), token, timeout, rateLimitMs);
   }

   public NetworkUtilsController.HttpResult postJsonWithAuth(String path, String body, Duration timeout, long rateLimitMs) {
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return NetworkUtilsController.HttpResult.failure("Gaia is disabled");
      }
      String token = resolveCurrentToken();
      if (token == null || token.isBlank()) {
         return NetworkUtilsController.HttpResult.failure("auth token is null or blank");
      }
      return NetworkUtilsController.postJsonWithAuth(toHttpUrl(path), body, token, timeout, rateLimitMs);
   }

   // No consent gate!! used for auth handshake, which is a prerequisite for consent being actionable.
   public NetworkUtilsController.HttpResult postJson(String path, String body, Duration timeout, long rateLimitMs) {
      return NetworkUtilsController.postJson(toHttpUrl(path), body, timeout, rateLimitMs);
   }

   public NetworkUtilsController.HttpResult putJsonWithAuth(String path, String body, Duration timeout, long rateLimitMs) {
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return NetworkUtilsController.HttpResult.failure("Gaia is disabled");
      }
      String token = resolveCurrentToken();
      if (token == null || token.isBlank()) {
         return NetworkUtilsController.HttpResult.failure("auth token is null or blank");
      }
      return NetworkUtilsController.putJsonWithAuth(toHttpUrl(path), body, token, timeout, rateLimitMs);
   }

   public NetworkUtilsController.HttpResult deleteWithAuth(String path, Duration timeout, long rateLimitMs) {
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return NetworkUtilsController.HttpResult.failure("Gaia is disabled");
      }
      String token = resolveCurrentToken();
      if (token == null || token.isBlank()) {
         return NetworkUtilsController.HttpResult.failure("auth token is null or blank");
      }
      return NetworkUtilsController.deleteWithAuth(toHttpUrl(path), token, timeout, rateLimitMs);
   }

   // No consent gate ! caller (GaiaGatewayClient) is responsible for checking isEnabled().
   public CompletableFuture<WebSocket> connectGatewayWithToken(HttpClient httpClient, WebSocket.Listener listener, String token) {
      String wsUrl = toGatewayUrl("/v1/gateway/connect") + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
      return httpClient.newWebSocketBuilder().buildAsync(URI.create(wsUrl), listener);
   }

   private String resolveCurrentToken() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return null;
      }
      String playerName = player.getName().getString();
      return GaiaAuthTokenStore.readToken(playerName);
   }

   private static String toHttpUrl(String path) {
      if (path == null || path.isBlank()) {
         return GAIA_ROOT;
      }
      return path.startsWith("/") ? GAIA_ROOT + path : GAIA_ROOT + "/" + path;
   }

   private static String toGatewayUrl(String path) {
      if (path == null || path.isBlank()) {
         return GAIA_GATEWAY_ROOT;
      }
      return path.startsWith("/") ? GAIA_GATEWAY_ROOT + path : GAIA_GATEWAY_ROOT + "/" + path;
   }

   private static String resolveApiRoot() {
      return FabricLoader.getInstance().isDevelopmentEnvironment()
              ? "http://localhost:3030"
              : "https://gaia.candycup.dev";
   }

   private static String resolveGatewayRoot() {
      return FabricLoader.getInstance().isDevelopmentEnvironment()
              ? "ws://localhost:3030"
              : "wss://gaia.candycup.dev";
   }
}
