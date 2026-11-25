package mix.client.mixui.management.script;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mix.client.mixui.Mix;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.JsonUtils;
import mix.client.mixui.utils.ServerUtils;
import mix.client.mixui.utils.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

public class ScriptManager {

	private static final long REFRESH_INTERVAL = 1000L;
	private static final int MAX_LINES = 32;
	private static final int MAX_COLORS = 8;

	private static final String DEFAULT_FPS_FILE = "fps.json";
	private static final String DEFAULT_COORDS_FILE = "coords.json";
	private static final String DEFAULT_README_FILE = "README.txt";

	private static final String DEFAULT_README_CONTENT = "MixClient 脚本 HUD 使用说明\n"
			+ "==============================\n"
			+ "\n"
			+ "这个目录里每个 .json 文件就是一块 HUD。改完保存，游戏里会自动读取（最多 1 秒延迟）；\n"
			+ "如果没反应，按一下「脚本 HUD」设置里绑定的重载键（默认没绑，自己设一个），不用重启游戏。\n"
			+ "\n"
			+ "一、字段\n"
			+ "  name        显示名，只用来区分，随便写\n"
			+ "  icon        图标字形，留空字符串就是不显示图标\n"
			+ "  enabled     true / false，是否显示这块 HUD\n"
			+ "  background  true / false，是否画半透明黑底\n"
			+ "  x, y        这块 HUD 相对「脚本 HUD」模块原点的偏移（像素，会跟着缩放的倍率走）\n"
			+ "  scale       这块 HUD 自己的缩放，0.1 - 5.0\n"
			+ "  lines       行数组，一行一条，最多 32 行\n"
			+ "  colors      颜色数组，按行号循环取色，最多 8 个\n"
			+ "\n"
			+ "二、颜色写法\n"
			+ "  \"#RRGGBB\"        不透明，例如 \"#FF5555\"\n"
			+ "  \"#AARRGGBB\"      带透明度，例如 \"#80FFFFFF\" 是半透明白\n"
			+ "\n"
			+ "三、可用占位符（写在大括号里，运行时替换成当前数值）\n"
			+ "  {fps}       当前帧数\n"
			+ "  {cps}       最近 1 秒的左键点击次数\n"
			+ "  {ping}      延迟，毫秒\n"
			+ "  {x} {y} {z} 玩家坐标，取整\n"
			+ "  {name}      玩家名字\n"
			+ "  {biome}     当前生物群系\n"
			+ "  {dimension} Overworld / Nether / End\n"
			+ "  {time}      游戏内时间 HH:MM\n"
			+ "  {health}    当前血量\n"
			+ "  {armor}     护甲值\n"
			+ "  取不到的值会显示成 -\n"
			+ "\n"
			+ "四、例子：一行帧数 + 坐标\n"
			+ "  {\n"
			+ "    \"name\": \"Info\",\n"
			+ "    \"icon\": \"i\",\n"
			+ "    \"enabled\": true,\n"
			+ "    \"background\": true,\n"
			+ "    \"x\": 20.0,\n"
			+ "    \"y\": 20.0,\n"
			+ "    \"scale\": 1.0,\n"
			+ "    \"lines\": [\"FPS: {fps}\", \"XYZ: {x} {y} {z}\", \"HP: {health}\"],\n"
			+ "    \"colors\": [\"#FFFFFFFF\", \"#FFD866FF\", \"#FF6B6BFF\"]\n"
			+ "  }\n"
			+ "\n"
			+ "五、菜单里能调什么\n"
			+ "  模组菜单 -> 脚本分类 -> 脚本 HUD：\n"
			+ "    整体缩放     所有脚本一起放大缩小\n"
			+ "    行间距       每行之间额外加多少像素\n"
			+ "    显示背景     关掉就是纯文字\n"
			+ "    显示图标     关掉就是只有文字\n"
			+ "    重载脚本     绑一个按键，改完 JSON 按一下立即生效\n"
			+ "  在 HUD 编辑界面里可以拖动位置，位置会被保存。\n"
			+ "\n"
			+ "六、注意\n"
			+ "  · 文件名随便起，但必须以 .json 结尾，别的扩展名不会被读取\n"
			+ "  · JSON 写坏了不会崩游戏，控制台里会有一行 Failed to parse script xxx.json\n"
			+ "  · 这个 README.txt 不会被当成脚本，留着方便你随时查\n";


