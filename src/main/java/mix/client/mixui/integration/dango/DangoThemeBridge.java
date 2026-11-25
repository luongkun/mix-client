package mix.client.mixui.integration.dango;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.logger.MixLogger;

/**
 * 把 DangoMusic 的主题色板换成 Mix 的色板。
 *
 * <p>DangoMusic 自己的界面是「深色玻璃 + 白字」风格：文字是纯白、卡片是白色低透明度、
 * 玻璃着色器里还有一条 {@code if (l > u_MaxLuma) col *= u_MaxLuma / l;} 把整窗亮度硬压到
 * 30%。这些值全部集中在 {@code me.sorihiko.dango.ui.Theme} 的 13 个 static final Color 与
 * 3 个 static final GlassStyle 里，界面各处都只通过 {@code Theme.INSTANCE.getXXX()} 取色
 * （引用次数 63/39/25/22/12/9/5/4…），所以换掉这 16 个字段就能整体换色，不必重编译
 * Dango 的 1545 个 class。</p>
 *
 * <p>为什么能改 static final：这些字段的类型是 {@code Color} / {@code GlassStyle} 对象，
 * 不是 String 或基本类型 —— 后两者会被 javac 内联进调用点，改了也读不到新值；对象类型
 * 走的是 GETSTATIC，反射 {@code modifiers &= ~FINAL} 后 {@code Field.set(null, ...)} 生效
 * （已在真机 JRE 1.8.0_51 上验证）。</p>
 */
public class DangoThemeBridge {

    private static boolean ready;

    private static boolean failed;

    private static Class<?> themeClass;

    private static Class<?> colorClass;

    private static Constructor<?> colorCtor;

    private static Class<?> glassClass;

    private static Constructor<?> glassCtor;

    private static Class<?> shadowClass;

    private static Constructor<?> shadowCtor;

    private static Class<?> vec2Class;

    private static Constructor<?> vec2Ctor;

    private static Class<?> uiKtClass;

    /** 由 {@code DangoBridge.init()} 与每次开屏前调用；重复调用只是重新对齐当前主题。 */
    public static void apply() {

        if (failed) {
            return;
        }

        try {
            if (!ready) {
                resolve();
                ready = true;
            }
        } catch (Throwable t) {
            failed = true;
            MixLogger.error("[dango] 主题类解析失败，保持 DangoMusic 原色板: " + t);
            return;
        }

        try {
            applyColors();
            applyGlass();
            applyAccent();
            MixLogger.info("[dango] 主题已换成 Mix 色板: text=" + read("TEXT_PRIMARY")
                    + " tint=" + glassTint("GLASS_WINDOW") + " maxLuma=" + glassMaxLuma("GLASS_WINDOW"));
        } catch (Throwable t) {
            failed = true;
            MixLogger.error("[dango] 主题换色失败，保持 DangoMusic 原色板: " + t);
        }
    }

    private static void resolve() throws Exception {

        themeClass = Class.forName("me.sorihiko.dango.ui.Theme");
        colorClass = Class.forName("me.sorihiko.dango.utils.Color");
        colorCtor = colorClass.getConstructor(float.class, float.class, float.class, float.class);

        glassClass = Class.forName("me.sorihiko.dango.ui.GlassStyle");
        glassCtor = glassClass.getConstructor(float.class, colorClass, float.class, float.class,
                float.class, float.class, float.class, float.class, float.class,
                Class.forName("me.sorihiko.dango.ui.ShadowStyle"), colorClass);

        shadowClass = Class.forName("me.sorihiko.dango.ui.ShadowStyle");
        vec2Class = Class.forName("me.sorihiko.dango.ui.Vec2");
        vec2Ctor = vec2Class.getConstructor(float.class, float.class);
        shadowCtor = shadowClass.getConstructor(colorClass, float.class, vec2Class);

        try {
            uiKtClass = Class.forName("me.sorihiko.dango.musicui.UiKt");
        } catch (Throwable ignored) {
            uiKtClass = null;
        }
    }

    private static void applyColors() throws Exception {

        java.awt.Color onSurface = ModMenuTheme.onSurface();
        java.awt.Color variant = ModMenuTheme.onSurfaceVariant();

        setField("TEXT_PRIMARY", color(onSurface, 0.96F));
        setField("TEXT_SECONDARY", color(variant, 0.86F));
        setField("TEXT_TERTIARY", color(variant, 0.58F));

        setField("SWITCH_OFF", color(variant, 0.30F));
        setField("TRACK", color(variant, 0.24F));

        setField("CARD", color(onSurface, 0.05F));
        setField("CARD_HOVER", color(onSurface, 0.09F));
        setField("HAIRLINE", color(onSurface, 0.10F));
        setField("SEPARATOR", color(onSurface, 0.08F));

        setField("ICON_DISABLED", color(ModMenuTheme.outline(), 1.0F));

        java.awt.Color success = ModMenuTheme.success();
        setField("GREEN", color(success, 1.0F));
    }

