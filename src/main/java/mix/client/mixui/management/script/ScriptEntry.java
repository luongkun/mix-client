package mix.client.mixui.management.script;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ScriptEntry {

	private String name, icon;
	private boolean enabled, background;
	private float x, y, scale;
	private List<String> lines;
	private List<Color> colors;

	public ScriptEntry(String name, String icon, boolean enabled, float x, float y, float scale, boolean background, List<String> lines, List<Color> colors) {

		this.name = name;
		this.icon = icon;
		this.enabled = enabled;
		this.x = x;
		this.y = y;
		this.scale = scale;
		this.background = background;
		this.lines = lines == null ? new ArrayList<String>() : lines;
		this.colors = colors == null ? new ArrayList<Color>() : colors;

		if(this.scale <= 0F || this.scale > 5F) {
			this.scale = 1.0F;
		}
	}

	public Color getColor(int index) {

		if(colors.isEmpty()) {
			return Color.WHITE;
		}

		return colors.get(Math.abs(index) % colors.size());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isBackground() {
		return background;
	}

	public void setBackground(boolean background) {
		this.background = background;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public List<String> getLines() {
		return lines;
	}

	public void setLines(List<String> lines) {
		this.lines = lines;
	}

	public List<Color> getColors() {
		return colors;
	}

	public void setColors(List<Color> colors) {
		this.colors = colors;
	}
}
