package mix.client.mixui.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mix.client.mixui.Mix;
import mix.client.mixui.MixAPI;
import mix.client.mixui.gui.modmenu.CategoryIslandRenderer;
import mix.client.mixui.gui.modmenu.FuriahSettingsPanel;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.gui.modmenu.category.CosmeticsTab;
import mix.client.mixui.management.cape.CapeCategory;
import mix.client.mixui.management.cape.CapeManager;
import mix.client.mixui.management.cape.impl.Cape;
import mix.client.mixui.management.cape.impl.CustomCape;
import mix.client.mixui.management.cape.impl.NormalCape;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.settings.Setting;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.ColorSetting;
import mix.client.mixui.management.mods.settings.impl.ComboSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.notification.NotificationType;
import mix.client.mixui.ui.comp.Comp;
import mix.client.mixui.ui.comp.impl.CompComboBox;
import mix.client.mixui.ui.comp.impl.CompSlider;
import mix.client.mixui.ui.comp.impl.CompToggleButton;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.SearchUtils;
import mix.client.mixui.utils.mouse.MouseUtils;
import mix.client.mixui.visuals.FuriahVisualMod;

public class CosmeticsCategory extends Category {

    private static final float MARGIN = 24F;
    private static final float CARD_GAP = 14F;
    private static final float CHIP_HEIGHT = 26F;
    private static final float CAPE_FILTER_HEIGHT = 22F;
    private static final float CAPE_CARD_TARGET_WIDTH = 160F;
    private static final float MODULE_CARD_TARGET_WIDTH = 270F;
    private static final float MODULE_CARD_HEIGHT = 164F;

    private final CategoryIslandRenderer chipIsland = new CategoryIslandRenderer();
    private final FuriahSettingsPanel furiahSettings = new FuriahSettingsPanel();
    private final Map<Setting, Comp> settingComponents = new HashMap<Setting, Comp>();
    private CosmeticsTab currentTab = CosmeticsTab.CAPE;
    private CapeCategory currentCapeCategory = CapeCategory.ALL;
    private Mod settingsMod;

    public CosmeticsCategory(GuiModMenu parent) {
        super(parent, TranslateText.COSMETICS, Icon.SHOPPING, true);
    }

    @Override
    public void onShown() {
        chipIsland.reset();
        closeSettings();
    }

    @Override
    public void initGui() {
        currentCapeCategory = CapeCategory.ALL;
    }

    @Override
    public void initCategory() {
    }

    public void selectTab(CosmeticsTab tab) {
        if (tab == null) {
            return;
        }
        currentTab = tab;
        closeSettings();
        scroll.setScrollPosition(0);
    }

    public void showSettings(Mod mod) {
        if (mod instanceof FuriahVisualMod) {
            settingsMod = null;
            furiahSettings.open((FuriahVisualMod) mod);
        } else {
            furiahSettings.close();
            settingsMod = mod;
        }
    }

    public void closeSettings() {
        settingsMod = null;
        furiahSettings.close();
    }

    @Override
    public boolean isSubPageOpen() {
        return settingsMod != null || furiahSettings.isOpen();
    }

