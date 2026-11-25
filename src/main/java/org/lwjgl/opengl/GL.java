package org.lwjgl.opengl;

import java.nio.ByteBuffer;

import org.lwjgl.system.FunctionProvider;

/**
 * Compatibility class for the lwjgl3 build bundled with the Mix UI.
 *
 * <p>The bundled {@code lwjgl-mix.jar} carries the parts of lwjgl3 the UI uses
 * ({@code system}, {@code nanovg}, {@code stb}, {@code util}) but not
 * {@code org.lwjgl.opengl}. {@code NanoVGGLConfig} resolves its function
 * provider reflectively through this class — first via
 * {@code GL.getCapabilities()} — so without it creating the NanoVG context
 * fails with {@code NoSuchMethodException} / {@code ClassNotFoundException}.
 *
 * <p>The jar also carries lwjgl2's {@code org/lwjgl/opengl} classes (see the
 * note in {@code build.gradle}) so that the package is defined unsealed; lwjgl2
 * itself seals it, and a sealed package would reject this class. lwjgl2 owns the
 * current GL context, and its address lookup is what the provider delegates to.
 */
public final class GL {

    private GL() {
    }

    /**
     * The entry point {@code NanoVGGLConfig} looks up. lwjgl3 returns a
     * {@code GLCapabilities}, which is itself a {@code FunctionProvider}.
     */
    public static FunctionProvider getCapabilities() {
        return new FunctionProvider() {
            @Override
            public long getFunctionAddress(ByteBuffer functionName) {
                return 0L;
            }

            @Override
            public long getFunctionAddress(CharSequence functionName) {
                if (functionName == null) {
                    return 0L;
                }
                return GLContext.getFunctionAddress(functionName.toString());
            }
        };
    }

    /** Alias kept for lwjgl3 call sites that use the older name. */
    public static FunctionProvider getFunctionProvider() {
        return getCapabilities();
    }
}