package mix.client.mixui.integration.haunt.module.watermark;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.module.watermark.SvgPath;

public final class HauntIconBridge {
   public static Shape discord(int size) {
      try {
         SvgPath svg = SvgPath.resource("/assets/furiahsvisuals/discord/Discord-Symbol-Blurple.svg");
         double[] box = svg.viewBox;
         double scale = Math.min((double)size * 0.92D / box[2], (double)size * 0.92D / box[3]);
         AffineTransform at = new AffineTransform();
         at.translate((double)size / 2.0D - box[2] * scale / 2.0D, (double)size / 2.0D - box[3] * scale / 2.0D);
         at.scale(scale, scale);
         at.translate(-box[0], -box[1]);
         return at.createTransformedShape(svg.path);
      } catch (RuntimeException | IOException var6) {
         FuriahsVisuals.LOG.warn("Header: the Discord symbol could not be read", var6);
         return null;
      }
   }
}
