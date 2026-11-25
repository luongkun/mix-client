package mix.client.mixui.management.quickplay.impl;

import java.util.ArrayList;

import mix.client.mixui.management.quickplay.QuickPlay;
import mix.client.mixui.management.quickplay.QuickPlayCommand;
import net.minecraft.util.ResourceLocation;

public class UHCQuickPlay extends QuickPlay{

	public UHCQuickPlay() {
		super("UHC", new ResourceLocation("mixclient/icons/hypixel/UHC.png"));
	}

	@Override
	public void addCommands() {
		ArrayList<QuickPlayCommand> commands = new ArrayList<QuickPlayCommand>();
		
		commands.add(new QuickPlayCommand("Lobby", "/l hc"));
		commands.add(new QuickPlayCommand("Solo", "/play uhc_solo"));
		commands.add(new QuickPlayCommand("Teams", "/play uhc_teams"));
		commands.add(new QuickPlayCommand("Events Mode", "/play /play uhc_events"));
		
		this.setCommands(commands);
	}
}