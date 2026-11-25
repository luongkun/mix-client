package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.fx.trail.TrailStrip;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CometStyle extends TrailStyleBase {
   public final SliderSetting headSize = (SliderSetting)this.setting((new SliderSetting("comet.headSize", "Head Size", "How big the orb is.", 0.1D, 1.0D, 0.4D, 0.01D)).unit(" blocks"));
   public final SliderSetting flare = (SliderSetting)this.setting((new SliderSetting("comet.flare", "Flare", "The four-point flare around the orb.", 0.0D, 100.0D, 60.0D, 1.0D)).unit("%"));
   public final SliderSetting sparkles = (SliderSetting)this.setting((new SliderSetting("comet.sparkles", "Sparkle Amount", "How many sparkles the tail sheds.", 0.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
   public final SliderSetting bob = (SliderSetting)this.setting((new SliderSetting("comet.bob", "Bob", "How much the orb floats up and down.", 0.0D, 100.0D, 50.0D, 1.0D)).unit("%"));
   private final Rng rng = new Rng();

   public CometStyle(TrailsModule module) {
      super(module, TrailStyle.COMET, spec().color("Trail", -15253).size(0.04D, 0.8D, 0.3D, 0.01D).taper(100.0D).anchor(TrailAnchor.BACK, false).animated());
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.headSize);
      out.add(this.flare);
      out.add(this.sparkles);
      out.add(this.bob);
   }

   public void render(PlayerTrail p, TrailFrame f) {
      TrailShape s = p.shape;
      float anim = this.animSpeed.getFloat() / 100.0F;
      float bobY = (float)Math.sin(f.now * (double)anim * 3.141592653589793D + (double)p.entityId * 1.7D) * 0.07F * this.bob.getFloat() / 100.0F;
      float glowAmount = this.glow.getFloat() / 100.0F;

      for(int k = 0; k < s.runs; ++k) {
         int n = this.fill(p, f, k, this.size.getFloat(), -1.0F, 0.0F);
         if(n >= 2) {
            if(k == s.runs - 1 && p.attached) {
               int from = s.runStart[k];

               for(int j = 0; j < n; ++j) {
                  Vfx.Y[j] += (double)(bobY * (1.0F - smooth(0.0F, 0.9F, s.along[from + j])));
               }
            }

            TrailStrip.sides(0, n, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
            RenderState.translucent(false);
            TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.3F, 0.0D, 0.55F);
            RenderState.additive();
            TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.5F + 2.8F * glowAmount, 0.0D, 0.55F * glowAmount);
            TrailStrip.draw(Textures.LINE_GLOW, 0, n, 0.95F, 0.0D, 0.6F);

            for(int j = 0; j < n; ++j) {
               int c = Vfx.C[j];
               Vfx.C[j] = c & -16777216 | ColorUtil.core(c) & 16777215;
            }

            TrailStrip.draw(Textures.LINE_CORE, 0, n, 0.32F, 0.0D, 0.9F);
         }
      }

      if(!p.ghost && p.style.head > 0.004F) {
         this.orb(p, f, bobY, anim);
      }

      if(p.attached && p.alpha > 0.05F) {
         this.shed(p, f);
      }

   }

   private void orb(PlayerTrail p, TrailFrame f, float bobY, float anim) {
      double wx = p.headX;
      double wy = p.headY + (double)bobY;
      double wz = p.headZ;
      float a = this.opacity.getFloat() / 100.0F * p.alpha * p.style.head * f.nearFade(wx, wy, wz) * f.fog(wx, wy, wz);
      if(a > 0.004F) {
         float boost = Math.max(0.0F, this.speedBoost(p.speed));
         float r = this.headSize.getFloat() * (float)Math.sqrt((double)p.widthScale);
         int c = this.colorAt(0.0F, 0.0F, p.speed, f.colorTime, 0.0F);
         double x = wx - f.viewX;
         double y = wy - f.viewY;
         double z = wz - f.viewZ;
         float g = this.glow.getFloat() / 100.0F;
         RenderState.translucent(false);
         Vfx.sprite(Textures.GLOW, x, y, z, (double)(r * 1.6F), 0.0F, ColorUtil.mulAlpha(c, a * 0.55F));
         RenderState.additive();
         Vfx.sprite(Textures.GLOW, x, y, z, (double)(r * (2.4F + 2.4F * g)), 0.0F, ColorUtil.mulAlpha(c, a * (0.4F + 0.45F * g)));
         Vfx.sprite(Textures.SPARK, x, y, z, (double)(r * 1.4F), 0.0F, ColorUtil.mulAlpha(ColorUtil.core(c), a));
         float fl = this.flare.getFloat() / 100.0F;
         if(fl > 0.0F) {
            float rot = (float)(f.now * 0.35D * (double)anim);
            Vfx.sprite(Textures.FLARE, x, y, z, (double)(r * (2.0F + 3.2F * fl) * (1.0F + 0.5F * boost)), rot, ColorUtil.mulAlpha(ColorUtil.core(c), a * Math.min(1.0F, 0.55F * fl + 0.3F * boost)));
         }
      }

   }

   private void shed(PlayerTrail p, TrailFrame f) {
      TrailShape s = p.shape;
      float perBlock = 3.0F * this.sparkles.getFloat() / 100.0F;
      if(perBlock > 0.0F && s.runs != 0 && p.path != null) {
         PlayerTrail.StyleState st = p.style;
         double traveled = p.path.traveled() + p.path.sinceLast(p.headX, p.headY, p.headZ);
         double step = 1.0D / (double)perBlock;
         if(st.next[p.side] < traveled - 2.0D || st.next[p.side] > traveled + step) {
            st.next[p.side] = traveled + (double)this.rng.next() * step;
         }

         int k = s.runs - 1;
         int from = s.runStart[k];
         int to = s.runEnd[k];
         float len = s.along[from];
         int spawned = 0;

         while(st.next[p.side] <= traveled && spawned < 8) {
            int n = p.side;
            st.next[n] += step * (0.6D + 0.8D * (double)this.rng.next());
            ++spawned;
            if(len >= 0.4F && !this.module.piecesFull(f)) {
               float want = len * (0.12F + 0.7F * this.rng.next());

               int i;
               for(i = from; i < to - 1 && s.along[i] > want; ++i) {
                  ;
               }

               double x = s.x[i];
               double y = s.y[i];
               double z = s.z[i];
               if(f.nearFade(x, y, z) >= 0.5F) {
                  float u = Math.min(1.0F, s.along[i] / Math.max(0.001F, s.length));
                  float life = (0.6F + 0.6F * this.rng.next()) * Math.max(0.6F, this.length.getFloat());
                  float sz = (0.1F + 0.08F * this.rng.next()) * this.headSize.getFloat() / 0.4F;
                  FxParticles pool = f.pieces(this.type);
                  pool.spawn(5, x, y, z, (this.rng.next() - 0.5F) * 0.5F, (this.rng.next() - 0.35F) * 0.4F, (this.rng.next() - 0.5F) * 0.5F, life, sz).drag(1.5F).alpha(this.opacity.getFloat() / 100.0F * p.alpha).flags(2).sway(0.0F, this.rng.next() * 10.0F).spin(this.rng.next() * 6.28F, (this.rng.next() - 0.5F) * 3.0F).fade(0.1F, 0.35F).end(sz * 0.4F);
                  this.colorPiece(pool, u, Math.min(1.0F, s.age[i] / Math.max(0.05F, p.maxAge)), s.speed[i]);
               }
            }
         }
      }

   }
}