    private static void applyGlass() throws Exception {

        // 玻璃着色器最后一步是 col = mix(col, u_Tint.rgb, u_Tint.a)，所以「面板是什么颜色」
        // 几乎完全由 tint 的 alpha 决定：
        //   tint.a = 0.14 -> 只剩 14% 白，86% 是背后被糊开的世界（草绿、天空蓝）-> 像一块彩色毛玻璃
        //   tint.a = 0.84 -> 84% 白，世界只透出一点光晕 -> 与 Mix 的 surface(254) 一致
        // 同时 maxLuma 必须接近 1：glass.frag 里 if (l > u_MaxLuma) col *= u_MaxLuma / l;
        // 原来的 0.3 会把整窗亮度硬压到 30%，既暗又和浅色面板完全冲突。
        Object windowShadow = shadowCtor.newInstance(color(ModMenuTheme.onSurfaceVariant(), 0.20F),
                Float.valueOf(26.0F), vec2(0.0F, -6.0F));
        // v12 实测（_scratch/d3-dango.png）：tint.a=0.84 时窗口内空白处仍被背景世界染成
        // (192,215,228) 的湖蓝与 (127,147,151) 的墨绿，同一块面板在不同位置不同色，
        // 读起来完全不像 Mix 的 254 面板。glass.frag 的 mix 是线性的，只要把 a 提到 0.96，
        // 世界只贡献 4%（约 ±10 灰度）作为「磨砂感」，底色就稳定落在 245~250。
        setField("GLASS_WINDOW", glass(18.0F, color(java.awt.Color.WHITE, 0.96F), 0.98F, 1.00F,
                0.66F, 8.0F, 12.0F, 0.05F, 0.30F, windowShadow, color(ModMenuTheme.surface(), 0.98F)));

        Object sidebarShadow = shadowCtor.newInstance(color(ModMenuTheme.onSurfaceVariant(), 0.12F),
                Float.valueOf(12.0F), vec2(0.0F, -3.0F));
        setField("GLASS_SIDEBAR", glass(14.0F, color(ModMenuTheme.mix(ModMenuTheme.surfaceContainerLow(),
                java.awt.Color.WHITE, 0.45F), 0.955F), 0.98F, 1.00F,
                0.55F, 6.0F, 10.0F, 0.04F, 0.26F, sidebarShadow,
                color(ModMenuTheme.surfaceContainerLow(), 0.97F)));

        Object selectionShadow = shadowCtor.newInstance(color(ModMenuTheme.onSurfaceVariant(), 0.10F),
                Float.valueOf(10.0F), vec2(0.0F, -2.0F));
        setField("GLASS_SELECTION", glass(10.0F, color(ModMenuTheme.primary(), 0.70F), 0.98F, 1.06F,
                0.46F, 5.0F, 8.0F, 0.05F, 0.30F, selectionShadow,
                color(ModMenuTheme.mix(ModMenuTheme.primary(), ModMenuTheme.surface(), 0.72F), 0.96F)));
    }

    /**
     * {@code UiKt.RED}（#FF375F）是 Dango 界面里滑条、开关启用态、输入框描边用的强调色，
     * 不走 Theme，单独换掉。它同样是 static final 对象字段，可以反射改。
     */
    private static void applyAccent() throws Exception {

        if (uiKtClass == null) {
            return;
        }

        setStaticField(uiKtClass, "RED", color(ModMenuTheme.primary(), 1.0F));
    }

    private static Object read(String name) throws Exception {
        Field field = themeClass.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object glassTint(String name) throws Exception {
        Object style = read(name);
        return glassClass.getMethod("getTint").invoke(style);
    }

    private static Object glassMaxLuma(String name) throws Exception {
        Object style = read(name);
        return glassClass.getMethod("getMaxLuma").invoke(style);
    }

    private static Object glass(float radius, Object tint, float maxLuma, float saturation, float frost,
            float refraction, float bevel, float dispersion, float highlight, Object shadow, Object fallback)
            throws Exception {

        return glassCtor.newInstance(Float.valueOf(radius), tint, Float.valueOf(maxLuma),
                Float.valueOf(saturation), Float.valueOf(frost), Float.valueOf(refraction),
                Float.valueOf(bevel), Float.valueOf(dispersion), Float.valueOf(highlight), shadow, fallback);
    }

    private static Object vec2(float x, float y) throws Exception {
        return vec2Ctor.newInstance(Float.valueOf(x), Float.valueOf(y));
    }

    private static Object color(java.awt.Color base, float alpha) throws Exception {
        return colorCtor.newInstance(Float.valueOf(base.getRed() / 255.0F),
                Float.valueOf(base.getGreen() / 255.0F), Float.valueOf(base.getBlue() / 255.0F),
                Float.valueOf(alpha));
    }

    private static void setField(String name, Object value) throws Exception {
        setStaticField(themeClass, name, value);
    }

    private static void setStaticField(Class<?> owner, String name, Object value) throws Exception {

        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);

        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Throwable ignored) {
            // JDK 12+ 把 modifiers 藏进反射过滤器；Java 8 上不会走到这里。
        }

        field.set(null, value);
    }
}
