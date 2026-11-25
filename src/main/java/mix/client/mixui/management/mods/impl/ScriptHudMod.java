package mix.client.mixui.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import mix.client.mixui.Mix;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventClickMouse;
import mix.client.mixui.management.event.impl.EventKey;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.HUDMod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.KeybindSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.script.ScriptEntry;
import mix.client.mixui.management.script.ScriptManager;

/**
 * HUD backed by JSON templates in mixclient/scripts.
 *
 * Every field of a script (name / icon / x / y / scale / lines / colors / enabled /
 * background) lives in the json file, so the module itself only exposes the knobs that
 * apply to the whole stack: global scale, line spacing, background override, icon
 * visibility and a reload key.
 */
public class ScriptHudMod extends HUDMod {

	private static final float PADDING = 5.0F;
	private static final float FONT_SIZE = 9.0F;
	private static final float ICON_SIZE = 10.4F;
	private static final float ICON_GAP = 4.0F;
	private static final float BACKGROUND_RADIUS = 4.0F;

	private NumberSetting scaleSetting = new NumberSetting(TranslateText.SCRIPT_HUD_SCALE, this, 1.0, 0.5, 2.5, false);
	private NumberSetting spacingSetting = new NumberSetting(TranslateText.SCRIPT_HUD_SPACING, this, 1.0, 0.0, 4.0, false);
	private BooleanSetting backgroundSetting = new BooleanSetting(TranslateText.SCRIPT_HUD_BACKGROUND, this, true);
	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.SCRIPT_HUD_ICON, this, true);
	private KeybindSetting reloadKeySetting = new KeybindSetting(TranslateText.SCRIPT_HUD_RELOAD, this, Keyboard.KEY_NONE);

	public ScriptHudMod() {
		super(TranslateText.SCRIPT_HUD, TranslateText.SCRIPT_HUD_DESCRIPTION);

		// 归入脚本分类，HUDMod 构造里默认给的是 HUD
		this.setCategory(ModCategory.SCRIPT);
	}

	@Override
	public void setup() {

		// 初始化脚本目录与文件监听，构造函数里调用，不依赖事件
		ScriptManager.getInstance().init();
	}

	@EventTarget
	public void onClickMouse(EventClickMouse event) {

		if(Mouse.getEventButtonState() && event.getButton() == 0) {
			ScriptManager.getInstance().addLeftClick();
		}
	}

	@EventTarget
	public void onKey(EventKey event) {

		int keyCode = reloadKeySetting.getKeyCode();

		if(keyCode != Keyboard.KEY_NONE && event.getKeyCode() == keyCode) {
			ScriptManager.getInstance().reload();
		}
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {

		ScriptManager scriptManager = ScriptManager.getInstance();

		// 文件改动检测在 ScriptManager 内部节流，最多每秒一次，渲染路径本身不读文件内容
		scriptManager.refresh();

		List<ScriptEntry> scripts = scriptManager.getScripts();
		boolean visible = hasVisibleScript(scripts);

		if(!visible) {

			this.setWidth(0);
			this.setHeight(0);

			if(this.isEditing()) {
				this.drawEmptyHint();
			}

			return;
		}

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		nvg.setupAndDraw(() -> this.drawScripts(nvg, scripts));
	}

	private boolean hasVisibleScript(List<ScriptEntry> scripts) {

		for(ScriptEntry entry : scripts) {

			if(!entry.isEnabled()) {
				continue;
			}

			if(entry.getLines() == null || entry.getLines().isEmpty()) {
				continue;
			}

			return true;
		}

		return false;
	}

	private void drawScripts(NanoVGManager nvg, List<ScriptEntry> scripts) {

		float maxWidth = 0F;
		float maxHeight = 0F;
		float scaleMultiplier = (float) scaleSetting.getValue();
		float lineSpacing = (float) spacingSetting.getValue();
		boolean showBackground = backgroundSetting.isToggled();
		boolean showIcon = iconSetting.isToggled();

		for(ScriptEntry entry : scripts) {

			if(!entry.isEnabled()) {
				continue;
			}

			List<String> lines = entry.getLines();

			if(lines == null || lines.isEmpty()) {
				continue;
			}

			float blockScale = this.getScale() * entry.getScale() * scaleMultiplier;
			float lineHeight = (FONT_SIZE * 1.2F) + lineSpacing;

			List<String> renderedLines = new ArrayList<String>();
			float contentWidth = 0F;

			for(String line : lines) {

				String text = ScriptManager.getInstance().replaceVariables(line);

				if(text == null) {
					text = "-";
				}

				renderedLines.add(text);
				contentWidth = Math.max(contentWidth, nvg.getTextWidth(text, FONT_SIZE, Fonts.REGULAR));
			}

			String icon = entry.getIcon();
			boolean hasIcon = showIcon && icon != null && !icon.isEmpty();
			float iconWidth = hasIcon ? nvg.getTextWidth(icon, ICON_SIZE, Fonts.ICON) + ICON_GAP : 0F;

			float blockWidth = iconWidth + contentWidth + (PADDING * 2F);
			float blockHeight = (PADDING * 2F) + (renderedLines.size() * lineHeight);

			// 条目坐标是相对本 mod 的偏移，随 mod 整体拖拽移动
			float renderX = this.getX() + (entry.getX() * this.getScale());
			float renderY = this.getY() + (entry.getY() * this.getScale());

			nvg.save();
			nvg.translate(renderX, renderY);
			nvg.scale(0F, 0F, blockScale);

			if(entry.isBackground() && showBackground) {
				nvg.drawRoundedRect(0F, 0F, blockWidth, blockHeight, BACKGROUND_RADIUS, new Color(0, 0, 0, 120));
			}

			if(hasIcon) {
				nvg.drawText(icon, PADDING, PADDING - 1F, this.getFontColor(), ICON_SIZE, Fonts.ICON);
			}

			for(int i = 0; i < renderedLines.size(); i++) {
				nvg.drawText(renderedLines.get(i), PADDING + iconWidth, PADDING + (i * lineHeight), entry.getColor(i), FONT_SIZE, Fonts.REGULAR);
			}

			nvg.restore();

			maxWidth = Math.max(maxWidth, entry.getX() + (blockWidth * blockScale));
			maxHeight = Math.max(maxHeight, entry.getY() + (blockHeight * blockScale));
		}

		this.setWidth((int) Math.ceil(maxWidth));
		this.setHeight((int) Math.ceil(maxHeight));
	}

	private void drawEmptyHint() {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		nvg.setupAndDraw(() -> {

			String hint = TranslateText.SCRIPT_HUD_EMPTY.getText();
			float hintWidth = this.getTextWidth(hint, FONT_SIZE, Fonts.REGULAR);

			this.drawBackground(hintWidth + (PADDING * 2F), FONT_SIZE + (PADDING * 2F));
			this.drawText(hint, PADDING, PADDING, FONT_SIZE, Fonts.REGULAR);

			this.setWidth((int) Math.ceil(hintWidth + (PADDING * 2F)));
			this.setHeight((int) Math.ceil(FONT_SIZE + (PADDING * 2F)));
		});
	}
}
