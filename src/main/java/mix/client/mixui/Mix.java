package mix.client.mixui;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import mix.client.mixui.integration.dango.DangoBridge;
import mix.client.bootstrap.MixTweaker;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.account.AccountManager;
import mix.client.mixui.management.cape.CapeManager;
import mix.client.mixui.management.changelog.ChangelogManager;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.command.CommandManager;
import mix.client.mixui.management.download.DownloadManager;
import mix.client.mixui.management.event.EventManager;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.management.language.LanguageManager;
import mix.client.mixui.management.mods.ModManager;
import mix.client.mixui.management.mods.impl.GlobalSettingsMod;
import mix.client.mixui.management.music.MusicManager;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.notification.NotificationManager;
import mix.client.mixui.management.profile.ProfileManager;
import mix.client.mixui.management.quickplay.QuickPlayManager;
import mix.client.mixui.management.screenshot.ScreenshotManager;
import mix.client.mixui.management.security.SecurityFeatureManager;
import mix.client.mixui.management.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public class Mix {

	private static Mix instance = new Mix();
	private Minecraft mc = Minecraft.getMinecraft();
	private MixAPI api;
	
	private String name, version;
	
	private NanoVGManager nanoVGManager;
	private FileManager fileManager;
	private DownloadManager downloadManager;
	private LanguageManager languageManager;
	private AccountManager accountManager;
	private EventManager eventManager;
	private ModManager modManager;
	private CapeManager capeManager;
	private ColorManager colorManager;
	private ProfileManager profileManager;
	private MusicManager musicManager;
	private CommandManager commandManager;
	private ScreenshotManager screenshotManager;
	private NotificationManager notificationManager;
	private SecurityFeatureManager securityFeatureManager;
	private QuickPlayManager quickPlayManager;
	private ChangelogManager changelogManager;
	private WaypointManager waypointManager;
	
	/* The managers below are built by start(). It is called from the game
	   bootstrap and again from whoever first opens the UI, so it has to be
	   idempotent: a second call would re-register every mod and re-emit a
	   second GlobalSettings toggle. */
	private boolean started;
	
	public Mix() {
		name = "Mix";
		Package pkg = Mix.class.getPackage();
		version = pkg != null && pkg.getImplementationVersion() != null
				? pkg.getImplementationVersion() : "dev";
	}
	
	public void start() {
		
		if(started) {
			return;
		}
		started = true;
		
		this.removeOptifineZoom();
		
		fileManager = new FileManager();
		downloadManager = new DownloadManager();
		languageManager = new LanguageManager();
		accountManager = new AccountManager();
		eventManager = new EventManager();
		modManager = new ModManager();
		
		modManager.init();
		
		capeManager = new CapeManager();
		colorManager = new ColorManager();
		profileManager = new ProfileManager();
		musicManager = new MusicManager();
		
		api = new MixAPI();
		api.init();
		
		commandManager = new CommandManager();
		screenshotManager = new ScreenshotManager();
		notificationManager = new NotificationManager();
		securityFeatureManager = new SecurityFeatureManager();
		quickPlayManager = new QuickPlayManager();
		changelogManager = new ChangelogManager();
		waypointManager = new WaypointManager();
		
		eventManager.register(new MixHandler());
		
		// DangoMusic（原 Forge 模组）合入后的启动与事件桥
		DangoBridge.init();
		
		setupLibraryPath();
		
		GlobalSettingsMod.getInstance().setToggled(true);
		mc.updateDisplay();
	}
	
	public void stop() {
		profileManager.save();
		accountManager.save();
	}
	
	private void removeOptifineZoom() {
		if(MixTweaker.hasOptifine) {
			try {
				this.unregisterKeybind((KeyBinding) GameSettings.class.getField("ofKeyBindZoom").get(mc.gameSettings));
			} catch(Exception e) {
				MixLogger.error("Failed to unregister zoom key", e);
			}
		}
	}
	
    private void unregisterKeybind(KeyBinding key) {
        if (Arrays.asList(mc.gameSettings.keyBindings).contains(key)) {
            mc.gameSettings.keyBindings = ArrayUtils.remove(mc.gameSettings.keyBindings, Arrays.asList(mc.gameSettings.keyBindings).indexOf(key));
            key.setKeyCode(0);
        }
    }
	
    private void setupLibraryPath() {
    	
    	File cefDir = new File(fileManager.getExternalDir(), "cef");
    	
        try {
			System.setProperty("jcef.path", cefDir.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	public static Mix getInstance() {
		return instance;
	}

	public MixAPI getApi() {
		return api;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public DownloadManager getDownloadManager() {
		return downloadManager;
	}

	public ModManager getModManager() {
		return modManager;
	}

	public LanguageManager getLanguageManager() {
		return languageManager;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public NanoVGManager getNanoVGManager() {
		/* The splash screen builds the context and hands it over, but on the Forge
		   path nothing draws a splash before the first menu frame. Build one here
		   so callers never see null -- creating a second context later is avoided
		   because the field is only assigned once. */
		if(nanoVGManager == null) {
			nanoVGManager = new NanoVGManager();
		}
		return nanoVGManager;
	}

	public ColorManager getColorManager() {
		return colorManager;
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}

	public MusicManager getMusicManager() {
		return musicManager;
	}

	public AccountManager getAccountManager() {
		return accountManager;
	}

	public CapeManager getCapeManager() {
		return capeManager;
	}

	public CommandManager getCommandManager() {
		return commandManager;
	}

	public ScreenshotManager getScreenshotManager() {
		return screenshotManager;
	}

	public void setNanoVGManager(NanoVGManager nanoVGManager) {
		this.nanoVGManager = nanoVGManager;
	}

	public NotificationManager getNotificationManager() {
		return notificationManager;
	}

	public SecurityFeatureManager getSecurityFeatureManager() {
		return securityFeatureManager;
	}

	public QuickPlayManager getQuickPlayManager() {
		return quickPlayManager;
	}

	public ChangelogManager getChangelogManager() {
		return changelogManager;
	}

	public WaypointManager getWaypointManager() {
		return waypointManager;
	}
}
