package dev.candycup.lifestealutils.interapi;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Greetings from LSU devs! This is the single class that raises the most eyebrows when
 * people inspect the source, and the single largest hot spot for flagging this mod as
 * a security issue or just outright malware by non-enterprise flaggers.
 * <p>
 * Every HTTP network request flows through this class. LSU makes HTTP requests for the
 * following purposes:
 * - fetching information about custom enchants & feature flags
 * - fetching usernames from UUIDs for alliance members using the GeyserMC API.
 * <p>
 * Requests made to either of these services are GET only, and never identify the user.
 * LSU devs have no access to either the Geyser API logs or the feature flag hosting logs.
 * This means that LSU devs can at no point see who is using the mod, or information about
 * requests made by users.
 * <p>
 * this class provides centralized http utilities for making network requests.
 * provides caching, rate limiting, and common configuration.
 */
public final class NetworkUtilsController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/network");
   private static final String USER_AGENT = "LifestealUtils/" + detectModVersion();
   private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

   // rate limiting: track last request time per host to avoid spamming
   private static final Map<String, Long> lastRequestTimePerHost = new ConcurrentHashMap<>();
   private static final long DEFAULT_RATE_LIMIT_MS = TimeUnit.SECONDS.toMillis(1);

   // track pending requests to avoid duplicates
   private static final Map<String, CompletableFuture<HttpResult>> pendingRequests = new ConcurrentHashMap<>();

   private NetworkUtilsController() {
   }

   /**
    * result of an http request.
    */
   public record HttpResult(int statusCode, String body, boolean success, String error) {
      public static HttpResult success(int statusCode, String body) {
         return new HttpResult(statusCode, body, true, null);
      }

      public static HttpResult failure(String error) {
         return new HttpResult(-1, null, false, error);
      }

      public static HttpResult failure(int statusCode, String error) {
         return new HttpResult(statusCode, null, false, error);
      }
   }

   /**
    * makes a blocking GET request to the specified url.
    *
    * @param url the url to request
    * @return the result of the request
    */
   public static HttpResult get(String url) {
      return get(url, DEFAULT_TIMEOUT, 0);
   }

   /**
    * makes a blocking GET request with custom timeout.
    *
    * @param url     the url to request
    * @param timeout the request timeout
    * @return the result of the request
    */
   public static HttpResult get(String url, Duration timeout) {
      return get(url, timeout, 0);
   }

   /**
    * makes a blocking GET request with rate limiting.
    *
    * @param url         the url to request
    * @param timeout     the request timeout
    * @param rateLimitMs minimum milliseconds between requests to the same host (0 to disable)
    * @return the result of the request
    */
   public static HttpResult get(String url, Duration timeout, long rateLimitMs) {
      if (url == null || url.isBlank()) {
         return HttpResult.failure("url is null or blank");
      }

      try {
         URI uri = new URI(url);
         String host = uri.getHost();

         // check rate limit
         if (rateLimitMs > 0 && host != null) {
            Long lastRequest = lastRequestTimePerHost.get(host);
            if (lastRequest != null && System.currentTimeMillis() - lastRequest < rateLimitMs) {
               return HttpResult.failure("rate limited");
            }
         }

         // update rate limit tracker
         if (host != null) {
            lastRequestTimePerHost.put(host, System.currentTimeMillis());
         }

         HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout((int) timeout.toMillis());
         connection.setReadTimeout((int) timeout.toMillis());
         connection.setRequestProperty("User-Agent", USER_AGENT);

         int responseCode = connection.getResponseCode();
         if (responseCode < 200 || responseCode >= 300) {
            return HttpResult.failure(responseCode, "non-ok status code: " + responseCode);
         }

         try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
               response.append(line);
            }
            return HttpResult.success(responseCode, response.toString());
         }
      } catch (Exception e) {
         LOGGER.debug("http request failed for {}: {}", url, e.getMessage());
         return HttpResult.failure("request failed: " + e.getMessage());
      }
   }

   /**
    * makes an async GET request, deduplicating concurrent requests to the same url.
    *
    * @param url         the url to request
    * @param timeout     the request timeout
    * @param rateLimitMs minimum milliseconds between requests to the same host (0 to disable)
    * @return a future containing the result
    */
   public static CompletableFuture<HttpResult> getAsync(String url, Duration timeout, long rateLimitMs) {
      if (url == null || url.isBlank()) {
         return CompletableFuture.completedFuture(HttpResult.failure("url is null or blank"));
      }

      // check for pending request to avoid duplicates
      CompletableFuture<HttpResult> pending = pendingRequests.get(url);
      if (pending != null) {
         return pending;
      }

      CompletableFuture<HttpResult> future = CompletableFuture.supplyAsync(() -> get(url, timeout, rateLimitMs));
      pendingRequests.put(url, future);

      future.whenComplete((result, error) -> pendingRequests.remove(url));

      return future;
   }

   /**
    * makes an async GET request with default settings.
    *
    * @param url the url to request
    * @return a future containing the result
    */
   public static CompletableFuture<HttpResult> getAsync(String url) {
      return getAsync(url, DEFAULT_TIMEOUT, 0);
   }

   /**
    * waits for an async result with a timeout.
    *
    * @param future    the future to wait for
    * @param timeoutMs maximum time to wait in milliseconds
    * @return the result, or a failure if timed out
    */
   public static HttpResult awaitResult(CompletableFuture<HttpResult> future, long timeoutMs) {
      try {
         return future.get(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
         return HttpResult.failure("request timed out or failed: " + e.getMessage());
      }
   }

   private static String detectModVersion() {
      return FabricLoader.getInstance()
              .getModContainer("lifestealutils")
              .map(container -> container.getMetadata().getVersion().getFriendlyString())
              .orElse("unknown");
   }
}
