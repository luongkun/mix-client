package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventZoomFov extends Event {

	private float fov;
	
	public EventZoomFov(float fov) {
		this.fov = fov;
	}

	public float getFov() {
		return fov;
	}

	public void setFov(float fov) {
		this.fov = fov;
	}
}
