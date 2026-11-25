package mix.client.mixui;

import java.io.File;

import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.management.file.FileManager;

public class MixAPI {

	private long launchTime;
	private GuiModMenu modMenu;
	private GuiMixMainMenu mainMenu;
	private File firstLoginFile;
	
	public MixAPI() {
		
		FileManager fileManager = Mix.getInstance().getFileManager();
		
		firstLoginFile = new File(fileManager.getCacheDir(), "first.tmp");
	}
	
	public void init() {
		launchTime = System.currentTimeMillis();
		modMenu = new GuiModMenu();
		mainMenu = new GuiMixMainMenu();
	}
	
	public boolean isSpecialUser() {
		return true;
	}
	
	public GuiModMenu getModMenu() {
		return modMenu;
	}

	public long getLaunchTime() {
		return launchTime;
	}

	public GuiMixMainMenu getMainMenu() {
		return mainMenu;
	}

	public void createFirstLoginFile() {
		Mix.getInstance().getFileManager().createFile(firstLoginFile);
	}
	
	public boolean isFirstLogin() {
		return !firstLoginFile.exists();
	}
}
