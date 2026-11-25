package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.kit.Smoke;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.fx.trail.TrailStrip;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.Minecraft;

public final class WraithStyle extends TrailStyleBase {
   private static final int MAX_TENDRILS = 5;
   private static final double BASE_WIDTH = 0.3D;
   private static final double SPREAD = 0.46D;
   private static final int CURL_STEP = 4;
   private static final double WISP_LENGTH = 1.6D;
   private static final double WISP_SPEED = 0.45D;
   private static final float TENDRIL_WIND = 0.35F;
   private static final float EMBER_WIND = 0.6F;
   private static final double WIND_AGE = 1.5D;
   public final ColorSetting embersColor = this.signatureColor("embersColor", "Embers", "The glowing cracks and the embers drifting up.", -45778, -15811);
   public final ColorSetting edgeColor = this.signatureColor("edgeColor", "Edge Light", "The faint light round the smoke\'s edges.", -4124129, -38339);
   public final SliderSetting tendrils;
   public final SliderSetting width;
   public final SliderSetting curl;
   public final SliderSetting embers;
   public final ModeSetting<Smoke.EdgeLight> edgeLight;
   private final Rng rng = new Rng();
   private final Map<PlayerTrail, WraithStyle.Tendrils[]> tendrilsOf = new IdentityHashMap();
   private final double[] flow = new double[3];
   private static final float[] U = new float[1024];
   private static final double[] FLOW_X = new double[258];
   private static final double[] FLOW_Y = new double[258];
   private static final double[] FLOW_Z = new double[258];

   public WraithStyle(TrailsModule module) {
      super(module, TrailStyle.WRAITH, spec().color("Smoke", -15593964).signature(-12966848).length(1.4D).anchor(TrailAnchor.BACK, false).animated().fade(TrailStyleBase.FadeCurve.LATE));
      this.doesNotApply(new Setting[]{this.size, this.taper});
      this.tendrils = (SliderSetting)this.setting(new SliderSetting("wraith.tendrils", "Tendrils", "How many smoke tendrils stream from your back.", 2.0D, 5.0D, 3.0D, 1.0D));
      this.width = (SliderSetting)this.setting((new SliderSetting("wraith.width", "Width", "How wide the tendrils are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%"));
      this.curl = (SliderSetting)this.setting((new SliderSetting("wraith.curl", "Curl", "How much the smoke curls toward the tail.", 0.0D, 100.0D, 50.0D, 1.0D)).unit("%"));
      this.embers = (SliderSetting)this.setting((new SliderSetting("wraith.embers", "Embers", "How strongly the cracks glow and how many embers drift up.", 0.0D, 100.0D, 40.0D, 1.0D)).unit("%"));
      this.edgeLight = (ModeSetting)this.setting((new ModeSetting("wraith.edgeLight", "Edge Light", "A faint light round the smoke: where it is dark (Night Visibility), always, or never.", Smoke.EdgeLight.class, Smoke.EdgeLight.AUTO)).style(ModeSetting.Style.CHIPS));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.width);
   }

