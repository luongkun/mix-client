package mix.client.mixui.viaversion;

import java.io.File;

import mix.client.mixui.Mix;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.viaversion.gui.AsyncVersionSlider;

public class ViaMix {
	
    public final static int NATIVE_VERSION = 47;
    private static ViaMix instance;

    public static void create() {
    	instance = new ViaMix();
    }

    private AsyncVersionSlider asyncVersionSlider;

    public ViaMix() {
    	
    	FileManager fileManager = Mix.getInstance().getFileManager();
    	
        ViaLoadingBase.ViaLoadingBaseBuilder.create().runDirectory(new File(fileManager.getMixDir(), "ViaVersion")).nativeVersion(NATIVE_VERSION).onProtocolReload(comparableProtocolVersion -> {
            if (getAsyncVersionSlider() != null) {
                getAsyncVersionSlider().setVersion(comparableProtocolVersion.getVersion());
            }
        }).build();
    }

    public static ViaMix getInstance() {
		return instance;
	}

	public void initAsyncSlider() {
        this.initAsyncSlider(5, 5, 110, 20);
    }

    public void initAsyncSlider(int x, int y, int width, int height) {
        asyncVersionSlider = new AsyncVersionSlider(-1, x, y, Math.max(width, 110), height);
    }

    public AsyncVersionSlider getAsyncVersionSlider() {
        return asyncVersionSlider;
    }
}
