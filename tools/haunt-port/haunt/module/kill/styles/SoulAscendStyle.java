package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PoseSnapshot;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Silhouette;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SoulAscendStyle extends KillStyleBase {
   private final SliderSetting rise = (SliderSetting)this.setting((new SliderSetting("soul.rise", "Rise Height", "How high the soul rises before it dissolves.", 1.5D, 5.0D, 3.0D, 0.25D)).unit(" blocks"));
   private final SliderSetting riseSpeed = (SliderSetting)this.setting((new SliderSetting("soul.speed", "Rise Speed", "How fast it rises.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting halo = (BooleanSetting)this.setting(new BooleanSetting("soul.halo", "Halo", "A thin ring floating over the soul\'s head.", true));
   private final Silhouette.Look ghost = new Silhouette.Look();
   private final double[] point = new double[3];

   public SoulAscendStyle(KillEffectsModule module) {
      super(module, KillStyle.SOUL_ASCEND, new ColorSetting("soul.color", "Color", "Color of the soul, its halo and sparkles.", -6297601));
   }

   private float riseSeconds() {
      return 2.2F / this.riseSpeed.getFloat();
   }

   public float seconds(KillFx fx) {
      return 0.3F + this.riseSeconds() + 0.9F;
   }

   public void start(KillFx fx) {
      SoulAscendStyle.State s = new SoulAscendStyle.State();
      s.pose.copy(fx.ctx.body.pose);
      s.pose.deathAngle = 0.0F;
      s.pose.limbSwingAmount = 0.0F;
      s.pose.swingProgress = 0.0F;
      s.pose.sneaking = false;
      s.pose.netHeadYaw = 0.0F;
      s.pose.headPitch = -8.0F;
      s.pose.heldItemRight = 0;
      fx.data = s;
   }

   public void render(KillFx fx, FxFrame f) {
      SoulAscendStyle.State s = (SoulAscendStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float up = Easing.inOutSine(Easing.clamp01((t - 0.3F) / this.riseSeconds()));
      double height = Math.min(fx.preview?1.6D:1.0E9D, (double)(this.rise.getFloat() * fx.size) * (fx.crit?1.25D:1.0D));
      float leave = Easing.clamp01((t - 0.3F - this.riseSeconds() * 0.85F) / 0.9F);
      fx.look.alpha = 1.0F - Easing.smoothstep(0.35F, 0.3F + this.riseSeconds() * 0.8F, t);
      fx.look.glow = this.color.getColor(0.3F);
      fx.look.glowAmount = 0.35F * (1.0F - Easing.smoothstep(0.2F, 1.2F, t)) * Easing.smoothstep(0.0F, 0.25F, t);
      fx.drawBody(f);
      fx.look.alpha = 1.0F;
      fx.look.glowAmount = 0.0F;
      RenderState.additive();
      double sway = Math.sin((double)t * 2.1D) * 0.12D * (double)up;
      double swayZ = Math.sin((double)t * 1.6D + 1.0D) * 0.07D * (double)up;
      double dy = height * (double)up;
      float emerge = Easing.smoothstep(0.05F, 0.45F, t);
      float ga = a * emerge * (1.0F - Easing.smoothstep(0.6F, 1.0F, leave));
      if(ga > 0.003F) {
         this.ghost.reset();
         this.ghost.color = this.color;
         this.ghost.alpha = ga;
         this.ghost.fill = 0.24F;
         this.ghost.rim = 0.75F;
         this.ghost.inflate = 0.35F;
         this.ghost.rimWidth = 0.9F;
         this.ghost.additive = true;
         this.ghost.shiftX = sway;
         this.ghost.shiftY = dy;
         this.ghost.shiftZ = swayZ;
         this.ghost.dissolve = leave > 0.0F?leave * 1.1F:-1.0F;
         Silhouette.draw(s.pose, f.viewX, f.viewY, f.viewZ, this.ghost);
         float rate = leave > 0.0F?120.0F:10.0F;
         s.sparkled += rate * f.dt * (fx.lod?0.4F:1.0F) * fx.intensity;

         while(s.sparkled >= 1.0F) {
            --s.sparkled;
            Silhouette.randomPoint(fx.rng.next(), fx.rng.next(), fx.rng.next(), this.point);
            fx.particles.spawn(5, this.point[0], this.point[1], this.point[2], fx.rng.signed() * 0.3F, 0.4F + fx.rng.next() * 0.6F, fx.rng.signed() * 0.3F, 0.7F + fx.rng.next() * 0.5F, (0.06F + fx.rng.next() * 0.05F) * fx.size).drag(0.8F).color(this.color, fx.rng.next()).alpha(fx.opacity).flags(2);
         }
      }

      if(this.halo.get() && ga > 0.01F) {
         double hx = f.rx(fx.ctx.feetX + sway);
         double hy = f.ry(fx.ctx.feetY + 2.15D + dy);
         double hz = f.rz(fx.ctx.feetZ + swayZ);
         float ha = ga * Easing.smoothstep(0.25F, 0.7F, t);
         double r = 0.3D * (double)fx.size;
         int c = this.color.getColor(0.9F);
         Vfx.flatRing(Textures.LINE_GLOW, hx, hy, hz, r, 0.16D * (double)fx.size, 48, alpha(c, 0.55F * ha));
         Vfx.flatRing(Textures.LINE_CORE, hx, hy, hz, r, 0.035D * (double)fx.size, 48, core(c, 0.95F * ha));
         Vfx.sprite(Textures.GLOW, hx, hy, hz, 0.9D * (double)fx.size, 0.0F, alpha(ColorUtil.core(c), 0.25F * ha * Math.max(0.3F, fx.glow)));
      }

      float lift;
      if((lift = a * Easing.smoothstep(0.1F, 0.4F, t) * (1.0F - Easing.smoothstep(0.6F, 1.6F, t))) > 0.01F) {
         Vfx.sprite(Textures.GLOW, f.rx(fx.ctx.midX()), f.ry(fx.ctx.midY() + 0.3D), f.rz(fx.ctx.midZ()), 2.2D * (double)fx.size, 0.0F, alpha(this.color.getColor(0.4F), 0.3F * lift * Math.max(0.3F, fx.glow)));
      }

   }

   private static final class State {
      final PoseSnapshot pose;
      float sparkled;

      private State() {
         this.pose = new PoseSnapshot();
      }
   }
}
