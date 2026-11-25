package mix.client.mixui.management.language;

import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import net.minecraft.util.ResourceLocation;

public enum Language {
	JAPANESE("ja", TranslateText.JAPANESE, new ResourceLocation("mixclient/flag/ja.png")), 
	CHINESE("cn", TranslateText.CHINESE, new ResourceLocation("mixclient/flag/cn.png")), 
	ENGLISH("en", TranslateText.ENGLISH_US, new ResourceLocation("mixclient/flag/us.png"));
	
	private SimpleAnimation animation = new SimpleAnimation();
	
	private String id;
	private TranslateText nameTranslate;
	private ResourceLocation flag;
	
	private Language(String id, TranslateText nameTranslate, ResourceLocation flag) {
		this.id = id;
		this.nameTranslate = nameTranslate;
		this.flag = flag;
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return nameTranslate.getText();
	}
	
	public ResourceLocation getFlag() {
		return flag;
	}

	public SimpleAnimation getAnimation() {
		return animation;
	}

	public TranslateText getNameTranslate() {
		return nameTranslate;
	}

	public static Language getLanguageById(String id) {
		
		for(Language lang : Language.values()) {
			if(lang.getId().equals(id)) {
				return lang;
			}
		}
		
		return Language.ENGLISH;
	}
}
