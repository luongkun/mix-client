package mix.client.mixui.gui.mainmenu;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lwjgl.input.Mouse;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.impl.AccountScene;
import mix.client.mixui.gui.mainmenu.impl.BackgroundScene;
import mix.client.mixui.gui.mainmenu.impl.MainScene;
import mix.client.mixui.gui.mainmenu.impl.MicrosoftLoginScene;
import mix.client.mixui.gui.mainmenu.impl.ShopScene;
import mix.client.mixui.gui.mainmenu.impl.TokenLoginScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.AccentColorSelectScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.CheckingDataScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.FirstLoginScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.LastMessageScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.LoginMessageScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.ThemeSelectScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.WelcomeMessageScene;
import mix.client.mixui.injection.interfaces.IMixinMinecraft;
import mix.client.mixui.integration.dango.DangoMusicApi;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.AccountManager;
import mix.client.mixui.management.account.AccountType;
import mix.client.mixui.management.account.skin.SkinDownloader;
import mix.client.mixui.management.account.token.TokenAuthentication;
import mix.client.mixui.management.color.palette.ColorPalette;
import mix.client.mixui.management.event.impl.EventRenderNotification;
import mix.client.mixui.management.music.MusicGate;
import mix.client.mixui.management.music.MusicManager;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.profile.mainmenu.impl.Background;
import mix.client.mixui.management.profile.mainmenu.impl.CustomBackground;
import mix.client.mixui.management.profile.mainmenu.impl.DefaultBackground;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.DecelerateAnimation;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

public class GuiMixMainMenu extends GuiScreen {

	private MainMenuScene currentScene;
	
	private SimpleAnimation closeFocusAnimation = new SimpleAnimation();
	private SimpleAnimation shopFocusAnimation = new SimpleAnimation();
	private SimpleAnimation backgroundSelectFocusAnimation = new SimpleAnimation();
	private SimpleAnimation[] backgroundAnimations = new SimpleAnimation[2];
	
    private boolean openAccount;
    private SimpleAnimation accountAnimation = new SimpleAnimation();
    
    private ArrayList<MainMenuScene> scenes = new ArrayList<MainMenuScene>();
    
    private Account removeAccount;
	private boolean sessionHeadRequested;
    
    private Animation fadeIconAnimation, fadeBackgroundAnimation;

    // 右上角那组按钮的「切换」过渡：0 = 刚切过去（透明 + 偏右），1 = 稳定状态。
    // 主菜单右上角是唯一常驻的控件（关窗 / 换背景 / 商店），场景一变它就"该有新东西"，
    // 但它是同一个界面画的、不会重建，所以只能靠这条动画让切换有可见反馈。
    private final SimpleAnimation buttonSwapAnimation = new SimpleAnimation(1.0F);

    // 动画进行中的横向偏移量。mouseClicked 也要用它：按钮在滑动时如果命中判定还按原位算，
    // 就会出现"看得见点不着"或者"点空白却触发了关窗"。
    private float buttonSwapOffset;
    
	public GuiMixMainMenu() {
		
		Mix instance = Mix.getInstance();
		
		for(int i = 0; i < backgroundAnimations.length; i++) {
			backgroundAnimations[i] = new SimpleAnimation();
		}
		
		scenes.add(new MainScene(this));
		scenes.add(new AccountScene(this));
		scenes.add(new BackgroundScene(this));
		scenes.add(new ShopScene(this));
		scenes.add(new WelcomeMessageScene(this));
		scenes.add(new ThemeSelectScene(this));
		scenes.add(new AccentColorSelectScene(this));
		scenes.add(new LoginMessageScene(this));
		scenes.add(new FirstLoginScene(this));
		scenes.add(new LastMessageScene(this));
		scenes.add(new CheckingDataScene(this));
		scenes.add(new MicrosoftLoginScene(this));
		scenes.add(new TokenLoginScene(this));
		
		if(instance.getApi().isFirstLogin()) {
			currentScene = getSceneByClass(WelcomeMessageScene.class);
		} else {
			// The launcher (PCL2 / official launcher) already handed us a session, so never
			// open the account scene on startup: only fall back to it when there is no
			// account AND no usable session name either.
			if(instance.getAccountManager().getCurrentAccount() == null && "Login".equals(getSessionDisplayName())) {
				currentScene = getSceneByClass(AccountScene.class);
			} else {
				currentScene = getSceneByClass(MainScene.class);
			}
		}
	}
	
