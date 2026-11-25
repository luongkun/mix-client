package mix.client.mixui.visuals.furiah.hook;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Hooks {
    public static final Set<String> INSTALLED = new HashSet<String>(Arrays.asList(
            "spawnHead", "spawnTail", "emitHead", "emitterSpawn", "drawParticle",
            "drawStart", "drawEnd", "bypassSetting", "critPrediction",
            "sharpPrediction", "critPacket"));

    public static void on(String... hooks) {
        INSTALLED.addAll(Arrays.asList(hooks));
    }

    public static void off(String... hooks) {
        for (String hook : hooks) {
            INSTALLED.remove(hook);
        }
    }

    public static Field field(Class<?> owner, String mcp, String srg) {
        for (String name : Srg.names(owner, false, mcp, srg)) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new RuntimeException("Missing field " + owner.getName() + "." + mcp);
    }
}
