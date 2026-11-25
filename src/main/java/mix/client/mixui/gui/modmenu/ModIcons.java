package mix.client.mixui.gui.modmenu;

import java.util.HashMap;

import mix.client.mixui.management.nanovg.font.MaterialIcons;

/**
 * 模组卡片图标注册表 —— MixClient v8.0 的那套 Material Symbols Rounded 码点。
 *
 * <p>7.1.2 原版用一个自制单字符 Icon.ttf，图标大多是「形状相近就凑合」的方块/虚线，
 * 分不清「目标信息」和「生命显示」。v8.0 改用 Google 的 Material Symbols Rounded，
 * 每个模组在手写清单里挑一个语义贴切的字形（{@link MaterialIcons}）。</p>
 *
 * <p>v8.0 自己的清单只有 23 个模组，这里 132 个是逐个按功能含义映射的：
 * v8.0 有对应项的直接沿用（如 Fullbright→LIGHTBULB、Zoom→ZOOM_IN、
 * Freelook→_360、OldAnimations→ANIMATION、ProjectileTrail→TRAIL_LENGTH），
 * 其余的按功能挑 Material Symbols 里语义最近的字形。</p>
 *
 * <p>查表顺序：先按类 simple name 精确命中，再退到 {@code infer} 里的关键词启发，
 * 最后才按分类给默认图标 —— 这样以后新加的模组也不会顶着一个不对的图标。</p>
 */
public final class ModIcons {

	private static final HashMap<String, String> ICONS = new HashMap<String, String>();