	@Override
	public void initGui() {
		currentScene.initGui();
		
		// 主菜单一露脸就静音，但**不在这里恢复**：关掉主菜单只代表玩家点了「单人游戏」
		// 或「多人游戏」，他还在挑存档/服务器，音乐不该在这时候响起来。
		// 欠下的恢复留给 MusicGate，由世界加载事件（EventLoadWorld）来还。
		MusicGate.pauseForMenu();
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		boolean isFirstLogin = instance.getApi().isFirstLogin();
		
		if(removeAccount != null) {
			instance.getAccountManager().getAccounts().remove(removeAccount);
			
			if(removeAccount.equals(instance.getAccountManager().getCurrentAccount())) {
				instance.getAccountManager().setCurrentAccount(null);
			}
			
			instance.getAccountManager().save();
			removeAccount = null;
		}
		
		backgroundAnimations[0].setAnimation(Mouse.getX(), 16);
		backgroundAnimations[1].setAnimation(Mouse.getY(), 16);
		
		nvg.setupAndDraw(() -> {
			
			drawNanoVG(sr, instance, nvg);
			
			if(!isFirstLogin) {
				drawButtons(mouseX, mouseY, sr, nvg);
			}
		});
		
		if(!isFirstLogin) {
			drawAccount(mouseX, mouseY, instance, nvg);
		}
		
		if(currentScene != null) {
			currentScene.drawScreen(mouseX, mouseY, partialTicks);
		}
		
		if(fadeBackgroundAnimation == null || (fadeBackgroundAnimation != null && !fadeBackgroundAnimation.isDone(Direction.FORWARDS))) {
			nvg.setupAndDraw(() -> drawSplashScreen(sr, nvg));
		}
		
		nvg.setupAndDraw(() -> {
			new EventRenderNotification().call();
		});
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	private void drawNanoVG(ScaledResolution sr, Mix instance, NanoVGManager nvg) {
		
		String copyright = "Copyright Mojang AB. Do not distribute!";
		Background currentBackground = instance.getProfileManager().getBackgroundManager().getCurrentBackground();
		
		if(currentBackground instanceof DefaultBackground) {
			
			DefaultBackground bg = (DefaultBackground) currentBackground;
			
			nvg.drawImage(bg.getImage(), -21 + backgroundAnimations[0].getValue() / 90, backgroundAnimations[1].getValue() * -1 / 90, sr.getScaledWidth() + 21, sr.getScaledHeight() + 20);
		}else if(currentBackground instanceof CustomBackground) {
			
			CustomBackground bg = (CustomBackground) currentBackground;
			
			nvg.drawImage(bg.getImage(), -21 + backgroundAnimations[0].getValue() / 90, backgroundAnimations[1].getValue() * -1 / 90, sr.getScaledWidth() + 21, sr.getScaledHeight() + 20);
		}

		nvg.drawText(copyright, sr.getScaledWidth() - (nvg.getTextWidth(copyright, 9, Fonts.REGULAR)) - 4, sr.getScaledHeight() - 12, new Color(255, 255, 255), 9, Fonts.REGULAR);
		nvg.drawText("Mix Client " + instance.getVersion(), 4, sr.getScaledHeight() - 12, new Color(255, 255, 255), 9, Fonts.REGULAR);
	}
	
	private void drawButtons(int mouseX, int mouseY, ScaledResolution sr, NanoVGManager nvg) {
		
		// 切换场景时整组按钮从右侧滑入并淡入：0 → 1 约 200ms。
		// 位移和透明度共用同一条曲线，观感上就是"这一组是刚换出来的"，
		// 而不是像原来那样换个界面按钮纹丝不动、只有背后的内容变了。
		buttonSwapAnimation.setAnimation(1.0F, 14);
		
		float swap = Math.max(0.0F, Math.min(1.0F, buttonSwapAnimation.getValue()));
		float eased = 1.0F - (1.0F - swap) * (1.0F - swap) * (1.0F - swap);
		float swapDX = (1.0F - eased) * 14.0F;
		
		buttonSwapOffset = swapDX;
		
		if(eased < 0.004F) {
			return;
		}
		
		// 命中判定要按"看得见的位置"算：按钮滑到哪，点就得跟到哪。
		int shiftedX = mouseX - Math.round(swapDX);
		float alpha = eased;
		
		nvg.save();
		nvg.setAlpha(alpha);
		
		closeFocusAnimation.setAnimation(MouseUtils.isInside(shiftedX, mouseY, sr.getScaledWidth() - 28, 6, 22, 22) ? 1.0F : 0.0F, 16);
		
		nvg.drawRoundedRect(sr.getScaledWidth() - 28 + swapDX, 6, 22, 22, 4, this.getBackgroundColor());
		nvg.drawCenteredText(Icon.X, sr.getScaledWidth() - 19F + swapDX, 8F, new Color(255, 255 - (int) (closeFocusAnimation.getValue() * 200), 255 - (int) (closeFocusAnimation.getValue() * 200)), 18, Fonts.ICON);
		
		backgroundSelectFocusAnimation.setAnimation(MouseUtils.isInside(shiftedX, mouseY, sr.getScaledWidth() - 28 - 28, 6, 22, 22) ? 1.0F : 0.0F, 16);
		
		nvg.drawRoundedRect(sr.getScaledWidth() - 28 - 28 + swapDX, 6, 22, 22, 4, this.getBackgroundColor());
		nvg.drawCenteredText(Icon.IMAGE, sr.getScaledWidth() - 19F - 26.5F + swapDX, 9.5F, new Color(255 - (int) (backgroundSelectFocusAnimation.getValue() * 200), 255, 255 - (int) (backgroundSelectFocusAnimation.getValue() * 200)), 15, Fonts.ICON);
		
		shopFocusAnimation.setAnimation(MouseUtils.isInside(shiftedX, mouseY, sr.getScaledWidth() - (28 * 3), 6, 22, 22) ? 1.0F : 0.0F, 16);
		
		nvg.drawRoundedRect(sr.getScaledWidth() - (28 * 3) + swapDX, 6, 22, 22, 4, this.getBackgroundColor());
		nvg.drawCenteredText(Icon.SHOPPING, sr.getScaledWidth() - (26 * 3) + 4.5F + swapDX, 9.5F, new Color(255 - (int) (shopFocusAnimation.getValue() * 200), 255, 255), 15, Fonts.ICON);
		
		nvg.restore();
	}
	
	private void drawAccount(int mouseX, int mouseY, Mix instance, NanoVGManager nvg) {
		
		Account currentAccount = instance.getAccountManager().getCurrentAccount();
		
		nvg.setupAndDraw(() -> drawAccountNanoVG(mouseX, mouseY, instance, nvg, currentAccount));
	}
	
	private void drawAccountNanoVG(int mouseX, int mouseY, Mix instance, NanoVGManager nvg, Account currentAccount) {
		
		AccountManager accountManager = instance.getAccountManager();
		ColorPalette palette = instance.getColorManager().getPalette();
		float progress = accountAnimation.getValue();
		String displayName = currentAccount == null ? getSessionDisplayName() : currentAccount.getName();
		float maxUserWidth = nvg.getTextWidth(displayName, 10.5F, Fonts.REGULAR);
		int others = 0;
		
		for(Account acc : accountManager.getAccounts()) {
			
			float tWidth = nvg.getTextWidth(acc.getName(), 10.5F, Fonts.REGULAR);
			
			if(tWidth > maxUserWidth) {
				maxUserWidth = tWidth;
			}
			
			if(currentAccount == null || !acc.equals(currentAccount)) {
				others++;
			}
		}
		
		float panelWidth = 6 + 18 + 8 + maxUserWidth + 30;
		float panelHeight = 28 + (progress * others * 24);
		
		boolean isInsideAccount = MouseUtils.isInside(mouseX, mouseY, 6, 6, panelWidth, 28);

		if (openAccount) {
			isInsideAccount = true;
		}

		if (MouseUtils.isInside(mouseX, mouseY, 6, 6, panelWidth, 28)) {
		    openAccount = isInsideAccount;
		} else {
		    openAccount = isInsideAccount && MouseUtils.isInside(mouseX, mouseY, 6, 6, panelWidth, 28 + (others * 24));
		}
		
		accountAnimation.setAnimation(openAccount ? 1.0F : 0F, 16);
		
		nvg.drawRoundedRect(6, 6, panelWidth, panelHeight, 8, this.getBackgroundColor());
		
		String headName = "Login".equals(displayName) ? null : displayName;
		File headFile = headName == null ? null : new File(instance.getFileManager().getCacheDir(), "head/" + headName + ".png");
		// Fetch the skin head through the launcher session when the cached file is missing or is
		// still a legacy 16x32 body sheet, so the panel never keeps showing a steve head.
		String sessionName = getSessionDisplayName();

		if((headFile == null || !SkinDownloader.isFaceShaped(headFile)) && !sessionHeadRequested && sessionName.equals(displayName)) {

			Session session = mc.getSession();

			if(session != null && session.getToken() != null && session.getToken().length() > 100) {
				sessionHeadRequested = true;
				new TokenAuthentication().downloadHeadOnly(session.getToken());
			}
		}
		
		if(headFile == null || !SkinDownloader.isFaceShaped(headFile)) {
			nvg.drawPlayerHead(new ResourceLocation("textures/entity/steve.png"), 12, 11, 18, 18, 4);
		}else {
			nvg.drawRoundedImage(headFile, 12, 11, 18, 18, 4);
		}
		
		nvg.drawText(displayName, 36, 14, Color.WHITE, 10.5F, Fonts.REGULAR);
		nvg.drawText(Icon.PLUS, 6 + panelWidth - 26, 11, new Color(255, 255, 255, 210), 14F, Fonts.ICON);
		
		if(others > 0 && progress > 0.01F) {
			
			int index = 0;
			int alpha = (int) (progress * 255);
			
			for(Account acc : accountManager.getAccounts()) {
				
				if(currentAccount != null && acc.equals(currentAccount)) {
					continue;
				}
				
				float rowY = 6 + 28 + (index * 24);
				File otherHeadFile = new File(instance.getFileManager().getCacheDir(), "head/" + acc.getName() + ".png");
				
				nvg.drawRoundedRect(6, rowY, panelWidth, 24, 4, new Color(255, 255, 255, 22));
				
				if(!SkinDownloader.isFaceShaped(otherHeadFile)) {
					nvg.drawPlayerHead(new ResourceLocation("textures/entity/steve.png"), 12, rowY + 4, 16, 16, 3, progress);
				}else {
					nvg.drawRoundedImage(otherHeadFile, 12, rowY + 4, 16, 16, 3, progress);
				}
				
				nvg.drawText(acc.getName(), 34, rowY + 7, new Color(255, 255, 255, alpha), 9.5F, Fonts.REGULAR);
				nvg.drawText(Icon.TRASH, 6 + panelWidth - 25, rowY + 5, palette.getMaterialRed(alpha), 10F, Fonts.ICON);
				
				index++;
			}
		}
	}
	
	private String getSessionDisplayName() {

		try {

			// Also called from the constructor, where the inherited mc field is still null.
			Minecraft client = mc != null ? mc : Minecraft.getMinecraft();
			Session session = client == null ? null : client.getSession();

			if(session != null) {

				String name = session.getUsername();

				if(name != null && !name.trim().isEmpty() && !"Player".equalsIgnoreCase(name)) {
					return name.trim();
				}
			}
		} catch (Exception e) {
		}

		return "Login";
	}

	private void drawSplashScreen(ScaledResolution sr, NanoVGManager nvg) {
		
		if(fadeIconAnimation == null) {
			fadeIconAnimation = new DecelerateAnimation(1000, 1);
			fadeIconAnimation.setDirection(Direction.FORWARDS);
			fadeIconAnimation.reset();
		}
		
		if(fadeIconAnimation != null) {
			
			if(fadeIconAnimation.isDone(Direction.FORWARDS) && fadeBackgroundAnimation == null) {
				fadeBackgroundAnimation = new DecelerateAnimation(1000, 1);
				fadeBackgroundAnimation.setDirection(Direction.FORWARDS);
				fadeBackgroundAnimation.reset();
			}
			
			nvg.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, fadeBackgroundAnimation != null ? (int) (255 - (fadeBackgroundAnimation.getValue() * 255)) : 255));
			nvg.drawCenteredText(Icon.MIX, sr.getScaledWidth() / 2, (sr.getScaledHeight() / 2) - (nvg.getTextHeight(Icon.MIX, 130, Fonts.ICON) / 2) - 1, new Color(255, 255, 255, (int) (255 - (fadeIconAnimation.getValue() * 255))), 130, Fonts.ICON);
		}
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		AccountManager accountManager = instance.getAccountManager();
		boolean isFirstLogin = instance.getApi().isFirstLogin();
		
