package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.look.LightBoost;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;

public final class TombstoneStyle extends KillStyleBase {
   private static final float RISE = 1.1F;
   private static final float SINK = 1.0F;
   private final ModeSetting<TombstoneStyle.Stone> stone = (ModeSetting)this.setting(new ModeSetting("tomb.stone", "Stone", "The gravestone\'s shape.", TombstoneStyle.Stone.class, TombstoneStyle.Stone.ROUNDED));
   private final BooleanSetting showName = (BooleanSetting)this.setting(new BooleanSetting("tomb.name", "Show Name", "The victim\'s name engraved on the front.", true));
   private final SliderSetting stay = (SliderSetting)this.setting((new SliderSetting("tomb.stay", "Stay Time", "How long the stone stays before it sinks back.", 1.0D, 10.0D, 4.0D, 0.5D)).unit(" s"));
   private double ox;
   private double oy;
   private double oz;
   private double ground;
   private TombstoneStyle.State st;
   private float alpha;
   private FxFrame frame;

   public TombstoneStyle(KillEffectsModule module) {
      super(module, KillStyle.TOMBSTONE, new ColorSetting("tomb.color", "Glow", "Color of the soft glow around the stone.", -4205569));
   }

   public float seconds(KillFx fx) {
      return 1.1F + this.stayTime(fx) + 1.0F;
   }

   private float stayTime(KillFx fx) {
      return fx.preview?Math.min(1.5F, this.stay.getFloat()):this.stay.getFloat();
   }

   public void start(KillFx fx) {
      TombstoneStyle.State s = new TombstoneStyle.State();
      fx.data = s;
      double fx0 = -fx.ctx.dirX;
      double fz0 = -fx.ctx.dirZ;
      double l = Math.sqrt(fx0 * fx0 + fz0 * fz0);
      if(l < 1.0E-4D) {
         fx0 = 0.0D;
         fz0 = 1.0D;
         l = 1.0D;
      }

      s.fx = fx0 / l;
      s.rx = s.fz = fz0 / l;
      s.rz = -s.fx;
      if(!fx.preview && Minecraft.getMinecraft().theWorld != null) {
         BlockPos pos = new BlockPos(fx.ctx.feetX, fx.ctx.groundY + 0.5D, fx.ctx.feetZ);
         s.brightness = Math.max(0.25F, LightBoost.lift(Minecraft.getMinecraft().theWorld.getLightBrightness(pos)));
      }

   }

   private float out(KillFx fx) {
      float t = fx.t;
      float up = Easing.outBack(Easing.clamp01((t - 0.2F) / 0.90000004F), 1.2F);
      float down = Easing.inCubic(Easing.clamp01((t - 1.1F - this.stayTime(fx)) / 1.0F));
      return Math.max(0.0F, Math.min(1.05F, up)) * (1.0F - down);
   }

   private double stoneHeight() {
      switch((TombstoneStyle.Stone)this.stone.get()) {
      case CROSS:
         return 1.55D;
      case OBELISK:
         return 1.9D;
      default:
         return 1.05D;
      }
   }

