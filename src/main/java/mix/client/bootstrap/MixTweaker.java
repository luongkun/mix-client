package mix.client.bootstrap;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import mix.client.mixui.injection.transformer.LwjglTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class MixTweaker implements ITweaker {

    public static boolean hasOptifine;

    private final boolean optifineMode = Boolean.getBoolean("mixclient.optifine");
    private final boolean forgeMode = Boolean.getBoolean("mixclient.forge");
    private final List<String> gameArgs = new ArrayList<>();

    public MixTweaker() {
        if (optifineMode || forgeMode) {
            return;
        }
        String encoded = System.getProperty("mixclient.gameArgs");
        if (encoded == null || encoded.isEmpty()) {
            return;
        }
        try {
            String joined = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            for (String arg : joined.split("\u0000", -1)) {
                if (!arg.isEmpty()) {
                    gameArgs.add(arg);
                }
            }
        } catch (IllegalArgumentException exception) {
            System.err.println("[MixClient] Could not decode launch arguments: " + exception.getMessage());
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        try {
            Class.forName("optifine.Patcher");
            hasOptifine = true;
        } catch (ClassNotFoundException ignored) {
        }
        if (!optifineMode && !forgeMode && gameArgs.isEmpty()) {
            gameArgs.addAll(args);
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.registerTransformer(LwjglTransformer.class.getName());
        MixinBootstrap.init();

        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        if (optifineMode) {
            env.setObfuscationContext("notch");
        } else if (forgeMode) {
            env.setObfuscationContext("searge");
        }
        Mixins.addConfiguration((optifineMode || forgeMode)
                ? "mixins.mixui-forge.json" : "mixins.mixui.json");
        env.setSide(MixinEnvironment.Side.CLIENT);
        MixinBootstrap.getPlatform().inject();
        unlockLwjgl();
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return optifineMode || forgeMode ? new String[0] : gameArgs.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private void unlockLwjgl() {
        try {
            Field transformerExceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            transformerExceptions.setAccessible(true);
            Object value = transformerExceptions.get(Launch.classLoader);
            ((Set<String>) value).remove("org.lwjgl.");
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }
}
