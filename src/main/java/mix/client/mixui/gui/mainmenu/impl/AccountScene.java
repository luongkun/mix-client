package mix.client.mixui.gui.mainmenu.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.MainMenuScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.LastMessageScene;
import mix.client.mixui.injection.interfaces.IMixinMinecraft;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.AccountManager;
import mix.client.mixui.management.account.AccountType;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.mcef.Mcef;
import mix.client.mixui.ui.comp.impl.field.CompMainMenuTextBox;
import mix.client.mixui.utils.ImageUtils;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.utils.file.FileUtils;
import mix.client.mixui.utils.mouse.MouseUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

public class AccountScene extends MainMenuScene {

	private static final Color PANEL_COLOR = new Color(56, 60, 68, 230);
	private static final Color CHIP_COLOR = new Color(255, 255, 255, 30);

	public CompMainMenuTextBox textBox = new CompMainMenuTextBox();
	
	private File offlineSkin;
	
	public AccountScene(GuiMixMainMenu parent) {
		super(parent);
	}

	@Override
	public void initScene() {
		if(!Mcef.isInitialized()) {
			Mcef.getClient();
		}
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.setupAndDraw(() -> drawNanoVG(mouseX, mouseY, partialTicks, sr, instance, nvg));
	}
	
	public void drawNanoVG(int mouseX, int mouseY, float partialTicks, ScaledResolution sr, Mix instance, NanoVGManager nvg) {
		
		int acWidth = 220;
		int acHeight = 138;
		int acX = sr.getScaledWidth() / 2 - (acWidth / 2);
		int acY = sr.getScaledHeight() / 2 - (acHeight / 2);
		
		String loginMessage = TranslateText.LOGIN_MESSAGE.getText();
		String microsoftLogin = TranslateText.MICROSOFT_LOGIN.getText();
		String offlineLogin = TranslateText.OFFLINE_LOGIN.getText();
		
		nvg.drawRoundedRect(acX, acY, acWidth, acHeight, 8, PANEL_COLOR);
		nvg.drawOutlineRoundedRect(acX, acY, acWidth, acHeight, 8, 1.0F, new Color(255, 255, 255, 42));
		nvg.drawCenteredText(loginMessage, acX + (acWidth / 2), acY + 9, Color.WHITE, 14, Fonts.REGULAR);
		
		nvg.drawRoundedRect(acX + 10, acY + 29, 200, 30, 5, CHIP_COLOR);
		
		nvg.drawText(Icon.LOGIN, acX + 18, acY + 38, Color.WHITE, 11, Fonts.ICON);
		nvg.drawText("Token Login", acX + 38, acY + 35, Color.WHITE, 10, Fonts.REGULAR);
		nvg.drawText("Minecraft access token", acX + 38, acY + 46, new Color(255, 255, 255, 170), 7, Fonts.REGULAR);
		

		
		nvg.drawCenteredText(offlineLogin, acX + (acWidth / 2), acY + 67, Color.WHITE, 14, Fonts.REGULAR);
		
		nvg.drawRoundedRect(acX + acWidth - 30, acY + 86, 20, 20, 4, CHIP_COLOR);
		nvg.drawText(Icon.USER, acX + acWidth - 25, acY + 91, Color.WHITE, 10, Fonts.ICON);
		
		textBox.setBackgroundColor(CHIP_COLOR);
		textBox.setFontColor(Color.WHITE);
		textBox.setPosition(acX + 10, acY + 86, 175, 20);
		textBox.setEmptyText(Icon.PENCIL, TranslateText.NAME.getText());
		textBox.draw(mouseX, mouseY, partialTicks);
		
		nvg.drawRoundedRect(acX + acWidth - 96 - 10, acY + 86 + 25, 96, 20, 4, CHIP_COLOR);
		nvg.drawCenteredText(TranslateText.LOGIN.getText(), acX + acWidth - (96 / 2) - 10, acY + 86 + 31, Color.WHITE, 10F, Fonts.REGULAR);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		Mix instance = Mix.getInstance();
		AccountManager accountManager = instance.getAccountManager();
		FileManager fileManager = instance.getFileManager();
		
		int acWidth = 220;
		int acHeight = 140;
		int acX = sr.getScaledWidth() / 2 - (acWidth / 2);
		int acY = sr.getScaledHeight() / 2 - (acHeight / 2);
		
		if(mouseButton == 0) {
			
			if(MouseUtils.isInside(mouseX, mouseY, acX + acWidth - 30, acY + 86, 20, 20)) {
				Multithreading.runAsync(() -> {
					
					File selectSkin = FileUtils.selectImageFile();
					
					if(selectSkin != null) {
						
						try {
							
							File copyFile = new File(fileManager.getCacheDir(), "skin/" + selectSkin.getName());
							BufferedImage image = ImageIO.read(selectSkin);
							
							if(image.getWidth() == 64 && image.getHeight() == 64) {
								FileUtils.copyFile(selectSkin, copyFile);
								offlineSkin = copyFile;
							}
							
						} catch(Exception e) {}
					}
				});
			}
			
			if(MouseUtils.isInside(mouseX, mouseY, acX + acWidth - 96 - 10, acY + 86 + 25, 96, 20)) {
				
				File renameFile = new File(fileManager.getCacheDir(), "skin/" + textBox.getText() + ".png");
				File headDir = new File(fileManager.getCacheDir(), "head");
				
				Account acc = new Account(textBox.getText(), "0", "0", AccountType.OFFLINE);
				
				if(offlineSkin != null) {
					offlineSkin.renameTo(renameFile);
					offlineSkin = renameFile;
				}
				
				if(offlineSkin != null && offlineSkin.exists()) {
					
					acc.setSkinFile(offlineSkin);
					
					try {
						
						BufferedImage rawImage = ImageIO.read(offlineSkin);
						BufferedImage headImage = ImageUtils.scissor(rawImage, 8, 8, 8, 8);
						BufferedImage layerImage = ImageUtils.scissor(rawImage, 40, 8, 8, 8);
						BufferedImage conbineImage = ImageUtils.combine(headImage, layerImage);
						
						ImageIO.write(ImageUtils.resize(conbineImage, 128, 128), "png", new File(headDir, acc.getName() + ".png"));
					} catch(Exception e) {}
				}
				
		        ((IMixinMinecraft) mc).setSession(new Session(acc.getName(), "0", "0", "mojang"));
		        
				if(accountManager.getAccountByName(acc.getName()) == null) {
					accountManager.getAccounts().add(acc);
				}
				
		        accountManager.setCurrentAccount(acc);
		        accountManager.save();
		        
		        offlineSkin = null;
		        
		        getAfterLoginRunnable().run();
			}
			
			if(MouseUtils.isInside(mouseX, mouseY, acX + 10, acY + 29, 200, 30)) {
				this.setCurrentScene(this.getSceneByClass(TokenLoginScene.class));
			}
		}
		
		textBox.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		
		textBox.keyTyped(typedChar, keyCode);
		
		if(keyCode == Keyboard.KEY_ESCAPE) {
			this.setCurrentScene(this.getSceneByClass(MainScene.class));
		}
	}
	
	private Runnable getAfterLoginRunnable() {
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
		        if(Mix.getInstance().getApi().isFirstLogin()) {
					setCurrentScene(getSceneByClass(LastMessageScene.class));
		        } else {
					setCurrentScene(getSceneByClass(MainScene.class));
		        }
			}
		};
		
		return runnable;
	}
}
