package mix.client.mixui.gui.mainmenu.impl;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.MainMenuScene;
import mix.client.mixui.gui.mainmenu.impl.welcome.LastMessageScene;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.token.TokenAuthentication;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.ui.comp.impl.field.CompMainMenuTextBox;
import mix.client.mixui.utils.mouse.MouseUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class TokenLoginScene extends MainMenuScene {

	private static final Color PANEL_COLOR = new Color(56, 60, 68, 230);
	private static final Color CHIP_COLOR = new Color(255, 255, 255, 30);

	private CompMainMenuTextBox textBox = new CompMainMenuTextBox();
	private TokenAuthentication authenticator = new TokenAuthentication();

	private volatile String status = "";
	private volatile boolean busy = false;
	private volatile boolean success = false;
	private long successTime = 0L;

	public TokenLoginScene(GuiMixMainMenu parent) {
		super(parent);
		// Minecraft access tokens are long JWTs (1000+ characters), the default textbox limit is 256.
		this.textBox.setMaxStringLength(8192);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		nvg.setupAndDraw(() -> drawNanoVG(mouseX, mouseY, partialTicks));

		if(success && System.currentTimeMillis() - successTime > 700L) {

			success = false;

			if(Mix.getInstance().getApi().isFirstLogin()) {
				this.setCurrentScene(this.getSceneByClass(LastMessageScene.class));
			} else {
				this.setCurrentScene(this.getSceneByClass(MainScene.class));
			}
		}
	}

	private void drawNanoVG(int mouseX, int mouseY, float partialTicks) {

		ScaledResolution sr = new ScaledResolution(mc);
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();

		int acWidth = 260;
		int acHeight = 164;
		int acX = sr.getScaledWidth() / 2 - (acWidth / 2);
		int acY = sr.getScaledHeight() / 2 - (acHeight / 2);

		nvg.drawRoundedRect(acX, acY, acWidth, acHeight, 8, PANEL_COLOR);
		nvg.drawOutlineRoundedRect(acX, acY, acWidth, acHeight, 8, 1.0F, new Color(255, 255, 255, 42));
		nvg.drawCenteredText("Token Login", acX + (acWidth / 2), acY + 9, Color.WHITE, 13, Fonts.REGULAR);
		nvg.drawCenteredText("Login with a Minecraft access token", acX + (acWidth / 2), acY + 27, new Color(255, 255, 255, 165), 7.5F, Fonts.REGULAR);

		textBox.setBackgroundColor(CHIP_COLOR);
		textBox.setFontColor(Color.WHITE);
		textBox.setPosition(acX + 12, acY + 44, 236, 22);
		textBox.draw(mouseX, mouseY, partialTicks);

		boolean hoverLogin = !busy && MouseUtils.isInside(mouseX, mouseY, acX + 12, acY + 74, 112, 22);

		nvg.drawRoundedRect(acX + 12, acY + 74, 112, 22, 4, hoverLogin ? new Color(0, 135, 247, 220) : CHIP_COLOR);
		nvg.drawCenteredText(busy ? "Logging in..." : "Login", acX + 12 + 56, acY + 81, Color.WHITE, 10F, Fonts.REGULAR);

		boolean hoverPaste = !busy && MouseUtils.isInside(mouseX, mouseY, acX + 132, acY + 74, 116, 22);

		nvg.drawRoundedRect(acX + 132, acY + 74, 116, 22, 4, hoverPaste ? new Color(255, 255, 255, 80) : CHIP_COLOR);
		nvg.drawCenteredText("Paste from clipboard", acX + 132 + 58, acY + 81, Color.WHITE, 8.5F, Fonts.REGULAR);

		nvg.drawCenteredText("Tip: you can also press Ctrl+V in the token field", acX + (acWidth / 2), acY + 116, new Color(255, 255, 255, 120), 7.5F, Fonts.REGULAR);

		if(!status.isEmpty()) {

			Color statusColor = Color.WHITE;

			if(success) {
				statusColor = new Color(120, 230, 120);
			} else if(!busy) {
				statusColor = new Color(255, 120, 120);
			}

			nvg.drawCenteredText(status, acX + (acWidth / 2), acY + 100, statusColor, 9, Fonts.REGULAR);
		}

		nvg.drawCenteredText("ESC - Back", acX + (acWidth / 2), acY + 138, new Color(255, 255, 255, 130), 8, Fonts.REGULAR);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		ScaledResolution sr = new ScaledResolution(mc);

		int acWidth = 260;
		int acHeight = 164;
		int acX = sr.getScaledWidth() / 2 - (acWidth / 2);
		int acY = sr.getScaledHeight() / 2 - (acHeight / 2);

		if(mouseButton == 0 && !busy && MouseUtils.isInside(mouseX, mouseY, acX + 12, acY + 74, 112, 22)) {
			startLogin();
			return;
		}

		if(mouseButton == 0 && !busy && MouseUtils.isInside(mouseX, mouseY, acX + 132, acY + 74, 116, 22)) {
			pasteFromClipboard();
			return;
		}

		textBox.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {

		textBox.keyTyped(typedChar, keyCode);

		if(keyCode == Keyboard.KEY_ESCAPE) {
			this.setCurrentScene(this.getSceneByClass(AccountScene.class));
		}

		if(keyCode == Keyboard.KEY_RETURN && !busy) {
			startLogin();
		}
	}

	private void pasteFromClipboard() {

		String clipboard = GuiScreen.getClipboardString();

		if(clipboard == null) {
			clipboard = "";
		}

		clipboard = clipboard.trim();

		if(clipboard.isEmpty()) {
			status = "Your clipboard is empty";
			success = false;
			return;
		}

		textBox.setText(clipboard);
		status = "Pasted a " + clipboard.length() + " character token";
		success = false;
	}

	private void startLogin() {

		final String token = textBox.getText().trim();

		if(token.isEmpty()) {
			status = "Please paste a token first";
			success = false;
			return;
		}

		busy = true;
		success = false;
		status = "Verifying token...";

		authenticator.loginWithToken(token, (account, error) -> {

			if(account != null) {
				busy = false;
				success = true;
				successTime = System.currentTimeMillis();
				status = "Logged in as " + account.getName();
			} else {
				busy = false;
				success = false;
				status = error == null ? "Login failed" : error;
			}
		});
	}
}