	private static final String[] PLACEHOLDERS = new String[] {
			"{fps}", "{cps}", "{ping}", "{x}", "{y}", "{z}", "{name}", "{biome}", "{dimension}", "{time}", "{health}", "{armor}"
	};

	private static final String DEFAULT_FPS_CONTENT = "{\n"
			+ "  \"name\": \"FPS\",\n"
			+ "  \"icon\": \"i\",\n"
			+ "  \"enabled\": true,\n"
			+ "  \"background\": true,\n"
			+ "  \"x\": 20.0,\n"
			+ "  \"y\": 20.0,\n"
			+ "  \"scale\": 1.0,\n"
			+ "  \"lines\": [\n"
			+ "    \"{fps} FPS\"\n"
			+ "  ],\n"
			+ "  \"colors\": [\"#FFFFFFFF\"]\n"
			+ "}\n";

	private static final String DEFAULT_COORDS_CONTENT = "{\n"
			+ "  \"name\": \"Coords\",\n"
			+ "  \"icon\": \"8\",\n"
			+ "  \"enabled\": true,\n"
			+ "  \"background\": true,\n"
			+ "  \"x\": 20.0,\n"
			+ "  \"y\": 44.0,\n"
			+ "  \"scale\": 1.0,\n"
			+ "  \"lines\": [\n"
			+ "    \"X: {x}\",\n"
			+ "    \"Y: {y}\",\n"
			+ "    \"Z: {z}\",\n"
			+ "    \"Biome: {biome}\",\n"
			+ "    \"Ping: {ping}ms\"\n"
			+ "  ],\n"
			+ "  \"colors\": [\"#FFFFFFFF\"]\n"
			+ "}\n";

	private static ScriptManager instance;

	private CopyOnWriteArrayList<ScriptEntry> scripts = new CopyOnWriteArrayList<ScriptEntry>();
	private final List<Long> leftClicks = new ArrayList<Long>();

	private File scriptDir;
	private long lastRefreshTime = -REFRESH_INTERVAL;
	private long lastSignature = Long.MIN_VALUE;
	private boolean loaded;

	public ScriptManager() {
		instance = this;
	}

	public static ScriptManager getInstance() {

		if(instance == null) {
			new ScriptManager().load();
		}

		return instance;
	}

	public void init() {
		load();
	}

	public void load() {

		if(loaded) {
			refreshNow();
			return;
		}

		loaded = true;

		try {

			scriptDir = new File(getMixDir(), "scripts");

			if(!scriptDir.exists() && !scriptDir.mkdirs() && !scriptDir.exists()) {
				MixLogger.error("Failed to create script directory " + scriptDir.getAbsolutePath());
				return;
			}

			writeDefaultScript(DEFAULT_FPS_FILE, DEFAULT_FPS_CONTENT);
			writeDefaultScript(DEFAULT_COORDS_FILE, DEFAULT_COORDS_CONTENT);
			writeDefaultScript(DEFAULT_README_FILE, DEFAULT_README_CONTENT);

		} catch(Exception e) {
			MixLogger.error("Failed to prepare script directory", e);
			return;
		}

		loadScripts();
	}

	/**
	 * 强制重读脚本目录。绑定到脚本 HUD 的重载按键上，玩家改完 json 不用重启游戏。
	 */
	public void reload() {

		if(!loaded || scriptDir == null) {
			load();
			return;
		}

		lastSignature = Long.MIN_VALUE;
		lastRefreshTime = System.currentTimeMillis();
		loadScripts();
	}

