package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventRenderVisualizer extends Event {

	private float partialTicks;
	
	public EventRenderVisualizer(float partialTicks) {
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}
