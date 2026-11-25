package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventRenderSelectedItem extends Event {

	private int color;
	
	public EventRenderSelectedItem(int color) {
		this.color = color;
	}
	
	public int getColor() {
		return color;
	}
}