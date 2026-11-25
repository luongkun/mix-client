package mix.client.mixui.management.mods.impl;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.GuiWebBrowser;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventKey;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.HUDMod;
import mix.client.mixui.management.mods.settings.impl.KeybindSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.notification.NotificationType;
import mix.client.mixui.mcef.Mcef;
import mix.client.mixui.mcef.McefBrowser;

public class WebBrowserMod extends HUDMod {

	private static WebBrowserMod instance;
	
	private NumberSetting widthSetting = new NumberSetting(TranslateText.WIDTH, this, 190, 10, 500, true);
	private NumberSetting heightSetting = new NumberSetting(TranslateText.HEIGHT, this, 100, 10, 500, true);
	private NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 1.0F, 0.0F, 1.0F, false);
	
	private KeybindSetting keybindSetting = new KeybindSetting(TranslateText.KEYBIND, this, Keyboard.KEY_J);
	
	private McefBrowser browser;
	
	public WebBrowserMod() {
		super(TranslateText.WEB_BROWSER, TranslateText.WEB_BROWSER_DESCRIPTION);
		
		instance = this;
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		
		nvg.setupAndDraw(() -> drawNanoVG(nvg));
	}
	
	private void drawNanoVG(NanoVGManager nvg) {
		
		int width = (int) (widthSetting.getValueInt() * this.getScale());
		int height = (int) (heightSetting.getValueInt() * this.getScale());
		
		if(browser != null) {
			nvg.drawShadow(this.getX(), this.getY(), width, height, 6 * this.getScale());
			nvg.drawRoundedImage(browser.getTexture(), this.getX(), this.getY(), width, height, 6 * this.getScale(), alphaSetting.getValueFloat());
		}

		this.setWidth((int) (width / this.getScale()));
		this.setHeight((int) (height / this.getScale()));
	}
	
	@EventTarget
	public void onKey(EventKey event) {
		
		if(event.getKeyCode() == keybindSetting.getKeyCode()) {
			
			if(browser == null) {
				
				if(!Mcef.isInitialized()) {
					Mcef.getClient();
				}
				
		        String url = "https://www.google.com/";
		        
		        boolean transparent = true;
		        browser = Mcef.createBrowser(url, transparent);
		        
		        if(browser == null) {
		        	Mix.getInstance().getNotificationManager().post(TranslateText.ERROR, TranslateText.REQUIRED_FILE_MISSING, NotificationType.ERROR);
		        }
			}
			
			if(browser != null) {
				mc.displayGuiScreen(new GuiWebBrowser(browser));
			}
		}
	}
	
	@Override
	public void onEnable() {
		super.onEnable();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		if(browser != null) {
			browser.close();
			browser = null;
		}
	}

	public static WebBrowserMod getInstance() {
		return instance;
	}
}