    @Override
    public boolean isEscapeConsumed() {
        if (isSubPageOpen()) {
            closeSettings();
            return true;
        }
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (furiahSettings.isOpen()) {
            furiahSettings.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Mix instance = Mix.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        AccentColor accent = instance.getColorManager().getCurrentColor();
        float chipStartX = getX() + MARGIN;
        float chipY = getY() + 10F;
        float chipRowWidth = getTabRowWidth(nvg);
        float filterY = chipY + CHIP_HEIGHT + 8F;
        boolean capeTab = currentTab == CosmeticsTab.CAPE;
        float listTop = CategoryIslandRenderer.viewportTop(chipY, CHIP_HEIGHT);
        float cardsTop = capeTab
                ? filterY + CAPE_FILTER_HEIGHT + 12F
                : CategoryIslandRenderer.contentTop(chipY, CHIP_HEIGHT);
        float viewportHeight = getY() + getHeight() - 12F - listTop;

        nvg.save();
        nvg.scissor(getX(), listTop, getWidth(), viewportHeight);
        nvg.translate(0, scroll.getValue());
        if (capeTab) {
            drawCapes(nvg, accent, mouseX, mouseY, cardsTop, chipStartX, chipY, chipRowWidth);
        } else {
            drawModules(nvg, accent, mouseX, mouseY, cardsTop);
        }
        nvg.restore();

        if (capeTab) {
            drawCapeFilters(nvg, accent, chipStartX, filterY, mouseX, mouseY);
            CategoryIslandRenderer.drawScrollFade(nvg, getX(), getWidth(), filterY,
                    CAPE_FILTER_HEIGHT, ModMenuTheme.surface());
        }
        CategoryIslandRenderer.drawScrollFade(nvg, getX(), getWidth(), chipY,
                CHIP_HEIGHT, ModMenuTheme.surface());
        chipIsland.draw(nvg, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT,
                getX() + getWidth() - MARGIN);
        drawTabs(nvg, accent, chipStartX, chipY, mouseX, mouseY);

        if (furiahSettings.isOpen()) {
            furiahSettings.draw(nvg, mouseX, mouseY, partialTicks,
                    getX(), getY(), getWidth(), getHeight());
        } else if (settingsMod != null) {
            drawSettings(nvg, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        if (furiahSettings.isOpen()) {
            furiahSettings.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (settingsMod != null) {
            settingsClicked(mouseX, mouseY);
            return;
        }
        Mix instance = Mix.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        float chipX = getX() + MARGIN;
        float chipY = getY() + 10F;
        for (CosmeticsTab tab : CosmeticsTab.values()) {
            float chipWidth = getTabWidth(nvg, tab);
            if (MouseUtils.isInside(mouseX, mouseY, chipX, chipY, chipWidth, CHIP_HEIGHT)) {
                if (currentTab != tab) {
                    currentTab = tab;
                    scroll.setScrollPosition(0);
                }
                return;
            }
            chipX += chipWidth + 8F;
        }
        if (currentTab == CosmeticsTab.CAPE) {
            float filterX = getX() + MARGIN;
            float filterY = chipY + CHIP_HEIGHT + 8F;
            for (CapeCategory category : CapeCategory.values()) {
                float width = nvg.getTextWidth(category.getName(), 9.5F, Fonts.MEDIUM) + 24F;
                if (MouseUtils.isInside(mouseX, mouseY, filterX, filterY, width, CAPE_FILTER_HEIGHT)) {
                    currentCapeCategory = category;
                    scroll.setScrollPosition(0);
                    return;
                }
                filterX += width + 6F;
            }
            capeClicked(mouseX, mouseY);
        } else {
            moduleClicked(mouseX, mouseY);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        furiahSettings.mouseReleased(mouseX, mouseY, mouseButton);
        for (Comp component : settingComponents.values()) {
            component.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    private void drawTabs(NanoVGManager nvg, AccentColor accent, float startX, float y,
                          int mouseX, int mouseY) {
        float x = startX;
        for (CosmeticsTab tab : CosmeticsTab.values()) {
            float width = getTabWidth(nvg, tab);
            boolean selected = tab == currentTab;
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, CHIP_HEIGHT);
            if (selected) {
                nvg.drawGradientRoundedRect(x, y, width, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
                        accent.getColor1(), accent.getColor2());
                nvg.drawText(tab.getIcon(), x + 10F, y + 8F, Color.WHITE, 10.5F, Fonts.ICON);
                nvg.drawText(tab.getName(), x + 27F, y + 8F, Color.WHITE, 10.5F, Fonts.MEDIUM);
            } else {
                nvg.drawRoundedRect(x, y, width, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
                        hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
                nvg.drawText(tab.getIcon(), x + 10F, y + 8F, ModMenuTheme.onSurfaceVariant(),
                        10.5F, Fonts.ICON);
                nvg.drawText(tab.getName(), x + 27F, y + 8F, ModMenuTheme.onSurfaceVariant(),
                        10.5F, Fonts.MEDIUM);
            }
            x += width + 8F;
        }
    }

    private void drawCapeFilters(NanoVGManager nvg, AccentColor accent, float startX, float y,
                                 int mouseX, int mouseY) {
        float x = startX;
        for (CapeCategory category : CapeCategory.values()) {
            float width = nvg.getTextWidth(category.getName(), 9.5F, Fonts.MEDIUM) + 24F;
            boolean selected = category == currentCapeCategory;
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, CAPE_FILTER_HEIGHT);
            nvg.drawRoundedRect(x, y, width, CAPE_FILTER_HEIGHT, CAPE_FILTER_HEIGHT / 2F,
                    selected ? ModMenuTheme.applyAlpha(accent.getColor1(), 55)
                            : (hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow()));
            nvg.drawCenteredText(category.getName(), x + width / 2F, y + 7F,
                    selected ? ModMenuTheme.onPrimaryContainer() : ModMenuTheme.onSurfaceVariant(),
                    9.5F, Fonts.MEDIUM);
            x += width + 6F;
        }
    }

    private void drawCapes(NanoVGManager nvg, AccentColor accent, int mouseX, int mouseY,
                           float cardsTop, float chipStartX, float chipY, float chipRowWidth) {
        CapeManager capeManager = Mix.getInstance().getCapeManager();
        int columns = getCapeColumns();
        float cardWidth = (getWidth() - MARGIN * 2F - CARD_GAP * (columns - 1)) / columns;
        float cardHeight = getCapeCardHeight(cardWidth);
        int visible = 0;
        for (Cape cape : capeManager.getCapes()) {
            if (!filterCape(cape)) {
                visible++;
            }
        }
        int rows = (visible + columns - 1) / columns;
        float listTop = CategoryIslandRenderer.viewportTop(chipY, CHIP_HEIGHT);
        float contentHeight = (cardsTop - listTop) + (rows == 0 ? 0F
                : rows * (cardHeight + CARD_GAP) - CARD_GAP);
        float viewportHeight = getY() + getHeight() - 12F - listTop;
        scroll.setMaxScroll(Math.max(0F, contentHeight - viewportHeight));
        int index = 0;
        for (Cape cape : capeManager.getCapes()) {
            if (filterCape(cape)) {
                continue;
            }
            int column = index % columns;
            int row = index / columns;
            float cardX = getX() + MARGIN + column * (cardWidth + CARD_GAP);
            float cardY = cardsTop + row * (cardHeight + CARD_GAP);
            if (cardY + scroll.getValue() + cardHeight < chipY
                    || cardY + scroll.getValue() > getY() + getHeight() - 12F) {
                index++;
                continue;
            }
            boolean selected = cape.equals(capeManager.getCurrentCape());
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, cardX, cardY + scroll.getValue(),
                    cardWidth, cardHeight)
                    && !CategoryIslandRenderer.overIsland(mouseX, mouseY, chipStartX, chipY,
                    chipRowWidth, CHIP_HEIGHT);
            cape.getAnimation().setAnimation(selected ? 1F : 0F, 16);
            nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 14,
                    hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
            float selectedValue = cape.getAnimation().getValue();
            if (selectedValue > 0.01F) {
                nvg.drawGradientOutlineRoundedRect(cardX, cardY, cardWidth, cardHeight, 14, 2F,
                        ColorUtils.applyAlpha(accent.getColor1(), (int) (selectedValue * 255)),
                        ColorUtils.applyAlpha(accent.getColor2(), (int) (selectedValue * 255)));
            }
            float sampleWidth = Math.min(cardWidth - 24F, (cardHeight - 46F) * (2F / 3F));
            float sampleHeight = sampleWidth * 1.5F;
            float sampleX = cardX + (cardWidth - sampleWidth) / 2F;
            float sampleY = cardY + 10F;
            if (cape instanceof NormalCape && ((NormalCape) cape).getSample() != null) {
                nvg.drawRoundedImage(((NormalCape) cape).getSample(), sampleX, sampleY,
                        sampleWidth, sampleHeight, 10F);
            } else if (cape instanceof CustomCape && ((CustomCape) cape).getSample() != null) {
                nvg.drawRoundedImage(((CustomCape) cape).getSample(), sampleX, sampleY,
                        sampleWidth, sampleHeight, 10F);
            }
            nvg.drawCenteredText(nvg.getLimitText(cape.getName(), 10.5F, Fonts.MEDIUM, cardWidth - 20F),
                    cardX + cardWidth / 2F, cardY + cardHeight - 28F,
                    ModMenuTheme.onSurface(), 10.5F, Fonts.MEDIUM);
            if (selectedValue > 0.01F) {
                float checkX = cardX + cardWidth - 30F;
                nvg.drawRoundedRect(checkX, cardY + 10F, 20F, 20F, 10F,
                        ModMenuTheme.applyAlpha(accent.getColor1(), (int) (selectedValue * 230)));
                nvg.drawCenteredText(Icon.CHECK, checkX + 10F, cardY + 14F,
                        new Color(255, 255, 255, (int) (selectedValue * 255)), 11, Fonts.ICON);
            }
            index++;
        }
    }

    private void drawModules(NanoVGManager nvg, AccentColor accent, int mouseX, int mouseY,
                             float cardsTop) {
        ArrayList<Mod> modules = getCurrentModules();
        int columns = getModuleColumns(modules.size());
        float available = getWidth() - MARGIN * 2F;
        float cardWidth = columns == 1 ? Math.min(360F, available)
                : (available - CARD_GAP * (columns - 1)) / columns;
        float rowWidth = columns * cardWidth + CARD_GAP * (columns - 1);
        float startX = getX() + (getWidth() - rowWidth) / 2F;
        int rows = (modules.size() + columns - 1) / columns;
        float listTop = CategoryIslandRenderer.viewportTop(getY() + 10F, CHIP_HEIGHT);
        float contentHeight = (cardsTop - listTop)
                + (rows == 0 ? 0F : rows * (MODULE_CARD_HEIGHT + CARD_GAP) - CARD_GAP);
        float viewportHeight = getY() + getHeight() - 12F - listTop;
        scroll.setMaxScroll(Math.max(0F, contentHeight - viewportHeight));
        for (int i = 0; i < modules.size(); i++) {
            Mod mod = modules.get(i);
            int column = i % columns;
            int row = i / columns;
            float cardX = startX + column * (cardWidth + CARD_GAP);
            float cardY = cardsTop + row * (MODULE_CARD_HEIGHT + CARD_GAP);
            if (cardY + scroll.getValue() + MODULE_CARD_HEIGHT < getY() + 10F
                    || cardY + scroll.getValue() > getY() + getHeight() - 12F) {
                continue;
            }
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, cardX,
                    cardY + scroll.getValue(), cardWidth, MODULE_CARD_HEIGHT);
            mod.getAnimation().setAnimation(mod.isToggled() ? 1F : 0F, 16);
            float enabled = mod.getAnimation().getValue();
            nvg.drawRoundedRect(cardX, cardY, cardWidth, MODULE_CARD_HEIGHT, 16,
                    hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
            if (enabled > 0.01F) {
                nvg.drawGradientOutlineRoundedRect(cardX, cardY, cardWidth, MODULE_CARD_HEIGHT,
                        16, 2F, ColorUtils.applyAlpha(accent.getColor1(), (int) (enabled * 255)),
                        ColorUtils.applyAlpha(accent.getColor2(), (int) (enabled * 255)));
            }
            nvg.drawGradientRoundedRect(cardX + 16F, cardY + 16F, 44F, 44F, 14,
                    ModMenuTheme.applyAlpha(accent.getColor1(), 220),
                    ModMenuTheme.applyAlpha(accent.getColor2(), 220));
            nvg.drawCenteredText(currentTab.getIcon(), cardX + 38F, cardY + 29F,
                    Color.WHITE, 19, Fonts.ICON);
            nvg.drawText(nvg.getLimitText(mod.getName(), 13F, Fonts.DEMIBOLD, cardWidth - 92F),
                    cardX + 72F, cardY + 21F, ModMenuTheme.onSurface(), 13F, Fonts.DEMIBOLD);
            nvg.drawText(nvg.getLimitText(mod.getName(), 9F, Fonts.MEDIUM, cardWidth - 92F),
                    cardX + 72F, cardY + 42F,
                    enabled > 0.5F ? ModMenuTheme.onPrimaryContainer() : ModMenuTheme.onSurfaceVariant(),
                    9F, Fonts.MEDIUM);
            nvg.drawText(nvg.getLimitText(mod.getDescription(), 9.5F, Fonts.REGULAR, cardWidth - 32F),
                    cardX + 16F, cardY + 78F, ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);
            drawModuleToggle(nvg, accent, cardX + 16F, cardY + MODULE_CARD_HEIGHT - 39F,
                    mod.isToggled(), enabled);
            float settingsX = cardX + cardWidth - 106F;
            float settingsY = cardY + MODULE_CARD_HEIGHT - 40F;
            boolean settingsHovered = MouseUtils.isInside(mouseX, mouseY, settingsX, settingsY,
                    90F, 27F);
            nvg.drawRoundedRect(settingsX, settingsY, 90F, 27F, 9F,
                    settingsHovered ? ModMenuTheme.surfaceContainerHighest()
                            : ModMenuTheme.surfaceContainer());
            nvg.drawText(Icon.SETTINGS, settingsX + 10F, settingsY + 8F,
                    ModMenuTheme.onSurfaceVariant(), 10F, Fonts.ICON);
            nvg.drawText(TranslateText.SETTINGS.getText(), settingsX + 28F, settingsY + 9F,
                    ModMenuTheme.onSurface(), 9F, Fonts.MEDIUM);
        }
    }

    private void drawModuleToggle(NanoVGManager nvg, AccentColor accent, float x, float y,
                                  boolean toggled, float animation) {
        nvg.drawRoundedRect(x, y, 48F, 20F, 10F, ModMenuTheme.surfaceContainerHighest());
        nvg.drawGradientRoundedRect(x, y, 48F, 20F, 10F,
                ModMenuTheme.applyAlpha(accent.getColor1(), (int) (animation * 255)),
                ModMenuTheme.applyAlpha(accent.getColor2(), (int) (animation * 255)));
        float circleX = x + 3F + animation * 25F;
        nvg.drawRoundedRect(circleX, y + 3F, 14F, 14F, 7F,
                toggled ? Color.WHITE : ModMenuTheme.onSurfaceVariant());
    }

    private void drawSettings(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        ArrayList<Setting> settings = Mix.getInstance().getModManager().getSettingsByMod(settingsMod);
        if (settings == null) {
            settings = new ArrayList<Setting>();
        }
        float panelWidth = Math.min(460F, getWidth() - 40F);
        float panelHeight = Math.min(getHeight() - 40F, 112F + settings.size() * 54F);
        float panelX = getX() + (getWidth() - panelWidth) / 2F;
        float panelY = getY() + (getHeight() - panelHeight) / 2F;
        nvg.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), 0,
                new Color(0, 0, 0, 115));
        nvg.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 18,
                ModMenuTheme.surfaceContainerLow());
        nvg.drawGradientOutlineRoundedRect(panelX, panelY, panelWidth, panelHeight, 18, 1.5F,
                ModMenuTheme.primaryContainer(), ModMenuTheme.primaryContainer());
        nvg.drawText(settingsMod.getName(), panelX + 20F, panelY + 20F,
                ModMenuTheme.onSurface(), 15F, Fonts.DEMIBOLD);
        nvg.drawText(TranslateText.SETTINGS.getText(), panelX + 20F, panelY + 42F,
                ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);
        nvg.drawRoundedRect(panelX + panelWidth - 38F, panelY + 16F, 22F, 22F, 8F,
                ModMenuTheme.surfaceContainerHigh());
        nvg.drawCenteredText(Icon.X, panelX + panelWidth - 27F, panelY + 21F,
                ModMenuTheme.onSurfaceVariant(), 10F, Fonts.ICON);
        if (settings.isEmpty()) {
            nvg.drawCenteredText(TranslateText.NO_SETTINGS.getText(), panelX + panelWidth / 2F,
                    panelY + 82F, ModMenuTheme.onSurfaceVariant(), 10F, Fonts.REGULAR);
            return;
        }
        for (int i = 0; i < settings.size(); i++) {
            Setting setting = settings.get(i);
            float rowY = panelY + 68F + i * 54F;
            nvg.drawText(setting.getName(), panelX + 20F, rowY + 4F,
                    ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.MEDIUM);
            Comp component = getSettingComponent(setting);
            if (setting instanceof BooleanSetting) {
                component.setX(panelX + panelWidth - 65F);
                component.setY(rowY);
            } else if (setting instanceof ComboSetting) {
                component.setX(panelX + panelWidth - 158F);
                component.setY(rowY);
            } else if (setting instanceof NumberSetting) {
                component.setX(panelX + 20F);
                component.setY(rowY + 23F);
                ((CompSlider) component).setWidth(panelWidth - 40F);
            }
            component.draw(mouseX, mouseY, partialTicks);
            if (setting instanceof ColorSetting) {
                drawColorPresets(nvg, (ColorSetting) setting, panelX + 20F, rowY + 23F,
                        mouseX, mouseY);
            }
        }
    }

    private void drawColorPresets(NanoVGManager nvg, ColorSetting setting, float x, float y,
                                  int mouseX, int mouseY) {
        for (int i = 0; i < 12; i++) {
            float swatchX = x + i * 22F;
            Color color = ColorUtils.applyAlpha(Color.getHSBColor(i / 12F, 0.78F, 1.0F),
                    setting.getAlpha());
            nvg.drawRoundedRect(swatchX, y, 18F, 14F, 4F,
                    MouseUtils.isInside(mouseX, mouseY, swatchX, y, 18F, 14F)
                            ? ModMenuTheme.mix(color, Color.WHITE, 0.25F) : color);
        }
    }

    private void settingsClicked(int mouseX, int mouseY) {
        ArrayList<Setting> settings = Mix.getInstance().getModManager().getSettingsByMod(settingsMod);
        if (settings == null) {
            settings = new ArrayList<Setting>();
        }
        float panelWidth = Math.min(460F, getWidth() - 40F);
        float panelHeight = Math.min(getHeight() - 40F, 112F + settings.size() * 54F);
        float panelX = getX() + (getWidth() - panelWidth) / 2F;
        float panelY = getY() + (getHeight() - panelHeight) / 2F;
        if (!MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)
                || MouseUtils.isInside(mouseX, mouseY, panelX + panelWidth - 38F,
                panelY + 16F, 22F, 22F)) {
            settingsMod = null;
            return;
        }
        for (int i = 0; i < settings.size(); i++) {
            Setting setting = settings.get(i);
            float rowY = panelY + 68F + i * 54F;
            Comp component = getSettingComponent(setting);
            if (setting instanceof BooleanSetting) {
                component.setX(panelX + panelWidth - 65F);
                component.setY(rowY);
            } else if (setting instanceof ComboSetting) {
                component.setX(panelX + panelWidth - 158F);
                component.setY(rowY);
            } else if (setting instanceof NumberSetting) {
                component.setX(panelX + 20F);
                component.setY(rowY + 23F);
                ((CompSlider) component).setWidth(panelWidth - 40F);
            }
            if (setting instanceof ColorSetting) {
                for (int swatch = 0; swatch < 12; swatch++) {
                    if (MouseUtils.isInside(mouseX, mouseY, panelX + 20F + swatch * 22F,
                            rowY + 23F, 18F, 14F)) {
                        ColorSetting colorSetting = (ColorSetting) setting;
                        colorSetting.setHue(swatch / 12F);
                        colorSetting.setSaturation(0.78F);
                        colorSetting.setBrightness(1.0F);
                        return;
                    }
                }
            } else {
                component.mouseClicked(mouseX, mouseY, 0);
            }
        }
    }

    private Comp getSettingComponent(Setting setting) {
        Comp component = settingComponents.get(setting);
        if (component == null) {
            if (setting instanceof BooleanSetting) {
                component = new CompToggleButton(0F, 0F, 0.9F, (BooleanSetting) setting);
            } else if (setting instanceof ComboSetting) {
                component = new CompComboBox(0F, 0F, 136F, (ComboSetting) setting);
            } else if (setting instanceof NumberSetting) {
                component = new CompSlider(0F, 0F, (NumberSetting) setting, 200F);
            } else {
                component = new Comp(0F, 0F);
            }
            settingComponents.put(setting, component);
        }
        return component;
    }

    private void capeClicked(int mouseX, int mouseY) {
        Mix instance = Mix.getInstance();
        MixAPI api = instance.getApi();
        CapeManager capeManager = instance.getCapeManager();
        int columns = getCapeColumns();
        float cardWidth = (getWidth() - MARGIN * 2F - CARD_GAP * (columns - 1)) / columns;
        float cardHeight = getCapeCardHeight(cardWidth);
        float filterY = getY() + 10F + CHIP_HEIGHT + 8F;
        float cardsTop = filterY + CAPE_FILTER_HEIGHT + 12F;
        float listTop = CategoryIslandRenderer.viewportTop(getY() + 10F, CHIP_HEIGHT);
        float listBottom = getY() + getHeight() - 12F;
        if (mouseY < cardsTop || mouseY > listBottom
                || !MouseUtils.isInside(mouseX, mouseY, getX(), listTop, getWidth(), listBottom - listTop)) {
            return;
        }
        int index = 0;
        for (Cape cape : capeManager.getCapes()) {
            if (filterCape(cape)) {
                continue;
            }
            int column = index % columns;
            int row = index / columns;
            float cardX = getX() + MARGIN + column * (cardWidth + CARD_GAP);
            float cardY = cardsTop + row * (cardHeight + CARD_GAP) + scroll.getValue();
            if (MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight)) {
                if (cape.isPremium() && !api.isSpecialUser()) {
                    instance.getNotificationManager().post(TranslateText.ERROR,
                            TranslateText.PREMIUM_ONLY, NotificationType.ERROR);
                } else {
                    capeManager.setCurrentCape(cape);
                }
                return;
            }
            index++;
        }
    }

