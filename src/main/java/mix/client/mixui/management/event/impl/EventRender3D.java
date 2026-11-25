package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventRender3D extends Event {

	private float partialTicks;
	
	public EventRender3D(float partialTicks) {
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}