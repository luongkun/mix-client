package mix.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MicrosoftAuth {
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String SCOPE = "XboxLive.signin offline_access";

    private MicrosoftAuth() {
    }

    public static final class Session {
        public String name;
        public String uuid;
        public String accessToken;
        public String refreshToken;
        public long expiresAt;
    }

    public static final class DeviceCode {
        public String deviceCode;
        public String userCode;
        public String verificationUri;
        public int interval;
        public long expiresIn;
    }

    public static DeviceCode requestDeviceCode(String clientId) throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("scope", SCOPE);
        JsonObject response = postForm(DEVICE_CODE_URL, form, true);
        DeviceCode code = new DeviceCode();
        code.deviceCode = response.get("device_code").getAsString();
        code.userCode = response.get("user_code").getAsString();
        code.verificationUri = response.get("verification_uri").getAsString();
        code.interval = response.has("interval") ? response.get("interval").getAsInt() : 5;
        code.expiresIn = response.has("expires_in") ? response.get("expires_in").getAsLong() : 900L;
        return code;
    }

    public static Session loginWithDeviceCode(String clientId, DeviceCode code, Consumer<String> status) throws Exception {
        long deadline = System.currentTimeMillis() + code.expiresIn * 1000L;
        int interval = Math.max(1, code.interval);

        while (System.currentTimeMillis() < deadline) {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            form.put("client_id", clientId);
            form.put("device_code", code.deviceCode);
            JsonObject response = postForm(TOKEN_URL, form, false);

            if (response.has("access_token")) {
                return finishAuth(response, null, null, status);
            }
            String error = response.has("error") ? response.get("error").getAsString() : "unknown_error";
            if ("authorization_pending".equals(error)) {
                Thread.sleep(interval * 1000L);
            } else if ("slow_down".equals(error)) {
                interval += 5;
                Thread.sleep(interval * 1000L);
            } else if ("expired_token".equals(error)) {
                throw new AuthException("Device code expired, try again.");
            } else {
                String description = response.has("error_description")
                        ? response.get("error_description").getAsString() : error;
                throw new AuthException("Microsoft login failed: " + description);
            }
        }
        throw new AuthException("Microsoft login timed out.");
    }

    public static Session refresh(Session old, String clientId) throws Exception {
        if (old == null || old.refreshToken == null || old.refreshToken.isEmpty()) {
            throw new AuthException("No Microsoft refresh token. Log in again.");
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId);
        form.put("refresh_token", old.refreshToken);
        form.put("scope", SCOPE);
        JsonObject response = postForm(TOKEN_URL, form, true);
        return finishAuth(response, old.name, old.uuid, message -> {
        });
    }

    private static Session finishAuth(JsonObject tokenResponse, String fallbackName, String fallbackUuid,
                                      Consumer<String> status) throws Exception {
        String msAccessToken = tokenResponse.get("access_token").getAsString();
        String refreshToken = tokenResponse.has("refresh_token")
                ? tokenResponse.get("refresh_token").getAsString() : null;

        status.accept("Authenticating with Xbox Live...");
        JsonObject xblRequest = new JsonObject();
        JsonObject xblProperties = new JsonObject();
        xblProperties.addProperty("AuthMethod", "RPS");
        xblProperties.addProperty("SiteName", "user.auth.xboxlive.com");
        xblProperties.addProperty("RpsTicket", "d=" + msAccessToken);
        xblRequest.add("Properties", xblProperties);
        xblRequest.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblRequest.addProperty("TokenType", "JWT");
        JsonObject xblResponse = postJson(XBOX_AUTH_URL, xblRequest, null);
        String xblToken = xblResponse.get("Token").getAsString();
        String userHash = xblResponse.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString();

        status.accept("Authorizing with XSTS...");
        JsonObject xstsRequest = new JsonObject();
        JsonObject xstsProperties = new JsonObject();
        xstsProperties.addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(xblToken);
        xstsProperties.add("UserTokens", userTokens);
        xstsRequest.add("Properties", xstsProperties);
        xstsRequest.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsRequest.addProperty("TokenType", "JWT");
        JsonObject xstsResponse = postJson(XSTS_URL, xstsRequest, null);
        if (!xstsResponse.has("Token")) {
            throw new AuthException(xstsError(xstsResponse));
        }
        String xstsToken = xstsResponse.get("Token").getAsString();

        status.accept("Logging into Minecraft services...");
        JsonObject loginRequest = new JsonObject();
        loginRequest.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
        JsonObject loginResponse = postJson(MC_LOGIN_URL, loginRequest, null);
        if (!loginResponse.has("access_token")) {
            throw new AuthException("Minecraft login rejected: " + loginResponse);
        }
        long expiresIn = loginResponse.has("expires_in") ? loginResponse.get("expires_in").getAsLong() : 86400L;

        Session session = new Session();
        session.accessToken = loginResponse.get("access_token").getAsString();
        session.refreshToken = refreshToken;
        session.expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn;

        try {
            JsonObject profile = getJson(MC_PROFILE_URL, session.accessToken);
            if (profile.has("id") && profile.has("name")) {
                session.uuid = canonicalUuid(profile.get("id").getAsString());
                session.name = profile.get("name").getAsString();
            }
        } catch (Exception ignored) {
        }
        if (session.uuid == null) {
            session.uuid = fallbackUuid;
        }
        if (session.name == null) {
            session.name = fallbackName;
        }
        if (session.name == null) {
            session.name = "Player";
        }
        return session;
    }

    private static String canonicalUuid(String id) {
        if (id == null) {
            return null;
        }
        String value = id.replace("-", "");
        if (value.length() != 32) {
            return id;
        }
        return value.substring(0, 8) + "-"
                + value.substring(8, 12) + "-"
                + value.substring(12, 16) + "-"
                + value.substring(16, 20) + "-"
                + value.substring(20);
    }

    private static String xstsError(JsonObject response) {
        if (response.has("XErr")) {
            long error = response.get("XErr").getAsLong();
            if (error == 2148916233L) {
                return "No Xbox account linked. Create one at xbox.com.";
            }
            if (error == 2148916235L) {
                return "Xbox Live is not available in this region.";
            }
            if (error == 2148916238L) {
                return "Child account. Add it to a Microsoft family first.";
            }
            return "XSTS error: " + error;
        }
        return "XSTS authorization failed.";
    }

    private static JsonObject postForm(String url, Map<String, String> form, boolean failOnHttpError) throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return request(url, "POST", "application/x-www-form-urlencoded", body.toString(), null, failOnHttpError);
    }

    private static JsonObject postJson(String url, JsonObject body, Map<String, String> headers) throws Exception {
        return request(url, "POST", "application/json", body.toString(), headers, true);
    }

    private static JsonObject getJson(String url, String bearer) throws Exception {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearer);
        return request(url, "GET", null, null, headers, true);
    }

    private static JsonObject request(String url, String method, String contentType, String body,
                                      Map<String, String> headers, boolean failOnHttpError) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("User-Agent", "MixClient/1.0");
        connection.setRequestProperty("Accept", "application/json");
        if (contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        if (body != null) {
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String response = read(stream);
        JsonObject json = response == null || response.isEmpty() ? new JsonObject()
                : new JsonParser().parse(response).getAsJsonObject();
        if (code >= 400 && failOnHttpError) {
            String description = json.has("error_description")
                    ? json.get("error_description").getAsString()
                    : (json.has("error") ? json.get("error").getAsString() : "HTTP " + code);
            throw new AuthException(description);
        }
        return json;
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public static final class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}
