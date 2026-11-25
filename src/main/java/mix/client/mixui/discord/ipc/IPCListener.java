package mix.client.mixui.discord.ipc;

import com.google.gson.JsonObject;

import mix.client.mixui.discord.ipc.entities.Packet;
import mix.client.mixui.discord.ipc.entities.User;

public interface IPCListener {
	
    default void onPacketSent(IPCClient client, Packet packet) {}
    default void onPacketReceived(IPCClient client, Packet packet) {}
    default void onActivityJoin(IPCClient client, String secret) {}
    default void onActivitySpectate(IPCClient client, String secret) {}
    default void onActivityJoinRequest(IPCClient client, String secret, User user) {}
    default void onReady(IPCClient client) {}
    default void onClose(IPCClient client, JsonObject json) {}
    default void onDisconnect(IPCClient client, Throwable t) {}
}
