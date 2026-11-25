package mix.client.mixui.integration.dango;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import mix.client.mixui.Mix;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.management.event.impl.EventTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * DangoMusic（原 Forge 1.8.9 模组）与 Mix 客户端之间的桥。
 *
 * <p>DangoMusic 原本靠 {@code me.sorihiko.dango.forge.DangoMusicMod} 与
 * {@code me.sorihiko.dango.forge.ForgeBridge} 挂在 Forge 的
 * {@code MinecraftForge.EVENT_BUS} / {@code FMLCommonHandler.bus()} 上。Mix 里没有
 * Forge，那两个类没有进包，改由这里转发同样三个时机：</p>
 *
 * <ul>
 *   <li>初始化：{@code LegacyDango.start()}，等价于 FMLInitializationEvent 那一次</li>
 *   <li>每 tick：先 {@code LegacyGl.init()}（幂等，首次真正链接 shader / 载字体），
 *       再 {@code DangoMusic.callEvent(TickEvent.INSTANCE)}，对应 ForgeBridge.onClientTick
 *       的 phase == START 分支</li>
 *   <li>世界渲染末尾：{@code DangoMusic.callEvent(new WorldRenderEndEvent(partialTicks))}，
 *       对应 ForgeBridge.onRenderWorldLast</li>
 * </ul>
 *
 * <p>全部走反射。合进来的 dango class 是「MCP 类名 + SRG 成员名」，而 Mix 源码按 MCP
 * 成员名编译，直接 import 会让构建链必须多挂一份只给 javac 看的 dango jar。反射把这份
 * 依赖推迟到运行期：类不在时只打一条日志，客户端照常启动。</p>
 */
public class DangoBridge {

    private static boolean initialized;

    private static boolean available;

    private static boolean tickFailureLogged;

    private static boolean renderFailureLogged;

    /**
     * 诊断开关。生产环境默认静默：只有加 {@code -Dmixclient.dango.diag=true} 启动才会打印
     * 事件转发计数、模块 enabled 状态、HUD 弹簧尺寸与帧缓冲顶部取样。
     *
     * <p>保留这段代码的价值在于：以后 HUD 又不见了，不用重新编译就能定位是「没转发」
     * 「模块没启用」还是「画了但被盖住」这三种完全不同的原因。</p>
     */
    private static final boolean DIAG = Boolean.getBoolean("mixclient.dango.diag");

    /** 诊断计数：证明 Mix 的事件真的每帧/每 tick 转发到了 Dango 侧。 */
    private static int tickCount;

    private static int renderCount;

    private static int renderLogged;

    private static boolean moduleDumped;

    private static Method dangoCallEvent;

    private static Object tickEventInstance;

    private static Constructor<?> worldRenderEndCtor;

    private static Object legacyGlInstance;

    private static Method legacyGlInit;

    private static Object legacyPlatformInstance;

    private static Method legacyPlatformOpenMusicScreen;

    private static Method legacyDangoStart;

    private static Class<?> legacyMusicScreenClass;

