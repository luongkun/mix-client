package mix.client.mixui.management.mods.impl;

import mix.client.mixui.discord.DiscordRPC;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class DiscordRPCMod extends Mod {

	private DiscordRPC discord = new DiscordRPC();
	
	public DiscordRPCMod() {
		super(TranslateText.DISCORD_RPC, TranslateText.DISCORD_RPC_DESCRIPTION, ModCategory.OTHER);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		discord.start();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		if(discord.isStarted()) {
			discord.stop();
		}
	}
}
