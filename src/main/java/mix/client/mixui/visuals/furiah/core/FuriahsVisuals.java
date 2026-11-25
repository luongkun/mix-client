package mix.client.mixui.visuals.furiah.core;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mix.client.mixui.visuals.furiah.module.Module;
import mix.client.mixui.visuals.furiah.module.ambience.AmbienceModule;
import mix.client.mixui.visuals.furiah.module.sky.SkyModule;
import mix.client.mixui.visuals.furiah.module.watermark.WaterMarkModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public final class FuriahsVisuals {
    public static final String MODID = "furiahsvisuals";
    public static final Logger LOG = LogManager.getLogger("Mix Furiah Visuals");
    public static FuriahsVisuals instance;

    private final ModuleManager modules = new ModuleManager();
    private final HitConfirmer hitConfirmer = new HitConfirmer();
    private AmbienceModule ambience;
    private SkyModule sky;
    private WaterMarkModule watermark;

    public static FuriahsVisuals create() {
        instance = new FuriahsVisuals();
        return instance;
    }

    public void register(Module module) {
        modules.register(module);
        if (module instanceof AmbienceModule) {
            ambience = (AmbienceModule) module;
        } else if (module instanceof WaterMarkModule) {
            watermark = (WaterMarkModule) module;
        } else if (module instanceof SkyModule) {
            sky = (SkyModule) module;
        }
    }

    public ModuleManager getModules() {
        return modules;
    }

    public HitConfirmer getHitConfirmer() {
        return hitConfirmer;
    }

    public AmbienceModule getAmbience() {
        return ambience;
    }

    public WaterMarkModule getWaterMark() {
        return watermark;
    }

    public SkyModule getSky() {
        return sky;
    }

    public ConfigManager getConfig() {
        return null;
    }

    public PresetManager getPresets() {
        return null;
    }

    public File getDataDir() {
        return new File(Minecraft.getMinecraft().mcDataDir, "mixclient/furiahvisuals");
    }

    public boolean isReduceFlashing() {
        return false;
    }

    public static void chat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
