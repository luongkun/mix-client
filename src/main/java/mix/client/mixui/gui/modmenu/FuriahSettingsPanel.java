package mix.client.mixui.gui.modmenu;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.mouse.MouseUtils;
import mix.client.mixui.utils.mouse.Scroll;
import mix.client.mixui.visuals.FuriahVisualMod;
import mix.client.mixui.visuals.furiah.module.particles.ImageAssignmentSetting;
import mix.client.mixui.visuals.furiah.module.particles.LibraryImageSetting;
import mix.client.mixui.visuals.furiah.module.particles.ParticlesModule;
import mix.client.mixui.visuals.furiah.setting.BooleanSetting;
import mix.client.mixui.visuals.furiah.setting.ColorSetting;
import mix.client.mixui.visuals.furiah.setting.EnumSetSetting;
import mix.client.mixui.visuals.furiah.setting.ModeSetting;
import mix.client.mixui.visuals.furiah.setting.Setting;
import mix.client.mixui.visuals.furiah.setting.SliderSetting;
import mix.client.mixui.visuals.furiah.setting.TextSetting;

public final class FuriahSettingsPanel {
    private final Scroll scroll = new Scroll();
    private final Map<Setting, Rect> rows = new HashMap<Setting, Rect>();
    private final Map<Setting, Rect> sliders = new HashMap<Setting, Rect>();
    private final Map<ColorSetting, Map<String, Rect>> colorSliders = new HashMap<ColorSetting, Map<String, Rect>>();
    private FuriahVisualMod mod;
    private Setting dragging;
    private ColorSetting colorDragging;
    private String colorDragKey;
    private double colorDragMin, colorDragMax;
    private Setting textEditing;
    private final StringBuilder textBuffer = new StringBuilder();
    private float panelX, panelY, panelWidth, panelHeight;
    private Rect closeButton;
    private Rect resetButton;
    private Rect imageFolderButton;

    public boolean isOpen() {
        return mod != null;
    }

    public void open(FuriahVisualMod mod) {
        this.mod = mod;
        this.dragging = null;
        this.colorDragging = null;
        this.colorDragKey = null;
        this.textEditing = null;
        scroll.resetAll();
        rows.clear();
        sliders.clear();
    }