	public void refresh() {

		long now = System.currentTimeMillis();

		if(now - lastRefreshTime < REFRESH_INTERVAL) {
			return;
		}

		lastRefreshTime = now;

		if(!loaded || scriptDir == null) {
			load();
			return;
		}

		refreshNow();
	}

	private void refreshNow() {

		try {

			long signature = getDirectorySignature();

			if(signature != lastSignature) {
				lastSignature = signature;
				loadScripts();
			}

		} catch(Exception e) {
			MixLogger.error("Failed to refresh scripts", e);
		}
	}

	private void loadScripts() {

		try {

			File[] files = scriptDir == null ? null : scriptDir.listFiles();

			if(files == null) {
				return;
			}

			List<File> jsonFiles = new ArrayList<File>();

			for(File file : files) {

				if(file.isFile() && "json".equalsIgnoreCase(FileUtils.getExtension(file))) {
					jsonFiles.add(file);
				}
			}

			Collections.sort(jsonFiles, new Comparator<File>() {

				@Override
				public int compare(File first, File second) {
					return first.getName().compareToIgnoreCase(second.getName());
				}
			});

			CopyOnWriteArrayList<ScriptEntry> result = new CopyOnWriteArrayList<ScriptEntry>();

			for(File file : jsonFiles) {

				ScriptEntry entry = parseScript(file);

				if(entry == null) {
					continue;
				}

				result.add(entry);
			}

			scripts = result;
			lastSignature = getDirectorySignature();

			MixLogger.info("Scripts: loaded " + result.size() + " from " + scriptDir.getAbsolutePath());

		} catch(Exception e) {
			MixLogger.error("Failed to load scripts", e);
		}
	}

	private ScriptEntry parseScript(File file) {

		InputStreamReader reader = null;

		try {

			reader = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));

			JsonElement element = new JsonParser().parse(reader);

			if(element == null || !element.isJsonObject()) {
				MixLogger.error("Invalid script root object in " + file.getName());
				return null;
			}

			JsonObject jsonObject = element.getAsJsonObject();

			String name = JsonUtils.getStringProperty(jsonObject, "name", null);

			if(name == null || name.trim().isEmpty()) {
				name = file.getName();
			}

			String icon = JsonUtils.getStringProperty(jsonObject, "icon", null);

			if(icon != null && icon.isEmpty()) {
				icon = null;
			}

			boolean enabled = JsonUtils.getBooleanProperty(jsonObject, "enabled", true);
			boolean background = JsonUtils.getBooleanProperty(jsonObject, "background", true);

			float x = (float) JsonUtils.getDoubleProperty(jsonObject, "x", 20.0D);
			float y = (float) JsonUtils.getDoubleProperty(jsonObject, "y", 20.0D);
			float scale = (float) JsonUtils.getDoubleProperty(jsonObject, "scale", 1.0D);

			List<String> lines = new ArrayList<String>();

			for(JsonElement lineElement : JsonUtils.getArrayProperty(jsonObject, "lines")) {

				if(lines.size() >= MAX_LINES) {
					break;
				}

				if(lineElement == null || !lineElement.isJsonPrimitive()) {
					continue;
				}

				lines.add(lineElement.getAsJsonPrimitive().getAsString());
			}

			if(lines.isEmpty()) {
				lines.add(name);
			}

			List<Color> colors = new ArrayList<Color>();

			for(JsonElement colorElement : JsonUtils.getArrayProperty(jsonObject, "colors")) {

				if(colors.size() >= MAX_COLORS) {
					break;
				}

				Color color = parseColor(colorElement);

				if(color != null) {
					colors.add(color);
				}
			}

