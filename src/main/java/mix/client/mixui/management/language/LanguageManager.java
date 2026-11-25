package mix.client.mixui.management.language;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import mix.client.mixui.logger.MixLogger;

public class LanguageManager {

	private HashMap<String, String> translateMap = new HashMap<String, String>();
	
	private Language currentLanguage;
	
	public LanguageManager() {
		setCurrentLanguage(Language.ENGLISH);
	}
	
	private void loadMap(HashMap<String, String> map, String language) {
		
		InputStream stream = LanguageManager.class.getClassLoader().getResourceAsStream("assets/minecraft/mixclient/language/" + language + ".properties");
		
		if(stream == null) {
			MixLogger.warn("Language file not found: " + language + ".properties");
			return;
		}
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			
			String s;
			
			while((s = reader.readLine()) != null) {
				
				if(s.isEmpty() || s.startsWith("#")) {
					continue;
				}
				
				// 旧实现用 split("=") 取 args[1]：值里再出现等号会被截断，
				// 值后面没有等号时（末尾是 '=' 或整行空值）还会数组越界，整份语言文件直接读崩。
				// 这里只切第一个等号，值原样保留。
				int index = s.indexOf('=');
				
				if(index <= 0) {
					continue;
				}
				
				String key = s.substring(0, index).trim();
				String value = s.substring(index + 1).trim();
				
				if(!key.isEmpty()) {
					map.put(key, value);
				}
			}
			
		} catch(Exception e) {
			MixLogger.error("Failed to load translate", e);
		}
	}

	public Language getCurrentLanguage() {
		return currentLanguage;
	}

	public void setCurrentLanguage(Language currentLanguage) {
		
		this.currentLanguage = currentLanguage;
		this.translateMap.clear();
		this.loadMap(translateMap, currentLanguage.getId());
		
		// 英文兜底：当前语言缺键时用英文，英文也缺时才由 TranslateText 自己推词，
		// 这样任何情况下界面上都不会再出现 "null"。
		HashMap<String, String> englishMap = null;
		
		if(currentLanguage != Language.ENGLISH) {
			englishMap = new HashMap<String, String>();
			this.loadMap(englishMap, Language.ENGLISH.getId());
		}
		
		int resolved = 0;
		int fallbackToEnglish = 0;
		StringBuilder missing = new StringBuilder();
		
		for(TranslateText text : TranslateText.values()) {
			
			String value = translateMap.get(text.getKey());
			
			if(value == null || value.isEmpty()) {
				if(englishMap != null) {
					value = englishMap.get(text.getKey());
				}
				
				if(value != null && !value.isEmpty()) {
					fallbackToEnglish++;
				}
			}
			
			if(value != null && !value.isEmpty()) {
				text.setText(value);
				resolved++;
			}else {
				// 走到这里说明两种语言都没有这个键，getText() 会退回调用键名推导出来的词
				text.setText(null);
				
				if(missing.length() < 400) {
					missing.append(text.getKey()).append(' ');
				}
			}
		}
		
		MixLogger.info("Language " + currentLanguage.getId() + ": " + resolved + "/" + TranslateText.values().length + " resolved (" + fallbackToEnglish + " from english), map=" + translateMap.size());
		
		if(missing.length() > 0) {
			MixLogger.warn("Language missing keys: " + missing.toString().trim());
		}
	}
}
