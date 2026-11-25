package mix.client.mixui.gui.modmenu.category.impl;

import java.util.ArrayList;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.gui.modmenu.category.impl.setting.SettingScene;
import mix.client.mixui.gui.modmenu.category.impl.setting.impl.AppearanceScene;
import mix.client.mixui.gui.modmenu.category.impl.setting.impl.LanguageScene;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.SmoothStepAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;

public class SettingCategory extends Category {

	private static final float MARGIN = 24F;
	private static final float CARD_HEIGHT = 72F;
	private static final float CARD_GAP = 10F;
	private static final float SCENE_OFFSET = 60F;

	private Animation sceneAnimation;

	private ArrayList<SettingScene> scenes = new ArrayList<SettingScene>();
	private SettingScene currentScene;

	public SettingCategory(GuiModMenu parent) {
		super(parent, TranslateText.SETTINGS, Icon.SETTINGS, false);

		scenes.add(new AppearanceScene(this));
		scenes.add(new LanguageScene(this));
	}

	@Override
	public void initGui() {
		sceneAnimation = new SmoothStepAnimation(240, 1.0);
		sceneAnimation.setValue(1.0);

		for(SettingScene scene : scenes) {
			scene.initGui();
		}
	}

	@Override
	public void initCategory() {
		// 滚动位置由 GuiModMenu 按分类记忆并恢复
		sceneAnimation = new SmoothStepAnimation(240, 1.0);
		sceneAnimation.setValue(1.0);
		currentScene = null;
		setCanClose(true);
	}

	@Override
	public boolean isSubPageOpen() {
		return currentScene != null;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();

		if(sceneAnimation.isDone(Direction.FORWARDS)) {
			currentScene = null;
			setCanClose(true);
		}

		// Settings browser
		nvg.save();
		nvg.translate(-(1.0F - (float) sceneAnimation.getValue()) * getWidth(), 0);

		float offsetY = getY() + 10F;

		for(SettingScene scene : scenes) {

			float cardX = getX() + MARGIN;
			float cardWidth = getWidth() - MARGIN * 2F;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, cardX, offsetY, cardWidth, CARD_HEIGHT);

			nvg.drawRoundedRect(cardX, offsetY, cardWidth, CARD_HEIGHT, 12,
					hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());

			nvg.drawRoundedRect(cardX + 14F, offsetY + 14F, 44F, 44F, 12F, ModMenuTheme.surfaceContainerHigh());
			nvg.drawCenteredText(scene.getIcon(), cardX + 36F, offsetY + 26F,
					ModMenuTheme.onSurfaceVariant(), 19, Fonts.ICON);

			nvg.drawText(scene.getName(), cardX + 72F, offsetY + 16F, ModMenuTheme.onSurface(), 12, Fonts.MEDIUM);
			nvg.drawText(scene.getDescription(), cardX + 72F, offsetY + 40F,
					ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);

			nvg.drawRoundedRect(cardX + cardWidth - 38F, offsetY + 19F, 24F, 24F, 12F,
					ModMenuTheme.surfaceContainerHigh());
			nvg.drawCenteredText(Icon.ARROW_RIGHT, cardX + cardWidth - 26F, offsetY + 24F,
					ModMenuTheme.onSurfaceVariant(), 12, Fonts.ICON);

			offsetY += CARD_HEIGHT + CARD_GAP;
		}

		nvg.restore();

		// Scene page
		nvg.save();
		nvg.translate((float) sceneAnimation.getValue() * getWidth(), 0);

		if(currentScene != null) {

			float headerX = getX() + MARGIN;
			float headerY = getY() + 8F;
			float headerWidth = getWidth() - MARGIN * 2F;

			nvg.drawRoundedRect(headerX, headerY, headerWidth, 44F, 12, ModMenuTheme.surfaceContainerLow());

			boolean backHovered = MouseUtils.isInside(mouseX, mouseY, headerX + 6F, headerY + 6F, 32F, 32F);
			nvg.drawRoundedRect(headerX + 6F, headerY + 6F, 32F, 32F, 16F,
					backHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
			nvg.drawCenteredText(Icon.ARROW_LEFT, headerX + 22F, headerY + 15F,
					ModMenuTheme.onSurface(), 15, Fonts.ICON);

			nvg.drawText(currentScene.getName(), headerX + 50F, headerY + 9F,
					ModMenuTheme.onSurface(), 13, Fonts.MEDIUM);
			nvg.drawText(currentScene.getDescription(), headerX + 50F, headerY + 26F,
					ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);

			currentScene.drawScreen(mouseX, mouseY, partialTicks);
		}

		nvg.restore();
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		if(currentScene != null) {

			if(!sceneAnimation.isDone(Direction.BACKWARDS)) {
				return;
			}

			float headerX = getX() + MARGIN;
			float headerY = getY() + 8F;

			if(mouseButton == 3 || (mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, headerX + 6F, headerY + 6F, 32F, 32F))) {
				sceneAnimation.setDirection(Direction.FORWARDS);
				return;
			}

			currentScene.mouseClicked(mouseX, mouseY, mouseButton);
			return;
		}

		if(mouseButton != 0 || !sceneAnimation.isDone(Direction.FORWARDS)) {
			return;
		}

		float offsetY = getY() + 10F;

		for(SettingScene scene : scenes) {

			float cardX = getX() + MARGIN;
			float cardWidth = getWidth() - MARGIN * 2F;

			if(MouseUtils.isInside(mouseX, mouseY, cardX, offsetY, cardWidth, CARD_HEIGHT)) {
				currentScene = scene;
				setCanClose(false);
				sceneAnimation.setDirection(Direction.BACKWARDS);
				return;
			}

			offsetY += CARD_HEIGHT + CARD_GAP;
		}
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {

		if(currentScene != null && sceneAnimation.isDone(Direction.BACKWARDS)) {
			currentScene.mouseReleased(mouseX, mouseY, mouseButton);
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {

		if(currentScene == null) {
			return;
		}

		if(keyCode == Keyboard.KEY_ESCAPE) {
			sceneAnimation.setDirection(Direction.FORWARDS);
			return;
		}

		currentScene.keyTyped(typedChar, keyCode);
	}

	public int getSceneX() {
		return getX() + (int) MARGIN;
	}

	public int getSceneY() {
		return getY() + (int) SCENE_OFFSET;
	}

	public int getSceneWidth() {
		return getWidth() - (int) (MARGIN * 2F);
	}

	public int getSceneHeight() {
		return getHeight() - (int) SCENE_OFFSET - 10;
	}
}
