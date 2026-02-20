package dev.candycup.lifestealutils.gaia;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.gaia.modules.collectivum.CollectivumModule;
import dev.candycup.lifestealutils.gaia.modules.curiositas.CuriositasModule;
import dev.candycup.lifestealutils.gaia.modules.gateway.GatewayModule;
import dev.candycup.lifestealutils.gaia.modules.imperium.AlliancesModule;
import dev.candycup.lifestealutils.gaia.modules.imperium.AuthModule;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class GaiaApiClient {
   public static final String GAIA_ROOT = "http://localhost:3030";
   public static final String GAIA_GATEWAY_ROOT = "ws://localhost:3030";

   private static final GaiaApiClient INSTANCE = new GaiaApiClient();

   private final AlliancesModule alliancesModule;
   private final CollectivumModule collectivumModule;
   private final CuriositasModule curiositasModule;
   private final AuthModule authModule;
   private final GatewayModule gatewayModule;

   private GaiaApiClient() {
      this.alliancesModule = new AlliancesModule(this);
      this.collectivumModule = new CollectivumModule(this);
      this.curiositasModule = new CuriositasModule(this);
      this.authModule = new AuthModule(this);
      this.gatewayModule = new GatewayModule(this);
   }

   public static GaiaApiClient getInstance() {
      return INSTANCE;
   }

   public AlliancesModule alliances() {
      return alliancesModule;
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

   public NetworkUtilsController.HttpResult getWithAuth(String path, Duration timeout, long rateLimitMs) {
      return executeGaiaRequest("GET " + path, () -> {
         String token = resolveCurrentToken();
         if (token == null || token.isBlank()) {
            return NetworkUtilsController.HttpResult.failure("auth token is null or blank");
         }
         return NetworkUtilsController.getWithAuth(toHttpUrl(path), token, timeout, rateLimitMs);
      });
   }

   public NetworkUtilsController.HttpResult postJsonWithAuth(String path, String body, Duration timeout, long rateLimitMs) {
      return executeGaiaRequest("POST " + path, () -> {
         String token = resolveCurrentToken();
         if (token == null || token.isBlank()) {
            return NetworkUtilsController.HttpResult.failure("auth token is null or blank");
         }
         return NetworkUtilsController.postJsonWithAuth(toHttpUrl(path), body, token, timeout, rateLimitMs);
      });
   }

   public NetworkUtilsController.HttpResult postJson(String path, String body, Duration timeout, long rateLimitMs) {
      return executeGaiaRequest("POST " + path, () -> NetworkUtilsController.postJson(toHttpUrl(path), body, timeout, rateLimitMs));
   }

   public CompletableFuture<WebSocket> connectGatewayWithToken(HttpClient httpClient, WebSocket.Listener listener, String token) {
      return executeGaiaRequest("WS /v1/gateway/connect", () -> {
         String wsUrl = toGatewayUrl("/v1/gateway/connect") + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
         return httpClient.newWebSocketBuilder().buildAsync(URI.create(wsUrl), listener);
      });
   }

   public <T> T executeGaiaRequest(String requestName, Supplier<T> requestSupplier) {
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         throw new GaiaConsentRequiredException();
      }
      return requestSupplier.get();
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
}