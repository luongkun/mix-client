package mix.client.mixui.visuals.furiah.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mix.client.mixui.Mix;
import mix.client.mixui.visuals.furiah.compat.EntityViewRenderEvent;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventAttackEntity;
import mix.client.mixui.management.event.impl.EventLoadWorld;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.management.event.impl.EventTick;
import mix.client.mixui.visuals.furiah.fx.FxFrame;
import mix.client.mixui.visuals.furiah.fx.kit.Comic;
import mix.client.mixui.visuals.furiah.fx.kit.KitTextures;
import mix.client.mixui.visuals.furiah.hook.ParticleSwaps;
import mix.client.mixui.visuals.furiah.module.Module;
import mix.client.mixui.visuals.furiah.module.ambience.AmbienceHooks;
import mix.client.mixui.visuals.furiah.module.ambience.AmbienceModule;
import mix.client.mixui.visuals.furiah.module.landing.LandingImpactModule;
import mix.client.mixui.visuals.furiah.module.particles.ParticlesModule;
import mix.client.mixui.visuals.furiah.module.trails.TrailsModule;
import mix.client.mixui.visuals.furiah.render.material.Matcaps;
import mix.client.mixui.visuals.furiah.setting.ColorSetting;
import mix.client.mixui.visuals.furiah.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

public final class FuriahRuntime {
    private static final FuriahRuntime INSTANCE = new FuriahRuntime();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final FuriahsVisuals visuals = FuriahsVisuals.create();
    private final TrailsModule trails = new TrailsModule();
    private final LandingImpactModule landing = new LandingImpactModule();
    private final AmbienceModule ambience = new AmbienceModule();
    private final ParticlesModule particles = new ParticlesModule();
    private final ColorSetting globalColor = ColorSetting.global("globalColor", "Global Color",
            "The color every effect set to Global uses.", ColorSetting.Mode.GRADIENT,
            -45096, -9806593, 1.0D);
    private boolean initialized;

    private FuriahRuntime() {
    }

    public static FuriahRuntime get() {
        return INSTANCE;
    }

    public static TrailsModule trails() {
        return get().trails;
    }

    public static LandingImpactModule landing() {
        return get().landing;
    }

    public static AmbienceModule ambience() {
        return get().ambience;
    }

    public static ParticlesModule particles() {
        return get().particles;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ColorSetting.setGlobal(globalColor);
        visuals.register(trails);
        visuals.register(landing);
        visuals.register(ambience);
        visuals.register(particles);
        trails.setEnabled(false);
        landing.setEnabled(false);
        ambience.setEnabled(false);
        particles.setEnabled(false);
        Mix.getInstance().getEventManager().register(this);
        try {
            Matcaps.prefetch();
            Comic.warmUp();
            KitTextures.warmUp();
        } catch (Throwable throwable) {
            FuriahsVisuals.LOG.warn("Furiah runtime warm-up failed; textures will be generated on demand", throwable);
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        GameClock.tick(mc);
        ClientClock.tick();
        Combat.tick(mc);
        visuals.getModules().tick();
        visuals.getModules().clientTick();
        ParticleSwaps.tickEnd();
        if (mc.theWorld != null) {
            ParticleSwaps.install();
            ParticleSwaps.syncWorld(true);
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.theWorld == null) {
            return;
        }
        GameClock.frame(mc, event.getPartialTicks());
        ClientClock.frame(mc, event.getPartialTicks());
        FxFrame.nextFrame();
        KitTextures.uploadReady();
        visuals.getModules().renderWorld(event.getPartialTicks(), false);
    }

    @EventTarget
    public void onAttack(EventAttackEntity event) {
        if (mc.thePlayer == null || event.getEntity() == null) {
            return;
        }
        ParticleSwaps.attack();
        visuals.getHitConfirmer().attack(event.getEntity());
        Combat.attacked();
        visuals.getModules().attack(mc.thePlayer, event.getEntity());
    }

    @EventTarget
    public void onLoadWorld(EventLoadWorld event) {
        visuals.getModules().worldChanged();
        ParticleSwaps.syncWorld(false);
    }

    public void fogColors(EntityViewRenderEvent.FogColors event) {
        visuals.getModules().fogColors(event);
    }

    public void renderFog(EntityViewRenderEvent.RenderFogEvent event) {
        visuals.getModules().renderFog(event);
    }

    public Vec3 skyColor(Vec3 sky) {
        return AmbienceHooks.skyColor(sky);
    }

    public Vec3 cloudColor(Vec3 clouds) {
        return AmbienceHooks.cloudColor(clouds);
    }

    public void afterSky(float partialTicks) {
        AmbienceHooks.afterSky(partialTicks);
    }

    public boolean isParticleHooksInstalled() {
        return ParticleSwaps.isInstalled();
    }

    public boolean isParticleWorldSynced() {
        return ParticleSwaps.isWorldSynced();
    }

    public static JsonObject save(Module module) {
        JsonObject json = new JsonObject();
        for (Setting setting : module.getSettings()) {
            if (setting.isSaved() && !"enabled".equals(setting.getId())) {
                json.add(setting.getId(), setting.toJson());
            }
        }
        return json;
    }

    public static void load(Module module, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            JsonElement parsed = new JsonParser().parse(value);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject json = parsed.getAsJsonObject();
            for (Setting setting : module.getSettings()) {
                if (setting.isSaved() && json.has(setting.getId())) {
                    setting.fromJson(json.get(setting.getId()));
                }
            }
        } catch (RuntimeException ignored) {
        }
    }
}
