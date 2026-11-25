package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventReceivePacket;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import net.minecraft.network.play.server.S02PacketChat;

public class MinemenMod extends Mod {

	private BooleanSetting autoPlaySetting = new BooleanSetting(TranslateText.AUTO_PLAY, this, false);
	
	public MinemenMod() {
		super(TranslateText.MINEMEN, TranslateText.MINEMEN_DESCRIPTION, ModCategory.OTHER);
	}

	@EventTarget
	public void onReceivePacket(EventReceivePacket event) {
		
		if(autoPlaySetting.isToggled() && event.getPacket() instanceof S02PacketChat) {
			
			S02PacketChat chatPacket = (S02PacketChat) event.getPacket();
			String raw = chatPacket.getChatComponent().toString();
			
			if (raw.contains("clickEvent=ClickEvent{action=RUN_COMMAND, value='/requeue")) {
				mc.thePlayer.sendChatMessage("/requeue");
			}
		}
	}
}
