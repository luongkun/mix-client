package mix.client.mixui.debug;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.GuiEditHUD;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.gui.modmenu.category.CosmeticsTab;
import mix.client.mixui.gui.modmenu.category.impl.CosmeticsCategory;
import mix.client.mixui.gui.modmenu.category.impl.ModuleCategory;
import mix.client.mixui.gui.modmenu.category.impl.MusicCategory;
import mix.client.mixui.gui.modmenu.category.impl.SettingCategory;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.event.impl.EventAttackEntity;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.management.event.impl.EventUpdate;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.HUDMod;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.visuals.FuriahVisualMod;
import mix.client.mixui.visuals.furiah.core.FuriahRuntime;
import mix.client.mixui.visuals.furiah.module.ambience.AmbientStyle;
import mix.client.mixui.visuals.furiah.module.landing.LandingStyle;
import mix.client.mixui.visuals.furiah.module.trails.TrailStyle;
import mix.client.mixui.visuals.furiah.module.trails.TrailsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * 自动化视觉验收用的调试钩子。
 *
 * <p>这台机器上抓不到 LWJGL 窗口的 GL 内容：GDI 拿到的永远是「白底 + 黑框」的
 * 未绘制 backing，桌面上换四个不同构建抓出来逐字节相同；而 keybd_event 抢焦点
 * 又会被终端反复抢回去。唯一可靠的取证方式是让客户端自己走流程并自己落盘截图。</p>
 *
 * <p>仅在 JVM 参数 -Dmixclient.uicapture=true 时启用，正常启动完全不做任何事。</p>
 */
public class UiCapture {

    private static final boolean ENABLED = Boolean.getBoolean("mixclient.uicapture");

    /**
     * 先建一个超平坦创造世界再开菜单。
     *
     * <p>坐在主菜单上截图时，菜单背后只有一张很暗的全景图 —— 顶部磨砂即便真的在
     * 跑，背后也没有任何可辨认的纹理，读起来依旧是一块白板。只有在世界里，磨砂
     * 才有「草地/天空被糊开」的样子可以判读。</p>
     */
    private static final boolean WITH_WORLD = Boolean.getBoolean("mixclient.uicapture.world");
    private static final boolean FULLSCREEN = Boolean.getBoolean("mixclient.uicapture.fullscreen");
    private static final boolean FRAME_PROBE = Boolean.getBoolean("mixclient.uicapture.frames");
    private static final boolean VISUALS = Boolean.getBoolean("mixclient.uicapture.visuals");

    private static int tick;
    private static boolean fullscreenToggled;
    private static boolean fullscreenRestored;
    private static double smokeStartX, smokeStartY, smokeStartZ;
    private static int particleRenderPeak;

    /** 已经触发过建世界。 */
    private static boolean worldRequested;

    /** 世界已经进得去、可以开始截图序列。 */
    private static boolean worldReady;

    /** 世界模式下的截图序列计数（与 tick 分开，避免等世界加载时把 40/80 这些点错过）。 */
    private static int seq;

    /** 调试开关：把遮罩纵向翻转，用来验证 shader 里的 texCoord 朝向。 */
    private static boolean frostInverted;

    /** 调试开关：把模糊半径压到 0，用来证明「顶部那块确实是模糊」而不是纯色渐变。 */
    private static boolean blurProbeOff;

    public static boolean isFrostInverted() {
        return frostInverted;
    }

    public static boolean isBlurProbeOff() {
        return blurProbeOff;
    }

    public static void setFrostInverted(boolean inverted) {
        frostInverted = inverted;
    }