   public int passes() {
      return FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   public void update(TrailFrame f) {
      super.update(f);
      if(!this.tendrilsOf.isEmpty()) {
         Iterator<Entry<PlayerTrail, WraithStyle.Tendrils[]>> it = this.tendrilsOf.entrySet().iterator();

         while(it.hasNext()) {
            WraithStyle.Tendrils[] t = (WraithStyle.Tendrils[])((Entry)it.next()).getValue();
            double seen = -1.0D;
            TrailFrame scene = null;

            for(WraithStyle.Tendrils one : t) {
               if(one != null) {
                  seen = Math.max(seen, one.seen);
                  scene = one.scene;
               }
            }

            if(scene == f && f.now - seen > 2.0D) {
               it.remove();
            }
         }
      }

   }

   private float edgeAmount() {
      switch((Smoke.EdgeLight)this.edgeLight.get()) {
      case ON:
         return 1.0F;
      case OFF:
         return 0.0F;
      default:
         return this.night;
      }
   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      TrailShape s = p.shape;
      if(s != null && s.runs != 0) {
         WraithStyle.Tendrils[] slots = (WraithStyle.Tendrils[])this.tendrilsOf.get(p);
         if(slots == null) {
            slots = new WraithStyle.Tendrils[4];
            this.tendrilsOf.put(p, slots);
         }

         int slot;
         if(slots[slot = p.side * 2 + (p.ghost?1:0)] == null) {
            slots[slot] = new WraithStyle.Tendrils();
         }

         WraithStyle.Tendrils t = slots[slot];
         t.seen = f.now;
         t.scene = f;
         if(pass == FxPass.TRANSLUCENT) {
            for(int k = 0; k < s.runs; ++k) {
               if(this.layout(p, f, k, t)) {
                  this.smoke(f, t);
               }
            }

            if(!p.ghost && !p.reduced && this.embers.getFloat() > 0.0F && this.spawnPass(pass)) {
               this.emit(p, f);
            }
         } else if(t.stamp == f.now) {
            this.light(p, f, t);
         }
      }

   }

   private boolean layout(PlayerTrail p, TrailFrame f, int k, WraithStyle.Tendrils t) {
      int n = this.fill(p, f, k, 1.0F, 0.0F, 0.0F);
      if(n < 2) {
         return false;
      } else {
         TrailShape s = p.shape;
         int from = s.runStart[k];
         float total = Math.max(0.001F, s.length);
         int count = p.reduced?2:this.tendrils.getInt();
         t.ensure(n);
         t.count = count;
         t.points = n;
         t.stamp = f.now;
         boolean mine = p.self && !f.preview && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
         float w0 = (float)(0.3D * (double)this.width.getFloat() / 100.0D) * (p.reduced?1.35F:1.0F);
         float curlAmt = this.curl.getFloat() / 100.0F;
         double time = f.now * (this.animSpeed == null?1.0D:(double)this.animSpeed.getFloat() / 100.0D);
         float[] air = this.breeze(f, p.headX, p.headY, p.headZ, 0.35F);
         double windX = (double)air[0];
         double windY = (double)air[1];
         double windZ = (double)air[2];

         for(int j = 0; j < n; ++j) {
            t.u[j] = Math.min(1.0F, s.along[from + j] / total);
            t.c[j] = Vfx.C[j] | -16777216;
         }

         t.length = total;
         int samples = (n - 1) / 4 + 2;

         for(int c = 0; c < count; ++c) {
            double across = count == 1?0.0D:((double)c / (double)(count - 1) - 0.5D) * 0.46D;
            double lift = (0.5D - Math.abs((double)c / (double)Math.max(1, count - 1) - 0.5D)) * -0.08D + Math.abs(across) * 0.25D;
            int base = c * n;
            if(curlAmt > 0.0F) {
               for(int i = 0; i < samples; ++i) {
                  int j = Math.min(n - 1, i * 4);
                  Smoke.curl(Vfx.X[j] + f.viewX + (double)c * 7.3D, Vfx.Y[j] + f.viewY, Vfx.Z[j] + f.viewZ - (double)c * 4.1D, time, 0.7D, this.flow);
                  FLOW_X[i] = this.flow[0];
                  FLOW_Y[i] = this.flow[1];
                  FLOW_Z[i] = this.flow[2];
               }
            }

            for(int j = 0; j < n; ++j) {
               float u = t.u[j];
               int ja = Math.max(0, j - 1);
               int jb = Math.min(n - 1, j + 1);
               double tx = Vfx.X[jb] - Vfx.X[ja];
               double tz;
               double tl = Math.sqrt(tx * tx + (tz = Vfx.Z[jb] - Vfx.Z[ja]) * tz);
               double sx = tl < 1.0E-6D?1.0D:-tz / tl;
               double sz = tl < 1.0E-6D?0.0D:tx / tl;
               double spread = across * (1.0D + 1.2D * (double)u);
               double ox = 0.0D;
               double oy = 0.0D;
               double oz = 0.0D;
               if(curlAmt > 0.0F) {
                  int i0 = j / 4;
                  int i1 = Math.min(samples - 1, i0 + 1);
                  int j0 = Math.min(n - 1, i0 * 4);
                  int j1 = Math.min(n - 1, i1 * 4);
                  double b = j1 > j0?(double)(j - j0) / (double)(j1 - j0):0.0D;
                  double amp = (double)curlAmt * 0.85D * Math.pow((double)u, 1.5D);
                  ox = (FLOW_X[i0] + (FLOW_X[i1] - FLOW_X[i0]) * b) * amp;
                  oy = (FLOW_Y[i0] + (FLOW_Y[i1] - FLOW_Y[i0]) * b) * amp * 0.6D;
                  oz = (FLOW_Z[i0] + (FLOW_Z[i1] - FLOW_Z[i0]) * b) * amp;
               }

               double drift = Math.min(1.5D, (double)s.age[from + j]);
               double x = Vfx.X[j] + sx * spread + ox + windX * drift;
               double y = Vfx.Y[j] + lift * (1.0D - (double)u) + oy + windY * drift;
               double z = Vfx.Z[j] + sz * spread + oz + windZ * drift;
               t.x[base + j] = (float)x;
               t.y[base + j] = (float)y;
               t.z[base + j] = (float)z;
               t.w[base + j] = w0 * (0.8F + 1.9F * u) * (float)Math.sqrt((double)Math.max(0.25F, 1.0F + this.speedBoost(s.speed[from + j]))) * p.widthScale;
               float torn = 1.0F - Easing.smoothstep(0.5F, 1.0F, u) * 0.7F * Easing.smoothstep(0.35F, 0.75F, (float)noise((double)u * 9.0D - time * 0.7D + (double)c * 3.1D));
               float a = POINT_ALPHA[j] * torn;
               if(mine) {
                  a = NearCamera.dark(Vfx.camera(), x, y, z, a);
               }

               t.a[base + j] = a;
            }
         }

         return true;
      }
   }

   private void smoke(TrailFrame f, WraithStyle.Tendrils t) {
      int n = t.points;
      double scroll = f.now * 0.45D;
      RenderState.translucent(false);
      int tex = Smoke.wisps();

      for(int c = 0; c < t.count; ++c) {
         int base = c * n;

         for(int j = 0; j < n; ++j) {
            Vfx.X[j] = (double)t.x[base + j];
            Vfx.Y[j] = (double)t.y[base + j];
            Vfx.Z[j] = (double)t.z[base + j];
            Vfx.W[j] = t.w[base + j];
            Vfx.C[j] = ColorUtil.mulAlpha(t.c[j], t.a[base + j]);
            U[j] = (float)((double)(t.u[j] * t.length) / 1.6D + scroll + (double)c * 0.31D);
         }

         TrailStrip.sides(0, n, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
         TrailStrip.drawAlong(tex, 0, n, 1.0F, 0.0D, 1.0F, U);
      }

   }

   private void light(PlayerTrail p, TrailFrame f, WraithStyle.Tendrils t) {
      int n = t.points;
      float glowAmount = this.glow.getFloat() / 100.0F;
      float em = p.reduced?0.0F:this.embers.getFloat() / 100.0F;
      float edge = this.edgeAmount();
      double time = f.now;
      double colorTime = f.colorTime;
      RenderState.additive();
      if(edge > 0.01F) {
         int tex = Smoke.wisps();
         double scroll = f.now * 0.45D;

         for(int c = 0; c < t.count; ++c) {
            int base = c * n;

            for(int j = 0; j < n; ++j) {
               Vfx.X[j] = (double)t.x[base + j];
               Vfx.Y[j] = (double)t.y[base + j];
               Vfx.Z[j] = (double)t.z[base + j];
               Vfx.W[j] = t.w[base + j] * 1.25F;
               Vfx.C[j] = ColorUtil.mulAlpha(this.slotColor(this.edgeColor, t.u[j], colorTime), t.a[base + j] * 0.16F * edge * (0.6F + 0.4F * glowAmount));
               U[j] = (float)((double)(t.u[j] * t.length) / 1.6D + scroll + (double)c * 0.31D + 0.5D);
            }

            TrailStrip.sides(0, n, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
            TrailStrip.drawAlong(tex, 0, n, 1.0F, 0.0D, 1.0F, U);
         }
      }

      if(em > 0.01F) {
         for(int c = 0; c < t.count; ++c) {
            int base = c * n;
            int m = 0;
            double side = 0.0D;

            for(int j = 0; j < n; ++j) {
               float u = t.u[j];
               float lit = Easing.smoothstep(0.1F, 0.2F, u) * (1.0F - Easing.smoothstep(0.45F, 0.6F, u));
               float a = lit * Easing.smoothstep(0.62F, 0.7F, (float)noise((double)u * 26.0D + (double)c * 3.3D - time * 0.15D)) * (0.6F + 0.4F * (float)noise(time * 9.0D + (double)j * 0.7D + (double)c * 5.0D)) * t.a[base + j] * em * (0.45F + 0.45F * glowAmount);
               if(a <= 0.01F) {
                  if(m >= 2) {
                     crackLine(f, m);
                  }

                  m = 0;
               } else {
                  if(m == 0) {
                     side = (hash(j * 3 + c * 17) - 0.5D) * 0.5D;
                  }

                  int ja = Math.max(0, j - 1);
                  int jb = Math.min(n - 1, j + 1);
                  double ty = (double)(t.y[base + jb] - t.y[base + ja]);
                  double vy;
                  double tz;
                  double vz;
                  double sx = ty * (vz = f.cam.posZ - (double)t.z[base + j]) - (tz = (double)(t.z[base + jb] - t.z[base + ja])) * (vy = f.cam.posY - (double)t.y[base + j]);
                  double sz;
                  double tx;
                  double vx;
                  double sy;
                  double sl = Math.sqrt(sx * sx + (sy = tz * (vx = f.cam.posX - (double)t.x[base + j]) - (tx = (double)(t.x[base + jb] - t.x[base + ja])) * vz) * sy + (sz = tx * vy - ty * vx) * sz);
                  double jag = sl < 1.0E-9D?0.0D:(side + (hash(j * 7 + c * 131) - 0.5D) * 0.1D) * (double)t.w[base + j] / sl;
                  Vfx.X[m] = (double)t.x[base + j] + sx * jag;
                  Vfx.Y[m] = (double)t.y[base + j] + sy * jag;
                  Vfx.Z[m] = (double)t.z[base + j] + sz * jag;
                  Vfx.W[m] = Math.max(0.01F, t.w[base + j] * 0.05F);
                  Vfx.C[m] = ColorUtil.mulAlpha(this.slotColor(this.embersColor, u, colorTime), Math.min(1.0F, a));
                  ++m;
               }
            }

            if(m >= 2) {
               crackLine(f, m);
            }
         }
      }

   }

   private static void crackLine(TrailFrame f, int m) {
      TrailStrip.sides(0, m, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
      TrailStrip.draw(Textures.LINE_GLOW, 0, m, 3.5F, 0.0D, 0.4F);
      TrailStrip.draw(Textures.LINE_CORE, 0, m, 1.0F, 0.0D, 0.85F);
   }

   private void emit(PlayerTrail p, TrailFrame f) {
      int due = this.piecesDue(p, 1.6D * (double)this.embers.getFloat() / 100.0D, 4);
      if(due != 0) {
         TrailShape s = p.shape;
         int k = s.runs - 1;
         int to = s.runEnd[k];
         int from = s.runStart[k];
         if(to - from >= 2) {
            FxParticles pool = f.pieces(this.type);
            float life = 0.8F + this.lengthNow(p.speed) * 0.4F;

            for(int i = 0; i < due && !this.module.piecesFull(f); ++i) {
               this.rng.seed((int)(f.now * 4507.0D) + i * 173 + p.entityId * 11);
               double want = (double)s.length * (0.1D + (double)this.rng.next() * 0.4D);

               int j;
               for(j = to - 1; j > from && (double)s.along[j] < want; --j) {
                  ;
               }

               double x = s.x[j] + (double)this.rng.signed() * 0.46D * 0.6D;
               double z;
               double y;
               if(f.nearFade(x, y = s.y[j] + (double)this.rng.signed() * 0.12D, z = s.z[j] + (double)this.rng.signed() * 0.46D * 0.6D) >= 0.4F) {
                  pool.spawn(7, x, y, z, this.rng.signed() * 0.2F, 0.4F + this.rng.next() * 0.4F, this.rng.signed() * 0.2F, life * (0.7F + this.rng.next() * 0.6F), 0.05F + this.rng.next() * 0.035F).gravity(-0.15F).drag(0.6F).alpha(this.opacity.getFloat() / 100.0F * p.alpha).flags(2).color(this.tint != 0?this.tint:this.embersColor.getColor(this.rng.next())).wind(0.6F);
               }
            }
         }
      }

   }

   public void renderPiecesPass(TrailFrame f, FxPass pass) {
      if(pass == FxPass.GLOW) {
         FxParticles pool = f.piecesIfAny(this.type);
         if(pool != null) {
            pool.render(f.viewX, f.viewY, f.viewZ);
         }
      }

   }

   private static double noise(double x) {
      double fl = Math.floor(x);
      int i = (int)fl;
      double t = x - fl;
      t = t * t * (3.0D - 2.0D * t);
      return hash(i) + (hash(i + 1) - hash(i)) * t;
   }

   private static double hash(int i) {
      int h = i * 668265261 ^ 374761393;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public String stateForTest() {
      return String.format(Locale.ROOT, "%d tendril(s), width %.0f%%, curl %.0f%%, embers %.0f%%, edge light %s", new Object[]{Integer.valueOf(this.tendrils.getInt()), Float.valueOf(this.width.getFloat()), Float.valueOf(this.curl.getFloat()), Float.valueOf(this.embers.getFloat()), ((Smoke.EdgeLight)this.edgeLight.get()).displayName()});
   }

   private static final class Tendrils {
      double stamp;
      double seen;
      TrailFrame scene;
      int count;
      int points;
      float length;
      float[] x;
      float[] y;
      float[] z;
      float[] w;
      float[] a;
      float[] u;
      int[] c;

      private Tendrils() {
         this.stamp = -1.0D;
         this.x = new float[0];
         this.y = new float[0];
         this.z = new float[0];
         this.w = new float[0];
         this.a = new float[0];
         this.u = new float[0];
         this.c = new int[0];
      }

      void ensure(int n) {
         int need = n * 5;
         if(this.x.length < need) {
            this.x = new float[need];
            this.y = new float[need];
            this.z = new float[need];
            this.w = new float[need];
            this.a = new float[need];
         }

         if(this.u.length < n) {
            this.u = new float[n];
            this.c = new int[n];
         }

      }
   }
}
