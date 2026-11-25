package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventText;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.TextSetting;

public class NameProtectMod extends Mod {

	private TextSetting nameSetting = new TextSetting(TranslateText.NAME, this, "You");
	
	public NameProtectMod() {
		super(TranslateText.NAME_PROTECT, TranslateText.NAME_PROTECT_DESCRIPTION, ModCategory.PLAYER);
	}
	
	@EventTarget
	public void onText(EventText event) {
		
		String username = mc.getSession() == null ? null : mc.getSession().getUsername();
		String target = nameSetting.getText();
		
		// 会话还没建立（或者启动器没给名字）时不处理，否则 replace("") 会把整段文本拆散
		if(username == null || username.isEmpty() || username.length() < 3) {
			return;
		}

		if(target == null) {
			target = "";
		}
		
		// 服务器常把名字切成 "§7Na§fm§7e" 之类，直接 replace 匹配不到；
		// 这里同时跑一遍带格式码感知的替换。
		event.replace(username, target);
		event.replaceIgnoringFormatting(username, target);
	}
}
