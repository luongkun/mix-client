package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.Arrays;
import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailPose;
import mix.client.mixui.integration.haunt.module.misc.MiscHooks;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PoseSnapshot;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Silhouette;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.item.ItemStack;

public final class AfterimageStyle extends TrailStyleBase {
   public final SliderSetting spacing = (SliderSetting)this.setting((new SliderSetting("afterimage.spacing", "Spacing", "How far apart the ghosts are left.", 0.4D, 3.0D, 1.0D, 0.1D)).unit(" blocks"));
   public final SliderSetting maxGhosts = (SliderSetting)this.setting(new SliderSetting("afterimage.max", "Max Ghosts per Player", "The most ghosts one player leaves at once.", 1.0D, 8.0D, 4.0D, 1.0D));
   public final BooleanSetting onlySprinting = (BooleanSetting)this.setting(new BooleanSetting("afterimage.sprintOnly", "Only While Sprinting", "Ghosts only while sprinting.", true));
   public final BooleanSetting drift = (BooleanSetting)this.setting(new BooleanSetting("afterimage.drift", "Drift", "Ghosts drift a little up and back as they fade.", false));
   public final ModeSetting<AfterimageStyle.Mode> mode = (ModeSetting)this.setting(new ModeSetting("afterimage.mode", "Mode", "How the ghosts look.", AfterimageStyle.Mode.class, AfterimageStyle.Mode.SOLID));
   private final Silhouette.Look look = new Silhouette.Look();
   private float fillMillis;
   private float rimMillis;

   public AfterimageStyle(TrailsModule module) {
      super(module, TrailStyle.AFTERIMAGE, spec().color("Ghost", -7362305).size(0.8D, 1.2D, 1.0D, 0.05D).sizeUnit("x").length(0.6D).anchor(TrailAnchor.WAIST, true).along(TrailStyleBase.ColorAlong.HEAD_TO_TAIL));
   }

   protected TrailStyleBase.Store newStore() {
      return new AfterimageStyle.Ghosts();
   }

   public boolean sprintOnly() {
      return this.onlySprinting.get();
   }

   public void render(PlayerTrail p, TrailFrame f) {
      if(!p.ghost && !p.reduced && (!this.onlySprinting.get() || (p.state & 2) != 0)) {
         int due = this.piecesDue(p, 1.0D / (double)this.spacing.getFloat(), 1);
         if(due != 0 && !this.module.piecesFull(f)) {
            AfterimageStyle.Ghosts g = (AfterimageStyle.Ghosts)this.store(f);
            int i = g.slot(p.entityId, this.maxGhosts.getInt());
            PoseSnapshot pose = g.pose[i];
            if(p.entity != null) {
               pose.capture(p.entity, f.partialTicks);
            } else {
               fromPose(p.pose, p.slim, pose);
            }

            pose.scale *= this.size.getFloat();
            g.born[i] = g.clock;
            g.life[i] = this.lengthNow(p.speed);
            g.alpha[i] = p.alpha;
            g.speed[i] = p.speed;
            double yaw = Math.toRadians((double)p.pose.bodyYaw);
            g.fx[i] = (float)(-Math.sin(yaw));
            g.fz[i] = (float)Math.cos(yaw);
            g.owner[i] = p.entityId;
            g.seed[i] = (int)(f.now * 1000.0D) + p.entityId * 31;
            g.armored[i] = wearsArmor(p);
            g.self[i] = p.self;
            g.tint[i] = this.tint;
            g.live[i] = true;
         }
      }

   }

   public String passMillisForTest() {
      return String.format(Locale.ROOT, "fills %.3f ms, rims %.3f ms", new Object[]{Float.valueOf(this.fillMillis), Float.valueOf(this.rimMillis)});
   }

   public String ghostsForTest(TrailFrame f, int owner) {
      AfterimageStyle.Ghosts g = (AfterimageStyle.Ghosts)f.store(this.type);
      if(g == null) {
         return "no ghosts";
      } else {
         int all = 0;
         int mine = 0;
         int armored = 0;

         for(int i = 0; i < 24; ++i) {
            if(g.live[i]) {
               ++all;
               if(g.owner[i] == owner) {
                  ++mine;
                  armored += g.armored[i]?1:0;
               }
            }
         }

         return all + " ghosts, " + mine + " of them yours, " + armored + " of those shaped over armor";
      }
   }

