package mix.client.mixui.gui.modmenu;

import java.awt.Color;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

import mix.client.mixui.management.nanovg.NanoVGManager;

/**
 * Ambient ring for the page header.
 *
 * <p>It never draws a container: every layer here is a stroke that hugs a shape
 * the caller already owns, so the page title and the search capsule keep their
 * own silhouette and nothing new wraps them. That is exactly the difference
 * between "adding a box around the header" (rejected) and "adding motion to the
 * header" (requested).</p>
 *
 * <p>Alpha changes on every frame, so the strokes are issued straight through
 * NanoVG with a reused {@link NVGColor}. Going through
 * {@code NanoVGManager#getColor} would create and permanently cache one colour
 * per alpha step, which grows without bound.</p>
 */
public final class HeaderGlow {

	private static final NVGColor RING = NVGColor.create();

	// Concentric strokes that define the halo.
	private static final int RING_LAYERS = 3;
	private static final float RING_STEP = 1.7F;
	private static final float RING_STROKE = 1.4F;
	private static final float RING_ALPHA = 96.0F;

	// Rings that travel outward and fade, phase shifted from each other.
	private static final int PULSE_COUNT = 2;
	private static final float PULSE_STEP = 7.0F;
	private static final float PULSE_ALPHA = 110.0F;
	private static final long PULSE_MS = 2600L;

	private static final long BREATH_MS = 2400L;

	private HeaderGlow() {
	}

	/** Slow breathing so an idle header is never perfectly static. */
	public static float breath() {
		double phase = (System.currentTimeMillis() % BREATH_MS) / (double) BREATH_MS;
		return (float) (0.5D - 0.5D * Math.cos(phase * Math.PI * 2.0D));
	}

	/**
	 * Draws the halo around an existing rounded shape. The bounds passed in are
	 * the caller's own bounds - this method only ever strokes outside them.
	 */
	public static void draw(NanoVGManager nvg, float x, float y, float width, float height, float radius, float intensity, Color color) {

		if (intensity <= 0.01F || width <= 1.0F || height <= 1.0F) {
			return;
		}

		long ctx = nvg.getContext();

		NanoVG.nvgSave(ctx);

		for (int layer = RING_LAYERS; layer >= 1; layer--) {

			float grow = layer * RING_STEP;
			float falloff = 1.0F - (layer - 1) / (float) RING_LAYERS;
			float alpha = RING_ALPHA * intensity * falloff * falloff;

			if (alpha < 1.0F) {
				continue;
			}

			setColor(RING, color, alpha);

			NanoVG.nvgBeginPath(ctx);
			NanoVG.nvgRoundedRect(ctx, x - grow, y - grow, width + grow * 2.0F, height + grow * 2.0F, radius + grow);
			NanoVG.nvgStrokeWidth(ctx, RING_STROKE);
			NanoVG.nvgStrokeColor(ctx, RING);
			NanoVG.nvgStroke(ctx);
		}

		long now = System.currentTimeMillis();

		for (int i = 0; i < PULSE_COUNT; i++) {

			float raw = (((now % PULSE_MS) / (float) PULSE_MS) + (i / (float) PULSE_COUNT)) % 1.0F;
			float grow = 1.0F + raw * PULSE_STEP;
			float fade = 1.0F - raw;
			float alpha = PULSE_ALPHA * intensity * fade * fade;

			if (alpha < 1.0F) {
				continue;
			}

			setColor(RING, color, alpha);

			NanoVG.nvgBeginPath(ctx);
			NanoVG.nvgRoundedRect(ctx, x - grow, y - grow, width + grow * 2.0F, height + grow * 2.0F, radius + grow);
			NanoVG.nvgStrokeWidth(ctx, 1.1F);
			NanoVG.nvgStrokeColor(ctx, RING);
			NanoVG.nvgStroke(ctx);
		}

		NanoVG.nvgRestore(ctx);
	}

	private static void setColor(NVGColor out, Color rgb, float alpha) {
		NanoVG.nvgRGBA((byte) rgb.getRed(), (byte) rgb.getGreen(), (byte) rgb.getBlue(),
				(byte) Math.max(0, Math.min(255, Math.round(alpha))), out);
	}
}
