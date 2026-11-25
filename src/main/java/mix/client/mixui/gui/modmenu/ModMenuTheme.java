package mix.client.mixui.gui.modmenu;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.color.Theme;
import mix.client.mixui.management.color.palette.ColorPalette;
import mix.client.mixui.management.color.palette.ColorType;

/**
 * Small Material 3 inspired surface palette used by the rebuilt 8.0 style menu.
 * The rest of the client still uses the two legacy palette colours, this class
 * only derives the extra tones the new menu needs.
 */
public class ModMenuTheme {

	public static Color surface() {
		return palette().getBackgroundColor(ColorType.DARK);
	}

	public static Color surfaceContainer() {
		return palette().getBackgroundColor(ColorType.NORMAL);
	}

	public static Color surfaceContainerLow() {
		return mix(surfaceContainer(), surface(), 0.55F);
	}

	public static Color surfaceContainerHigh() {
		return mix(surfaceContainer(), onSurface(), isDark() ? 0.10F : 0.06F);
	}

	public static Color surfaceContainerHighest() {
		return mix(surfaceContainer(), onSurface(), isDark() ? 0.15F : 0.10F);
	}

	public static Color onSurface() {
		return palette().getFontColor(ColorType.DARK);
	}

	public static Color onSurfaceVariant() {
		return palette().getFontColor(ColorType.NORMAL);
	}

	public static Color outline() {
		return mix(onSurfaceVariant(), surface(), 0.62F);
	}

	public static Color outlineVariant() {
		return mix(onSurfaceVariant(), surface(), 0.82F);
	}

	public static Color primary() {
		return accent().getColor1();
	}

	public static Color secondary() {
		return accent().getColor2();
	}

	public static Color primaryContainer() {
		return applyAlpha(accent().getColor1(), isDark() ? 46 : 34);
	}

	public static Color onPrimaryContainer() {
		return isDark() ? mix(accent().getColor1(), Color.WHITE, 0.45F) : mix(accent().getColor2(), Color.BLACK, 0.25F);
	}

	public static Color error() {
		return new Color(236, 72, 79);
	}

	public static Color success() {
		return new Color(76, 175, 111);
	}

	public static boolean isDark() {
		Theme theme = theme();
		return theme == Theme.DARK || theme == Theme.DARK_BLUE || theme == Theme.MIDNIGHT || theme == Theme.DARK_PURPLE;
	}

	public static Color applyAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
	}

	public static Color mix(Color a, Color b, float amount) {
		amount = Math.max(0.0F, Math.min(1.0F, amount));
		return new Color(
				(int) (a.getRed() + (b.getRed() - a.getRed()) * amount),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount),
				(int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * amount));
	}

	private static ColorPalette palette() {
		ColorManager manager = Mix.getInstance().getColorManager();
		return manager.getPalette();
	}

	private static AccentColor accent() {
		return Mix.getInstance().getColorManager().getCurrentColor();
	}

	private static Theme theme() {
		return Mix.getInstance().getColorManager().getTheme();
	}
}