   private static boolean wearsArmor(PlayerTrail p) {
      if(p.entity == null) {
         return false;
      } else {
         for(int slot = 0; slot < 4; ++slot) {
            ItemStack s = p.entity.getCurrentArmor(slot);
            if(s != null && !MiscHooks.hideArmor(p.entity, slot + 1)) {
               return true;
            }
         }

         return false;
      }
   }

   private static void fromPose(TrailPose t, boolean slim, PoseSnapshot out) {
      out.mannequin(t.age, slim, t.y);
      out.x = t.x;
      out.z = t.z;
      out.bodyYaw = t.bodyYaw;
      out.limbSwing = t.limbSwing;
      out.limbSwingAmount = t.limbAmount;
      out.swingProgress = t.swing;
      out.sneaking = t.sneaking;
      out.heldItemRight = t.held;
   }

   protected void drawStore(TrailFrame f, TrailStyleBase.Store store) {
      AfterimageStyle.Ghosts g = (AfterimageStyle.Ghosts)store;
      Textures.ensureLoaded();
      int n = 0;

      for(int i = 0; i < 24; ++i) {
         if(g.live[i]) {
            PoseSnapshot ps = g.pose[i];
            double dx = ps.x - f.eyeX;
            double dy = ps.y + 0.9D - f.eyeY;
            double dz = ps.z - f.eyeZ;
            g.dist[i] = dx * dx + dy * dy + dz * dz;

            int j;
            for(j = n++; j > 0 && g.dist[g.order[j - 1]] < g.dist[i]; --j) {
               g.order[j] = g.order[j - 1];
            }

            g.order[j] = i;
         }
      }

      AfterimageStyle.Mode m = (AfterimageStyle.Mode)this.mode.get();
      float glowAmount = this.glow.getFloat() / 100.0F;
      Camera cam = Vfx.camera();
      long t0 = System.nanoTime();
      if(m != AfterimageStyle.Mode.OUTLINE) {
         if(m == AfterimageStyle.Mode.HOLOGRAM) {
            RenderState.additive();
         } else {
            RenderState.translucent(false);
         }

         for(int o = 0; o < n; ++o) {
            int ix = g.order[o];
            if(this.prepare(g, ix, f, m, cam, 0)) {
               Silhouette.drawPass(g.pose[ix], f.viewX, f.viewY, f.viewZ, this.look, 1);
            }
         }
      }

      RenderState.additive();
      long t1 = System.nanoTime();

      for(int ox = 0; ox < n; ++ox) {
         int ix = g.order[ox];

         for(int copy = 0; copy < (m == AfterimageStyle.Mode.GLITCH?3:1); ++copy) {
            if(this.prepare(g, ix, f, m, cam, copy)) {
               Silhouette.drawPass(g.pose[ix], f.viewX, f.viewY, f.viewZ, this.look, 2);
            }
         }
      }

      Vfx.flush();
      long t2 = System.nanoTime();
      this.fillMillis += ((float)(t1 - t0) / 1000000.0F - this.fillMillis) * 0.05F;
      this.rimMillis += ((float)(t2 - t1) / 1000000.0F - this.rimMillis) * 0.05F;
      if(m == AfterimageStyle.Mode.HOLOGRAM || m == AfterimageStyle.Mode.GLITCH) {
         for(int ox = 0; ox < n; ++ox) {
            int ix = g.order[ox];
            if(this.prepare(g, ix, f, m, cam, 0)) {
               if(m == AfterimageStyle.Mode.HOLOGRAM) {
                  this.scanLines(f, cam, g.pose[ix], this.look, this.look.alpha, this.look.color != null?0:this.look.staticColor, g.seed[ix], glowAmount);
               } else {
                  this.glitchBars(f, cam, g.pose[ix], this.look, this.look.alpha, this.look.color != null?0:this.look.staticColor, g.seed[ix], (int)Math.floor(f.now * 14.0D));
               }
            }
         }
      }

      RenderState.additive();
   }

