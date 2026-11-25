package mix.client.mixui.gui.modmenu.category;

import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.ui.comp.impl.field.CompSearchBox;
import mix.client.mixui.utils.animation.ColorAnimation;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.mouse.Scroll;
import net.minecraft.client.Minecraft;

public class Category {

	public Minecraft mc = Minecraft.getMinecraft();
	
	private String icon;
	private GuiModMenu parent;
	
	private TranslateText nameTranslate;
	
	private SimpleAnimation textAnimation = new SimpleAnimation();
	private ColorAnimation textColorAnimation = new ColorAnimation();
	private SimpleAnimation categoryAnimation = new SimpleAnimation();
	
	private boolean initialized;
	
	public Scroll scroll;
	
	private boolean showSearchBox;
	
	public Category(GuiModMenu parent, TranslateText nameTranslate, String icon, boolean showSearchBox) {
		this.nameTranslate = nameTranslate;
		this.parent = parent;
		this.icon = icon;
		this.initialized = false;
		this.scroll = parent.getScroll();
		this.showSearchBox = showSearchBox;
	}
	
	public void initGui() {}
	
	public void initCategory() {}
	
	// 被左侧栏切到前台时调用（每次进入都触发，可在这里重播入场动画）。
	public void onShown() {}
	
	// 该分类内部是否打开了子页面（例如模组的设置/调试面板）。
	// 子页面打开时滚轮交给子页面处理，父级不再滚动分类列表。
	public boolean isSubPageOpen() { return false; }
	
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {}
	
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {}
	
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}
	
	public void keyTyped(char typedChar, int keyCode) {}
	
	// 该分类内部是否有输入框正在吃 ESC。
	// 返回 true 时父级不再把 ESC 当成「关菜单」，让分类自己先退出编辑态。
	public boolean isEscapeConsumed() { return false; }
	
	public String getName() {
		return nameTranslate.getText();
	}

	public String getNameKey() {
		return nameTranslate.getKey();
	}

	public String getIcon() {
		return icon;
	}

	public GuiModMenu getParent() {
		return parent;
	}

	public int getX() {
		return parent.getX() + parent.getRailWidth();
	}
	
	public int getY() {
		return parent.getY() + parent.getHeaderHeight();
	}
	
	public int getWidth() {
		return parent.getWidth() - parent.getRailWidth();
	}
	
	public int getHeight() {
		return parent.getHeight() - parent.getHeaderHeight();
	}

	public ColorAnimation getTextColorAnimation() {
		return textColorAnimation;
	}

	public SimpleAnimation getTextAnimation() {
		return textAnimation;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public SimpleAnimation getCategoryAnimation() {
		return categoryAnimation;
	}

	public boolean isShowSearchBox() {
		return showSearchBox;
	}
	
	public CompSearchBox getSearchBox() {
		return parent.getSearchBox();
	}
	
	public boolean isCanClose() {
		return parent.isCanClose();
	}
	
	public void setCanClose(boolean canClose) {
		parent.setCanClose(canClose);
	}
}