   public void update(KillFx fx, float dt) {
      TombstoneStyle.State s = (TombstoneStyle.State)fx.data;
      float t = fx.t;
      boolean moving = t > 0.15F && t < 1.1F || t > 1.1F + this.stayTime(fx) && t < 1.1F + this.stayTime(fx) + 0.9F;
      if(moving && !fx.lod) {
         s.puffed += dt * 26.0F;

         while(s.puffed >= 1.0F) {
            --s.puffed;
            double a = (double)fx.rng.next() * 6.2832D;
            double r = 0.35D + (double)fx.rng.next() * 0.3D;
            fx.particles.spawn(8, fx.ctx.feetX + Math.cos(a) * r, fx.ctx.groundY + 0.05D, fx.ctx.feetZ + Math.sin(a) * r, (float)Math.cos(a) * 0.8F, 0.35F + fx.rng.next() * 0.3F, (float)Math.sin(a) * 0.8F, 0.9F + fx.rng.next() * 0.5F, (0.25F + fx.rng.next() * 0.2F) * fx.size).end(0.6F * fx.size).drag(1.6F).color(-7569556).alpha(0.55F * fx.opacity);
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      TombstoneStyle.State s = (TombstoneStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float sink = Easing.inCubic(Easing.clamp01(t / 0.95F));
      if(sink < 1.0F) {
         double down = (double)sink * 2.1D;

         for(int i = 0; i < 6; ++i) {
            BodyProxy.Part p = fx.parts[i];
            p.copy(fx.rest[i]);
            p.y -= down;
            p.clip = true;
            p.cnx = 0.0D;
            p.cny = 1.0D;
            p.cnz = 0.0D;
            p.cd = fx.ctx.groundY + 0.002D;
            p.capColor = 0;
         }

         fx.drawBody(f);
      }

      float o;
      if((o = this.out(fx)) > 0.002F) {
         double lift = ((double)o - 1.0D) * (this.stoneHeight() + 0.2D);
         this.drawStone(fx, f, s, lift, a);
         RenderState.additive();
         float glow = a * Math.max(0.3F, fx.glow) * Easing.smoothstep(0.4F, 1.2F, t) * o;
         Vfx.sprite(Textures.GLOW, f.rx(fx.ctx.feetX), f.ry(fx.ctx.groundY + lift + this.stoneHeight() * 0.55D), f.rz(fx.ctx.feetZ), this.stoneHeight() * 1.6D * (double)fx.size, 0.0F, alpha(this.color.getColor(0.5F), 0.14F * glow));
         if(this.showName.get()) {
            this.engrave(fx, f, s, lift, a);
         }
      }

   }

   private void drawStone(KillFx fx, FxFrame f, TombstoneStyle.State s, double lift, float a) {
      this.st = s;
      this.frame = f;
      this.alpha = a;
      this.ox = fx.ctx.feetX;
      this.oy = fx.ctx.groundY + lift;
      this.oz = fx.ctx.feetZ;
      this.ground = fx.ctx.groundY;
      double k = (double)fx.size;
      RenderState.translucent(true);
      switch((TombstoneStyle.Stone)this.stone.get()) {
      case CROSS:
         this.box(-0.3D * k, 0.0D, -0.2D * k, 0.3D * k, 0.18D * k, 0.2D * k);
         this.box(-0.11D * k, 0.18D * k, -0.09D * k, 0.11D * k, 1.55D * k, 0.09D * k);
         this.box(-0.42D * k, 0.95D * k, -0.09D * k, 0.42D * k, 1.17D * k, 0.09D * k);
         break;
      case OBELISK:
         this.box(-0.36D * k, 0.0D, -0.36D * k, 0.36D * k, 0.16D * k, 0.36D * k);
         this.taper(0.16D * k, 1.55D * k, 0.23D * k, 0.14D * k);
         this.pyramid(1.55D * k, 1.9D * k, 0.14D * k);
         break;
      default:
         this.box(-0.5D * k, 0.0D, -0.22D * k, 0.5D * k, 0.14D * k, 0.22D * k);
         this.rounded(0.14D * k, 0.62D * k, 0.4D * k, 0.1D * k);
      }

      Vfx.flush();
   }

   private double wx(double x, double z) {
      return this.ox + this.st.rx * x + this.st.fx * z;
   }

   private double wz(double x, double z) {
      return this.oz + this.st.rz * x + this.st.fz * z;
   }

   private void face(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double nx, double ny, double nz) {
      double l = 0.35D * nx + 0.8D * ny + 0.45D * nz;
      float shade = (float)(0.55D + 0.45D * Math.max(0.0D, l)) * this.st.brightness;
      int g = Math.min(255, Math.round(255.0F * shade));
      int c = Math.max(0, Math.min(255, Math.round(this.alpha * 255.0F))) << 24 | g << 16 | g << 8 | g;
      double ax = Math.abs(nx);
      double ay = Math.abs(ny);
      Vfx.quadUV(Textures.STONE, this.frame.rx(this.wx(x0, z0)), this.frame.ry(Math.max(this.ground, this.oy + y0)), this.frame.rz(this.wz(x0, z0)), uv(ax, ay, x0, y0, z0, true), uv(ax, ay, x0, y0, z0, false), c, this.frame.rx(this.wx(x1, z1)), this.frame.ry(Math.max(this.ground, this.oy + y1)), this.frame.rz(this.wz(x1, z1)), uv(ax, ay, x1, y1, z1, true), uv(ax, ay, x1, y1, z1, false), c, this.frame.rx(this.wx(x2, z2)), this.frame.ry(Math.max(this.ground, this.oy + y2)), this.frame.rz(this.wz(x2, z2)), uv(ax, ay, x2, y2, z2, true), uv(ax, ay, x2, y2, z2, false), c, this.frame.rx(this.wx(x3, z3)), this.frame.ry(Math.max(this.ground, this.oy + y3)), this.frame.rz(this.wz(x3, z3)), uv(ax, ay, x3, y3, z3, true), uv(ax, ay, x3, y3, z3, false), c);
   }

   private static double uv(double ax, double ay, double x, double y, double z, boolean u) {
      return ay > 0.7D?(u?x:z) * 0.9D:(ax > 0.7D?(u?z:-y) * 0.9D:(u?x:-y) * 0.9D);
   }

   private void box(double x0, double y0, double z0, double x1, double y1, double z1) {
      this.face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.0D, 0.0D, 1.0D);
      this.face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.0D, 0.0D, -1.0D);
      this.face(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0.0D, 1.0D, 0.0D);
      this.face(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0D, 0.0D, 0.0D);
      this.face(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0D, 0.0D, 0.0D);
   }

   private void taper(double y0, double y1, double b, double t) {
      double slope = (b - t) / (y1 - y0);
      this.face(-b, y0, b, b, y0, b, t, y1, t, -t, y1, t, 0.0D, slope, 1.0D);
      this.face(b, y0, -b, -b, y0, -b, -t, y1, -t, t, y1, -t, 0.0D, slope, -1.0D);
      this.face(b, y0, b, b, y0, -b, t, y1, -t, t, y1, t, 1.0D, slope, 0.0D);
      this.face(-b, y0, -b, -b, y0, b, -t, y1, t, -t, y1, -t, -1.0D, slope, 0.0D);
   }

   private void pyramid(double y0, double y1, double h) {
      this.face(-h, y0, h, h, y0, h, 0.0D, y1, 0.0D, 0.0D, y1, 0.0D, 0.0D, 0.6D, 1.0D);
      this.face(h, y0, -h, -h, y0, -h, 0.0D, y1, 0.0D, 0.0D, y1, 0.0D, 0.0D, 0.6D, -1.0D);
      this.face(h, y0, h, h, y0, -h, 0.0D, y1, 0.0D, 0.0D, y1, 0.0D, 1.0D, 0.6D, 0.0D);
      this.face(-h, y0, -h, -h, y0, h, 0.0D, y1, 0.0D, 0.0D, y1, 0.0D, -1.0D, 0.6D, 0.0D);
   }

   private void rounded(double y0, double y1, double r, double d) {
      int n = 12;
      this.face(-r, y0, d, r, y0, d, r, y1, d, -r, y1, d, 0.0D, 0.0D, 1.0D);
      this.face(r, y0, -d, -r, y0, -d, -r, y1, -d, r, y1, -d, 0.0D, 0.0D, -1.0D);
      this.face(r, y0, d, r, y0, -d, r, y1, -d, r, y1, d, 1.0D, 0.0D, 0.0D);
      this.face(-r, y0, -d, -r, y0, d, -r, y1, d, -r, y1, -d, -1.0D, 0.0D, 0.0D);

      for(int i = 0; i < n; ++i) {
         double a0 = 3.141592653589793D * (double)i / (double)n;
         double a1 = 3.141592653589793D * (double)(i + 1) / (double)n;
         double x0 = Math.cos(a0) * r;
         double h0 = Math.sin(a0) * r;
         double x1 = Math.cos(a1) * r;
         double h1 = Math.sin(a1) * r;
         this.face(0.0D, y1, d, x0, y1 + h0, d, x1, y1 + h1, d, x1, y1 + h1, d, 0.0D, 0.0D, 1.0D);
         this.face(0.0D, y1, -d, x1, y1 + h1, -d, x0, y1 + h0, -d, x0, y1 + h0, -d, 0.0D, 0.0D, -1.0D);
         double nx = Math.cos((a0 + a1) / 2.0D);
         double ny = Math.sin((a0 + a1) / 2.0D);
         this.face(x0, y1 + h0, -d, x0, y1 + h0, d, x1, y1 + h1, d, x1, y1 + h1, -d, nx, ny, 0.0D);
      }

   }

   private void engrave(KillFx fx, FxFrame f, TombstoneStyle.State s, double lift, float a) {
      if(a > 0.05F) {
         Minecraft mc = Minecraft.getMinecraft();
         FontRenderer font = mc.fontRendererObj;
         if(font != null) {
            double k = (double)fx.size;
            double room;
            double textY;
            double faceZ;
            switch((TombstoneStyle.Stone)this.stone.get()) {
            case CROSS:
               faceZ = 0.09D * k;
               textY = 1.06D * k;
               room = 0.78D * k;
               break;
            case OBELISK:
               faceZ = 0.2D * k;
               textY = 0.7D * k;
               room = 0.36D * k;
               break;
            default:
               faceZ = 0.1D * k;
               textY = 0.58D * k;
               room = 0.7D * k;
            }

            String name = fx.ctx.victimName != null && !fx.ctx.victimName.isEmpty()?fx.ctx.victimName:"?";
            int w = font.getStringWidth(name);
            double scale = Math.min(0.0125D * k, room / (double)Math.max(1, w));
            double yaw = Math.toDegrees(Math.atan2(s.fx, -s.fz));
            float bright = s.brightness;
            int dark = shade(2894376, bright, a);
            int light = shade(12170412, bright, a);
            Vfx.flush();
            RenderState.suspend();
            GlStateManager.pushMatrix();

            try {
               GlStateManager.enableDepth();
               GlStateManager.depthMask(true);
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
               GlStateManager.disableLighting();
               double y = f.ry(fx.ctx.groundY + lift + textY);
               if(fx.ctx.groundY + lift + textY - 0.06D > fx.ctx.groundY) {
                  this.line(font, f, s, faceZ + 0.004D, y, yaw, scale, name, dark, light);
                  if(this.stone.get() != TombstoneStyle.Stone.CROSS) {
                     double ry = f.ry(fx.ctx.groundY + lift + textY + (this.stone.get() == TombstoneStyle.Stone.OBELISK?0.28D:0.2D) * k);
                     if(fx.ctx.groundY + lift + textY + 0.14D > fx.ctx.groundY) {
                        this.line(font, f, s, faceZ + 0.004D, ry, yaw, Math.min(scale * 1.1D, 0.012D * k), "R.I.P.", dark, light);
                     }
                  }
               }
            } finally {
               GlStateManager.popMatrix();
               GlStateManager.disableBlend();
               RenderState.resume();
            }
         }
      }

   }

   private void line(FontRenderer font, FxFrame f, TombstoneStyle.State s, double faceZ, double y, double yaw, double scale, String text, int dark, int light) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(f.rx(this.ox + s.fx * faceZ), y, f.rz(this.oz + s.fz * faceZ));
      GlStateManager.rotate((float)(-yaw), 0.0F, 1.0F, 0.0F);
      GlStateManager.scale((float)(-scale), (float)(-scale), (float)scale);
      int w = font.getStringWidth(text);
      font.drawString(text, (float)(-w / 2) + 0.6F, -3.4F, light, false);
      GlStateManager.translate(0.0F, 0.0F, -0.002F / (float)scale);
      font.drawString(text, (float)(-w / 2), -4.0F, dark, false);
      GlStateManager.popMatrix();
   }

   private static int shade(int rgb, float bright, float a) {
      int r = Math.round((float)(rgb >> 16 & 255) * bright);
      int g = Math.round((float)(rgb >> 8 & 255) * bright);
      int b = Math.round((float)(rgb & 255) * bright);
      int al = Math.max(8, Math.min(255, Math.round(a * 255.0F)));
      return al << 24 | r << 16 | g << 8 | b;
   }

   private static final class State {
      double rx;
      double rz;
      double fx;
      double fz;
      float brightness;
      float puffed;

      private State() {
         this.brightness = 1.0F;
      }
   }

   static enum Stone implements Displayable {
      ROUNDED("Rounded", "A classic headstone"),
      CROSS("Cross", "A stone cross"),
      OBELISK("Obelisk", "A tall, tapering pillar");

      private final String displayName;
      private final String description;

      private Stone(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