    public void close() {
        mod = null;
        dragging = null;
        colorDragging = null;
        colorDragKey = null;
        textEditing = null;
        rows.clear();
        sliders.clear();
        colorSliders.clear();
    }

    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks,
                     float categoryX, float categoryY, float categoryWidth, float categoryHeight) {
        if (mod == null) {
            return;
        }
        panelWidth = Math.min(540F, categoryWidth - 40F);
        panelHeight = Math.min(440F, categoryHeight - 40F);
        panelX = categoryX + (categoryWidth - panelWidth) / 2F;
        panelY = categoryY + (categoryHeight - panelHeight) / 2F;
        closeButton = new Rect(panelX + panelWidth - 40F, panelY + 16F, 24F, 24F);
        resetButton = new Rect(panelX + panelWidth - 148F, panelY + 16F, 96F, 24F);
        imageFolderButton = mod.getEffect() instanceof ParticlesModule
                ? new Rect(panelX + panelWidth - 252F, panelY + 16F, 92F, 24F) : null;

        nvg.drawRoundedRect(categoryX, categoryY, categoryWidth, categoryHeight, 0F,
                new Color(0, 0, 0, 120));
        nvg.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 18F,
                ModMenuTheme.surfaceContainerLow());
        nvg.drawGradientOutlineRoundedRect(panelX, panelY, panelWidth, panelHeight, 18F, 1.5F,
                ModMenuTheme.primaryContainer(), ModMenuTheme.primaryContainer());
        nvg.drawText(mod.getName(), panelX + 20F, panelY + 20F, ModMenuTheme.onSurface(),
                15F, Fonts.DEMIBOLD);
        nvg.drawText(TranslateText.SETTINGS.getText(), panelX + 20F, panelY + 42F,
                ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);
        drawButton(nvg, resetButton, TranslateText.RESET.getText(), false);
        if (imageFolderButton != null) {
            drawButton(nvg, imageFolderButton, TranslateText.IMAGE.getText(), false);
        }
        drawButton(nvg, closeButton, Icon.X, true);

        List<Setting> settings = mod.getFuriahSettings();
        float viewportY = panelY + 66F;
        float viewportHeight = panelHeight - 84F;
        float contentHeight = 0F;
        for (Setting setting : settings) {
            contentHeight += height(setting) + 8F;
        }
        scroll.setMaxScroll(Math.max(0F, contentHeight - viewportHeight));
        if (MouseUtils.isInside(mouseX, mouseY, panelX + 12F, viewportY,
                panelWidth - 24F, viewportHeight)) {
            scroll.onScroll();
        }
        scroll.onAnimation();
        updateDragging(mouseX);

        rows.clear();
        sliders.clear();
        colorSliders.clear();
        nvg.save();
        nvg.scissor(panelX + 12F, viewportY, panelWidth - 24F, viewportHeight);
        nvg.translate(0F, scroll.getValue());
        float y = viewportY + 4F;
        for (Setting setting : settings) {
            float h = height(setting);
            Rect row = new Rect(panelX + 16F, y, panelWidth - 32F, h);
            rows.put(setting, row);
            nvg.drawRoundedRect(row.x, row.y, row.width, row.height, 10F,
                    ModMenuTheme.surfaceContainer());
            drawSetting(nvg, setting, row, mouseX, mouseY);
            y += h + 8F;
        }
        nvg.restore();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mod == null || mouseButton != 0) {
            return;
        }
        if (closeButton != null && closeButton.contains(mouseX, mouseY)) {
            close();
            return;
        }
        if (imageFolderButton != null && imageFolderButton.contains(mouseX, mouseY)) {
            try {
                Desktop.getDesktop().open(((ParticlesModule) mod.getEffect()).imageFolder());
            } catch (Exception ignored) {
            }
            return;
        }
        if (resetButton != null && resetButton.contains(mouseX, mouseY)) {
            mod.resetSettings();
            textEditing = null;
            return;
        }
        if (!MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            close();
            return;
        }
        for (Map.Entry<Setting, Rect> entry : rows.entrySet()) {
            Setting setting = entry.getKey();
            Rect row = entry.getValue();
            if (!row.contains(mouseX, mouseY)) {
                continue;
            }
            float localY = mouseY - row.y;
            if (setting instanceof BooleanSetting) {
                BooleanSetting bool = (BooleanSetting) setting;
                bool.set(!bool.get());
                mod.saveSettings();
            } else if (setting instanceof SliderSetting) {
                Rect track = sliders.get(setting);
                if (track != null && track.contains(mouseX, mouseY)) {
                    dragging = setting;
                    updateSlider(setting, mouseX, track);
                }
            } else if (setting instanceof ModeSetting) {
                ModeSetting<?> mode = (ModeSetting<?>) setting;
                if (mouseX > row.x + row.width - 44F) {
                    mode.setIndex(mode.getIndex() + 1);
                } else if (mouseX > row.x + row.width - 176F) {
                    mode.setIndex(mode.getIndex() - 1);
                } else {
                    mode.setIndex(mode.getIndex() + 1);
                }
                mod.saveSettings();
            } else if (setting instanceof ColorSetting) {
                colorClicked((ColorSetting) setting, row, mouseX, mouseY);
            } else if (setting instanceof EnumSetSetting) {
                enumClicked((EnumSetSetting<?>) setting, row, mouseX, localY);
            } else if (setting instanceof TextSetting) {
                beginText(setting, ((TextSetting) setting).get());
            } else if (setting instanceof LibraryImageSetting) {
                beginText(setting, ((LibraryImageSetting) setting).get());
            } else if (setting instanceof ImageAssignmentSetting) {
                imageAssignmentClicked((ImageAssignmentSetting) setting, row, mouseX, localY);
            }
            return;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        dragging = null;
        colorDragging = null;
        colorDragKey = null;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (textEditing == null) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
            finishText();
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (textBuffer.length() > 0) {
                textBuffer.deleteCharAt(textBuffer.length() - 1);
            }
            applyText();
        } else if (typedChar >= ' ' && typedChar != 127) {
            int max = textEditing instanceof TextSetting
                    ? ((TextSetting) textEditing).getMaxLength() : 64;
            if (textBuffer.length() < max) {
                textBuffer.append(typedChar);
                applyText();
            }
        }
    }

    private void drawSetting(NanoVGManager nvg, Setting setting, Rect row, int mouseX, int mouseY) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting) setting;
            nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 12F,
                    ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
            float x = row.x + row.width - 54F;
            float y = row.y + 9F;
            nvg.drawRoundedRect(x, y, 42F, 18F, 9F, ModMenuTheme.surfaceContainerHighest());
            nvg.drawGradientRoundedRect(x, y, 42F, 18F, 9F,
                    ModMenuTheme.applyAlpha(ModMenuTheme.primary(), bool.get() ? 255 : 0),
                    ModMenuTheme.applyAlpha(ModMenuTheme.secondary(), bool.get() ? 255 : 0));
            nvg.drawRoundedRect(x + (bool.get() ? 24F : 3F), y + 3F, 12F, 12F, 6F,
                    bool.get() ? Color.WHITE : ModMenuTheme.onSurfaceVariant());
        } else if (setting instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) setting;
            nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 8F,
                    ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
            nvg.drawText(slider.format(), row.x + row.width - 74F, row.y + 8F,
                    ModMenuTheme.onSurfaceVariant(), 9F, Fonts.REGULAR);
            Rect track = new Rect(row.x + 12F, row.y + 28F, row.width - 24F, 6F);
            sliders.put(setting, track);
            drawSlider(nvg, slider, track);
        } else if (setting instanceof ModeSetting) {
            ModeSetting<?> mode = (ModeSetting<?>) setting;
            nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 12F,
                    ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
            float x = row.x + row.width - 176F;
            nvg.drawRoundedRect(x, row.y + 7F, 164F, 22F, 8F, ModMenuTheme.surfaceContainerHighest());
            nvg.drawText("<", x + 8F, row.y + 13F, ModMenuTheme.onSurfaceVariant(), 10F, Fonts.REGULAR);
            nvg.drawText(">", x + 148F, row.y + 13F, ModMenuTheme.onSurfaceVariant(), 10F, Fonts.REGULAR);
            nvg.drawCenteredText(nvg.getLimitText(ModeSetting.displayName(mode.get()), 9.5F,
                    Fonts.MEDIUM, 126F), x + 82F, row.y + 14F, Color.WHITE, 9.5F, Fonts.MEDIUM);
        } else if (setting instanceof ColorSetting) {
            drawColor(nvg, (ColorSetting) setting, row);
        } else if (setting instanceof EnumSetSetting) {
            drawEnumSet(nvg, (EnumSetSetting<?>) setting, row);
        } else if (setting instanceof TextSetting || setting instanceof LibraryImageSetting) {
            String value = setting instanceof TextSetting
                    ? ((TextSetting) setting).get() : ((LibraryImageSetting) setting).get();
            nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 7F,
                    ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
            nvg.drawRoundedRect(row.x + 12F, row.y + 22F, row.width - 24F, 22F, 7F,
                    ModMenuTheme.surfaceContainerHighest());
            nvg.drawText(value + (textEditing == setting ? "|" : ""), row.x + 20F, row.y + 29F,
                    ModMenuTheme.onSurface(), 9.5F, Fonts.REGULAR);
        } else if (setting instanceof ImageAssignmentSetting) {
            drawImageAssignment(nvg, (ImageAssignmentSetting) setting, row);
        } else {
            nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 12F,
                    ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
        }
    }

    private void drawColor(NanoVGManager nvg, ColorSetting setting, Rect row) {
        nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 8F,
                ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
        float modeX = row.x + row.width - 142F;
        nvg.drawRoundedRect(modeX, row.y + 5F, 130F, 22F, 8F, ModMenuTheme.surfaceContainerHighest());
        nvg.drawCenteredText(ModeSetting.displayName(setting.getMode()), modeX + 65F, row.y + 13F,
                Color.WHITE, 9.5F, Fonts.MEDIUM);
        float paletteX = row.x + 74F;
        for (int i = 0; i < 12; i++) {
            Color color = Color.getHSBColor(i / 12F, 0.78F, 1.0F);
            nvg.drawRoundedRect(paletteX + i * 20F, row.y + 34F, 16F, 14F, 4F, color);
        }
        nvg.drawText("A", row.x + 12F, row.y + 35F, ModMenuTheme.onSurfaceVariant(), 9F, Fonts.MEDIUM);
        nvg.drawRoundedRect(row.x + 34F, row.y + 33F, 26F, 16F, 5F,
                new Color(setting.getColorA(), true));
        float y = row.y + 58F;
        if (setting.getMode() == ColorSetting.Mode.GRADIENT) {
            for (int i = 0; i < 12; i++) {
                Color color = Color.getHSBColor(i / 12F, 0.78F, 1.0F);
                nvg.drawRoundedRect(paletteX + i * 20F, y, 16F, 14F, 4F, color);
            }
            nvg.drawText("B", row.x + 12F, y + 1F, ModMenuTheme.onSurfaceVariant(), 9F, Fonts.MEDIUM);
            nvg.drawRoundedRect(row.x + 34F, y - 1F, 26F, 16F, 5F,
                    new Color(setting.getColorB(), true));
            y += 26F;
            y = drawSmallSlider(nvg, setting, "Scroll", setting.getScrollSpeed(), 0D, 3D,
                    row.x + 12F, y, row.width - 24F);
        } else if (setting.getMode() == ColorSetting.Mode.CHROMA) {
            y = drawSmallSlider(nvg, setting, "Speed", setting.getChromaSpeed(), 0.1D, 3D,
                    row.x + 12F, y, row.width - 24F);
            y = drawSmallSlider(nvg, setting, "Saturation", setting.getChromaSaturation(), 0D, 1D,
                    row.x + 12F, y, row.width - 24F);
            y = drawSmallSlider(nvg, setting, "Brightness", setting.getChromaBrightness(), 0.1D, 1D,
                    row.x + 12F, y, row.width - 24F);
            drawSmallSlider(nvg, setting, "Spread", setting.getChromaSpread(), 0D, 1D,
                    row.x + 12F, y, row.width - 24F);
        }
    }

    private void drawEnumSet(NanoVGManager nvg, EnumSetSetting setting, Rect row) {
        nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 8F,
                ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
        float x = row.x + 12F;
        float y = row.y + 30F;
        for (Enum<?> value : setting.values()) {
            String name = ModeSetting.displayName(value);
            float width = nvg.getTextWidth(name, 9F, Fonts.MEDIUM) + 22F;
            if (x + width > row.x + row.width - 12F) {
                x = row.x + 12F;
                y += 25F;
            }
            boolean on = setting.includes(value);
            nvg.drawRoundedRect(x, y, width, 20F, 8F,
                    on ? ModMenuTheme.primaryContainer() : ModMenuTheme.surfaceContainerHighest());
            nvg.drawCenteredText(name, x + width / 2F, y + 7F,
                    on ? ModMenuTheme.onPrimaryContainer() : ModMenuTheme.onSurfaceVariant(),
                    9F, Fonts.MEDIUM);
            x += width + 6F;
        }
    }

    private void drawImageAssignment(NanoVGManager nvg, ImageAssignmentSetting setting, Rect row) {
        nvg.drawText(setting.getLabel(), row.x + 12F, row.y + 8F,
                ModMenuTheme.onSurface(), 10F, Fonts.MEDIUM);
        nvg.drawRoundedRect(row.x + row.width - 142F, row.y + 5F, 130F, 22F, 8F,
                ModMenuTheme.surfaceContainerHighest());
        nvg.drawCenteredText(ModeSetting.displayName(setting.getMode()),
                row.x + row.width - 77F, row.y + 13F, Color.WHITE, 9.5F, Fonts.MEDIUM);
        if (mod.getEffect() instanceof ParticlesModule) {
            ParticlesModule particles = (ParticlesModule) mod.getEffect();
            particles.ensureLibrary();
            List<String> names = particles.imageNames();
            float x = row.x + 12F;
            float y = row.y + 34F;
            int shown = 0;
            for (String name : names) {
                if (shown++ >= 8) {
                    break;
                }
                float width = Math.min(150F, nvg.getTextWidth(name, 8.5F, Fonts.REGULAR) + 20F);
                if (x + width > row.x + row.width - 12F) {
                    x = row.x + 12F;
                    y += 24F;
                }
                boolean on = setting.uses(name);
                nvg.drawRoundedRect(x, y, width, 20F, 7F,
                        on ? ModMenuTheme.primaryContainer() : ModMenuTheme.surfaceContainerHighest());
                nvg.drawCenteredText(nvg.getLimitText(name, 8.5F, Fonts.REGULAR, width - 12F),
                        x + width / 2F, y + 7F,
                        on ? ModMenuTheme.onPrimaryContainer() : ModMenuTheme.onSurfaceVariant(),
                        8.5F, Fonts.REGULAR);
                x += width + 6F;
            }
        }
    }

    private float drawSmallSlider(NanoVGManager nvg, ColorSetting setting, String label,
                                  double value, double min, double max,
                                  float x, float y, float width) {
        nvg.drawText(label, x, y, ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);
        nvg.drawText(String.format("%.2f", value), x + width - 42F, y,
                ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);
        Rect track = new Rect(x, y + 14F, width, 4F);
        Map<String, Rect> tracks = colorSliders.get(setting);
        if (tracks == null) {
            tracks = new HashMap<String, Rect>();
            colorSliders.put(setting, tracks);
        }
        tracks.put(label, track);
        float fraction = (float) ((value - min) / (max - min));
        nvg.drawRoundedRect(track.x, track.y, track.width, track.height, 2F,
                ModMenuTheme.surfaceContainerHighest());
        nvg.drawGradientRoundedRect(track.x, track.y, track.width * fraction, track.height, 2F,
                ModMenuTheme.primary(), ModMenuTheme.secondary());
        return y + 34F;
    }

    private void drawSlider(NanoVGManager nvg, SliderSetting slider, Rect track) {
        float fraction = (float) ((slider.get() - slider.getMin()) / (slider.getMax() - slider.getMin()));
        nvg.drawRoundedRect(track.x, track.y, track.width, track.height, 3F,
                ModMenuTheme.surfaceContainerHighest());
        nvg.drawGradientRoundedRect(track.x, track.y, track.width * fraction, track.height, 3F,
                ModMenuTheme.primary(), ModMenuTheme.secondary());
        nvg.drawRoundedRect(track.x + track.width * fraction - 5F, track.y - 3F, 10F, 12F, 6F,
                Color.WHITE);
    }

    private void drawButton(NanoVGManager nvg, Rect rect, String label, boolean iconOnly) {
        nvg.drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 8F,
                ModMenuTheme.surfaceContainerHigh());
        if (iconOnly) {
            nvg.drawCenteredText(label, rect.x + rect.width / 2F, rect.y + 8F,
                    ModMenuTheme.onSurfaceVariant(), 10F, Fonts.ICON);
        } else {
            nvg.drawCenteredText(label, rect.x + rect.width / 2F, rect.y + 8F,
                    ModMenuTheme.onSurface(), 9F, Fonts.MEDIUM);
        }
    }

    private void colorClicked(ColorSetting setting, Rect row, int mouseX, int mouseY) {
        float localY = mouseY - row.y;
        Map<String, Rect> tracks = colorSliders.get(setting);
        if (tracks != null) {
            for (Map.Entry<String, Rect> entry : tracks.entrySet()) {
                if (entry.getValue().contains(mouseX, mouseY)) {
                    colorDragging = setting;
                    colorDragKey = entry.getKey();
                    if ("Speed".equals(colorDragKey)) {
                        colorDragMin = 0.1D;
                        colorDragMax = 3.0D;
                    } else if ("Brightness".equals(colorDragKey)) {
                        colorDragMin = 0.1D;
                        colorDragMax = 1.0D;
                    } else if ("Scroll".equals(colorDragKey)) {
                        colorDragMin = 0.0D;
                        colorDragMax = 3.0D;
                    } else {
                        colorDragMin = 0.0D;
                        colorDragMax = 1.0D;
                    }
                    updateColorSlider(mouseX, entry.getValue());
                    return;
                }
            }
        }
        if (mouseX > row.x + row.width - 142F && localY < 32F) {
            ColorSetting.Mode[] values = ColorSetting.Mode.values();
            for (int i = 1; i <= values.length; i++) {
                ColorSetting.Mode next = values[(setting.getMode().ordinal() + i) % values.length];
                if (next != ColorSetting.Mode.GLOBAL || setting.allowsGlobal()) {
                    setting.setMode(next);
                    break;
                }
            }
            mod.saveSettings();
            return;
        }
        if (mouseX >= row.x + 74F && mouseX <= row.x + 314F && localY >= 31F && localY <= 51F) {
            int index = Math.max(0, Math.min(11, (int) ((mouseX - row.x - 74F) / 20F)));
            setting.setColorA(Color.getHSBColor(index / 12F, 0.78F, 1.0F).getRGB());
            mod.saveSettings();
            return;
        }
        if (setting.getMode() == ColorSetting.Mode.GRADIENT
                && mouseX >= row.x + 74F && mouseX <= row.x + 314F
                && localY >= 55F && localY <= 75F) {
            int index = Math.max(0, Math.min(11, (int) ((mouseX - row.x - 74F) / 20F)));
            setting.setColorB(Color.getHSBColor(index / 12F, 0.78F, 1.0F).getRGB());
            mod.saveSettings();
        }
    }

    private void enumClicked(EnumSetSetting setting, Rect row, int mouseX, float localY) {
        float x = row.x + 12F;
        float y = row.y + 30F;
        for (Enum<?> value : setting.values()) {
            String name = ModeSetting.displayName(value);
            float width = Mix.getInstance().getNanoVGManager().getTextWidth(name, 9F, Fonts.MEDIUM) + 22F;
            if (x + width > row.x + row.width - 12F) {
                x = row.x + 12F;
                y += 25F;
            }
            if (mouseX >= x && mouseX <= x + width && mouseYOrLocal(localY, y, row.y)) {
                setting.toggle(value);
                mod.saveSettings();
                return;
            }
            x += width + 6F;
        }
    }

    private boolean mouseYOrLocal(float localY, float chipY, float rowY) {
        return localY >= chipY - rowY && localY <= chipY - rowY + 20F;
    }

    private void imageAssignmentClicked(ImageAssignmentSetting setting, Rect row, int mouseX, float localY) {
        if (mouseX > row.x + row.width - 142F && localY < 32F) {
            ImageAssignmentSetting.Mode[] values = ImageAssignmentSetting.Mode.values();
            setting.setMode(values[(setting.getMode().ordinal() + 1) % values.length]);
            mod.saveSettings();
            return;
        }
        if (!(mod.getEffect() instanceof ParticlesModule)) {
            return;
        }
        ParticlesModule particles = (ParticlesModule) mod.getEffect();
        particles.ensureLibrary();
        List<String> names = particles.imageNames();
        float x = row.x + 12F;
        float y = row.y + 34F;
        int shown = 0;
        for (String name : names) {
            if (shown++ >= 8) {
                break;
            }
            float width = Math.min(150F, Mix.getInstance().getNanoVGManager()
                    .getTextWidth(name, 8.5F, Fonts.REGULAR) + 20F);
            if (x + width > row.x + row.width - 12F) {
                x = row.x + 12F;
                y += 24F;
            }
            if (mouseX >= x && mouseX <= x + width && localY >= y - row.y && localY <= y - row.y + 20F) {
                setting.click(name);
                mod.saveSettings();
                return;
            }
            x += width + 6F;
        }
    }

    private void updateDragging(int mouseX) {
        if (dragging instanceof SliderSetting) {
            Rect track = sliders.get(dragging);
            if (track != null) {
                updateSlider(dragging, mouseX, track);
            }
        } else if (colorDragging != null) {
            Map<String, Rect> tracks = colorSliders.get(colorDragging);
            Rect track = tracks == null ? null : tracks.get(colorDragKey);
            if (track != null) {
                updateColorSlider(mouseX, track);
            }
        }
    }

    private void updateColorSlider(int mouseX, Rect track) {
        float fraction = Math.max(0F, Math.min(1F, (mouseX - track.x) / track.width));
        double value = colorDragMin + fraction * (colorDragMax - colorDragMin);
        if ("Speed".equals(colorDragKey)) {
            colorDragging.setChromaSpeed(value);
        } else if ("Saturation".equals(colorDragKey)) {
            colorDragging.setChromaSaturation((float) value);
        } else if ("Brightness".equals(colorDragKey)) {
            colorDragging.setChromaBrightness((float) value);
        } else if ("Spread".equals(colorDragKey)) {
            colorDragging.setChromaSpread((float) value);
        } else if ("Scroll".equals(colorDragKey)) {
            colorDragging.setScrollSpeed(value);
        }
        mod.saveSettings();
    }

    private void updateSlider(Setting setting, int mouseX, Rect track) {
        SliderSetting slider = (SliderSetting) setting;
        float fraction = Math.max(0F, Math.min(1F, (mouseX - track.x) / track.width));
        slider.set(slider.getMin() + fraction * (slider.getMax() - slider.getMin()));
        mod.saveSettings();
    }

    private void beginText(Setting setting, String value) {
        textEditing = setting;
        textBuffer.setLength(0);
        textBuffer.append(value);
    }

    private void applyText() {
        if (textEditing instanceof TextSetting) {
            ((TextSetting) textEditing).set(textBuffer.toString());
        } else if (textEditing instanceof LibraryImageSetting) {
            ((LibraryImageSetting) textEditing).set(textBuffer.toString());
        }
        mod.saveSettings();
    }

    private void finishText() {
        applyText();
        textEditing = null;
    }

    private float height(Setting setting) {
        if (setting instanceof ColorSetting) {
            ColorSetting.Mode mode = ((ColorSetting) setting).getMode();
            return mode == ColorSetting.Mode.CHROMA ? 210F : (mode == ColorSetting.Mode.GRADIENT ? 142F : 92F);
        }
        if (setting instanceof ImageAssignmentSetting) {
            return 116F;
        }
        if (setting instanceof EnumSetSetting) {
            return 70F;
        }
        if (setting instanceof SliderSetting) {
            return 48F;
        }
        return 38F;
    }

    private static final class Rect {
        private final float x, y, width, height;

        private Rect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