    /** 由 {@code Mix.start()} 调用 —— 那时 GL 上下文已经随 startGame 建立好了。 */
    public static void init() {

        if (initialized) {
            return;
        }

        initialized = true;

        try {
            resolve();
        } catch (Throwable t) {
            available = false;
            logFailure("[dango] DangoMusic 类解析失败，音乐播放器不可用", t);
            return;
        }

        try {
            legacyDangoStart.invoke(null);
            MixLogger.info("[dango] DangoMusic started");
        } catch (Throwable t) {
            available = false;
            logFailure("[dango] DangoMusic start 失败", t);
            return;
        }

        available = true;

        if (DIAG) {
            probeNativeHud("DynamicIslandModule");
            probeNativeHud("SongCardModule");
        }

        // 把 Dango 的深色玻璃色板换成 Mix 的色板（反射改 Theme 的 static final 字段）
        DangoThemeBridge.apply();

        try {
            Mix.getInstance().getEventManager().register(new DangoBridge());
            MixLogger.info("[dango] DangoMusic 事件桥已注册");
        } catch (Throwable t) {
            MixLogger.error("[dango] DangoMusic 事件桥注册失败: " + t);
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * 打印反射调用的真实失败原因。
     *
     * <p>{@code InvocationTargetException.toString()} 只有包装类名，真正抛出的异常在
     * {@code getCause()} 里。之前把 {@code t} 直接拼进日志，于是「DangoMusic start 失败」
     * 永远只显示一个 {@code java.lang.reflect.InvocationTargetException}，既看不出是哪一行、
     * 也看不出是什么异常，只能靠猜。这里剥掉包装层并让 log4j 打完整堆栈。</p>
     */
    private static void logFailure(String message, Throwable thrown) {

        Throwable root = thrown;
        while (root instanceof java.lang.reflect.InvocationTargetException
                && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        MixLogger.error(message + ": " + root);
        MixLogger.getLogger().error("[dango] " + message + " 堆栈", root);
    }

    /** 打开 DangoMusic 自己的音乐界面（自绘 Skia 风格界面），关掉后回到游戏。 */
    public static void openScreen() {
        openScreen(null);
    }

    /**
     * 打开 DangoMusic 音乐界面，{@code parent} 作为它的父界面 —— 界面里的返回按钮会回到
     * {@code parent}，而不是直接掉回游戏。{@code parent} 为 null 时与原 Forge 行为一致。
     */
    public static void openScreen(Object parent) {

        if (!available) {
            MixLogger.info("[dango] openScreen skipped: bridge not available");
            return;
        }

        if (parent == null) {
            try {
                legacyPlatformOpenMusicScreen.invoke(legacyPlatformInstance);
                MixLogger.info("[dango] opened music screen");
            } catch (Throwable t) {
                MixLogger.error("[dango] openScreen 失败: " + t);
            }
            return;
        }

        try {
            // LegacyMusicScreen(GuiScreen parent) —— 参数类型是 notch 的 GuiScreen，
            // 编译期拿不到那个名字，所以按「能被当前界面实例赋值」这个条件挑构造函数。
            Constructor<?> ctor = null;
            for (Constructor<?> candidate : legacyMusicScreenClass.getConstructors()) {
                Class<?>[] params = candidate.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(parent.getClass())) {
                    ctor = candidate;
                    break;
                }
            }

            if (ctor == null) {
                legacyPlatformOpenMusicScreen.invoke(legacyPlatformInstance);
                MixLogger.info("[dango] opened music screen (no parent ctor, fallback)");
                return;
            }

            Object screen = ctor.newInstance(parent);
            Minecraft.getMinecraft().displayGuiScreen((GuiScreen) screen);
            MixLogger.info("[dango] opened music screen with parent " + parent.getClass().getSimpleName());
        } catch (Throwable t) {
            MixLogger.error("[dango] openScreen(parent) 失败: " + t);
        }
    }

    /**
     * 每 tick 转发。ForgeBridge 只在 {@code TickEvent.ClientTickEvent.phase == START}
     * 时发一次；Mix 的 {@code EventTick} 出自 {@code MixinMinecraft} 的 runTick 注入点，
     * 一侧一次，频率等价。
     */
    @EventTarget
    public void onTick(EventTick event) {

        if (!available) {
            return;
        }

        try {
            // LegacyGl.init() 里 glReady 是纯 static 标记，第一次置 true 后永不重试 ——
            // 所以必须等到 GL 就绪之后再进来，失败了也不会把状态永久锁死在这里。
            legacyGlInit.invoke(legacyGlInstance);
            dangoCallEvent.invoke(null, tickEventInstance);

            if (DIAG) {
                tickCount++;
                if (tickCount == 1) {
                    MixLogger.info("[dango] diag tick forwarded (first)");
                }
                if (tickCount == 3 && !moduleDumped) {
                    moduleDumped = true;
                    dumpModules();
                }
            }
        } catch (Throwable t) {
            if (!tickFailureLogged) {
                tickFailureLogged = true;
                MixLogger.error("[dango] tick 转发失败: " + t);
            }
        }
    }

    /**
     * 世界渲染末尾转发。{@code EventRender3D} 出自
     * {@code MixinEntityRenderer#renderWorldPass} 对 {@code renderHand} 字段的注入
     * （shift = BEFORE），此时投影还是 3D 那套、MODELVIEW 已位移到相机位置，与 Forge
     * {@code RenderWorldLastEvent} 语义一致 —— Dango 的浮动歌词正需要这个时机。
     */
    @EventTarget
    public void onRender3D(EventRender3D event) {

        if (!available) {
            return;
        }

        try {
            int bindingBefore = -1;
            if (DIAG) {
                try {
                    bindingBefore = GL11.glGetInteger(36006);
                } catch (Throwable ignored) {
                    // 极端情况下拿不到，保持 -1
                }
            }

            Object renderEvent = worldRenderEndCtor.newInstance(Float.valueOf(event.getPartialTicks()));
            dangoCallEvent.invoke(null, renderEvent);

            if (!DIAG) {
                return;
            }

            renderCount++;

            if (renderCount == 1) {
                MixLogger.info("[dango] diag render forwarded (first) partialTicks=" + event.getPartialTicks());
            } else if (renderCount == 2 || renderCount == 10 || renderCount == 60
                    || renderCount == 300 || renderCount == 1800) {
                if (renderLogged < 6) {
                    renderLogged++;
                    MixLogger.info("[dango] diag render forwarded " + renderCount);
                }
            }

            // 诊断：渲染阶段再 dump 一次弹簧尺寸 + 直接从帧缓冲读回顶部像素。
            // 前者判定「render 是不是提前 return 了」，后者判定「画了但看不见」。
            if (renderCount == 300 || renderCount == 1800) {
                int bindingAfter = -1;
                try {
                    bindingAfter = GL11.glGetInteger(36006);
                } catch (Throwable ignored) {
                    // ignore
                }
                MixLogger.info("[dango] diag fbo frame" + renderCount
                        + " before=" + bindingBefore + " after=" + bindingAfter
                        + " screen=" + Minecraft.getMinecraft().displayWidth + "x" + Minecraft.getMinecraft().displayHeight);
                dumpIsland("frame" + renderCount);
                probeTop("frame" + renderCount, "current");
                probeTop("frame" + renderCount, "screen");
            }
        } catch (Throwable t) {
            if (!renderFailureLogged) {
                renderFailureLogged = true;
                MixLogger.error("[dango] world render 转发失败: " + t);
            }
        }
    }


    private static void probeNativeHud(String simpleName) {
        try {
            Object module = moduleInstance("me.sorihiko.dango.module.impls.music." + simpleName);
            if (module != null) {
                Object event = worldRenderEndCtor.newInstance(0.0F);
                module.getClass().getMethod("render", event.getClass()).invoke(module, event);
            }
        } catch (Throwable t) {
            MixLogger.error("[dango] native HUD probe failed: " + simpleName + ": " + t);
            t.printStackTrace();
        }
    }

    /** 诊断用：按类名取一个已注册模块实例，取不到返回 null。 */
    private static Object moduleInstance(String className) {

        ClassLoader loader = DangoBridge.class.getClassLoader();

        try {
            Class<?> dangoMusic = Class.forName("me.sorihiko.dango.DangoMusic", true, loader);
            Object companion = dangoMusic.getField("Companion").get(null);
            Object instance = companion.getClass().getMethod("getInstance").invoke(companion);
            if (instance == null) {
                return null;
            }
            Object moduleManager = dangoMusic.getMethod("getModuleManager").invoke(instance);
            Class<?> type = Class.forName(className, true, loader);
            return moduleManager.getClass().getMethod("get", Class.class).invoke(moduleManager, type);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 诊断用：打印 DynamicIslandModule 的弹簧尺寸 / 可见性状态。 */
    private static void dumpIsland(String tag) {

        Object module = moduleInstance("me.sorihiko.dango.module.impls.music.DynamicIslandModule");
        if (module == null) {
            MixLogger.info("[dango] diag island " + tag + ": module is null");
            return;
        }

        Object ctx = readField(module, "context");
        MixLogger.info("[dango] diag island " + tag
                + " enabled=" + readField(module, "enabled")
                + " w=" + readFloat(module, "w")
                + " h=" + readFloat(module, "h")
                + " vw=" + readFloat(module, "vw")
                + " vh=" + readFloat(module, "vh")
                + " alpha=" + readFloat(module, "contentAlpha")
                + " shown=" + readField(module, "shown")
                + " lastNanos=" + readField(module, "lastNanos")
                + " glass=" + readSetting(module, "glass")
                + " scale=" + readSetting(module, "scale")
                + " ctx=" + (ctx == null ? "null" : ctx.getClass().getSimpleName()));
    }

    /**
     * 诊断用：直接把当前绑定帧缓冲顶部 96 行像素读回来，统计亮度。
     *
     * <p>这是唯一能区分「Dango 根本没画」与「画了但被别的东西盖住 / 画在视口外」的手段 ——
     * 截图是帧末的结果，读回是这一帧这个时刻的结果。</p>
     */
    private static void probeTop(String tag, String target) {

        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;
        int band = Math.min(96, height);

        int previous = -1;
        if ("screen".equals(target)) {
            try {
                previous = GL11.glGetInteger(36006);
                org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT(
                        org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            } catch (Throwable t) {
                MixLogger.error("[dango] diag probeTop " + tag + " bind screen failed: " + t);
                return;
            }
        }

        java.nio.ByteBuffer buffer = BufferUtils.createByteBuffer(width * band * 4);

        try {
            GL11.glReadPixels(0, height - band, width, band, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        } catch (Throwable t) {
            MixLogger.error("[dango] diag probeTop " + tag + " readPixels failed: " + t);
            buffer = null;
        } finally {
            if ("screen".equals(target) && previous >= 0) {
                try {
                    org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT(
                            org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT, previous);
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }

        if (buffer == null) {
            return;
        }

        int maxLuma = 0;
        int bright = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;

        for (int row = 0; row < band; row++) {
            for (int col = 0; col < width; col++) {
                int idx = (row * width + col) * 4;
                int r = buffer.get(idx) & 0xFF;
                int g = buffer.get(idx + 1) & 0xFF;
                int b = buffer.get(idx + 2) & 0xFF;
                int luma = (r * 299 + g * 587 + b * 114) / 1000;
                if (luma > maxLuma) {
                    maxLuma = luma;
                }
                if (luma > 200) {
                    bright++;
                    // row 0 = 屏幕最上面一行（我们是从底部往上读的）
                    int screenY = band - 1 - row;
                    if (col < minX) minX = col;
                    if (col > maxX) maxX = col;
                    if (screenY < minY) minY = screenY;
                    if (screenY > maxY) maxY = screenY;
                }
            }
        }

        MixLogger.info("[dango] diag probeTop " + tag + "/" + target + " size=" + width + "x" + band
                + " maxLuma=" + maxLuma + " bright(>200)=" + bright
                + " bbox=" + (maxX < 0 ? "none" : "(" + minX + "," + minY + ")-(" + maxX + "," + maxY + ")"));
    }

    /**
     * 诊断用：把 4 个音乐模块的 enabled 状态、以及 DynamicIslandModule 的弹簧尺寸/内容透明度
     * 打印出来。HUD 不出现时，靠这几个数字区分「模块没启用」和「尺寸弹簧没长起来」。
     */
    private static void dumpModules() {

        ClassLoader loader = DangoBridge.class.getClassLoader();

        String[] names = new String[] {
                "me.sorihiko.dango.module.impls.music.NeteaseMusicModule",
                "me.sorihiko.dango.module.impls.music.LyricsModule",
                "me.sorihiko.dango.module.impls.music.SongCardModule",
                "me.sorihiko.dango.module.impls.music.DynamicIslandModule"
        };

        StringBuilder sb = new StringBuilder("[dango] diag modules:");

        Class<?> dangoMusic;
        Object instance;
        Object moduleManager;
        try {
            dangoMusic = Class.forName("me.sorihiko.dango.DangoMusic", true, loader);
            Object companion = dangoMusic.getField("Companion").get(null);
            instance = companion.getClass().getMethod("getInstance").invoke(companion);
            if (instance == null) {
                MixLogger.info("[dango] diag modules: DangoMusic instance is null");
                return;
            }
            moduleManager = dangoMusic.getMethod("getModuleManager").invoke(instance);
        } catch (Throwable t) {
            MixLogger.error("[dango] diag modules 反射失败: " + t);
            return;
        }

        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, true, loader);
                Object module = moduleManager.getClass().getMethod("get", Class.class).invoke(moduleManager, type);
                if (module == null) {
                    sb.append(' ').append(type.getSimpleName()).append("=null");
                    continue;
                }
                Object enabled = type.getMethod("getEnabled").invoke(module);
                sb.append(' ').append(type.getSimpleName()).append('=').append(enabled);

                if ("DynamicIslandModule".equals(type.getSimpleName())) {
                    sb.append(" w=").append(readFloat(module, "w"));
                    sb.append(" h=").append(readFloat(module, "h"));
                    sb.append(" vw=").append(readFloat(module, "vw"));
                    sb.append(" vh=").append(readFloat(module, "vh"));
                    sb.append(" alpha=").append(readFloat(module, "contentAlpha"));
                    sb.append(" shown=").append(readField(module, "shown"));
                    sb.append(" lastNanos=").append(readField(module, "lastNanos"));
                }
            } catch (Throwable t) {
                sb.append(' ').append(name.substring(name.lastIndexOf('.') + 1)).append("=ERR(")
                        .append(t.getClass().getSimpleName()).append(')');
            }
        }

        MixLogger.info(sb.toString());

        dumpListeners(instance);
    }

    /** 诊断用：直接读 Dango EventManager 的 listenerStore，确认 WorldRenderEndEvent 真的有订阅者。 */
    private static void dumpListeners(Object dangoMusic) {

        try {
            Object eventManager = dangoMusic.getClass().getMethod("getEventManager").invoke(dangoMusic);
            Field storeField = eventManager.getClass().getDeclaredField("listenerStore");
            storeField.setAccessible(true);
            Object store = storeField.get(eventManager);
            if (!(store instanceof java.util.Map)) {
                MixLogger.info("[dango] diag listeners: listenerStore is " + store);
                return;
            }
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) store;
            StringBuilder sb = new StringBuilder("[dango] diag listeners keys=").append(map.size());
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                int size = value instanceof java.util.Collection ? ((java.util.Collection<?>) value).size() : -1;
                String key = String.valueOf(entry.getKey());
                sb.append(' ').append(key.substring(key.lastIndexOf('.') + 1)).append(':').append(size);
            }
            MixLogger.info(sb.toString());
        } catch (Throwable t) {
            MixLogger.error("[dango] diag listeners 反射失败: " + t);
        }
    }

    private static Object readField(Object target, String field) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field f = type.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable t) {
                return "ERR";
            }
        }
        return "ERR";
    }

