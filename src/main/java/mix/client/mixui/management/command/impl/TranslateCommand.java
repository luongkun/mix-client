package mix.client.mixui.management.command.impl;

import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.command.Command;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.impl.ChatTranslateMod;
import mix.client.mixui.management.mods.settings.impl.ComboSetting;
import mix.client.mixui.management.mods.settings.impl.combo.Option;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.utils.translate.Translator;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class TranslateCommand extends Command {

	private String to = Translator.JAPANESE;
	
	public TranslateCommand() {
		super("translate");
	}

	@Override
	public void onCommand(String message) {
		
		ComboSetting setting = ChatTranslateMod.getInstance().getLanguageSetting();
		Option option = setting.getOption();
		
		if(option.getTranslate().equals(TranslateText.JAPANESE)) {
			to = Translator.JAPANESE;
		} else if(option.getTranslate().equals(TranslateText.ENGLISH)) {
			to = Translator.ENGLISH;
		} else if(option.getTranslate().equals(TranslateText.CHINESE)) {
			to = Translator.CHINESE_SIMPLIFIED;
		} else if(option.getTranslate().equals(TranslateText.POLISH)) {
			to = Translator.POLISH;
		}
		
		String text = message;
		
		Multithreading.runAsync(()-> {
			try {
				mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Translate] " + EnumChatFormatting.WHITE + Translator.translate(text, Translator.AUTO_DETECT, to)));
			} catch (Exception e) {
				MixLogger.error("Failed translate", e);
			}
		});
	}
}
