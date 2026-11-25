package mix.client.mixui.integration.haunt.haunt;

import java.util.Arrays;
import java.util.Locale;
import mix.client.mixui.integration.haunt.core.GameClock;
import mix.client.mixui.integration.haunt.core.HitConfirmer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;

public final class HitReg implements HitConfirmer.Listener {
   public static final HitReg INSTANCE = new HitReg();
   private static final int SAMPLES = 64;
   private final float[] pings = new float[64];
   private int pingCount;
   private int pingNext;
   private final float[] confirms = new float[64];
   private int confirmCount;
   private int confirmNext;
   private long attacks;
   private long confirmed;
   private int ticks;

   public static void syncLook(Minecraft mc) {
      EntityPlayerSP p = mc == null?null:mc.thePlayer;
      if(p != null) {
         p.rotationYawHead = p.rotationYaw;
         p.prevRotationYawHead = p.prevRotationYaw;
      }

   }

   public void onAttack() {
      ++this.attacks;
   }

   public void onConfirmedHit(HitConfirmer.Attack attack) {
      ++this.confirmed;
      float ms = (float)(GameClock.nanos() - attack.nanos) / 1000000.0F;
      this.confirms[this.confirmNext] = ms;
      this.confirmNext = (this.confirmNext + 1) % 64;
      this.confirmCount = Math.min(64, this.confirmCount + 1);
   }

   public void tick(Minecraft mc) {
      if(++this.ticks % 20 == 0 && mc.getNetHandler() != null && mc.thePlayer != null) {
         NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
         if(info != null && info.getResponseTime() > 0) {
            this.pings[this.pingNext] = (float)info.getResponseTime();
            this.pingNext = (this.pingNext + 1) % 64;
            this.pingCount = Math.min(64, this.pingCount + 1);
         }
      }

   }

   public void reset() {
      this.pingCount = this.pingNext = this.confirmCount = this.confirmNext = 0;
      this.attacks = this.confirmed = 0L;
   }

   private static float mean(float[] a, int n) {
      float s = 0.0F;

      for(int i = 0; i < n; ++i) {
         s += a[i];
      }

      return n == 0?0.0F:s / (float)n;
   }

   private static float spread(float[] a, int n) {
      float m = mean(a, n);
      float s = 0.0F;

      for(int i = 0; i < n; ++i) {
         s += (a[i] - m) * (a[i] - m);
      }

      return n < 2?0.0F:(float)Math.sqrt((double)(s / (float)(n - 1)));
   }

   private static float median(float[] a, int n) {
      if(n == 0) {
         return 0.0F;
      } else {
         float[] c = Arrays.copyOf(a, n);
         Arrays.sort(c);
         return n % 2 == 1?c[n / 2]:(c[n / 2 - 1] + c[n / 2]) * 0.5F;
      }
   }

   public String[] report() {
      if(this.pingCount == 0 && this.attacks == 0L) {
         return new String[]{"No measurements yet: play on a server for a moment and hit something."};
      } else {
         String ping = this.pingCount == 0?"Ping: the server does not report it.":String.format(Locale.ROOT, "Ping: %.0f ms on average, varying by %.0f ms (%d samples).", new Object[]{Float.valueOf(mean(this.pings, this.pingCount)), Float.valueOf(spread(this.pings, this.pingCount)), Integer.valueOf(this.pingCount)});
         String hits = this.attacks == 0L?"Hits: no attacks yet.":String.format(Locale.ROOT, "Hits: %d of %d attacks were shown by the server as a hit (%.0f%%)%s.", new Object[]{Long.valueOf(this.confirmed), Long.valueOf(this.attacks), Double.valueOf(100.0D * (double)this.confirmed / (double)this.attacks), this.confirmCount == 0?"":String.format(Locale.ROOT, ", %.0f ms from your click to the hit (median)", new Object[]{Float.valueOf(median(this.confirms, this.confirmCount))})});
         return new String[]{ping, hits, "Mouse-look fix: on (your clicks use the direction you see). Nothing is sent to the server by it."};
      }
   }
}