   private boolean prepare(AfterimageStyle.Ghosts g, int i, TrailFrame f, AfterimageStyle.Mode m, Camera cam, int copy) {
      PoseSnapshot ps = g.pose[i];
      float age = g.clock - g.born[i];
      float u = Math.min(1.0F, age / Math.max(0.05F, g.life[i]));
      float a = this.opacity.getFloat() / 100.0F * g.alpha[i] * Easing.smoothstep(0.0F, 0.1F, age) * this.fade(u) * f.fog(ps.x, ps.y + 0.9D, ps.z);
      if(a <= 0.004F) {
         return false;
      } else {
         boolean up = this.colorAlong.get() == TrailStyleBase.ColorAlong.HEAD_TO_TAIL && g.tint[i] == 0 && !f.lit;
         int whole = g.tint[i] != 0?g.tint[i]:(up?0:this.colorAt(0.0F, u, g.speed[i], f.colorTime, 0.0F));
         if(f.lit) {
            whole = lit(whole, f, ps.x, ps.y + 1.0D, ps.z);
         }

         this.look.reset();
         this.look.color = up?this.color:null;
         this.look.staticColor = whole;
         this.look.levels = up?2:1;
         this.look.inflate = g.armored[i]?1.1F:0.35F;
         this.look.rimWidth = 0.9F;
         this.look.nearInner = f.nearInner;
         this.look.nearOuter = f.nearOuter * 1.4F;
         if(this.drift.get()) {
            this.look.shiftX = (double)(-g.fx[i]) * 0.35D * (double)u;
            this.look.shiftY = 0.25D * (double)u;
            this.look.shiftZ = (double)(-g.fz[i]) * 0.35D * (double)u;
         }

         switch(m) {
         case OUTLINE:
            this.look.fill = 0.0F;
            this.look.rim = 1.2F;
            this.look.alpha = a;
            break;
         case HOLOGRAM:
            this.look.fill = 0.32F;
            this.look.rim = 0.85F;
            this.look.alpha = a * (0.72F + 0.28F * (float)Math.sin(f.now * 41.0D + (double)g.seed[i]));
            break;
         case GLITCH:
            int slot = (int)Math.floor(f.now * 14.0D);
            float jump = Rng.hash(slot, g.seed[i]) > 0.7F?(Rng.hash(slot + 7, g.seed[i]) * 2.0F - 1.0F) * 0.12F:0.0F;
            double split = 0.05D + 0.04D * (double)Rng.hash(slot + 3, g.seed[i]);
            this.look.fill = 0.4F;
            this.look.rim = 0.9F;
            this.look.alpha = a;
            double shift = (double)jump;
            if(copy > 0) {
               this.look.fill = 0.0F;
               this.look.color = null;
               this.look.staticColor = copy == 1?-49810:-12720385;
               this.look.alpha = a * 0.6F;
               shift = copy == 1?(double)jump - split:(double)jump + split;
            }

            this.look.shiftX += cam.rightX * shift;
            this.look.shiftZ += cam.rightZ * shift;
            break;
         default:
            this.look.fill = 0.5F;
            this.look.rim = 1.0F;
            this.look.alpha = a;
         }

         return true;
      }
   }

   private void glitchBars(TrailFrame f, Camera cam, PoseSnapshot ps, Silhouette.Look l, float a, int whole, int seed, int slot) {
      if(Rng.hash(slot + 11, seed) >= 0.45F) {
         double h = 1.85D * (double)ps.scale / 0.9375D * (ps.sneaking?0.88D:1.0D);
         double cx = ps.x + l.shiftX - f.viewX;
         double cz = ps.z + l.shiftZ - f.viewZ;
         double base = ps.y + l.shiftY - f.viewY;
         double half = 0.42D * (double)ps.scale / 0.9375D;

         for(int k = 0; k < 3; ++k) {
            float t = 0.08F + 0.84F * Rng.hash(slot * 3 + k, seed);
            double shift = (double)(Rng.hash(slot * 5 + k, seed + 1) * 2.0F - 1.0F) * 0.22D;
            double y = base + (double)t * h;
            double x0 = cx + cam.rightX * (shift - half);
            double z0 = cz + cam.rightZ * (shift - half);
            double x1 = cx + cam.rightX * (shift + half);
            double z1 = cz + cam.rightZ * (shift + half);
            int c = ColorUtil.mulAlpha(whole != 0?whole:this.color.getColor(t, f.colorTime), a * 0.7F);
            Vfx.line(Textures.LINE_GLOW, x0, y, z0, x1, y, z1, 0.05D + 0.05D * (double)Rng.hash(slot + k, seed + 2), c, c);
         }
      }

   }

