package mix.client.mixui;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

import mix.client.mixui.debug.UiCapture;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.AccountManager;
import mix.client.mixui.management.account.AccountType;
import mix.client.mixui.management.cape.CapeManager;
import mix.client.mixui.management.cape.impl.Cape;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventClickMouse;
import mix.client.mixui.management.event.impl.EventJoinServer;
import mix.client.mixui.management.event.impl.EventLoadWorld;
import mix.client.mixui.management.event.impl.EventLocationCape;
import mix.client.mixui.management.event.impl.EventLocationSkin;
import mix.client.mixui.management.event.impl.EventReceivePacket;
import mix.client.mixui.management.event.impl.EventTick;
import mix.client.mixui.management.event.impl.EventUpdate;
import mix.client.mixui.management.music.MusicGate;
import mix.client.mixui.management.profile.Profile;
import mix.client.mixui.utils.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.util.ResourceLocation;

public class MixHandler {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private Mix instance;
	
	private String prevOfflineName;
	private ResourceLocation offlineSkin;
	
	public MixHandler() {
		instance = Mix.getInstance();
	}
	
	@EventTarget
	public void onTick(EventTick event) {
		UiCapture.tick();
	}
	
	/**
	 * 世界加载完成 —— 音乐真正的恢复点。
	 *
	 * <p>放在这里而不是主菜单的 {@code onGuiClosed()}：关掉主菜单只代表玩家点了
	 * 「单人游戏」或「多人游戏」，他还在存档列表 / 服务器列表里挑，
	 * 那时候放歌就是用户抱怨的「还没进世界就开始播」。
	 * 只有世界真的开始加载了才算「开玩」。</p>
	 *
	 * <p>顺序刻意排在 {@link #onJoinServer} 之前：进服时那个方法会
	 * {@code disableAll()} + 载入档案，可能连带动到音乐模块的设置，
	 * 先把歌恢复起来再让档案去改，才不会出现「刚 play 又被配置覆盖」。</p>
	 */
	@EventTarget
	public void onLoadWorld(EventLoadWorld event) {
		MusicGate.resumeOnWorldJoin();
	}
	
	@EventTarget
	public void onJoinServer(EventJoinServer event) {
		for(Profile p : instance.getProfileManager().getProfiles()) {
			if(!p.getServerIp().isEmpty() && StringUtils.containsIgnoreCase(event.getIp(), p.getServerIp())) {
				instance.getModManager().disableAll();
				instance.getProfileManager().load(p.getJsonFile());
				break;
			}
		}
	}
	
	@EventTarget
	public void onUpdate(EventUpdate event) {
		TargetUtils.onUpdate();
	}
	
	@EventTarget
	public void onClickMouse(EventClickMouse event) {
        if (mc.gameSettings.keyBindTogglePerspective.isPressed()) {
            mc.gameSettings.thirdPersonView = (mc.gameSettings.thirdPersonView + 1) % 3;
            mc.renderGlobal.setDisplayListEntitiesDirty();
        }
	}
	
	@EventTarget
	public void onReceivePacket(EventReceivePacket event) {
    	if(event.getPacket() instanceof S2EPacketCloseWindow && mc.currentScreen instanceof GuiModMenu) {
    		event.setCancelled(true);
    	}
	}
	
	@EventTarget
	public void onCape(EventLocationCape event) {
		
		CapeManager capeManager = instance.getCapeManager();
		
		if(event.getPlayerInfo() != null && event.getPlayerInfo().getGameProfile().getId().equals(mc.thePlayer.getGameProfile().getId())) {
			
			Cape currentCape = capeManager.getCurrentCape();
			
			if(!currentCape.equals(capeManager.getCapeByName("None"))) {
				event.setCancelled(true);
				event.setCape(currentCape.getCape());
			}
		}
	}
	
	@EventTarget
	public void onSkin(EventLocationSkin event) {
		
		if(event.getPlayerInfo() != null && event.getPlayerInfo().getGameProfile().getId().equals(mc.thePlayer.getGameProfile().getId())) {
			
			AccountManager accountManager = instance.getAccountManager();
			Account currentAccount = accountManager == null ? null : accountManager.getCurrentAccount();
			
			// No account at all (e.g. a launcher that hands over no session) used to
			// throw from inside the render loop on every frame; just leave the skin alone.
			if(currentAccount == null) {
				return;
			}
			
			if(currentAccount.getType().equals(AccountType.OFFLINE)) {
				
				if(prevOfflineName != currentAccount.getName()) {
					
					if(currentAccount.getSkinFile() != null) {
						try {
							
				            BufferedImage t = ImageIO.read(currentAccount.getSkinFile());
				            DynamicTexture nibt = new DynamicTexture(t);
				            
				            offlineSkin = mc.getTextureManager().getDynamicTextureLocation("offlineskin", nibt);
						} catch(Exception e) {
							MixLogger.error("Failed to load offline skin", e);
						}
					}
				}
				
				if(offlineSkin != null) {
					event.setCancelled(true);
					event.setSkin(offlineSkin);
				}
			}
		}
	}
}
