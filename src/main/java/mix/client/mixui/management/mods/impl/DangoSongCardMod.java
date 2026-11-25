package mix.client.mixui.management.mods.impl;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
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

public class DangoSongCardMod extends HUDMod {

    private static final float CARD_WIDTH = 160.0F;
    private static final float CARD_HEIGHT = 40.0F;
    private static final float COVER_SIZE = 28.0F;
    private static final float APPEAR_SPEED = 20.0F;
    private static final float SWITCH_SPEED = 28.0F;
    private static final long STALL_MS = 250L;
    private static final float HIDDEN_EPSILON = 0.004F;
    private static final float CROSSFADE_MIN_APPEAR = 0.6F;
    private static final float APPEAR_RISE = 3.0F;

    private final BooleanSetting coverSetting = new BooleanSetting(TranslateText.DANGO_COVER, this, true);
    private final BooleanSetting progressSetting = new BooleanSetting(TranslateText.DANGO_PROGRESS, this, true);
    private final BooleanSetting waveSetting = new BooleanSetting(TranslateText.DANGO_WAVE, this, true);
    private final SimpleAnimation appearanceAnimation = new SimpleAnimation(0.0F);
    private final SimpleAnimation switchAnimation = new SimpleAnimation(1.0F);

    private boolean placed;
    private String lastKey;
    private long lastTickMs;
    private String previousTitle;
    private String previousArtists;
    private ResourceLocation previousCover;
    private boolean previousHasTrack;
    private float previousProgress;
    private Object shownTrack;
    private boolean liveHasTrack;
    private boolean shownHasTrack;
    private String shownTitle;
    private String shownArtists;
    private ResourceLocation shownCover;
    private float shownProgress;

    public DangoSongCardMod() {
        super(TranslateText.DANGO_SONG_CARD, TranslateText.DANGO_SONG_CARD_DESCRIPTION);
        this.setToggled(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!DangoMusicApi.isAvailable()) {
            return;
        }
        Object track = DangoMusicApi.current();
        boolean hasTrack = track != null;
        liveHasTrack = hasTrack;
        tick(appearanceAnimation, hasTrack || isEditing() ? 1.0F : 0.0F, APPEAR_SPEED);
        float appear = appearanceAnimation.getValue();

        if (hasTrack || isEditing()) {
            String key = keyOf(track);
            if (lastKey == null || !key.equals(lastKey)) {
                boolean crossFade = lastKey != null && shownTitle != null && appear >= CROSSFADE_MIN_APPEAR;
                if (crossFade) {
                    previousTitle = shownTitle;
                    previousArtists = shownArtists;
                    previousCover = shownCover;
                    previousHasTrack = shownHasTrack;
                    previousProgress = shownProgress;
                    switchAnimation.setValue(0.0F);
                } else {
                    switchAnimation.setValue(1.0F);
                }
                lastKey = key;
            }
            shownTrack = track;
            shownHasTrack = hasTrack;
            shownTitle = hasTrack ? DangoMusicApi.nameOf(track) : TranslateText.DANGO_EMPTY.getText();
            shownArtists = hasTrack ? DangoMusicApi.artistsOf(track) : "";
            if (!hasTrack) {
                shownCover = null;
                shownProgress = 0.0F;
            }
        }

        tick(switchAnimation, 1.0F, SWITCH_SPEED);
        if (appear < HIDDEN_EPSILON) {
            appearanceAnimation.setValue(0.0F);
            return;
        }

        NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
        nvg.setupAndDraw(() -> drawCard(nvg, appear, switchAnimation.getValue()));
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
        String title = DangoMusicApi.nameOf(track);
        String artists = DangoMusicApi.artistsOf(track);
        return (title == null ? "" : title) + "\n" + (artists == null ? "" : artists);
    }