    public static void setBlurProbeOff(boolean off) {
        blurProbeOff = off;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /** 由 MixHandler.onTick 每 tick 调用一次。 */
    public static void tick() {

        if (!ENABLED) {
            return;
        }

        tick++;

        Minecraft mc = Minecraft.getMinecraft();

        if (FULLSCREEN && tick == 10) {
            for (int i = 0; i < 32 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
            }
            mc.toggleFullscreen();
            fullscreenToggled = true;
            MixLogger.info("[uicapture] fullscreen=" + Display.isFullscreen()
                    + " display=" + mc.displayWidth + "x" + mc.displayHeight
                    + " fbo=" + OpenGlHelper.isFramebufferEnabled());
        }
        if (FULLSCREEN && tick == 40 && !Display.isFullscreen()) {
            fail("fullscreen toggle did not take effect");
            return;
        }

        if (WITH_WORLD) {

            if (!worldRequested) {
                if (tick >= 20) {
                    worldRequested = launchWorld(mc);
                }
                return;
            }

            if (!worldReady) {
                if (mc.theWorld != null && mc.currentScreen == null) {
                    worldReady = true;
                    seq = 0;
                    MixLogger.info("[uicapture] world ready");
                } else if (tick > 2400) {
                    worldReady = true;
                    seq = 0;
                    MixLogger.info("[uicapture] world wait timeout, continue anyway");
                }
                return;
            }

            seq++;
            if (VISUALS) {
                runVisualSequence(mc, seq);
            } else {
                runSequence(mc, seq);
            }
            return;
        }

        runSequence(mc, tick);
    }

    private static void runVisualSequence(Minecraft mc, int step) {
        switch (step) {
            case 20:
                for (int i = 0; i < 32 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
                }
                openMenu(mc);
                gotoCategory(mc, CosmeticsCategory.class);
                selectCosmeticsTab(mc, CosmeticsTab.CAPE);
                break;
            case 40:
                shot("ui-cosmetics-cape");
                break;
            case 44:
                selectCosmeticsTab(mc, CosmeticsTab.KILL_EFFECT);
                break;
            case 52:
                shot("ui-cosmetics-kill");
                break;
            case 56:
                selectCosmeticsTab(mc, CosmeticsTab.HIT_EFFECT);
                break;
            case 64:
                shot("ui-cosmetics-hit");
                break;
            case 68:
                selectCosmeticsTab(mc, CosmeticsTab.TRAIL);
                break;
            case 76:
                shot("ui-cosmetics-trail");
                break;
            case 80:
                selectCosmeticsTab(mc, CosmeticsTab.LANDING);
                break;
            case 88:
                shot("ui-cosmetics-landing");
                break;
            case 92:
                selectCosmeticsTab(mc, CosmeticsTab.AMBIENCE);
                break;
            case 100:
                shot("ui-cosmetics-ambience");
                break;
            case 104:
                selectCosmeticsTab(mc, CosmeticsTab.PARTICLES);
                break;
            case 112:
                shot("ui-cosmetics-particles");
                break;
            case 116:
                CosmeticsCategory settingsCategory = getCosmeticsCategory(mc);
                if (settingsCategory != null) {
                    settingsCategory.showSettings(Mix.getInstance().getModManager()
                            .getModByTranslateKey(TranslateText.TRAILS.getKey()));
                }
                break;
            case 124:
                shot("ui-cosmetics-settings");
                break;
            case 128:
                CosmeticsCategory activeCategory = getCosmeticsCategory(mc);
                if (activeCategory != null) {
                    activeCategory.closeSettings();
                }
                enableVisual(TranslateText.TRAILS.getKey());
                enableVisual(TranslateText.LANDING_IMPACT.getKey());
                enableVisual(TranslateText.AMBIENCE.getKey());
                enableVisual(TranslateText.VISUAL_PARTICLES.getKey());
                mc.displayGuiScreen(null);
                for (int i = 0; i < 32 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
                }
                MixLogger.info("[uicapture] visual modules enabled");
                break;
            case 132:
                smokeStartX = mc.thePlayer.posX;
                smokeStartY = mc.thePlayer.posY;
                smokeStartZ = mc.thePlayer.posZ;
                mc.thePlayer.onGround = false;
                mc.thePlayer.fallDistance = 1.0F;
                setSmokePosition(smokeStartX, smokeStartY + 0.05D, smokeStartZ);
                new EventUpdate().call();
                break;
            case 134:
            case 136:
            case 138:
            case 140:
            case 142:
            case 144:
            case 146:
            case 148:
            case 150:
            case 152:
                double travel = (step - 132) / 20.0D;
                setSmokePosition(smokeStartX + travel * 3.0D,
                        smokeStartY + Math.sin(travel * Math.PI) * 1.1D, smokeStartZ);
                mc.thePlayer.onGround = false;
                if (step == 152) {
                    mc.thePlayer.onGround = true;
                    mc.thePlayer.fallDistance = 1.0F;
                }
                new EventUpdate().call();
                break;
            case 156:
                shot("ui-visuals-trail");
                MixLogger.info("[uicapture] trailStyle=" + FuriahRuntime.trails().getSelectedStyle().getType()
                        + " landingPending=" + visualListSize(TranslateText.LANDING_IMPACT.getKey(), "pending")
                        + " ambience=" + FuriahRuntime.ambience().countsForTest());
                break;
            case 160:
                mc.thePlayer.motionX = 0.0D;
                mc.thePlayer.motionZ = 0.0D;
                mc.thePlayer.rotationPitch = 25.0F;
                shot("ui-visuals-landing");
                break;
            case 164:
                shot("ui-visuals-landing-close");
                break;
            case 176:
                Vec3 look = mc.thePlayer.getLookVec();
                EntityCow cow = new EntityCow(mc.theWorld);
                cow.setPosition(mc.thePlayer.posX + look.xCoord * 2.2D,
                        mc.thePlayer.posY, mc.thePlayer.posZ + look.zCoord * 2.2D);
                new EventAttackEntity(cow).call();
                mc.gameSettings.particleSetting = 2;
                FuriahRuntime.particles().burst();
                for (int i = 0; i < 16; i++) {
                    mc.effectRenderer.spawnEffectParticle(net.minecraft.util.EnumParticleTypes.CRIT.getParticleID(),
                            cow.posX + (i % 4) * 0.12D, cow.posY + 0.8D + (i / 4) * 0.10D,
                            cow.posZ + (i % 3) * 0.10D, 0.0D, 0.02D, 0.0D, new int[0]);
                }
                mc.theWorld.spawnParticle(net.minecraft.util.EnumParticleTypes.CRIT, true,
                        cow.posX, cow.posY + 1.0D, cow.posZ, 0.0D, 0.0D, 0.0D, new int[0]);
                break;
            case 180:
                shot("ui-visuals-particles");
                particleRenderPeak = Math.max(particleRenderPeak, FuriahRuntime.particles().renderedParticles());
                MixLogger.info("[uicapture] particle renderer=" + mc.effectRenderer.getClass().getName()
                        + " paused=" + mc.isGamePaused()
                        + " worldSynced=" + FuriahRuntime.get().isParticleWorldSynced()
                        + " bursts=" + mix.client.mixui.visuals.furiah.module.particles.ParticleHooks.emittedBursts()
                        + " spawns=" + mix.client.mixui.visuals.furiah.module.particles.ParticleHooks.spawnedCount()
                        + " rendered=" + particleRenderPeak);
                break;
            case 200:
                Mod trailsMod = Mix.getInstance().getModManager()
                        .getModByTranslateKey(TranslateText.TRAILS.getKey());
                if (trailsMod instanceof FuriahVisualMod) {
                    FuriahVisualMod visual = (FuriahVisualMod) trailsMod;
                    ((TrailsModule) visual.getEffect()).style.set(TrailStyle.COMET);
                    visual.saveSettings();
                }
                shot("ui-visuals-world");
                break;
            case 216:
                int error = GL11.glGetError();
                int enabled = 0;
                for (Mod mod : Mix.getInstance().getModManager().getMods()) {
                    if (mod.getNameKey().equals(TranslateText.TRAILS.getKey())
                            || mod.getNameKey().equals(TranslateText.LANDING_IMPACT.getKey())
                            || mod.getNameKey().equals(TranslateText.AMBIENCE.getKey())
                            || mod.getNameKey().equals(TranslateText.VISUAL_PARTICLES.getKey())) {
                        if (mod.isToggled()) {
                            enabled++;
                        }
                    }
                }
                int handlers = Mix.getInstance().getEventManager().get(EventRender3D.class) == null
                        ? 0 : Mix.getInstance().getEventManager().get(EventRender3D.class).size();
                boolean particleHooks = FuriahRuntime.get().isParticleHooksInstalled();
                int trailStyles = TrailStyle.values().length;
                int landingStyles = LandingStyle.values().length;
                int ambienceStyles = AmbientStyle.values().length;
                MixLogger.info("[uicapture] visuals enabled=" + enabled
                        + " render3DHandlers=" + handlers + " particleHooks=" + particleHooks
                        + " styles=" + trailStyles + "/" + landingStyles + "/" + ambienceStyles
                        + " GL error=" + error);
                if (enabled != 4 || handlers == 0 || !particleHooks || particleRenderPeak == 0
                        || trailStyles != 24 || landingStyles != 24 || ambienceStyles != 12
                        || error != GL11.GL_NO_ERROR) {
                    fail("visual module runtime verification failed");
                    return;
                }
                System.out.println("[MixClient] MIXCLIENT_SMOKE_OK");
                mc.shutdown();
                break;
            default:
                break;
        }
    }

    private static CosmeticsCategory getCosmeticsCategory(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiModMenu)) {
            return null;
        }
        Category category = ((GuiModMenu) mc.currentScreen).getCategoryByClass(CosmeticsCategory.class);
        return category instanceof CosmeticsCategory ? (CosmeticsCategory) category : null;
    }

    private static void selectCosmeticsTab(Minecraft mc, CosmeticsTab tab) {
        CosmeticsCategory cosmetics = getCosmeticsCategory(mc);
        if (cosmetics != null) {
            cosmetics.selectTab(tab);
        }
    }

    private static void enableVisual(String key) {
        Mod mod = Mix.getInstance().getModManager().getModByTranslateKey(key);
        if (mod != null && !mod.isToggled()) {
            mod.setToggled(true);
        }
    }

    private static void setSmokePosition(double x, double y, double z) {
        net.minecraft.client.entity.EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        player.setPosition(x, y, z);
        player.prevPosX = x;
        player.prevPosY = y;
        player.prevPosZ = z;
        player.lastTickPosX = x;
        player.lastTickPosY = y;
        player.lastTickPosZ = z;
    }

    private static int visualListSize(String key, String fieldName) {
        Mod mod = Mix.getInstance().getModManager().getModByTranslateKey(key);
        Object target = mod instanceof FuriahVisualMod ? ((FuriahVisualMod) mod).getEffect() : mod;
        if (target == null) {
            return -1;
        }
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof java.util.Collection ? ((java.util.Collection<?>) value).size() : -1;
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static boolean launchWorld(Minecraft mc) {
        try {
            if (mc.theWorld != null) {
                return true;
            }

            // GameType 用静态工厂取：这份 mcp jar 里 WorldSettings$GameType 的
            // InnerClasses 名字被重映射成了 a，javac 不认 WorldSettings.GameType 这写法。
            WorldSettings settings = new WorldSettings(4321L, WorldSettings.getGameTypeById(1),
                    true, false, WorldType.FLAT);
            settings.setWorldName("uicapture");
            settings.enableCommands();

            mc.launchIntegratedServer("uicapture", "uicapture", settings);
            MixLogger.info("[uicapture] launching flat capture world");
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            MixLogger.info("[uicapture] world launch failed");
            return false;
        }
    }

    private static void runSequence(Minecraft mc, int step) {

        switch (step) {
            case 40:
                openMenu(mc);
                dumpTheme();
                break;
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
                if (FULLSCREEN && FRAME_PROBE) {
                    shot("flicker-" + step);
                }
                break;
            case 70:
                if (FULLSCREEN && mc.currentScreen instanceof GuiModMenu) {
                    long started = System.nanoTime();
                    ((GuiModMenu) mc.currentScreen).keyTyped((char) 0, Keyboard.KEY_ESCAPE);
                    long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                    boolean closed = mc.currentScreen == null;
                    MixLogger.info("[uicapture] Esc closed=" + closed + " elapsedMs=" + elapsedMs);
                    if (!closed || elapsedMs > 500L) {
                        fail("Esc close delayed or failed");
                        return;
                    }
                    openMenu(mc);
                }
                break;
            case 80:
                shot("ui-home");
                break;
            case 84:
                gotoCategory(mc, ModuleCategory.class);
                break;
            case 120:
                shot("ui-module");
                break;
            case 124:
                gotoCategory(mc, SettingCategory.class);
                break;
            case 160:
                shot("ui-setting");
                break;
            case 164:
                gotoCategory(mc, MusicCategory.class);
                break;
            case 200:
                shot("ui-music");
                break;
            case 204:
                gotoCategory(mc, ModuleCategory.class);
                break;
            case 240:
                shot("ui-module2");
                break;
            case 244:
                shot("ui-frost-on");
                break;
            case 248:
                // 对照：同样一帧、同样分类，只把模糊半径压到 0。
                // 两张图在顶部那一带的差异就是「模糊本身」贡献的像素。
                blurProbeOff = true;
                MixLogger.info("[uicapture] blur probe off");
                break;
            case 284:
                shot("ui-frost-off");
                break;
            case 288:
                blurProbeOff = false;
                MixLogger.info("[uicapture] sequence finished");
                break;
            case 292:
                // 合入的 DangoMusic 界面：走它自己的入口（LegacyPlatform.openMusicScreen），
                // 与玩家点「DangoMusic」按钮时执行的完全是同一条路径。
                mix.client.mixui.integration.dango.DangoBridge.openScreen(mc.currentScreen);
                break;
            case 320:
                shot("ui-dango");
                break;
            case 340:
                if (mc.currentScreen != null) {
                    mc.displayGuiScreen(null);
                }
                MixLogger.info("[uicapture] dango screen closed, available="
                    + mix.client.mixui.integration.dango.DangoBridge.isAvailable());
                break;
            case 350:
                if (FULLSCREEN) {
                    mc.displayGuiScreen(new GuiEditHUD(true));
                }
                break;
            case 370:
                shot("ui-hud-editor");
                break;
            case 375:
                if (mc.currentScreen instanceof GuiEditHUD) {
                    mc.displayGuiScreen(null);
                }
                break;
            case 380:
                shot("ui-hud-fullscreen");
                break;
            case 390:
                if (FULLSCREEN && fullscreenToggled && !fullscreenRestored) {
                    int error = GL11.glGetError();
                    MixLogger.info("[uicapture] fullscreen GL error=" + error);
                    if (error != GL11.GL_NO_ERROR) {
                        fail("GL error " + error + " after fullscreen menu render");
                        return;
                    }
                    mc.toggleFullscreen();
                    fullscreenRestored = true;
                }
                break;
            case 400:
                int enabledHud = 0;
                for (Mod mod : Mix.getInstance().getModManager().getMods()) {
                    if (mod instanceof HUDMod && mod.isToggled()) {
                        enabledHud++;
                    }
                }
                int renderHandlers = Mix.getInstance().getEventManager().get(EventRender2D.class) == null
                        ? 0 : Mix.getInstance().getEventManager().get(EventRender2D.class).size();
                MixLogger.info("[uicapture] HUD enabled=" + enabledHud
                        + " renderHandlers=" + renderHandlers + " renderEvents=" + EventRender2D.getCount());
                // 关掉所有 GUI 后再截：Dango 的 DynamicIsland / SongCard 是挂在
                // WorldRenderEndEvent 上的 HUD，之前每张图都带着整屏菜单，必然被盖掉。
                // 这里留 60 tick（约 3 秒）让弹簧把 w/h 从 0 长到目标尺寸。
                shot("ui-hud");
                System.out.println("[MixClient] MIXCLIENT_SMOKE_OK");
                mc.shutdown();
                break;
            default:
                break;
        }
    }

    private static void fail(String reason) {
        System.out.println("[MixClient] MIXCLIENT_SMOKE_FAIL: " + reason);
        Minecraft.getMinecraft().shutdown();
    }

    private static void dumpTheme() {
        try {
            MixLogger.info("[uicapture] surface=" + mix.client.mixui.gui.modmenu.ModMenuTheme.surface()
                + " container=" + mix.client.mixui.gui.modmenu.ModMenuTheme.surfaceContainer()
                + " containerLow=" + mix.client.mixui.gui.modmenu.ModMenuTheme.surfaceContainerLow()
                + " onSurface=" + mix.client.mixui.gui.modmenu.ModMenuTheme.onSurface()
                + " onSurfaceVariant=" + mix.client.mixui.gui.modmenu.ModMenuTheme.onSurfaceVariant()
                + " primary=" + mix.client.mixui.gui.modmenu.ModMenuTheme.primary()
                + " isDark=" + mix.client.mixui.gui.modmenu.ModMenuTheme.isDark());
            Minecraft mc = Minecraft.getMinecraft();
            net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
            MixLogger.info("[uicapture] guiScale=" + sr.getScaleFactor() + " scaled=" + sr.getScaledWidth() + "x" + sr.getScaledHeight()
                + " display=" + mc.displayWidth + "x" + mc.displayHeight
                + " world=" + (mc.theWorld != null));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void openMenu(Minecraft mc) {

        if (!(mc.currentScreen instanceof GuiModMenu)) {
            mc.displayGuiScreen(Mix.getInstance().getApi().getModMenu());
            MixLogger.info("[uicapture] opened mod menu");
        }
    }

    private static void gotoCategory(Minecraft mc, Class<?> clazz) {

        if (!(mc.currentScreen instanceof GuiModMenu)) {
            MixLogger.info("[uicapture] not in menu, skip " + clazz.getSimpleName());
            return;
        }

        GuiModMenu menu = (GuiModMenu) mc.currentScreen;
        Category category = menu.getCategoryByClass(clazz);

        if (category != null) {
            menu.setCurrentCategory(category);
            MixLogger.info("[uicapture] category -> " + clazz.getSimpleName());
        }
    }

    private static void shot(String name) {

        Minecraft mc = Minecraft.getMinecraft();

        try {
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            MixLogger.info("[uicapture] saved " + name);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
