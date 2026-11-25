package mix.client.mixui.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;

public class ServerUtils {

	private static Minecraft mc = Minecraft.getMinecraft();
	
	public static boolean isInTablist(Entity entity) {
		if(isJoinServer()) {
			for (NetworkPlayerInfo item : mc.getNetHandler().getPlayerInfoMap()) {
				NetworkPlayerInfo playerInfo = (NetworkPlayerInfo) item;

				if (playerInfo != null && playerInfo.getGameProfile() != null && playerInfo.getGameProfile().getName().contains(entity.getName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean isJoinServer() {
		return !mc.isSingleplayer() && mc.getCurrentServerData() != null;
	}
	
	public static String getServerIP() {
		
		String ip;
		
		if(isJoinServer()) {
			ip = mc.getCurrentServerData().serverIP;
		}else {
			ip = "Single Player";
		}
		
		return ip;
	}
	
	// ping 缓存：每 5 秒重新测量一次，避免 HUD 每帧都去翻 tab 列表。
	private static final long PING_REFRESH_TIME = 5000L;
	private static int cachedPing = 0;
	private static long lastPingRefresh = 0L;

	public static int getPing() {

		long now = System.currentTimeMillis();

		if(now - lastPingRefresh < PING_REFRESH_TIME) {
			return cachedPing;
		}

		lastPingRefresh = now;

		int measured = measurePing();

		if(measured > 0) {
			cachedPing = measured;
		}else if(mc.thePlayer == null || mc.isSingleplayer() || !isJoinServer()) {
			cachedPing = 0;
		}

		return cachedPing;
	}

	private static int measurePing() {

		if(mc.thePlayer == null || mc.getNetHandler() == null) {
			return 0;
		}

		// 优先按名字匹配：Bungee/离线 UUID 与本地 session 不一致时，按 UUID 查会查不到。
		String selfName = mc.thePlayer.getName();
		NetworkPlayerInfo byName = null;
		NetworkPlayerInfo byUuid = null;

		for(NetworkPlayerInfo item : mc.getNetHandler().getPlayerInfoMap()) {

			if(item == null || item.getGameProfile() == null) {
				continue;
			}

			if(selfName != null && selfName.equalsIgnoreCase(item.getGameProfile().getName())) {
				byName = item;
				break;
			}

			if(byUuid == null && mc.thePlayer.getUniqueID() != null && mc.thePlayer.getUniqueID().equals(item.getGameProfile().getId())) {
				byUuid = item;
			}
		}

		NetworkPlayerInfo info = byName != null ? byName : byUuid;

		if(info != null && info.getResponseTime() > 0) {
			return info.getResponseTime();
		}

		if(isJoinServer()) {
			return (int) Math.max(0.0F, mc.getCurrentServerData().pingToServer);
		}

		return 0;
	}
	
	public static boolean isHypixel() {
		return getServerIP().contains("hypixel");
	}
	
	public static boolean isMinemen() {
		return getServerIP().contains("minemen");
	}
}
