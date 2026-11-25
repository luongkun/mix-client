package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;
import net.minecraft.client.renderer.chunk.RenderChunk;

public class EventPreRenderChunk extends Event {

	private RenderChunk renderChunk;
	
	public EventPreRenderChunk(RenderChunk renderChunk) {
		this.renderChunk = renderChunk;
	}

	public RenderChunk getRenderChunk() {
		return renderChunk;
	}
}