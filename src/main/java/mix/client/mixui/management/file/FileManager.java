package mix.client.mixui.management.file;

import java.io.File;
import java.io.IOException;

import mix.client.mixui.Mix;
import mix.client.mixui.logger.MixLogger;
import net.minecraft.client.Minecraft;

public class FileManager {

	private File mixDir, profileDir, cacheDir, musicDir, externalDir,
					screenshotDir;
	
	public FileManager() {
		
		mixDir = new File(Minecraft.getMinecraft().mcDataDir, "mixclient");
		profileDir = new File(mixDir, "profile");
		cacheDir = new File(mixDir, "cache");
		musicDir = new File(mixDir, "music");
		externalDir = new File(mixDir, "external");
		screenshotDir = new File(mixDir, "screenshots");
		
		createDir(mixDir);
		createDir(profileDir);
		createDir(cacheDir);
		createDir(musicDir);
		createDir(externalDir);
		createDir(screenshotDir);
		
		createVersionFile();
	}
	
	private void createVersionFile() {
		
		File versionDir = new File(cacheDir, "version");
		
		createDir(versionDir);
		createFile(new File(versionDir, Mix.getInstance().getVersion() + ".tmp"));
	}
	
	public void createDir(File file) {
		file.mkdir();
	}
	
	public void createFile(File file) {
		try {
			file.createNewFile();
		} catch (IOException e) {
			MixLogger.error("Failed to create file " + file.getName(), e);
		}
	}

	public File getScreenshotDir() {
		return screenshotDir;
	}

	public File getMixDir() {
		return mixDir;
	}

	public File getProfileDir() {
		return profileDir;
	}

	public File getCacheDir() {
		return cacheDir;
	}

	public File getMusicDir() {
		return musicDir;
	}

	public File getExternalDir() {
		return externalDir;
	}
}
