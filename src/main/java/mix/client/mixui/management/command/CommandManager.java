package mix.client.mixui.management.command;

import java.util.ArrayList;

import mix.client.mixui.Mix;
import mix.client.mixui.management.command.impl.ScreenshotCommand;
import mix.client.mixui.management.command.impl.TranslateCommand;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventSendChat;

public class CommandManager {

	private ArrayList<Command> commands = new ArrayList<Command>();
	
	public CommandManager() {
		
		commands.add(new ScreenshotCommand());
		commands.add(new TranslateCommand());
		
		Mix.getInstance().getEventManager().register(this);
	}
	
	@EventTarget
	public void onSendChat(EventSendChat event) {
		
		if(event.getMessage().startsWith(".mixcmd")) {
			
			event.setCancelled(true);
			
			String[] args = event.getMessage().split(" ");
			
			if(args.length > 1) {
				for(Command c : commands) {
					if(args[1].equals(c.getPrefix())) {
						c.onCommand(event.getMessage().replace(".mixcmd ", "").replace(args[1] + " ", ""));
					}
				}
			}
		}
	}

	public ArrayList<Command> getCommands() {
		return commands;
	}
}
