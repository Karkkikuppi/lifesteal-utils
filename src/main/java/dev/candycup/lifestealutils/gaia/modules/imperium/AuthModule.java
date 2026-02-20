package dev.candycup.lifestealutils.gaia.modules.imperium;

import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;

import java.time.Duration;

public final class AuthModule {
   private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(5);
   private static final String CONFIRM_HANDSHAKE_PATH = "/v1/imperium/auth/confirm-handshake";

   private final GaiaApiClient apiClient;

   public AuthModule(GaiaApiClient apiClient) {
      this.apiClient = apiClient;
   }

   public NetworkUtilsController.HttpResult confirmHandshake(String payloadJson) {
      return apiClient.postJson(CONFIRM_HANDSHAKE_PATH, payloadJson, AUTH_TIMEOUT, 0);
   }
}