    private void moduleClicked(int mouseX, int mouseY) {
        ArrayList<Mod> modules = getCurrentModules();
        int columns = getModuleColumns(modules.size());
        float available = getWidth() - MARGIN * 2F;
        float cardWidth = columns == 1 ? Math.min(360F, available)
                : (available - CARD_GAP * (columns - 1)) / columns;
        float rowWidth = columns * cardWidth + CARD_GAP * (columns - 1);
        float startX = getX() + (getWidth() - rowWidth) / 2F;
        float cardsTop = CategoryIslandRenderer.contentTop(getY() + 10F, CHIP_HEIGHT);
        float listTop = CategoryIslandRenderer.viewportTop(getY() + 10F, CHIP_HEIGHT);
        float listBottom = getY() + getHeight() - 12F;
        if (mouseY < listTop || mouseY > listBottom
                || !MouseUtils.isInside(mouseX, mouseY, getX(), listTop, getWidth(), listBottom - listTop)) {
            return;
        }
        for (int i = 0; i < modules.size(); i++) {
            Mod mod = modules.get(i);
            int column = i % columns;
            int row = i / columns;
            float cardX = startX + column * (cardWidth + CARD_GAP);
            float cardY = cardsTop + row * (MODULE_CARD_HEIGHT + CARD_GAP) + scroll.getValue();
            if (!MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, MODULE_CARD_HEIGHT)) {
                continue;
            }
            if (MouseUtils.isInside(mouseX, mouseY, cardX + cardWidth - 106F,
                    cardY + MODULE_CARD_HEIGHT - 40F, 90F, 27F)) {
                showSettings(mod);
            } else {
                mod.toggle();
            }
            return;
        }
    }

    private ArrayList<Mod> getCurrentModules() {
        ArrayList<Mod> modules = new ArrayList<Mod>();
        switch (currentTab) {
            case KILL_EFFECT:
                addModule(modules, TranslateText.KILL_EFFECTS.getKey());
                break;
            case HIT_EFFECT:
                addModule(modules, TranslateText.HIT_COLOR.getKey());
                addModule(modules, TranslateText.DAMAGE_PARTICLES.getKey());
                break;
            case TRAIL:
                addModule(modules, TranslateText.TRAILS.getKey());
                break;
            case LANDING:
                addModule(modules, TranslateText.LANDING_IMPACT.getKey());
                break;
            case AMBIENCE:
                addModule(modules, TranslateText.AMBIENCE.getKey());
                break;
            case PARTICLES:
                addModule(modules, TranslateText.VISUAL_PARTICLES.getKey());
                break;
            default:
                break;
        }
        String search = getSearchBox().getText();
        if (!search.isEmpty()) {
            for (int i = modules.size() - 1; i >= 0; i--) {
                Mod mod = modules.get(i);
                if (!SearchUtils.isSimillar(mod.getName(), search)
                        && !SearchUtils.isSimillar(mod.getDescription(), search)) {
                    modules.remove(i);
                }
            }
        }
        return modules;
    }

    private void addModule(ArrayList<Mod> modules, String key) {
        Mod mod = Mix.getInstance().getModManager().getModByTranslateKey(key);
        if (mod != null) {
            modules.add(mod);
        }
    }

    private float getTabRowWidth(NanoVGManager nvg) {
        float width = 0F;
        for (CosmeticsTab tab : CosmeticsTab.values()) {
            width += getTabWidth(nvg, tab) + 8F;
        }
        return width - 8F;
    }

    private float getTabWidth(NanoVGManager nvg, CosmeticsTab tab) {
        return nvg.getTextWidth(tab.getName(), 10.5F, Fonts.MEDIUM) + 40F;
    }

    private int getCapeColumns() {
        return Math.max(1, (int) ((getWidth() - MARGIN * 2F + CARD_GAP) / CAPE_CARD_TARGET_WIDTH));
    }

    private int getModuleColumns(int moduleCount) {
        int availableColumns = Math.max(1, (int) ((getWidth() - MARGIN * 2F + CARD_GAP)
                / MODULE_CARD_TARGET_WIDTH));
        return Math.max(1, Math.min(moduleCount, availableColumns));
    }

    private float getCapeCardHeight(float cardWidth) {
        return cardWidth * 1.26F + 40F;
    }

    private boolean filterCape(Cape cape) {
        if (currentCapeCategory != CapeCategory.ALL && currentCapeCategory != cape.getCategory()) {
            return true;
        }
        String search = getSearchBox().getText();
        return !search.isEmpty() && !SearchUtils.isSimillar(cape.getName(), search);
    }
}