			return new ScriptEntry(name, icon, enabled, x, y, scale, background, lines, colors);

		} catch(Exception e) {
			MixLogger.error("Failed to parse script " + file.getName(), e);
			return null;
		} finally {

			if(reader != null) {

				try {
					reader.close();
				} catch(Exception e) {}
			}
		}
	}

	private Color parseColor(JsonElement element) {

		if(element == null || !element.isJsonPrimitive()) {
			return null;
		}

		String text = element.getAsJsonPrimitive().getAsString();

		if(text == null) {
			return null;
		}

		String value = text.trim();

		try {

			if(value.startsWith("#")) {
				value = value.substring(1);
			}

			if(value.length() == 8) {

				long argb = Long.parseLong(value, 16);
				int alpha = (int) ((argb >> 24) & 0xFF);
				int red = (int) ((argb >> 16) & 0xFF);
				int green = (int) ((argb >> 8) & 0xFF);
				int blue = (int) (argb & 0xFF);

				return new Color(red, green, blue, alpha);
			}

			if(value.length() == 6) {
				return new Color(Integer.parseInt(value, 16));
			}

		} catch(Exception e) {
			MixLogger.error("Failed to parse script color " + text);
		}

		return null;
	}

	private long getDirectorySignature() {

		long signature = 17L;
		File[] files = scriptDir == null ? null : scriptDir.listFiles();

		if(files == null) {
			return signature;
		}

		for(File file : files) {

			if(!file.isFile() || !"json".equalsIgnoreCase(FileUtils.getExtension(file))) {
				continue;
			}

			signature = (signature * 31L) + file.getName().hashCode();
			signature = (signature * 31L) + file.length();
			signature = (signature * 31L) + file.lastModified();
		}

		return signature;
	}

	private void writeDefaultScript(String fileName, String content) {

		try {

			File file = new File(scriptDir, fileName);

			if(file.exists()) {
				return;
			}

			OutputStreamWriter writer = null;

			try {

				writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
				writer.write(content);
				writer.flush();

			} finally {

				if(writer != null) {
					writer.close();
				}
			}

			MixLogger.info("Created default script " + fileName);

		} catch(Exception e) {
			MixLogger.error("Failed to write default script " + fileName, e);
		}
	}

	/**
	 * 把模板行里的占位符替换成当前数值，任何取不到的值替换成 "-"。
	 */
	public String replaceVariables(String template) {

		if(template == null) {
			return "";
		}

		if(template.indexOf('{') < 0) {
			return template;
		}

		String result = template;

		for(String placeholder : PLACEHOLDERS) {

			if(result.contains(placeholder)) {
				result = result.replace(placeholder, getVariableValue(placeholder));
			}
		}

		return result;
	}

	private String getVariableValue(String placeholder) {

		try {

			if("{fps}".equals(placeholder)) {
				return getFPS();
			}

			if("{cps}".equals(placeholder)) {
				return getCPS();
			}

			if("{ping}".equals(placeholder)) {
				return getPing();
			}

			if("{x}".equals(placeholder)) {
				return getPosition(0);
			}

			if("{y}".equals(placeholder)) {
				return getPosition(1);
			}

			if("{z}".equals(placeholder)) {
				return getPosition(2);
			}

			if("{name}".equals(placeholder)) {
				return getName();
			}

			if("{biome}".equals(placeholder)) {
				return getBiome();
			}

			if("{dimension}".equals(placeholder)) {
				return getDimension();
			}

			if("{time}".equals(placeholder)) {
				return getTime();
			}

			if("{health}".equals(placeholder)) {
				return getHealth();
			}

			if("{armor}".equals(placeholder)) {
				return getArmor();
			}

		} catch(Exception e) {
			MixLogger.error("Failed to read script variable " + placeholder, e);
			return "-";
		}

		return "-";
	}

	private String getFPS() {

		int fps = Minecraft.getDebugFPS();

		return fps > 0 ? String.valueOf(fps) : "-";
	}

	/**
	 * 记录一次左键点击，供 {cps} 占位符统计。由 ScriptHudMod 的 EventClickMouse 调用。
	 */
	public void addLeftClick() {
		leftClicks.add(Long.valueOf(System.currentTimeMillis()));
	}

	private String getCPS() {

		long now = System.currentTimeMillis();

		for(int i = leftClicks.size() - 1; i >= 0; i--) {

			if(now - leftClicks.get(i).longValue() > 1000L) {
				leftClicks.remove(i);
			}
		}

		return String.valueOf(leftClicks.size());
	}

	private String getHealth() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null) {
			return "-";
		}

		return String.format("%.1f", Float.valueOf(mc.thePlayer.getHealth()));
	}

	private String getArmor() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null) {
			return "-";
		}

		return String.valueOf(mc.thePlayer.getTotalArmorValue());
	}

	private String getPing() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null) {
			return "-";
		}

		if(mc.getNetHandler() != null) {

			NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());

			if(info != null && info.getResponseTime() > 0) {
				return String.valueOf(info.getResponseTime());
			}
		}

		int ping = ServerUtils.getPing();

		return ping > 0 ? String.valueOf(ping) : "-";
	}

	private String getPosition(int type) {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null) {
			return "-";
		}

		if(type == 0) {
			return String.format("%.0f", mc.thePlayer.posX);
		}

		if(type == 1) {
			return String.format("%.0f", mc.thePlayer.posY);
		}

		return String.format("%.0f", mc.thePlayer.posZ);
	}

	private String getName() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null || mc.thePlayer.getName() == null || mc.thePlayer.getName().isEmpty()) {
			return "-";
		}

		return mc.thePlayer.getName();
	}

	private String getBiome() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null || mc.theWorld == null) {
			return "-";
		}

		try {

			BlockPos pos = new BlockPos(mc.thePlayer);
			BiomeGenBase biome = mc.theWorld.getBiomeGenForCoords(pos);

			if(biome != null && biome.biomeName != null) {
				return biome.biomeName;
			}

		} catch(Exception e) {

			try {

				BlockPos pos = new BlockPos(mc.thePlayer);
				Chunk chunk = mc.theWorld.getChunkFromBlockCoords(pos);

				if(chunk != null) {
					return chunk.getBiome(pos, mc.theWorld.getWorldChunkManager()).biomeName;
				}

			} catch(Exception e2) {
				MixLogger.error("Failed to read script biome", e2);
			}
		}

		return "-";
	}

	private String getDimension() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.thePlayer == null) {
			return "-";
		}

		int dimension = mc.thePlayer.dimension;

		if(dimension == 0) {
			return "Overworld";
		}

		if(dimension == -1) {
			return "Nether";
		}

		if(dimension == 1) {
			return "End";
		}

		return String.valueOf(dimension);
	}

	private String getTime() {

		Minecraft mc = Minecraft.getMinecraft();

		if(mc == null || mc.theWorld == null) {
			return "-";
		}

		long worldTime = mc.theWorld.getWorldTime();
		long dayTime = ((worldTime % 24000L) + 24000L) % 24000L;
		int hour = (int) ((dayTime / 1000L) + 6L) % 24;
		int minute = (int) ((dayTime % 1000L) * 60L / 1000L);

		return String.format("%02d:%02d", hour, minute);
	}

	private File getMixDir() {

		Mix mix = Mix.getInstance();

		if(mix != null && mix.getFileManager() != null) {
			FileManager fileManager = mix.getFileManager();

			if(fileManager.getMixDir() != null) {
				return fileManager.getMixDir();
			}
		}

		return new File(Minecraft.getMinecraft().mcDataDir, "mixclient");
	}

	public File getScriptDir() {
		return scriptDir;
	}

	public CopyOnWriteArrayList<ScriptEntry> getScripts() {
		return scripts;
	}

	public void setScripts(CopyOnWriteArrayList<ScriptEntry> scripts) {
		this.scripts = scripts;
	}

	public static String getDefaultIcon() {
		return Icon.FILE;
	}
}
