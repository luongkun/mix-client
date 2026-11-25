package mix.client.mixui.integration.haunt.module.trails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.pass.FxPasses;
import mix.client.mixui.integration.haunt.fx.pass.PassItem;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;

public final class TrailPreview {
   private final TrailsModule module;
   private final TrailPreview.Path path;
   private final float size;
   private final double startAt;
   public final PlayerTrail trail;
   public final TrailFrame frame;
   public TrailPreview.Move move;
   private double clock;
   private double t;
   private float speed;
   private double jumpStart;
   private double nextJump;
   private long updates;
   public double x;
   public double y;
   public double z;
   public float bodyYaw;
   public float limbSwing;
   public float limbAmount;
   public boolean sneaking;
   private final List<TrailPreview.PreviewItem> items;
   private TrailPreview.PreviewItem[] itemPool;

   public TrailPreview(TrailsModule module, TrailPreview.Path path, float size) {
      this(module, path, size, false, 0.0D);
   }

   TrailPreview(TrailsModule module, TrailPreview.Path path, float size, boolean other, double startAt) {
      this.frame = new TrailFrame();
      this.move = TrailPreview.Move.WALK;
      this.jumpStart = -1.0D;
      this.nextJump = 0.6D;
      this.items = new ArrayList();
      this.itemPool = new TrailPreview.PreviewItem[0];
      this.module = module;
      this.path = path;
      this.size = size;
      this.startAt = startAt;
      this.trail = new PlayerTrail(other?-2:-1, !other);
      this.frame.preview = true;
      this.frame.fog = false;
      this.frame.pieceCapacity = 160;
      this.t = startAt;
      this.place();
   }

   public void restart() {
      this.trail.reset();
      this.frame.clearPieces((TrailStyle)null);
      this.clock = 0.0D;
      this.t = this.startAt;
      this.speed = 0.0F;
      this.jumpStart = -1.0D;
      this.nextJump = 0.6D;
      this.place();
   }

   public double clock() {
      return this.clock;
   }

   public float pathSize() {
      return this.size;
   }

   public boolean figureEight() {
      return this.path == TrailPreview.Path.FIGURE_EIGHT;
   }

   public void step(float dt, TrailStyleBase[] styles, int count) {
      if(dt > 0.0F) {
         dt = Math.min(dt, 0.1F);
         this.clock += (double)dt;
         float target = this.move.speed;
         float rate = 14.0F * dt;
         this.speed += Math.max(-rate, Math.min(rate, target - this.speed));
         double ds = (double)(this.speed * dt);
         this.advancePath(ds);
         double jy = 0.0D;
         if(this.move == TrailPreview.Move.JUMP && this.speed > 1.0F && this.jumpStart < 0.0D && this.clock >= this.nextJump) {
            this.jumpStart = this.clock;
         }

         if(this.jumpStart >= 0.0D) {
            double u = (this.clock - this.jumpStart) / 0.6D;
            if(u >= 1.0D) {
               this.jumpStart = -1.0D;
               this.nextJump = this.clock + 0.4D;
            } else {
               jy = 5.0D * u * (1.0D - u);
            }
         }

         double oldX = this.x;
         double oldY = this.y;
         double oldZ = this.z;
         this.place();
         this.y = jy;
         this.sneaking = this.move == TrailPreview.Move.SNEAK;
         float amount = Math.min(1.0F, 4.0F * this.speed / 20.0F);
         this.limbAmount += (amount - this.limbAmount) * Math.min(1.0F, dt * 8.0F);
         this.limbSwing += this.limbAmount * dt * 20.0F;
         PlayerTrail p = this.trail;
         p.pose.x = this.x;
         p.pose.y = this.y;
         p.pose.z = this.z;
         p.pose.bodyYaw = this.bodyYaw;
         p.pose.sneaking = this.sneaking;
         p.pose.riding = false;
         p.pose.other = false;
         p.pose.limbSwing = this.limbSwing;
         p.pose.limbAmount = this.limbAmount;
         p.pose.swing = 0.0F;
         p.pose.pitch = 0.0F;
         p.pose.age = (float)(this.clock * 20.0D);
         p.pose.held = 0;
         double dx = this.x - oldX;
         double dy = this.y - oldY;
         double dz = this.z - oldZ;
         double perTick = Math.sqrt(dx * dx + dy * dy + dz * dz) / (double)dt * 0.05D;
         int state = (this.jumpStart >= 0.0D?0:1) | (this.move == TrailPreview.Move.SPRINT?2:0) | (this.sneaking?4:0);
         this.module.advance(p, true, perTick, false, state, this.clock, dt, styles, count, ++this.updates);
         this.frame.dt = dt;
         this.frame.now = this.clock;

         for(int k = 0; k < count; ++k) {
            styles[k].update(this.frame);
         }
      }

   }

