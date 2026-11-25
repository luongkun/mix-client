package mix.client.mixui.discord.ipc.entities.pipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import mix.client.mixui.discord.ipc.IPCClient;
import mix.client.mixui.discord.ipc.entities.Callback;
import mix.client.mixui.discord.ipc.entities.Packet;
import mix.client.mixui.discord.ipc.entities.serialize.PacketDeserializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class WindowsPipe extends Pipe {

    private static final Logger LOGGER = LogManager.getLogger(WindowsPipe.class);

    private final RandomAccessFile file;

    WindowsPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) {
        super(ipcClient, callbacks);
        try {
            this.file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    /** Longest we wait for Discord to write a packet before giving up. */
    private static final long READ_TIMEOUT_MILLIS = 30_000L;

    @Override
    public Packet read() throws IOException {

        /* Discord's IPC endpoint is a plain file, so there is no blocking read to
   wait on — the original spun on a 50 ms sleep with no exit, which burned CPU
   for as long as the pipe stayed empty. Poll more slowly and give up after
   READ_TIMEOUT_MILLIS so the reader thread can end. */
        long deadline = System.currentTimeMillis() + READ_TIMEOUT_MILLIS;
        while (file.length() == 0 && status == PipeStatus.CONNECTED) {
            if (System.currentTimeMillis() > deadline) {
                throw new IOException("Timed out waiting for a Discord IPC packet");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for a Discord IPC packet");
            }
        }

        if(status==PipeStatus.DISCONNECTED) {
            throw new IOException("Disconnected!");
        }

        if(status==PipeStatus.CLOSED) {
            return new Packet(Packet.OpCode.CLOSE, null);
        }

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(file.readInt())];
        int len = Integer.reverseBytes(file.readInt());
        byte[] d = new byte[len];

        file.readFully(d);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Packet.class, new PacketDeserializer(op))
                .create();
        JsonObject jsonObject = gson.fromJson(new String(d), JsonObject.class);
        Packet p = gson.fromJson(jsonObject, Packet.class);

        LOGGER.debug(String.format("Received packet: %s", p.toString()));
        
        if(listener != null) {
            listener.onPacketReceived(ipcClient, p);
        }
        
        return p;
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("Closing IPC pipe...");
        send(Packet.OpCode.CLOSE, new JsonObject(), null);
        status = PipeStatus.CLOSED;
        file.close();
    }
}
