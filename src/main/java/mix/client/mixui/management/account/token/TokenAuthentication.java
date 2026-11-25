package mix.client.mixui.management.account.token;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mix.client.mixui.Mix;
import mix.client.mixui.injection.interfaces.IMixinMinecraft;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.AccountManager;
import mix.client.mixui.management.account.AccountType;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.utils.ImageUtils;
import mix.client.mixui.utils.Multithreading;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

public class TokenAuthentication {

	public static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

	public interface Callback {
		void onResult(Account account, String error);
	}

	public void loginWithToken(final String rawToken, final Callback callback) {

		final String token = normalizeToken(rawToken);

		if(token.isEmpty()) {
			if(callback != null) {
				callback.onResult(null, "Please paste a token first");
			}
			return;
		}

		Multithreading.runAsync(() -> {

			try {

				HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL).openConnection();

				connection.setRequestMethod("GET");
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(10000);
				connection.setRequestProperty("Authorization", "Bearer " + token);
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("User-Agent", "MixClient/7.1.2");

				MixLogger.info("Token login: tokenChars=" + token.length());

				int code = connection.getResponseCode();
				InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
				String body = stream == null ? "" : readString(stream);

				MixLogger.info("Token login: HTTP " + code);

				if(code == 401 || code == 403) {
					MixLogger.error("Token login rejected: HTTP " + code + " body=" + snippet(body));
					fail(callback, "Invalid or expired token");
					return;
				}

				if(code != 200) {
					MixLogger.error("Token login failed: HTTP " + code + " body=" + snippet(body));
					fail(callback, "Authentication failed (HTTP " + code + ")");
					return;
				}

				JsonObject profile = new JsonParser().parse(body).getAsJsonObject();

				if(profile == null || !profile.has("name") || !profile.has("id")) {
					fail(callback, "Unexpected response from Mojang API");
					return;
				}

				final String name = profile.get("name").getAsString();
				final String uuid = formatUuid(profile.get("id").getAsString());
				final Account account = new Account(name, uuid, token, AccountType.TOKEN);

				Minecraft.getMinecraft().addScheduledTask(() -> {
					applyAccount(account);
				});

				downloadSkin(account, profile);

				if(callback != null) {
					callback.onResult(account, null);
				}

			} catch (Exception e) {
				MixLogger.error("Failed to login with token", e);
				fail(callback, "Login failed: " + e.getClass().getSimpleName());
			}
		});
	}

	private String normalizeToken(String rawToken) {

		if(rawToken == null) {
			return "";
		}

		String token = rawToken.trim();

		if(token.toLowerCase(java.util.Locale.ROOT).startsWith("bearer ")) {
			token = token.substring(7).trim();
		}

		return token.replaceAll("\\s", "");
	}

	private String snippet(String body) {

		if(body == null) {
			return "";
		}

		String text = body.trim();

		if(text.length() > 300) {
			text = text.substring(0, 300) + "...";
		}

		return text;
	}

	// Fetches the profile behind a launcher session token and caches only the head image.
	// Used to show the real avatar on the main menu before the player adds an account.
	public void downloadHeadOnly(final String rawToken) {

		final String token = normalizeToken(rawToken);

		if(token.length() < 100) {
			return;
		}

		Multithreading.runAsync(() -> {

			try {

				HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL).openConnection();

				connection.setRequestMethod("GET");
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(10000);
				connection.setRequestProperty("Authorization", "Bearer " + token);
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("User-Agent", "MixClient/7.1.2");

				if(connection.getResponseCode() != 200) {
					return;
				}

				InputStream stream = connection.getInputStream();
				String body = stream == null ? "" : readString(stream);
				JsonObject profile = new JsonParser().parse(body).getAsJsonObject();

				if(profile == null || !profile.has("name") || !profile.has("id")) {
					return;
				}

				Account account = new Account(profile.get("name").getAsString(), formatUuid(profile.get("id").getAsString()), token, AccountType.TOKEN);

				downloadSkin(account, profile);

			} catch (Exception e) {
				MixLogger.error("Failed to cache the session head", e);
			}
		});
	}

	private void applyAccount(Account account) {

		Minecraft mc = Minecraft.getMinecraft();
		AccountManager accountManager = Mix.getInstance().getAccountManager();

		((IMixinMinecraft) mc).setSession(new Session(account.getName(), account.getUuid(), account.getRefreshToken(), "mojang"));

		Account existing = accountManager.getAccountByName(account.getName());

		if(existing != null) {
			accountManager.getAccounts().remove(existing);
		}

		accountManager.getAccounts().add(account);
		accountManager.setCurrentAccount(account);
		accountManager.save();
	}

	private void downloadSkin(Account account, JsonObject profile) {

		try {

			if(!profile.has("skins") || !profile.get("skins").isJsonArray()) {
				return;
			}

			JsonArray skins = profile.getAsJsonArray("skins");
			String url = null;

			for(JsonElement element : skins) {

				JsonObject skin = element.getAsJsonObject();

				if(skin.has("state") && "ACTIVE".equalsIgnoreCase(skin.get("state").getAsString()) && skin.has("url")) {
					url = skin.get("url").getAsString();
					break;
				}
			}

			if(url == null && skins.size() > 0 && skins.get(0).getAsJsonObject().has("url")) {
				url = skins.get(0).getAsJsonObject().get("url").getAsString();
			}

			if(url == null) {
				return;
			}

			FileManager fileManager = Mix.getInstance().getFileManager();
			File skinFile = new File(fileManager.getCacheDir(), "skin/" + account.getName() + ".png");
			File headFile = new File(fileManager.getCacheDir(), "head/" + account.getName() + ".png");

			if(skinFile.getParentFile() != null && !skinFile.getParentFile().exists()) {
				skinFile.getParentFile().mkdirs();
			}

			if(headFile.getParentFile() != null && !headFile.getParentFile().exists()) {
				headFile.getParentFile().mkdirs();
			}

			downloadFile(url, skinFile);

			BufferedImage rawImage = ImageIO.read(skinFile);

			if(rawImage == null) {
				return;
			}

			BufferedImage headImage = ImageUtils.scissor(rawImage, 8, 8, 8, 8);
			BufferedImage layerImage = ImageUtils.scissor(rawImage, 40, 8, 8, 8);
			BufferedImage combineImage = ImageUtils.combine(headImage, layerImage);

			ImageIO.write(ImageUtils.resize(combineImage, 128, 128), "png", headFile);

			account.setSkinFile(skinFile);

		} catch (Exception e) {
			MixLogger.error("Failed to download skin", e);
		}
	}

	private void downloadFile(String url, File outputFile) throws Exception {

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

		connection.setConnectTimeout(10000);
		connection.setReadTimeout(15000);
		connection.setRequestProperty("User-Agent", "MixClient/7.1.2");

		try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(outputFile)) {

			byte[] buffer = new byte[8192];
			int read;

			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
	}

	private String readString(InputStream stream) throws Exception {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

			StringBuilder builder = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			return builder.toString();
		}
	}

	private String formatUuid(String id) {

		if(id == null) {
			return "";
		}

		String raw = id.replace("-", "");

		if(raw.length() != 32) {
			return id;
		}

		return raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
	}

	private void fail(Callback callback, String message) {
		if(callback != null) {
			callback.onResult(null, message);
		}
	}
}
