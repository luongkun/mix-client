package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventRenderTooltip extends Event {

	private float partialTicks;
	
	public EventRenderTooltip(float partialTicks) {
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}
