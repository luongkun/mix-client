package mix.client.mixui.management.event.impl;

import java.util.ArrayList;
import java.util.List;

import mix.client.mixui.management.event.Event;
import net.minecraft.client.shader.ShaderGroup;

public class EventShader extends Event {
	
	private List<ShaderGroup> groups = new ArrayList<ShaderGroup>();

	public List<ShaderGroup> getGroups() {
		return groups;
	}
}
