package mix.client.mixui.utils.mouse;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import mix.client.mixui.utils.animation.simple.SimpleAnimation;

public class Scroll {

	private float maxScroll = Float.MAX_VALUE, minScroll = 0, rawScroll;
	private SimpleAnimation scrollAnimation = new SimpleAnimation(0.0F);
	
	public void onScroll() {
		onScroll(2);
	}
	
	public void onAnimation() {
		onAnimation(14);
	}
	
	private void onScroll(int scrollSpeed) {
		
		int dWheel = Mouse.getDWheel();
		
		if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
	        rawScroll += dWheel / scrollSpeed;
		}else {
	        rawScroll += dWheel / (scrollSpeed * 2);
		}

        rawScroll = Math.max(Math.min(minScroll, rawScroll), -maxScroll);
	}
	
	private void onAnimation(int animationSpeed) {
		scrollAnimation.setAnimation(rawScroll, animationSpeed);
	}
	
	public void setMaxScroll(float scroll) {
		maxScroll = scroll;
		rawScroll = Math.max(Math.min(minScroll, rawScroll), -maxScroll);
	}
	
	public float getValue() {
        return scrollAnimation.getValue();
	}
	
	// 返回滚动目标位置（未插值的原始偏移），用于保存/恢复滚动位置
	public float getScrollPosition() {
		return rawScroll;
	}

	public void setScrollPosition(float scroll) {
		rawScroll = scroll;
		scrollAnimation.setValue(scroll);
	}

	public void reset() {
		rawScroll = minScroll;
	}
	
	public void resetAll() {
		rawScroll = minScroll;
		scrollAnimation.setValue(minScroll);
	}
}