    private static Object readFloat(Object target, String field) {
        return readField(target, field);
    }

    /** 诊断用：读 Dango 的 BooleanValue / FloatValue 这类配置对象，取出它当前的值。 */
    private static Object readSetting(Object target, String field) {
        Object value = readField(target, field);
        if (value == null || value instanceof String) {
            return value;
        }
        for (String method : new String[] { "get", "getValue" }) {
            try {
                return value.getClass().getMethod(method).invoke(value);
            } catch (Throwable ignored) {
                // 试下一个
            }
        }
        return value;
    }

    private static void resolve() throws Exception {

        ClassLoader loader = DangoBridge.class.getClassLoader();

        Class<?> dangoEvent = Class.forName("me.sorihiko.dango.event.Event", true, loader);
        Class<?> dangoMusic = Class.forName("me.sorihiko.dango.DangoMusic", true, loader);
        dangoCallEvent = dangoMusic.getMethod("callEvent", dangoEvent);

        Class<?> tickEvent = Class.forName("me.sorihiko.dango.events.TickEvent", true, loader);
        tickEventInstance = tickEvent.getField("INSTANCE").get(null);

        Class<?> worldRenderEnd = Class.forName("me.sorihiko.dango.events.WorldRenderEndEvent", true, loader);
        worldRenderEndCtor = worldRenderEnd.getConstructor(float.class);

        Class<?> legacyGl = Class.forName("me.sorihiko.dango.legacy.LegacyGl", true, loader);
        legacyGlInstance = legacyGl.getField("INSTANCE").get(null);
        legacyGlInit = legacyGl.getMethod("init");

        Class<?> legacyPlatform = Class.forName("me.sorihiko.dango.legacy.LegacyPlatform", true, loader);
        legacyPlatformInstance = legacyPlatform.getField("INSTANCE").get(null);
        legacyPlatformOpenMusicScreen = legacyPlatform.getMethod("openMusicScreen");

        Class<?> legacyDango = Class.forName("me.sorihiko.dango.legacy.LegacyDango", true, loader);
        legacyDangoStart = legacyDango.getMethod("start");

        legacyMusicScreenClass = Class.forName("me.sorihiko.dango.legacy.LegacyMusicScreen", true, loader);

        if (tickEventInstance == null || legacyGlInstance == null || legacyPlatformInstance == null) {
            throw new IllegalStateException("dango singleton INSTANCE is null");
        }
    }
}