   private void scanLines(TrailFrame f, Camera cam, PoseSnapshot ps, Silhouette.Look l, float a, int whole, int seed, float glowAmount) {
      double h = 1.85D * (double)ps.scale / 0.9375D * (ps.sneaking?0.88D:1.0D);
      double cx = ps.x + l.shiftX - f.viewX;
      double cz = ps.z + l.shiftZ - f.viewZ;
      double base = ps.y + l.shiftY - f.viewY;
      double half = 0.38D * (double)ps.scale / 0.9375D;

      for(int k = 0; k < 6; ++k) {
         double t = (f.now * 0.6D + (double)k / 6.0D + (double)Rng.hash(seed, k) * 0.05D) % 1.0D;
         double y = base + t * h;
         int c = ColorUtil.mulAlpha(whole != 0?whole:this.color.getColor((float)t, f.colorTime), a * 0.55F * (0.5F + 0.5F * glowAmount));
         Vfx.line(Textures.LINE_GLOW, cx - cam.rightX * half, y, cz - cam.rightZ * half, cx + cam.rightX * half, y, cz + cam.rightZ * half, 0.035D, c, c);
      }

   }

   static final class Ghosts implements TrailStyleBase.Store {
      static final int CAP = 24;
      final PoseSnapshot[] pose = new PoseSnapshot[24];
      final float[] born = new float[24];
      final float[] life = new float[24];
      final float[] alpha = new float[24];
      final float[] fx = new float[24];
      final float[] fz = new float[24];
      final float[] speed = new float[24];
      final int[] owner = new int[24];
      final int[] seed = new int[24];
      final int[] tint = new int[24];
      final boolean[] armored = new boolean[24];
      final boolean[] self = new boolean[24];
      final boolean[] live = new boolean[24];
      final int[] order = new int[24];
      final double[] dist = new double[24];
      float clock;
      int count;

      Ghosts() {
         for(int i = 0; i < 24; ++i) {
            this.pose[i] = new PoseSnapshot();
         }

      }

      int slot(int ownerId, int max) {
         int mine = 0;
         int oldestMine = -1;
         int oldest = -1;
         int free = -1;

         for(int i = 0; i < 24; ++i) {
            if(!this.live[i]) {
               if(free < 0) {
                  free = i;
               }
            } else {
               if(this.owner[i] == ownerId) {
                  ++mine;
                  if(oldestMine < 0 || this.born[i] < this.born[oldestMine]) {
                     oldestMine = i;
                  }
               }

               if(oldest < 0 || this.born[i] < this.born[oldest]) {
                  oldest = i;
               }
            }
         }

         if(mine >= max && oldestMine >= 0) {
            return oldestMine;
         } else {
            return free >= 0?free:oldest;
         }
      }

      public void update(float dt) {
         this.clock += dt;
         this.count = 0;

         for(int i = 0; i < 24; ++i) {
            if(this.live[i] && this.clock - this.born[i] >= this.life[i]) {
               this.live[i] = false;
            }

            if(this.live[i]) {
               ++this.count;
            }
         }

      }

      public int size() {
         return this.count;
      }

      public void clear() {
         Arrays.fill(this.live, false);
         this.count = 0;
      }
   }

   public static enum Mode implements Displayable {
      SOLID("Solid", "A flat color with a brighter rim"),
      OUTLINE("Outline", "The rim only"),
      HOLOGRAM("Hologram", "Scan lines and a light flicker"),
      GLITCH("Glitch", "Split red and cyan copies that jump sideways");

      private final String name;
      private final String description;

      private Mode(String name, String description) {
         this.name = name;
         this.description = description;
      }

      public String displayName() {
         return this.name;
      }

      public String description() {
         return this.description;
      }
   }
}
