package mix.client.mixui.management.nanovg.font;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.util.ResourceLocation;

public class Fonts {
	
	private static final String PATH = "mixclient/fonts/";
	
	public static final Font REGULAR = new Font("regular", new ResourceLocation(PATH + "MiSans-Regular.ttf"));
	public static final Font MEDIUM = new Font("medium", new ResourceLocation(PATH + "MiSans-Medium.ttf"));
	public static final Font DEMIBOLD = new Font("demi-bold", new ResourceLocation(PATH + "MiSans-Demibold.ttf"));
	public static final Font ICON = new Font("icon", new ResourceLocation(PATH + "Icon.ttf"));

	/**
	 * Material Symbols Rounded —— MixClient v8.0 用的那套图标字体。
	 *
	 * <p>7.1.2 自带的 Icon.ttf 只有 200 来个自制单字符字形，图标全靠 {@code Icon.XXX}
	 * 里那串 "A"/"b"/"g" 之类的编码；v8.0 换成了 Google 的 Material Symbols Rounded
	 * （见 {@link MaterialIcons} 的码点表，3394 个字形），图标语义一眼可辨。
	 * 这里把两套并存：老代码继续用 {@link #ICON}，模组卡片改用 {@link #MATERIAL}。</p>
	 */
	public static final Font MATERIAL = new Font("material", new ResourceLocation(PATH + "MaterialSymbolsRounded.ttf"));

	public static ArrayList<Font> getFonts() {
		return new ArrayList<Font>(Arrays.asList(MEDIUM, DEMIBOLD, REGULAR, ICON, MATERIAL));
	}
}
