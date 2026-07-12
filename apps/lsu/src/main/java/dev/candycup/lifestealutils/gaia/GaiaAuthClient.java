package dev.candycup.lifestealutils.gaia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import dev.candycup.lifestealutils.mixin.ClientHandshakePacketListenerImplAuthInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * Handles Gaia authentication using a simplified flow.
 * <p>
 * The client generates a random server ID prefixed with 'lsu-gaia', verifies it with Mojang,
 * and sends it to Gaia's handshake endpoint. Gaia confirms the details with Mojang
 * to get the actual user who verified, then returns a JWT token.
 */
public final class GaiaAuthClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/gaia-auth");
    private static final Gson GSON = new GsonBuilder().create();
    private static final String SERVER_ID_PREFIX = "lsu-gaia-";
    private static final String TOKEN_MISSING_MESSAGE = "gaia auth response missing token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private GaiaAuthClient() {
    }

    /**
     * Confirms the client handshake against the Gaia auth service.
     * <p>
     * This method sends the server ID that was verified with Mojang to Gaia for confirmation.
     *
     * @param serverId the server id that was verified with Mojang
     * @param username the client's username
     * @param playerId the client's uuid
     * @return true if the request succeeded, false otherwise
     */
    public static boolean confirmHandshake(String serverId, String username, UUID playerId) {
        ConfirmHandshakePayload payload = new ConfirmHandshakePayload(serverId, username);
        String json = GSON.toJson(payload);
        NetworkUtilsController.HttpResult result = GaiaApiClient.getInstance().auth().confirmHandshake(json);
        if (!result.success()) {
            LOGGER.warn("Gaia login failed while confirming the handshake with Gaia: {}", result.error());
            return false;
        }

        ConfirmHandshakeResponse response = parseConfirmHandshakeResponse(result.body());
        if (response == null || !response.success()) {
            LOGGER.warn("Gaia login handshake was rejected: {}", response == null ? "invalid response" : response.error());
            return false;
        }

        if (response.token() == null || response.token().isBlank()) {
            LOGGER.error("Gaia login failed: {}", TOKEN_MISSING_MESSAGE);
            return false;
        }

        GaiaAuthTokenStore.saveToken(username, response.token());
        LOGGER.info("gaia auth confirmed handshake for {} ({})", username, playerId);
        return true;
    }

    /**
     * Executes a background Gaia handshake confirmation on startup.
     * <p>
     * This generates a random server ID prefixed with 'lsu-gaia', verifies it with Mojang's
     * session servers, and then sends it to Gaia for confirmation.
     *
     * @param username the client's username
     * @param playerId the client's uuid
     * @return a future containing the confirmation result
     */
    public static CompletableFuture<Boolean> confirmHandshakeOnStartup(String username, UUID playerId) {
        CompletableFuture<Boolean> login = CompletableFuture.supplyAsync(() -> {
            if (GaiaAuthTokenStore.hasValidToken(username, playerId)) {
                LOGGER.debug("gaia auth using cached token for {}", username);
                return true;
            }

            LOGGER.info("gaia auth starting handshake confirmation");

            // generate a random server id with the lsu-gaia prefix
            // the client generates the server id themselves to avoid security issues
            // where the server could technically trick the client into verifying
            // arbitrary connection attempts
            String serverId = generateServerId();
            LOGGER.debug("gaia auth generated server id: {}", serverId);

            // authenticate with mojang using the generated server id
            if (!authenticateWithMojang(serverId)) {
                LOGGER.warn("gaia auth: mojang authentication failed");
                return false;
            }

            LOGGER.info("gaia auth: mojang authentication succeeded, calling confirmHandshake");
            boolean result = confirmHandshake(serverId, username, playerId);
            LOGGER.info("gaia auth: confirmHandshake result: {}", result);
            return result;
        });

        // Async exceptions otherwise bypass the callers' thenAccept handlers and leave no
        // evidence when another mixin breaks the vanilla authentication invoker at runtime.
        return login.whenComplete((success, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Gaia login failed with an unexpected error", throwable);
            }
        });
    }

    /**
     * Generates a random server ID prefixed with 'lsu-gaia'.
     * <p>
     * The prefix ensures this is identifiable as an unofficial server ID generated
     * specifically for Gaia authentication, not an actual server.
     *
     * @return a server id string prefixed with 'lsu-gaia-'
     */
    private static String generateServerId() {
        byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);

        // convert to a hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }

        return SERVER_ID_PREFIX + sb;
    }

    /**
     * Authenticates with Mojang's session servers using the provided server ID.
     * <p>
     * This uses Minecraft's built-in authentication method via mixin invoker to avoid
     * directly accessing the user's access token (which would be blocked by security measures).
     * Mojang will record this join, which Gaia can later verify.
     *
     * @param serverId the server id to authenticate with
     * @return true if authentication succeeded, false otherwise
     */
    private static boolean authenticateWithMojang(String serverId) {
        ClientHandshakePacketListenerImpl listener = createHandshakeListener();
        if (listener == null) {
            LOGGER.error("Gaia login failed because the vanilla handshake listener could not be created");
            return false;
        }

        ClientHandshakePacketListenerImplAuthInvoker invoker = (ClientHandshakePacketListenerImplAuthInvoker) listener;
        Component result = invoker.lifestealutils$authenticateServer(serverId);
        if (result != null) {
            LOGGER.warn("Gaia login was rejected by Mojang's session service: {}", result.getString());
            return false;
        }

        LOGGER.info("gaia auth joined mojang session for server id {}", serverId);
        return true;
    }

    /**
     * Creates a handshake listener for authentication purposes.
     * <p>
     * This is used to invoke the vanilla authentication method without directly
     * accessing sensitive authentication tokens.
     *
     * @return a handshake listener instance, or null if creation failed
     */
    private static ClientHandshakePacketListenerImpl createHandshakeListener() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Connection connection = new Connection(PacketFlow.CLIENTBOUND);
            if (!prepareViaFabricPlusConnection(connection)) {
                return null;
            }

            //? if >1.21.8 {
            return ClientHandshakePacketListenerImplAuthInvoker.lifestealutils$create(
                    connection, minecraft, null, null, false, null, component -> {
                    }, null, null);
            //?} else {
         /*return ClientHandshakePacketListenerImplAuthInvoker.lifestealutils$create(
                 connection, minecraft, null, null, false, null, component -> {
                 }, null);
         *///?}
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.error("Gaia login failed while creating the vanilla handshake listener", exception);
            return null;
        }
    }

    /**
     * Gives ViaFabricPlus the protocol metadata it normally installs while opening a socket.
     * Gaia's authentication connection never opens a socket, but ViaFabricPlus still reads
     * this value from its authentication mixin and otherwise dereferences {@code null}.
     */
    private static boolean prepareViaFabricPlusConnection(Connection connection) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("viafabricplus")) {
            return true;
        }

        try {
            Class<?> connectionAccess = Class.forName("com.viaversion.viafabricplus.injection.access.core.IConnection");
            Class<?> protocolTranslator = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
            Class<?> protocolVersion = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            Field nativeVersionField = protocolTranslator.getField("NATIVE_VERSION");
            Object nativeVersion = nativeVersionField.get(null);
            Method setTargetVersion = connectionAccess.getMethod(
                    "viaFabricPlus$setTargetVersion",
                    protocolVersion
            );
            setTargetVersion.invoke(connection, nativeVersion);
            LOGGER.debug("Prepared Gaia authentication connection for ViaFabricPlus using its native protocol");
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Gaia login could not initialize ViaFabricPlus compatibility metadata", exception);
            return false;
        }
    }

    /**
     * Parses the confirm handshake response from Gaia.
     *
     * @param json the response body as JSON
     * @return the parsed response, or null if parsing failed
     */
    private static ConfirmHandshakeResponse parseConfirmHandshakeResponse(String json) {
        try {
            return GSON.fromJson(json, ConfirmHandshakeResponse.class);
        } catch (Exception e) {
            LOGGER.error("Gaia login failed to parse the handshake response", e);
            return null;
        }
    }

    /**
     * Payload for Gaia confirm handshake requests.
     *
     * @param serverId the server id that was verified with Mojang
     * @param username the player's username
     */
    public record ConfirmHandshakePayload(String serverId, String username) {
    }

    /**
     * Response from Gaia's confirm handshake endpoint.
     *
     * @param success whether the handshake confirmation was successful
     * @param token   the JWT token if successful
     * @param error   the error message if unsuccessful
     */
    private record ConfirmHandshakeResponse(boolean success, String token, String error) {
    }
}