   public void render(TrailStyleBase[] styles, int count, float viewportHeight, double projectionScaleY) {
      TrailFrame f = this.frame;
      RenderState.begin();

      try {
         f.cam.captureModelView();
         Vfx.setCamera(f.cam);
         f.viewZ = 0.0D;
         f.viewY = 0.0D;
         f.viewX = 0.0D;
         f.eyeX = f.cam.posX;
         f.eyeY = f.cam.posY;
         f.eyeZ = f.cam.posZ;
         f.colorTime = ColorSetting.clock();
         f.nearOuter = 0.0F;
         f.pixelsPerBlock = (float)((double)viewportHeight * projectionScaleY * 0.5D);
         int passStyles = 0;

         for(int k = 0; k < count; ++k) {
            if(styles[k].passes() != 0) {
               ++passStyles;
            } else {
               this.module.draw(f, this.trail, styles[k], this.clock, 1.0F);
               styles[k].renderPieces(f);
               RenderState.additive();
            }
         }

         if(passStyles > 0) {
            this.items.clear();

            for(int var12 = 0; var12 < count; ++var12) {
               if(styles[var12].passes() != 0) {
                  this.items.add(this.item(this.items.size(), styles[var12], this.trail));
                  this.items.add(this.item(this.items.size(), styles[var12], (PlayerTrail)null));
               }
            }

            FxPasses.drawNow(this.items);
         }
      } finally {
         RenderState.end();
      }

   }

   private TrailPreview.PreviewItem item(int i, TrailStyleBase style, PlayerTrail of) {
      if(i >= this.itemPool.length) {
         this.itemPool = (TrailPreview.PreviewItem[])Arrays.copyOf(this.itemPool, i + 4);
      }

      if(this.itemPool[i] == null) {
         this.itemPool[i] = new TrailPreview.PreviewItem();
      }

      TrailPreview.PreviewItem it = this.itemPool[i];
      it.style = style;
      it.trail = of;
      return it;
   }

   private void advancePath(double ds) {
      if(this.path == TrailPreview.Path.LOOP) {
         this.t += ds / (double)this.size;
      } else {
         double sub = ds / 4.0D;

         for(int i = 0; i < 4; ++i) {
            double vx = (double)this.size * Math.cos(this.t);
            double vz = (double)this.size * Math.cos(2.0D * this.t);
            this.t += sub / Math.max(0.2D, Math.sqrt(vx * vx + vz * vz));
         }
      }

   }

   private void place() {
      double tz;
      double tx;
      if(this.path == TrailPreview.Path.LOOP) {
         this.x = (double)this.size * Math.sin(this.t);
         this.z = (double)this.size * Math.cos(this.t);
         tx = Math.cos(this.t);
         tz = -Math.sin(this.t);
      } else {
         this.x = (double)this.size * Math.sin(this.t);
         this.z = (double)this.size * Math.sin(this.t) * Math.cos(this.t);
         tx = Math.cos(this.t);
         tz = Math.cos(2.0D * this.t);
      }

      this.bodyYaw = (float)Math.toDegrees(Math.atan2(-tx, tz));
   }

   public static enum Move {
      WALK(4.3F),
      SPRINT(5.6F),
      JUMP(4.3F),
      SNEAK(1.3F),
      STOP(0.0F);

      final float speed;

      private Move(float speed) {
         this.speed = speed;
      }
   }

   public static enum Path {
      LOOP,
      FIGURE_EIGHT;
   }

   private final class PreviewItem implements PassItem {
      TrailStyleBase style;
      PlayerTrail trail;

      private PreviewItem() {
      }

      public int passes() {
         return this.style.passes();
      }

      public void drawPass(FxPass pass) {
         TrailPreview.this.module.drawInPass(TrailPreview.this.frame, this.trail, this.style, TrailPreview.this.clock, pass);
      }

      public double passDistanceSq() {
         return 0.0D;
      }
   }
}