	static {

		// HUD 与信息显示

		put("ArmorStatusMod", MaterialIcons.SHIELD);
		put("ArrayListMod", MaterialIcons.FORMAT_LIST_BULLETED);
		put("HealthDisplayMod", MaterialIcons.FAVORITE);
		put("BossHealthMod", MaterialIcons.MONITOR_HEART);
		put("PotionStatusMod", MaterialIcons.SCIENCE);
		put("PotionCounterMod", MaterialIcons.SCIENCE);
		put("ScoreboardMod", MaterialIcons.SCOREBOARD);
		put("TabEditorMod", MaterialIcons.TABLE_ROWS);
		put("PlayerListMod", MaterialIcons.LIST);
		put("PlayerCounterMod", MaterialIcons.GROUP);
		put("PlayerDisplayMod", MaterialIcons.PERSON);
		put("PlayTimeDisplayMod", MaterialIcons.TIMER);
		put("SessionInfoMod", MaterialIcons.TIMER);
		put("MemoryUsageMod", MaterialIcons.MEMORY);
		put("PingDisplayMod", MaterialIcons.WIFI);
		put("PitchDisplayMod", MaterialIcons.HEIGHT);
		put("YawDisplayMod", MaterialIcons.COMPASS_CALIBRATION);
		put("ServerIPDisplayMod", MaterialIcons.LAN);
		put("CPSDisplayMod", MaterialIcons.MOUSE);
		put("KeystrokesMod", MaterialIcons.KEYBOARD);
		put("MouseStrokesMod", MaterialIcons.MOUSE);
		put("GameModeDisplayMod", MaterialIcons.SPORTS_ESPORTS);
		put("ClockMod", MaterialIcons.SCHEDULE);
		put("StopwatchMod", MaterialIcons.TIMER);
		put("CalendarMod", MaterialIcons.CALENDAR_MONTH);
		put("DayCounterMod", MaterialIcons.WB_SUNNY);
		put("FPSDisplayMod", MaterialIcons.MONITOR);
		put("ComboCounterMod", MaterialIcons.LOCAL_FIRE_DEPARTMENT);
		put("CoordsMod", MaterialIcons.MY_LOCATION);
		put("CompassMod", MaterialIcons.EXPLORE);
		put("SpeedometerMod", MaterialIcons.SPEED);
		put("ReachDisplayMod", MaterialIcons.STRAIGHTEN);
		put("WeatherDisplayMod", MaterialIcons.CLOUD);
		put("ItemInfoMod", MaterialIcons.INFO);
		put("BlockInfoMod", MaterialIcons.VIEW_IN_AR);
		put("PackDisplayMod", MaterialIcons.FOLDER);
		put("InventoryDisplayMod", MaterialIcons.INVENTORY_2);
		put("PlayerPredicatorMod", MaterialIcons.ONLINE_PREDICTION);
		put("UHCOverlayMod", MaterialIcons.MONITOR_HEART);
		put("TNTTimerMod", MaterialIcons.LOCAL_FIRE_DEPARTMENT);
		put("SoundSubtitlesMod", MaterialIcons.SUBTITLES);
		put("MusicInfoMod", MaterialIcons.MUSIC_NOTE);
		put("MinimapMod", MaterialIcons.MAP);
		put("WaypointMod", MaterialIcons.PIN_DROP);
		put("BreadcrumbsMod", MaterialIcons.ROUTE);

		// 移动、视角与手感

		put("ToggleSprintMod", MaterialIcons.KEYBOARD_ARROW_UP);
		put("ToggleSneakMod", MaterialIcons.KEYBOARD_ARROW_DOWN);
		put("FreelookMod", MaterialIcons.THREE_SIXTY);
		put("TaplookMod", MaterialIcons.TOUCH_APP);
		put("RearviewMod", MaterialIcons.ARROW_BACK);
		put("ZoomMod", MaterialIcons.ZOOM_IN);
		put("BowZoomMod", MaterialIcons.TARGET);
		put("RawInputMod", MaterialIcons.MOUSE);
		put("SnapTapMod", MaterialIcons.KEYBOARD_KEYS);
		put("NoJumpDelayMod", MaterialIcons.KEYBOARD_DOUBLE_ARROW_UP);
		put("HitDelayFixMod", MaterialIcons.TIMER_OFF);
		put("GodbridgeAssistMod", MaterialIcons.CONSTRUCTION);
		put("MinimalViewBobbingMod", MaterialIcons.DIRECTIONS_WALK);
		put("MotionBlurMod", MaterialIcons.MOTION_PHOTOS_ON);
		put("SlowSwingMod", MaterialIcons.SWIPE);
		put("MinimalDamageShakeMod", MaterialIcons.VIBRATION);
		put("OldAnimationsMod", MaterialIcons.ANIMATION);
		put("DamageTiltMod", MaterialIcons.ROTATE_RIGHT);
		put("FovModifierMod", MaterialIcons.CAMERA);
		put("FarCameraMod", MaterialIcons.VIDEO_CAMERA_FRONT);
		put("CrosshairMod", MaterialIcons.GPS_FIXED);
		put("HorseStatsMod", MaterialIcons.EMOJI_NATURE);

		// 渲染与视觉

		put("FullbrightMod", MaterialIcons.LIGHTBULB);
		put("ClearGlassMod", MaterialIcons.WINDOW);
		put("ClearWaterMod", MaterialIcons.WATER_DROP);
		put("BlockOverlayMod", MaterialIcons.HIGHLIGHT);
		put("ChunkBordersMod", MaterialIcons.BORDER_OUTER);
		put("ChunkAnimatorMod", MaterialIcons.GRID_VIEW);
		put("ItemPhysicsMod", MaterialIcons.SCIENCE);
		put("Items2DMod", MaterialIcons.VIEW_IN_AR);
		put("CustomHeldItemsMod", MaterialIcons.BACK_HAND);
		put("GlintColorMod", MaterialIcons.AUTO_AWESOME);
		put("ColorSaturationMod", MaterialIcons.PALETTE);
		put("DamageTintMod", MaterialIcons.COLORIZE);
		put("HitColorMod", MaterialIcons.COLORIZE);
		put("HitBoxMod", MaterialIcons.CROP_SQUARE);
		put("ClickEffectMod", MaterialIcons.TOUCH_APP);
		put("DamageParticlesMod", MaterialIcons.GRAIN);
		put("ParticleCustomizerMod", MaterialIcons.GRAIN);
		put("BloodParticlesMod", MaterialIcons.WATER_DROP);
		put("KillEffectsMod", MaterialIcons.AUTO_AWESOME);
		put("ProjectileTrailMod", MaterialIcons.TRAIL_LENGTH);
		put("ShinyPotsMod", MaterialIcons.DIAMOND);
		put("EntityCullingMod", MaterialIcons.VISIBILITY_OFF);
		put("Skin3DMod", MaterialIcons.PERSON);
		put("WaveyCapesMod", MaterialIcons.AIR);
		put("MoBendsMod", MaterialIcons.ACCESSIBILITY_NEW);
		put("ModernHotbarMod", MaterialIcons.VIEW_AGENDA);
		put("InventoryMod", MaterialIcons.BACKPACK);

		// 世界、时间与天气

		put("TimeChangerMod", MaterialIcons.UPDATE);
		put("WeatherChangerMod", MaterialIcons.SUNNY);
		put("ViaVersionMod", MaterialIcons.SYNC);
		put("HypixelMod", MaterialIcons.STADIUM);
		put("HypixelQuickPlayMod", MaterialIcons.PLAY_ARROW);
		put("MinemenMod", MaterialIcons.SPORTS_ESPORTS);

		// 其他

		put("AppleSkinMod", MaterialIcons.NUTRITION);
		put("AsyncScreenshotMod", MaterialIcons.PHOTO_CAMERA);
		put("AutoTextMod", MaterialIcons.KEYBOARD);
		put("BorderlessFullscreenMod", MaterialIcons.FULLSCREEN);
		put("ChatMod", MaterialIcons.CHAT);
		put("ChatTranslateMod", MaterialIcons.TRANSLATE);
		put("ClientSpooferMod", MaterialIcons.BADGE);
		put("DangoIslandMod", MaterialIcons.MUSIC_NOTE);
		put("DangoSongCardMod", MaterialIcons.ALBUM);
		put("DiscordRPCMod", MaterialIcons.FORUM);
		put("FPSBoostMod", MaterialIcons.SPEED);
		put("FPSLimiterMod", MaterialIcons.SLOW_MOTION_VIDEO);
		put("FPSSpooferMod", MaterialIcons.MONITOR);
		put("GlobalSettingsMod", MaterialIcons.SETTINGS);
		put("ImageDisplayMod", MaterialIcons.IMAGE);
		put("JumpCircleMod", MaterialIcons.RING_VOLUME);
		put("JumpResetIndicatorMod", MaterialIcons.REPLAY);
		put("KillSoundsMod", MaterialIcons.VOLUME_UP);
		put("MechvibesMod", MaterialIcons.KEYBOARD);
		put("NameDisplayMod", MaterialIcons.BADGE);
		put("NameProtectMod", MaterialIcons.SHIELD);
		put("NametagMod", MaterialIcons.LABEL);
		put("NametagsMod", MaterialIcons.LABEL);
		put("OverlayEditorMod", MaterialIcons.FILTER_FRAMES);
		put("QuickSwitchMod", MaterialIcons.SWAP_HORIZ);
		put("ReachCirclesMod", MaterialIcons.RADAR);
		put("ScriptHudMod", MaterialIcons.CODE);
		put("SkinProtectMod", MaterialIcons.SHIELD);
		put("SoundModifierMod", MaterialIcons.GRAPHIC_EQ);
		put("TargetIndicatorMod", MaterialIcons.MY_LOCATION);
		put("TargetInfoMod", MaterialIcons.BADGE);
		put("WebBrowserMod", MaterialIcons.PUBLIC);

	}

