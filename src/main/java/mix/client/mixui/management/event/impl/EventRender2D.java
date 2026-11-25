package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventRender2D extends Event {
	
	private static int count;
	
	private float partialTicks;
	
	public EventRender2D(float partialTicks) {
		this.partialTicks = partialTicks;
		count++;
	}
	
	public static int getCount() {
		return count;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}
