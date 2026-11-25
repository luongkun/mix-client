package mix.client.mixui.management.mods.impl;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.integration.dango.DangoMusicApi;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.HUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class DangoIslandMod extends HUDMod {

    private static final float COVER_SIZE = 12.0F;
    private static final float WAVE_WIDTH = 18.0F;
    private static final float SWITCH_SPEED = 28.0F;
    private static final long STALL_MS = 250L;
    private static final float HIDDEN_EPSILON = 0.004F;

    private final BooleanSetting coverSetting = new BooleanSetting(TranslateText.DANGO_COVER, this, true);
    private final BooleanSetting waveSetting = new BooleanSetting(TranslateText.DANGO_WAVE, this, true);
    private final SimpleAnimation switchAnimation = new SimpleAnimation(1.0F);

    private static boolean toggledOnce;
    private static boolean loggedMissing;
    private boolean placed;
    private String lastKey;
    private long lastTickMs;
    private String previousText;
    private ResourceLocation previousCover;
    private float previousTextWidth;
    private String shownText;
    private ResourceLocation shownCover;
    private float shownTextWidth;

    public DangoIslandMod() {
        super(TranslateText.DANGO_ISLAND, TranslateText.DANGO_ISLAND_DESCRIPTION);
        this.setToggled(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        toggledOnce = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!DangoMusicApi.isAvailable()) {
            if (!loggedMissing) {
                loggedMissing = true;
            }
            return;
        }
        NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
        nvg.setupAndDraw(() -> drawIsland(nvg));
    }

    private void drawIsland(NanoVGManager nvg) {
        Object track = DangoMusicApi.current();
        boolean hasTrack = track != null;
        String title = hasTrack ? DangoMusicApi.nameOf(track) : TranslateText.NOTHING_IS_PLAYING.getText();
        String limited = nvg.getLimitText(title == null ? "" : title, HUD_FONT_SIZE, Fonts.REGULAR, 170.0F);
        float textWidth = getTextWidth(limited, HUD_FONT_SIZE, Fonts.REGULAR);
        String key = keyOf(track);

        if (lastKey == null || !key.equals(lastKey)) {
            if (lastKey != null) {
                previousText = shownText;
                previousCover = shownCover;
                previousTextWidth = shownTextWidth;
                switchAnimation.setValue(0.0F);
            } else {
                switchAnimation.setValue(1.0F);
            }
            lastKey = key;
        }

        tick(switchAnimation, 1.0F, SWITCH_SPEED);
        float t = switchAnimation.getValue();
        Color ink = getFontColor();
        float coverBlock = coverSetting.isToggled() ? COVER_SIZE + HUD_ICON_GAP : 0.0F;
        float waveBlock = waveSetting.isToggled() ? WAVE_WIDTH : 0.0F;
        float width = HUD_PADDING_X * 2.0F + coverBlock + textWidth + waveBlock;
        if (t < 1.0F) {
            float previousWidth = HUD_PADDING_X * 2.0F + coverBlock + previousTextWidth + waveBlock;
            width = previousWidth + ((width - previousWidth) * t);
        }

        setWidth((int) Math.ceil(width));
        setHeight((int) HUD_HEIGHT);
        if (!hasPosition()) {
            ScaledResolution sr = new ScaledResolution(mc);
            setX(Math.round((sr.getScaledWidth() - (width * getScale())) / 2.0F));
            setY(4);
            placed = true;
        }

        drawBackground(width, HUD_HEIGHT);
        float cursorX = HUD_PADDING_X;
        float coverY = (HUD_HEIGHT - COVER_SIZE) / 2.0F;
        float textY = HUD_PADDING_Y;
        float slide = (1.0F - t) * 1.5F;
        ResourceLocation cover = hasTrack ? DangoMusicApi.coverOf(track) : null;

        if (coverSetting.isToggled()) {
            if (t < 1.0F) {
                drawCoverSlot(nvg, previousCover, ink, cursorX, coverY, 1.0F - t);
            }
            drawCoverSlot(nvg, cover, ink, cursorX, coverY, t);
            cursorX += COVER_SIZE + HUD_ICON_GAP;
        }

        if (t < 1.0F) {
            drawText(previousText == null ? "" : previousText, cursorX, textY - slide, HUD_FONT_SIZE, Fonts.REGULAR, fade(ink, 1.0F - t));
        }
        drawText(limited, cursorX, textY + slide, HUD_FONT_SIZE, Fonts.REGULAR, fade(ink, t));

        if (waveSetting.isToggled()) {
            drawWave(cursorX + textWidth + HUD_ICON_GAP, (HUD_HEIGHT - 8.0F) / 2.0F, 8.0F, DangoMusicApi.isPlaying(), ink);
        }

        shownText = limited;
        shownCover = cover;
        shownTextWidth = textWidth;
    }

    private void tick(SimpleAnimation animation, float target, float speed) {
        long now = System.currentTimeMillis();
        if (lastTickMs != 0L && now - lastTickMs > STALL_MS) {
            animation.setAnimation(animation.getValue(), speed);
        }
        lastTickMs = now;
        animation.setAnimation(target, speed);
    }

    private String keyOf(Object track) {
        if (track == null) {
            return "";
        }
        String name = DangoMusicApi.nameOf(track);
        String artists = DangoMusicApi.artistsOf(track);
        return (name == null ? "" : name) + "\n" + (artists == null ? "" : artists);
    }

    private void drawCoverSlot(NanoVGManager nvg, ResourceLocation cover, Color ink, float x, float y, float alpha) {
        if (alpha <= HIDDEN_EPSILON) {
            return;
        }
        if (cover != null) {
            nvg.drawRoundedTexture(cover, getX() + x * getScale(), getY() + y * getScale(),
                    COVER_SIZE * getScale(), COVER_SIZE * getScale(), 3.0F * getScale(), alpha);
        } else {
            drawRoundedRect(x, y, COVER_SIZE, COVER_SIZE, 3.0F, fade(getFontColor(28), alpha));
            drawText(Icon.MUSIC, x + 1.0F, y + 1.0F, HUD_ICON_SIZE, Fonts.ICON, fade(ink, alpha));
        }
    }

    private void drawWave(float x, float y, float maxHeight, boolean playing, Color color) {
        long now = System.currentTimeMillis();
        float barWidth = 1.5F;
        float gap = 2.0F;
        for (int i = 0; i < 5; i++) {
            double phase = now / 190.0 + i * 0.85;
            float factor = playing ? 0.35F + (float) ((Math.sin(phase) + 1.0) * 0.325) : 0.22F;
            float barHeight = Math.max(1.5F, maxHeight * factor);
            drawRoundedRect(x + i * (barWidth + gap), y + maxHeight - barHeight, barWidth, barHeight, barWidth / 2.0F, color);
        }
    }

    private static Color fade(Color color, float alpha) {
        int value = Math.round(color.getAlpha() * clamp01(alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, value)));
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
