package mix.client.mixui.management.notification;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.TimerUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.easing.EaseBackIn;
import mix.client.mixui.utils.buffer.ScreenAlpha;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Notification {

	private Animation animation;
	private TranslateText title, message;
	private NotificationType type;
	private TimerUtils timer;
	
	private ScreenAlpha screenAlpha = new ScreenAlpha();
	
	public Notification(TranslateText title, TranslateText message, NotificationType type) {
		this.title = title;
		this.message = message;
		this.type = type;
		this.timer = new TimerUtils();
	}
	
	public void draw() {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		
		screenAlpha.wrap(() -> drawNanoVG(nvg), animation.getValueFloat());
	}
	
	private void drawNanoVG(NanoVGManager nvg) {
		
		ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
		Mix instance = Mix.getInstance();
		AccentColor currentColor = instance.getColorManager().getCurrentColor();
		
		float maxWidth;
		float titleWidth = nvg.getTextWidth(title.getText(), 9.6F, Fonts.MEDIUM);
		float messageWidth = nvg.getTextWidth(message.getText(), 7.6F, Fonts.REGULAR);
		
		if(titleWidth > messageWidth) {
			maxWidth = titleWidth;
		}else {
			maxWidth = messageWidth;
		}
		
		maxWidth = maxWidth + 31;
		
		int x = (int) (sr.getScaledWidth() - maxWidth) - 8;
		int y = sr.getScaledHeight() - 29 - 8;
		
		if(timer.delay(3000)) {
			animation.setDirection(Direction.BACKWARDS);
		}
		
		nvg.save();
		nvg.translate(160 - (animation.getValueFloat() * 160), 0);
		
		nvg.drawShadow(x, y, maxWidth, 29, 6);
		nvg.drawGradientRoundedRect(x, y, maxWidth, 29, 6, ColorUtils.applyAlpha(currentColor.getColor1(), 220), ColorUtils.applyAlpha(currentColor.getColor2(), 220));
		nvg.drawText(type.getIcon(), x + 5, y + 6F, Color.WHITE, 17, Fonts.ICON);
		nvg.drawText(title.getText(), x + 26, y + 6F, Color.white, 9.6F, Fonts.MEDIUM);
		nvg.drawText(message.getText(), x + 26, y + 17.5F, Color.WHITE, 7.5F, Fonts.REGULAR);
		
		nvg.restore();
	}
	
	public void show() {
		animation = new EaseBackIn(300, 1, 0);
		animation.setDirection(Direction.FORWARDS);
		animation.reset();
		timer.reset();
	}
	
	public boolean isShown() {
		return !animation.isDone(Direction.BACKWARDS);
	}
	
	public Animation getAnimation() {
		return animation;
	}

	public TimerUtils getTimer() {
		return timer;
	}
}