		if(mouseButton == 0 && !isFirstLogin) {
			
			// 切换动画期间按钮是滑着走的，命中判定必须跟着滑：
			// 用绘制时的同一份偏移把鼠标坐标换算回"按钮静止时的坐标系"，
			// 否则会出现看得见点不着、或者点到空白却关了游戏。
			int buttonMouseX = mouseX - Math.round(buttonSwapOffset);
			
			if(MouseUtils.isInside(buttonMouseX, mouseY, sr.getScaledWidth() - 28, 6, 22, 22)) {
				mc.shutdown();
			}
			
			if(MouseUtils.isInside(buttonMouseX, mouseY, sr.getScaledWidth() - 28 - 28, 6, 22, 22)) {
				this.setCurrentScene(this.getSceneByClass(BackgroundScene.class));
			}
			
			if(MouseUtils.isInside(buttonMouseX, mouseY, sr.getScaledWidth() - (28 * 3), 6, 22, 22)) {
				this.setCurrentScene(this.getSceneByClass(ShopScene.class));
			}
			
			Account currentAccount = accountManager.getCurrentAccount();
			String displayName = currentAccount == null ? getSessionDisplayName() : currentAccount.getName();
			float maxUserWidth = nvg.getTextWidth(displayName, 10.5F, Fonts.REGULAR);
			int others = 0;
			
			for(Account acc : accountManager.getAccounts()) {
				
				float tWidth = nvg.getTextWidth(acc.getName(), 10.5F, Fonts.REGULAR);
				
				if(tWidth > maxUserWidth) {
					maxUserWidth = tWidth;
				}
				
				if(currentAccount == null || !acc.equals(currentAccount)) {
					others++;
				}
			}
			
			float panelWidth = 6 + 18 + 8 + maxUserWidth + 30;
			
			if(openAccount) {
				
				if(MouseUtils.isInside(mouseX, mouseY, 6 + panelWidth - 28, 6, 28, 28)) {
					currentScene = getSceneByClass(AccountScene.class);
				}
				
				int index = 0;
				
				for(Account acc : accountManager.getAccounts()) {
					
					if(currentAccount != null && acc.equals(currentAccount)) {
						continue;
					}
					
					float rowY = 6 + 28 + (index * 24);
					
					if(MouseUtils.isInside(mouseX, mouseY, 6 + panelWidth - 27, rowY + 3, 22, 20)) {
						removeAccount = acc;
					}else if(MouseUtils.isInside(mouseX, mouseY, 6, rowY, panelWidth, 24)) {
						
						if(AccountType.TOKEN.equals(acc.getType()) || AccountType.LAUNCHER.equals(acc.getType())) {
							((IMixinMinecraft) mc).setSession(new Session(acc.getName(), acc.getUuid(), acc.getRefreshToken(), "mojang"));
							accountManager.setCurrentAccount(acc);
							accountManager.save();
						}else if(AccountType.MICROSOFT.equals(acc.getType())) {
							accountManager.getAuthenticator().loginWithRefreshToken(acc.getRefreshToken());
						}else {
							((IMixinMinecraft) mc).setSession(new Session(acc.getName(), "0", "0", "mojang"));
							accountManager.setCurrentAccount(acc);
							accountManager.save();
						}
					}
					
					index++;
				}
			}
			
			if(currentAccount == null && MouseUtils.isInside(mouseX, mouseY, 6, 6, panelWidth, 28)) {
				currentScene = getSceneByClass(AccountScene.class);
			}
		}
		