	/** 内部小工具：HashMap.put 的简写。 */
	private static void put(String key, String value) {
		ICONS.put(key, value);
	}

	private ModIcons() {
	}
	public static String iconFor(String className) {

		if (className == null || className.length() == 0) {
			return null;
		}

		String exact = ICONS.get(className);

		if (exact != null) {
			return exact;
		}

		return infer(className);
	}

	/**
	 * 关键词兜底。给「以后新加、还没进清单」的模组一个语义不算错的图标，
	 * 比一律返回分类默认图标要好认。顺序按先窄后宽：越具体的词越靠前。
	 */
	private static String infer(String className) {

		if (className.contains("TargetIndicator") || className.contains("Crosshair") || className.contains("Aim")) {
			return MaterialIcons.MY_LOCATION;
		}

		if (className.contains("TargetInfo")) {
			return MaterialIcons.BADGE;
		}

		if (className.contains("Nametag") || className.contains("Name")) {
			return MaterialIcons.LABEL;
		}

		if (className.contains("Scoreboard")) {
			return MaterialIcons.SCOREBOARD;
		}

		if (className.contains("FPS") || className.contains("Ram") || className.contains("Memory")) {
			return MaterialIcons.MONITOR;
		}

		if (className.contains("Clock") || className.contains("Time") || className.contains("Stopwatch")) {
			return MaterialIcons.SCHEDULE;
		}

		if (className.contains("Coords") || className.contains("Compass") || className.contains("Yaw") || className.contains("Pitch")) {
			return MaterialIcons.EXPLORE;
		}

		if (className.contains("Music") || className.contains("Dango") || className.contains("Sound") || className.contains("Kill")) {
			return MaterialIcons.MUSIC_NOTE;
		}

		if (className.contains("Screenshot") || className.contains("Image") || className.contains("Camera")) {
			return MaterialIcons.PHOTO_CAMERA;
		}

		if (className.contains("Key") || className.contains("Chat") || className.contains("Text")) {
			return MaterialIcons.KEYBOARD;
		}

		if (className.contains("Health") || className.contains("Hurt") || className.contains("Damage")) {
			return MaterialIcons.FAVORITE;
		}

		if (className.contains("Potion")) {
			return MaterialIcons.SCIENCE;
		}

		if (className.contains("Chat") || className.contains("Translate")) {
			return MaterialIcons.TRANSLATE;
		}

		if (className.contains("Sound")) {
			return MaterialIcons.VOLUME_UP;
		}

		if (className.contains("Skin")) {
			return MaterialIcons.PERSON;
		}

		if (className.contains("Block")) {
			return MaterialIcons.HIGHLIGHT;
		}

		if (className.contains("Overlay")) {
			return MaterialIcons.FILTER_FRAMES;
		}

		if (className.contains("Player") || className.contains("User")) {
			return MaterialIcons.PERSON;
		}

		if (className.contains("Score") || className.contains("List")) {
			return MaterialIcons.FORMAT_LIST_BULLETED;
		}

		if (className.contains("Sprint") || className.contains("Walk") || className.contains("Speed")) {
			return MaterialIcons.SPEED;
		}

		if (className.contains("Sneak") || className.contains("Jump")) {
			return MaterialIcons.KEYBOARD_ARROW_DOWN;
		}

		return null;
	}
}
