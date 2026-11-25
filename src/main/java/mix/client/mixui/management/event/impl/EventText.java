package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventText extends Event {

	private String text;
	private String outputText;
	
	public EventText(String text) {
		this.text = text;
		this.outputText = text;
	}
	
	public String getText() {
		return this.text;
	}
	
	public String getOutputText() {
		return this.outputText;
	}
	
	public String replace(String src, String target) {
		this.outputText = this.outputText.replace(src, target);
		return this.outputText;
	}
	
	/**
	 * 和 replace 一样，但是搜索时跳过 § 颜色与格式码。
	 * 服务器发来的聊天/计分板文本常常把名字切成 "§7Na§fm§7e" 这种样子，
	 * 直接用 String.replace 是匹配不到的，所以这里按"可见字符"来比对，
	 * 命中后整段（含中间的格式码）一起换成 target。
	 */
	public String replaceIgnoringFormatting(String src, String target) {
		
		if(src == null || src.isEmpty() || target == null) {
			return this.outputText;
		}
		
		String current = this.outputText;
		
		if(current == null || current.isEmpty()) {
			return this.outputText;
		}
		
		StringBuilder result = new StringBuilder(current.length());
		
		int i = 0;
		
		while(i < current.length()) {
			
			if(matchesAt(current, i, src)) {
				result.append(target);
				i += consumedBy(current, i, src.length());
			}else {
				result.append(current.charAt(i));
				i++;
			}
		}
		
		this.outputText = result.toString();
		
		return this.outputText;
	}
	
	// current 从 index 开始（跳过 § 码后）是否等于 src
	private boolean matchesAt(String current, int index, String src) {
		
		int i = index;
		
		for(int s = 0; s < src.length(); s++) {
			
			i = skipFormatting(current, i);
			
			if(i >= current.length()) {
				return false;
			}
			
			if(current.charAt(i) != src.charAt(s)) {
				return false;
			}
			
			i++;
		}
		
		return true;
	}
	
	// 命中后要吃掉多少个原串字符（含中间夹的 § 码）
	private int consumedBy(String current, int index, int visibleLength) {
		
		int i = index;
		
		for(int s = 0; s < visibleLength; s++) {
			i = skipFormatting(current, i) + 1;
		}
		
		return i - index;
	}
	
	private int skipFormatting(String current, int index) {
		
		int i = index;
		
		while(i + 1 < current.length() && current.charAt(i) == '§') {
			i += 2;
		}
		
		return i;
	}
}
