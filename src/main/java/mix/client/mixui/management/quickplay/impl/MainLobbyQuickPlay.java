package mix.client.mixui.management.quickplay.impl;

import java.util.ArrayList;

import mix.client.mixui.management.quickplay.QuickPlay;
import mix.client.mixui.management.quickplay.QuickPlayCommand;
import net.minecraft.util.ResourceLocation;

public class MainLobbyQuickPlay extends QuickPlay{

	public MainLobbyQuickPlay() {
		super("MainLobby", new ResourceLocation("mixclient/icons/hypixel/MainLobby.png"));
	}

	@Override
	public void addCommands(){
		ArrayList<QuickPlayCommand> commands = new ArrayList<QuickPlayCommand>();
		
		commands.add(new QuickPlayCommand("Lobby", "/lobby main"));
		
		this.setCommands(commands);
	}
}