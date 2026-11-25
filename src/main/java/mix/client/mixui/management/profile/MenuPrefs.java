package mix.client.mixui.management.profile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import mix.client.mixui.Mix;
import mix.client.mixui.logger.MixLogger;

/**
 * Lightweight per-install menu preferences (UI scale + favourite mods).
 *
 * Kept in its own file instead of the profile json so the menu can persist a
 * change immediately: mixclient/menu-prefs.json
 */
public class MenuPrefs {

	private static final float MIN_MENU_SCALE = 0.5F;
	private static final float MAX_MENU_SCALE = 2.0F;

	private static MenuPrefs instance;

	private final ArrayList<String> favorites = new ArrayList<String>();

	private float menuScale = 1.0F;
	private boolean loaded;

	public static MenuPrefs getInstance() {

		if(instance == null) {
			instance = new MenuPrefs();
			instance.load();
		}

		return instance;
	}

	private File getPrefsFile() {
		return new File(Mix.getInstance().getFileManager().getMixDir(), "menu-prefs.json");
	}

	private void load() {

		if(loaded) {
			return;
		}

		loaded = true;

		File file = getPrefsFile();

		if(!file.exists()) {
			return;
		}

		try(FileReader reader = new FileReader(file)) {

			JsonObject json = new Gson().fromJson(reader, JsonObject.class);

			if(json == null) {
				return;
			}

			if(json.has("MenuScale") && json.get("MenuScale").isJsonPrimitive()) {
				menuScale = clampScale(json.get("MenuScale").getAsFloat());
			}

			if(json.has("Favorites") && json.get("Favorites").isJsonArray()) {

				JsonArray array = json.getAsJsonArray("Favorites");

				for(JsonElement element : array) {

					if(element == null || element.isJsonNull()) {
						continue;
					}

					String nameKey = element.getAsString();

					if(nameKey != null && !nameKey.isEmpty() && !favorites.contains(nameKey)) {
						favorites.add(nameKey);
					}
				}
			}

		} catch(Exception e) {
			MixLogger.error("Failed to load menu prefs", e);
		}
	}

	public void save() {

		try {
			File file = getPrefsFile();
			File dir = file.getParentFile();

			if(dir != null && !dir.exists()) {
				dir.mkdirs();
			}
		} catch(Exception e) {
			MixLogger.error("Failed to prepare menu prefs dir", e);
		}

		try(FileWriter writer = new FileWriter(getPrefsFile())) {

			JsonObject json = new JsonObject();

			json.addProperty("MenuScale", menuScale);

			JsonArray array = new JsonArray();

			for(String favorite : favorites) {
				// MC 1.8.9 ships Gson 2.2.4, which has no JsonArray.add(String).
				array.add(new JsonPrimitive(favorite));
			}

			json.add("Favorites", array);

			writer.write(new Gson().toJson(json));

		} catch(Exception e) {
			MixLogger.error("Failed to save menu prefs", e);
		}
	}

	private static float clampScale(float scale) {
		return Math.max(MIN_MENU_SCALE, Math.min(MAX_MENU_SCALE, scale));
	}

	public float getMenuScale() {
		return menuScale;
	}

	public void setMenuScale(float scale) {

		float clamped = clampScale(scale);

		if(Math.abs(clamped - menuScale) < 0.001F) {
			return;
		}

		menuScale = clamped;
		save();
	}

	public boolean isFavorite(String nameKey) {
		return nameKey != null && favorites.contains(nameKey);
	}

	public void setFavorite(String nameKey, boolean favorite) {

		if(nameKey == null) {
			return;
		}

		if(favorite) {

			if(!favorites.contains(nameKey)) {
				favorites.add(nameKey);
				save();
			}

		}else if(favorites.remove(nameKey)) {
			save();
		}
	}

	public void toggleFavorite(String nameKey) {
		setFavorite(nameKey, !isFavorite(nameKey));
	}

	public List<String> getFavorites() {
		return favorites;
	}

	public int getFavoriteCount() {
		return favorites.size();
	}
}