		currentScene.mouseClicked(mouseX, mouseY, mouseButton);
		try {
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		currentScene.mouseReleased(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		currentScene.keyTyped(typedChar, keyCode);
	}
	
	@Override
	public void handleInput() {
		
		if(currentScene instanceof MicrosoftLoginScene) {
			currentScene.handleInput();
		} else {
			try {
				super.handleInput();
			} catch (java.io.IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void onGuiClosed() {
		currentScene.onGuiClosed();
		
		// 这里以前会把暂停的音乐恢复回去，那是错的：点「单人游戏」进的是存档列表，
		// 点「多人游戏」进的是服务器列表，玩家还没进任何世界，音乐就已经响了。
		// 恢复点已挪到 MusicGate.resumeOnWorldJoin()（世界加载时才触发）。
	}
	
	public MainMenuScene getCurrentScene() {
		return currentScene;
	}

	public void setCurrentScene(MainMenuScene currentScene) {
		
		if(this.currentScene != null) {
			this.currentScene.onSceneClosed();
		}
		
		// 真的换了场景才重置。AccountScene 之类会反复调 setCurrentScene(自己)，
		// 无条件归零会让右上角按钮一直重播淡入。
		if(currentScene != this.currentScene) {
			buttonSwapAnimation.setValue(0.0F);
		}
		
		this.currentScene = currentScene;
		
		if(this.currentScene != null) {
			this.currentScene.initScene();
		}
	}
	
	public boolean isDoneBackgroundAnimation() {
		return fadeBackgroundAnimation != null && fadeBackgroundAnimation.isDone(Direction.FORWARDS);
	}
	
	public MainMenuScene getSceneByClass(Class<? extends MainMenuScene > clazz) {
		
		for(MainMenuScene s : scenes) {
			if(s.getClass().equals(clazz)) {
				return s;
			}
		}
		
		return null;
	}
	
	public Color getBackgroundColor() {
		return new Color(230, 230, 230, 120);
	}
}
