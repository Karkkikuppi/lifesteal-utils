package dev.candycup.lifestealutils.gaia.modules.gateway;

import dev.candycup.lifestealutils.gaia.GaiaApiClient;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

public final class GatewayModule {
   private final GaiaApiClient apiClient;

   public GatewayModule(GaiaApiClient apiClient) {
      this.apiClient = apiClient;
   }

   public CompletableFuture<WebSocket> connectWithToken(HttpClient httpClient, WebSocket.Listener listener, String token) {
      return apiClient.connectGatewayWithToken(httpClient, listener, token);
   }
}