    private void drawCard(NanoVGManager nvg, float appear, float t) {
        Color ink = getFontColor();
        Color subInk = getFontColor(180);
        boolean refresh = liveHasTrack;
        ResourceLocation cover = shownCover;
        float progress = shownProgress;

        if (refresh) {
            cover = DangoMusicApi.coverOf(shownTrack);
            progress = DangoMusicApi.progress();
            shownCover = cover;
            shownProgress = progress;
        }

        setWidth((int) CARD_WIDTH);
        setHeight((int) CARD_HEIGHT);
        if (!hasPosition()) {
            ScaledResolution sr = new ScaledResolution(mc);
            setX(Math.round(sr.getScaledWidth() - CARD_WIDTH * getScale() - 8.0F));
            setY(Math.round(sr.getScaledHeight() - CARD_HEIGHT * getScale() - 8.0F));
            placed = true;
        }

        nvg.save();
        try {
            nvg.translate(0.0F, (1.0F - appear) * APPEAR_RISE);
            drawBackground(CARD_WIDTH, CARD_HEIGHT, 6.0F, appear);

            float textX = 10.0F;
            if (coverSetting.isToggled()) {
                float coverY = (CARD_HEIGHT - COVER_SIZE) / 2.0F;
                if (t < 1.0F) {
                    drawCoverSlot(nvg, previousCover, subInk, 7.0F, coverY, appear * (1.0F - t));
                }
                drawCoverSlot(nvg, cover, subInk, 7.0F, coverY, appear * t);
                textX = 7.0F + COVER_SIZE + 9.0F;
            }

            float rightEdge = CARD_WIDTH - 10.0F;
            float waveReserve = waveSetting.isToggled() ? 20.0F : 0.0F;
            float textMax = Math.max(40.0F, rightEdge - textX - waveReserve);
            float slide = (1.0F - t) * 3.0F;

            if (t < 1.0F) {
                drawTitleBlock(nvg, previousTitle, previousArtists, textX - slide, textMax, ink, subInk, appear * (1.0F - t));
            }
            drawTitleBlock(nvg, shownTitle, shownArtists, textX + slide, textMax, ink, subInk, appear * t);

            if (waveSetting.isToggled()) {
                drawWave(rightEdge - 18.0F, 15.0F, 10.0F, refresh && DangoMusicApi.isPlaying(), fade(ink, appear));
            }

            if (progressSetting.isToggled()) {
                float barX = textX;
                float barWidth = Math.max(40.0F, rightEdge - barX);
                if (t < 1.0F && previousHasTrack) {
                    drawProgressBlock(nvg, barX, barWidth, previousProgress, appear * (1.0F - t));
                }
                if (shownHasTrack) {
                    drawProgressBlock(nvg, barX, barWidth, progress, appear * t);
                }
            }
        } finally {
            nvg.restore();
        }
    }

    private void drawCoverSlot(NanoVGManager nvg, ResourceLocation cover, Color subInk, float x, float y, float alpha) {
        if (alpha <= HIDDEN_EPSILON) {
            return;
        }
        if (cover != null) {
            nvg.drawRoundedTexture(cover, getX() + x * getScale(), getY() + y * getScale(),
                    COVER_SIZE * getScale(), COVER_SIZE * getScale(), 5.0F * getScale(), alpha);
        } else {
            drawRoundedRect(x, y, COVER_SIZE, COVER_SIZE, 5.0F, fade(getFontColor(22), alpha));
            drawCenteredText(Icon.MUSIC, x + COVER_SIZE / 2.0F, y + COVER_SIZE / 2.0F + 2.0F, 15.0F, Fonts.ICON, fade(subInk, alpha));
        }
    }

    private void drawTitleBlock(NanoVGManager nvg, String title, String artists, float x, float maxWidth, Color ink, Color subInk, float alpha) {
        if (alpha <= HIDDEN_EPSILON || title == null) {
            return;
        }
        String limitedTitle = nvg.getLimitText(title, HUD_FONT_SIZE, Fonts.REGULAR, maxWidth);
        String limitedArtists = nvg.getLimitText(artists == null ? "" : artists, 7.5F, Fonts.REGULAR, maxWidth);
        drawText(limitedTitle, x, 5.0F, HUD_FONT_SIZE, Fonts.REGULAR, fade(ink, alpha));
        drawText(limitedArtists, x, 18.0F, 7.5F, Fonts.REGULAR, fade(subInk, alpha));
    }

    private void drawProgressBlock(NanoVGManager nvg, float x, float width, float progress, float alpha) {
        if (alpha <= HIDDEN_EPSILON) {
            return;
        }
        drawRoundedRect(x, 33.0F, width, 2.0F, 1.0F, fade(getFontColor(30), alpha));
        float filled = width * clamp01(progress);
        if (filled > 0.5F) {
            nvg.drawGradientRoundedRect(getX() + x * getScale(), getY() + 33.0F * getScale(),
                    filled * getScale(), 2.0F * getScale(), 1.0F * getScale(),
                    fade(ModMenuTheme.primary(), alpha), fade(ModMenuTheme.secondary(), alpha));
        }
    }

    private void drawWave(float x, float y, float maxHeight, boolean playing, Color color) {
        long now = System.currentTimeMillis();
        float barWidth = 1.5F;
        float gap = 2.0F;
        for (int i = 0; i < 4; i++) {
            double phase = now / 190.0 + i * 0.9;
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
